package vn.edu.kma.ucon.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import vn.edu.kma.ucon.engine.pdp.PolicyModelSemanticValidator;
import vn.edu.kma.ucon.engine.pdp.PolicyValidator;

/**
 * Dedicated metamodel/XMI checks for the UCON policy artifact.
 */
class XmiEcoreConformanceTest extends AbstractUconIntegrationTest {

    @Autowired
    PolicyModelSemanticValidator semanticValidator;
    @Autowired
    PolicyValidator policyValidator;

    @Test
    @DisplayName("XMI loads with Ecore metamodel")
    void xmi_shouldLoadWithEcore() {
        Resource xmiResource = loadXmiResource();

        assertTrue(xmiResource.getErrors().isEmpty());
        assertFalse(xmiResource.getContents().isEmpty());
    }

    @Test
    @DisplayName("XMI contains every policy declared in DSL")
    void xmi_shouldContainAllPoliciesFromDsl() throws Exception {
        EObject root = loadXmiResource().getContents().get(0);
        Set<String> xmiPolicyIds = collectXmiPolicyIds(root);
        Set<String> dslPolicyIds = collectDslPolicyIds();

        assertFalse(dslPolicyIds.isEmpty());
        assertTrue(xmiPolicyIds.containsAll(dslPolicyIds));
    }

    @Test
    @DisplayName("XMI policies and expressions expose required fields")
    void xmi_shouldHaveExplicitRequiredFields() {
        EObject root = loadXmiResource().getContents().get(0);

        for (EObject node : allNodes(root)) {
            String type = node.eClass().getName();
            if ("Policy".equals(type)) {
                assertRequired(node, "predicate");
                assertRequired(node, "phase");
                assertRequired(node, "updateTiming");
                assertRequired(node, "targetAction");
                assertRequired(node, "effect");
                assertRequired(node, "policyStatus");
                assertRequired(node, "uconVariant");
            } else if ("VariableAccess".equals(type)) {
                assertRequired(node, "entity");
                assertRequired(node, "path");
            } else if ("LogicalOperator".equals(type) || "RelationalOperator".equals(type)) {
                assertRequired(node, "operator");
            } else if ("UpdateStatement".equals(type)) {
                EObject target = (EObject) node.eGet(node.eClass().getEStructuralFeature("target"));
                assertNotNull(target);
                assertRequired(target, "entity");
                assertRequired(target, "path");
            }
        }
    }

    @Test
    @DisplayName("XMI passes semantic and policy validation")
    void xmi_shouldPassSemanticValidator() {
        EObject root = loadXmiResource().getContents().get(0);

        semanticValidator.validate(root);
        policyValidator.validate(root);
    }

    private Resource loadXmiResource() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

        ResourceSet resourceSet = new ResourceSetImpl();
        File ecoreFile = resolveExistingFile("../metamodel/ucon.ecore", "metamodel/ucon.ecore");
        Resource ecoreResource = resourceSet.getResource(URI.createFileURI(ecoreFile.getAbsolutePath()), true);
        EPackage ePackage = (EPackage) ecoreResource.getContents().get(0);
        EPackage.Registry.INSTANCE.put(ePackage.getNsURI(), ePackage);

        File xmiFile = resolveExistingFile("../xmi/ucon_policy.xmi", "xmi/ucon_policy.xmi");
        return resourceSet.getResource(URI.createFileURI(xmiFile.getAbsolutePath()), true);
    }

    private Set<String> collectXmiPolicyIds(EObject root) {
        Set<String> policyIds = new HashSet<>();
        for (EObject node : allNodes(root)) {
            if ("Policy".equals(node.eClass().getName())) {
                policyIds.add((String) node.eGet(node.eClass().getEStructuralFeature("policyId")));
            }
        }
        return policyIds;
    }

    private Set<String> collectDslPolicyIds() throws Exception {
        File dslFile = resolveExistingFile("../dsl/ucon_policy.dsl", "dsl/ucon_policy.dsl");
        Pattern pattern = Pattern.compile("^\\s*policy\\s+([A-Za-z0-9_]+)\\s*\\{");
        Set<String> policyIds = new HashSet<>();
        for (String line : Files.readAllLines(dslFile.toPath())) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                policyIds.add(matcher.group(1));
            }
        }
        return policyIds;
    }

    private List<EObject> allNodes(EObject root) {
        List<EObject> nodes = new java.util.ArrayList<>();
        nodes.add(root);
        TreeIterator<EObject> iterator = root.eAllContents();
        while (iterator.hasNext()) {
            nodes.add(iterator.next());
        }
        return nodes;
    }

    private void assertRequired(EObject node, String featureName) {
        Object value = node.eGet(node.eClass().getEStructuralFeature(featureName));
        assertNotNull(value, node.eClass().getName() + "." + featureName + " must not be null");
    }
}
