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
    
    private EObject policyModelRoot;
    private EPackage uconPackage;

    @PostConstruct
    public void init() {
        log.info("Initializing UCON Policy Decision Point...");
        
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        
        ResourceSet resSet = new ResourceSetImpl();
        
        try {
            // Load Ecore Metamodel
            File ecoreFile = new File("../metamodel/ucon.ecore");
            if (!ecoreFile.exists()) ecoreFile = new File("metamodel/ucon.ecore");
            if (!ecoreFile.exists()) ecoreFile = new File(System.getProperty("user.dir"), "metamodel/ucon.ecore");
            Resource ecoreResource = resSet.getResource(URI.createFileURI(ecoreFile.getAbsolutePath()), true);
            this.uconPackage = (EPackage) ecoreResource.getContents().get(0);
            EPackage.Registry.INSTANCE.put(uconPackage.getNsURI(), uconPackage);
            log.info("Successfully loaded Ecore metamodel.");
            
            // Load XMI Policy Instance
            File xmiFile = new File("../xmi/ucon_policy.xmi");
            if (!xmiFile.exists()) xmiFile = new File("xmi/ucon_policy.xmi");
            if (!xmiFile.exists()) xmiFile = new File(System.getProperty("user.dir"), "xmi/ucon_policy.xmi");
            Resource xmiResource = resSet.getResource(URI.createFileURI(xmiFile.getAbsolutePath()), true);
            this.policyModelRoot = xmiResource.getContents().get(0);
            
            @SuppressWarnings("unchecked")
            List<EObject> policies = (List<EObject>) policyModelRoot.eGet(((org.eclipse.emf.ecore.EClass) uconPackage.getEClassifier("PolicyModel")).getEStructuralFeature("policies"));
            
            log.info("Successfully loaded {} UCON policies from XMI into Memory-Tree.", policies.size());
            
        } catch (Exception e) {
            log.error("Failed to load UCON Policy Engine files!", e);
        }
    }

    public EObject getPolicyModelRoot() {
        return policyModelRoot;
    }

    public EPackage getUconPackage() {
        return uconPackage;
    }
}
