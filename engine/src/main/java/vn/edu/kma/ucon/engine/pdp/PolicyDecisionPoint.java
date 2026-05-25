package vn.edu.kma.ucon.engine.pdp;

import java.io.File;
import java.util.List;

import jakarta.annotation.PostConstruct;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PolicyDecisionPoint {

    private static final Logger log = LoggerFactory.getLogger(PolicyDecisionPoint.class);
    private final PolicyValidator policyValidator;
    private final PolicyAnalyzer policyAnalyzer;
    private final PolicyAdministrationPoint policyAdministrationPoint;
    
    private EObject policyModelRoot;
    private EPackage uconPackage;

    public PolicyDecisionPoint(PolicyValidator policyValidator,
                               PolicyAnalyzer policyAnalyzer,
                               PolicyAdministrationPoint policyAdministrationPoint) {
        this.policyValidator = policyValidator;
        this.policyAnalyzer = policyAnalyzer;
        this.policyAdministrationPoint = policyAdministrationPoint;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing UCON Policy Decision Point...");
        
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        
        ResourceSet resSet = new ResourceSetImpl();
        
        try {
            File ecoreFile = new File("../metamodel/ucon.ecore");
            if (!ecoreFile.exists()) ecoreFile = new File("metamodel/ucon.ecore");
            if (!ecoreFile.exists()) ecoreFile = new File(System.getProperty("user.dir"), "metamodel/ucon.ecore");
            Resource ecoreResource = resSet.getResource(URI.createFileURI(ecoreFile.getAbsolutePath()), true);
            this.uconPackage = (EPackage) ecoreResource.getContents().get(0);
            EPackage.Registry.INSTANCE.put(uconPackage.getNsURI(), uconPackage);
            log.info("Ecore metamodel loaded.");
            
            File xmiFile = new File("../xmi/ucon_policy.xmi");
            if (!xmiFile.exists()) xmiFile = new File("xmi/ucon_policy.xmi");
            if (!xmiFile.exists()) xmiFile = new File(System.getProperty("user.dir"), "xmi/ucon_policy.xmi");
            Resource xmiResource = resSet.getResource(URI.createFileURI(xmiFile.getAbsolutePath()), true);
            EObject rawPolicyModel = xmiResource.getContents().get(0);
            policyValidator.validate(rawPolicyModel);
            PolicyAnalysisReport analysisReport = policyAnalyzer.analyze(rawPolicyModel);
            analysisReport.warnings().forEach(warning ->
                    log.warn("Policy analysis warning [{}] {}: {}",
                            warning.type(), warning.policy(), warning.message()));
            this.policyModelRoot = policyAdministrationPoint.activateValidatedPolicies(rawPolicyModel);

            @SuppressWarnings("unchecked")
            List<EObject> policies = (List<EObject>) policyModelRoot.eGet(((org.eclipse.emf.ecore.EClass) uconPackage.getEClassifier("PolicyModel")).getEStructuralFeature("policies"));
            log.info("Loaded {} ACTIVE policies with {} analysis warnings.", policies.size(), analysisReport.warnings().size());
            
        } catch (Exception e) {
            log.error("Failed to load UCON Policy Engine files!", e);
            throw new IllegalStateException("UCON PDP startup failed because metamodel, policy model, or semantic validation is invalid.", e);
        }
    }

    public EObject getPolicyModelRoot() {
        return policyModelRoot;
    }

    public EPackage getUconPackage() {
        return uconPackage;
    }
}
