package vn.edu.kma.ucon.parser;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;



public class UconDslToXmiParser {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Step 7: DSL to XMI Serialization...");

        // 1. Setup EMF and Load ucon.ecore
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        
        ResourceSet resSet = new ResourceSetImpl();
        File ecoreFile = new File("metamodel/ucon.ecore");
        Resource ecoreResource = resSet.getResource(URI.createFileURI(ecoreFile.getAbsolutePath()), true);
        EPackage uconPackage = (EPackage) ecoreResource.getContents().get(0);
        EPackage.Registry.INSTANCE.put(uconPackage.getNsURI(), uconPackage);

        // 2. Parse the DSL file using ANTLR
        File dslFile = new File("dsl/ucon_policy.dsl");
        UconPolicyLexer lexer = new UconPolicyLexer(CharStreams.fromFileName(dslFile.getAbsolutePath()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        UconPolicyParser parser = new UconPolicyParser(tokens);
        ParseTree tree = parser.policyModel();

        // 3. Visit the AST and build EObjects
        UconAstVisitor visitor = new UconAstVisitor(uconPackage);
        EObject rootModel = visitor.visit(tree);

        // 4. Serialize to XMI
        File xmiOutputFile = new File("xmi/ucon_policy.xmi");
        Resource xmiResource = resSet.createResource(URI.createFileURI(xmiOutputFile.getAbsolutePath()));
        xmiResource.getContents().add(rootModel);
        xmiResource.save(Collections.EMPTY_MAP);
        System.out.println("Serialization complete! File saved at: " + xmiOutputFile.getAbsolutePath());

        // 5. Round-trip validation
        Resource checkResource = resSet.getResource(URI.createFileURI(xmiOutputFile.getAbsolutePath()), true);
        checkResource.load(Collections.EMPTY_MAP);
        System.out.println("Round-trip validation success. Loaded elements: " + checkResource.getContents().size());
    }
}

class UconAstVisitor extends UconPolicyBaseVisitor<EObject> {
    private final EPackage uconPkg;
    private final EFactory factory;

    public UconAstVisitor(EPackage uconPkg) {
        this.uconPkg = uconPkg;
        this.factory = uconPkg.getEFactoryInstance();
    }

    private EClass getCls(String name) {
        return (EClass) uconPkg.getEClassifier(name);
    }

    private Object getEnumVal(String enumName, String literal) {
        EEnum eEnum = (EEnum) uconPkg.getEClassifier(enumName);
        return eEnum.getEEnumLiteral(literal).getInstance();
    }

    @Override
    public EObject visitPolicyModel(UconPolicyParser.PolicyModelContext ctx) {
        EObject model = factory.create(getCls("PolicyModel"));
        List<EObject> policies = new ArrayList<>();
        for (UconPolicyParser.PolicyContext pCtx : ctx.policy()) {
            policies.add(visit(pCtx));
        }
        model.eSet(getCls("PolicyModel").getEStructuralFeature("policies"), policies);
        return model;
    }

    @Override
    public EObject visitPolicy(UconPolicyParser.PolicyContext ctx) {
        EObject policy = factory.create(getCls("Policy"));
        policy.eSet(getCls("Policy").getEStructuralFeature("policyId"), ctx.ID().getText());
        policy.eSet(getCls("Policy").getEStructuralFeature("type"), getEnumVal("PolicyType", ctx.policyType().getText()));
        policy.eSet(getCls("Policy").getEStructuralFeature("targetAction"), getEnumVal("ActionType", ctx.actionType().getText()));
        policy.eSet(getCls("Policy").getEStructuralFeature("effect"), getEnumVal("PolicyEffect", ctx.policyEffect().getText()));
        policy.eSet(getCls("Policy").getEStructuralFeature("priority"), Integer.parseInt(ctx.INT().getText()));
        policy.eSet(getCls("Policy").getEStructuralFeature("description"), ctx.STRING(0).getText().replace("\"", ""));
        
        if (ctx.STRING().size() > 1) {
            String denyReasonStr = ctx.STRING(1).getText().replace("\"", "");
            policy.eSet(getCls("Policy").getEStructuralFeature("denyReason"), denyReasonStr);
        }

        EObject condition = visit(ctx.expression());
        policy.eSet(getCls("Policy").getEStructuralFeature("condition"), condition);

        if (ctx.updateStatement() != null && !ctx.updateStatement().isEmpty()) {
            List<EObject> postUpdates = new ArrayList<>();
            for (UconPolicyParser.UpdateStatementContext uCtx : ctx.updateStatement()) {
                postUpdates.add(visit(uCtx));
            }
            policy.eSet(getCls("Policy").getEStructuralFeature("postUpdates"), postUpdates);
        }

        return policy;
    }

    @Override
    public EObject visitOrExpression(UconPolicyParser.OrExpressionContext ctx) {
        if (ctx.andExpression().size() == 1) {
            return visit(ctx.andExpression(0));
        }
        EObject left = visit(ctx.andExpression(0));
        for (int i = 1; i < ctx.andExpression().size(); i++) {
            EObject op = factory.create(getCls("LogicalOperator"));
            op.eSet(getCls("LogicalOperator").getEStructuralFeature("operator"), getEnumVal("LogicalOp", "OR"));
            op.eSet(getCls("LogicalOperator").getEStructuralFeature("left"), left);
            op.eSet(getCls("LogicalOperator").getEStructuralFeature("right"), visit(ctx.andExpression(i)));
            left = op;
        }
        return left;
    }

    @Override
    public EObject visitAndExpression(UconPolicyParser.AndExpressionContext ctx) {
        if (ctx.notExpression().size() == 1) {
            return visit(ctx.notExpression(0));
        }
        EObject left = visit(ctx.notExpression(0));
        for (int i = 1; i < ctx.notExpression().size(); i++) {
            EObject op = factory.create(getCls("LogicalOperator"));
            op.eSet(getCls("LogicalOperator").getEStructuralFeature("operator"), getEnumVal("LogicalOp", "AND"));
            op.eSet(getCls("LogicalOperator").getEStructuralFeature("left"), left);
            op.eSet(getCls("LogicalOperator").getEStructuralFeature("right"), visit(ctx.notExpression(i)));
            left = op;
        }
        return left;
    }

    @Override
    public EObject visitNotExpression(UconPolicyParser.NotExpressionContext ctx) {
        if (ctx.relationalExpression() != null) {
            if (ctx.NOT() != null) {
                EObject op = factory.create(getCls("LogicalOperator"));
                op.eSet(getCls("LogicalOperator").getEStructuralFeature("operator"), getEnumVal("LogicalOp", "NOT"));
                op.eSet(getCls("LogicalOperator").getEStructuralFeature("left"), visit(ctx.relationalExpression()));
                return op;
            } else {
                return visit(ctx.relationalExpression());
            }
        }
        return null; // Should not reach here
    }

    @Override
    public EObject visitRelationalExpression(UconPolicyParser.RelationalExpressionContext ctx) {
        if (ctx.arithmeticExpression().size() == 1) {
            return visit(ctx.arithmeticExpression(0));
        }
        EObject left = visit(ctx.arithmeticExpression(0));
        for (int i = 1; i < ctx.arithmeticExpression().size(); i++) {
            EObject op = factory.create(getCls("RelationalOperator"));
            String relText = ctx.relationalOp(i-1).getText();
            String enumLit = mapRelationalOp(relText);
            op.eSet(getCls("RelationalOperator").getEStructuralFeature("operator"), getEnumVal("RelationalOp", enumLit));
            op.eSet(getCls("RelationalOperator").getEStructuralFeature("left"), left);
            op.eSet(getCls("RelationalOperator").getEStructuralFeature("right"), visit(ctx.arithmeticExpression(i)));
            left = op;
        }
        return left;
    }

    private String mapRelationalOp(String text) {
        switch(text) {
            case "==": return "EQUALS";
            case "!=": return "NOT_EQUALS";
            case ">": return "GREATER_THAN";
            case "<": return "LESS_THAN";
            case ">=": return "GREATER_OR_EQUALS";
            case "<=": return "LESS_OR_EQUALS";
            default: return text; // IN, CONTAINS, SUBSET_OF, OVERLAPS
        }
    }

    @Override
    public EObject visitArithmeticExpression(UconPolicyParser.ArithmeticExpressionContext ctx) {
        if (ctx.primaryExpression().size() == 1) {
            return visit(ctx.primaryExpression(0));
        }
        EObject left = visit(ctx.primaryExpression(0));
        for (int i = 1; i < ctx.primaryExpression().size(); i++) {
            EObject op = factory.create(getCls("ArithmeticOperator"));
            String mathText = ctx.arithmeticOp(i-1).getText();
            String enumLit = mathText.equals("+") ? "ADD" : "SUBTRACT";
            op.eSet(getCls("ArithmeticOperator").getEStructuralFeature("operator"), getEnumVal("ArithmeticOp", enumLit));
            op.eSet(getCls("ArithmeticOperator").getEStructuralFeature("left"), left);
            op.eSet(getCls("ArithmeticOperator").getEStructuralFeature("right"), visit(ctx.primaryExpression(i)));
            left = op;
        }
        return left;
    }

    @Override
    public EObject visitPrimaryExpression(UconPolicyParser.PrimaryExpressionContext ctx) {
        if (ctx.expression() != null) return visit(ctx.expression());
        if (ctx.variableAccess() != null) return visit(ctx.variableAccess());
        if (ctx.constant() != null) return visit(ctx.constant());
        if (ctx.listConstant() != null) return visitListConstant(ctx.listConstant());
        if (ctx.functionCall() != null) return visit(ctx.functionCall());
        return null;
    }

    @Override
    public EObject visitVariableAccess(UconPolicyParser.VariableAccessContext ctx) {
        EObject obj = factory.create(getCls("VariableAccess"));
        obj.eSet(getCls("VariableAccess").getEStructuralFeature("entity"), getEnumVal("EntityScope", ctx.entityScope().getText().toUpperCase()));
        obj.eSet(getCls("VariableAccess").getEStructuralFeature("path"), ctx.qualifiedName().getText());
        return obj;
    }

    @Override
    public EObject visitConstant(UconPolicyParser.ConstantContext ctx) {
        EObject obj = factory.create(getCls("Constant"));
        String type;
        if (ctx.STRING() != null) type = "STRING";
        else if (ctx.BOOLEAN() != null) type = "BOOLEAN";
        else type = "INTEGER";
        
        obj.eSet(getCls("Constant").getEStructuralFeature("type"), getEnumVal("DataType", type));
        obj.eSet(getCls("Constant").getEStructuralFeature("value"), ctx.getText().replace("\"", ""));
        return obj;
    }

    @Override
    public EObject visitListConstant(UconPolicyParser.ListConstantContext ctx) {
        EObject obj = factory.create(getCls("ListConstant"));
        List<String> values = new ArrayList<>();
        for (var strNode : ctx.STRING()) {
            values.add(strNode.getText().replace("\"", ""));
        }
        obj.eSet(getCls("ListConstant").getEStructuralFeature("values"), values);
        // Ngầm định list string chứa Enum string (như Documented Step 5)
        obj.eSet(getCls("ListConstant").getEStructuralFeature("elementType"), getEnumVal("DataType", "ENUM"));
        return obj;
    }

    @Override
    public EObject visitFunctionCall(UconPolicyParser.FunctionCallContext ctx) {
        EObject obj = factory.create(getCls("FunctionCall"));
        obj.eSet(getCls("FunctionCall").getEStructuralFeature("functionName"), ctx.ID().getText());
        if (ctx.expression() != null) {
            List<EObject> args = new ArrayList<>();
            for (UconPolicyParser.ExpressionContext eCtx : ctx.expression()) {
                args.add(visit(eCtx));
            }
            obj.eSet(getCls("FunctionCall").getEStructuralFeature("arguments"), args);
        }
        return obj;
    }

    @Override
    public EObject visitCreateTransactionStatement(UconPolicyParser.CreateTransactionStatementContext ctx) {
        EObject obj = factory.create(getCls("CreateTransactionStatement"));
        obj.eSet(getCls("CreateTransactionStatement").getEStructuralFeature("entityName"), ctx.ID().getText());
        if (ctx.expression() != null) {
            List<EObject> args = new ArrayList<>();
            for (UconPolicyParser.ExpressionContext eCtx : ctx.expression()) {
                args.add(visit(eCtx));
            }
            obj.eSet(getCls("CreateTransactionStatement").getEStructuralFeature("arguments"), args);
        }
        return obj;
    }

    @Override
    public EObject visitAuditLogStatement(UconPolicyParser.AuditLogStatementContext ctx) {
        EObject obj = factory.create(getCls("AuditLogStatement"));
        if (ctx.expression() != null) {
            List<EObject> args = new ArrayList<>();
            for (UconPolicyParser.ExpressionContext eCtx : ctx.expression()) {
                args.add(visit(eCtx));
            }
            obj.eSet(getCls("AuditLogStatement").getEStructuralFeature("arguments"), args);
        }
        return obj;
    }

    @Override
    public EObject visitStandardUpdateStatement(UconPolicyParser.StandardUpdateStatementContext ctx) {
        EObject obj = factory.create(getCls("UpdateStatement"));
        obj.eSet(getCls("UpdateStatement").getEStructuralFeature("target"), visit(ctx.variableAccess()));
        
        String opText = ctx.assignmentOp().getText();
        String enumLit = opText.equals("=") ? "ASSIGN" : opText;
        obj.eSet(getCls("UpdateStatement").getEStructuralFeature("operator"), getEnumVal("AssignmentOp", enumLit));
        
        obj.eSet(getCls("UpdateStatement").getEStructuralFeature("value"), visit(ctx.expression()));
        return obj;
    }

    @Override
    public EObject visitDeleteTransactionStatement(UconPolicyParser.DeleteTransactionStatementContext ctx) {
        EObject obj = factory.create(getCls("DeleteTransactionStatement"));
        obj.eSet(getCls("DeleteTransactionStatement").getEStructuralFeature("entityName"), ctx.ID().getText());
        if (ctx.expression() != null) {
            List<EObject> args = new ArrayList<>();
            for (UconPolicyParser.ExpressionContext eCtx : ctx.expression()) {
                args.add(visit(eCtx));
            }
            obj.eSet(getCls("DeleteTransactionStatement").getEStructuralFeature("arguments"), args);
        }
        return obj;
    }
}
