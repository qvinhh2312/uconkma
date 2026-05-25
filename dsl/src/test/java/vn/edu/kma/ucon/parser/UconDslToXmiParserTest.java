package vn.edu.kma.ucon.parser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UconDslToXmiParserTest {

    @TempDir
    Path tempDir;

    @Test
    void dslToXmi_shouldGenerateValidPolicyModel() throws Exception {
        File ecoreFile = Path.of("..", "metamodel", "ucon.ecore").normalize().toFile();
        File dslFile = Path.of("ucon_policy.dsl").toFile();
        File outputFile = tempDir.resolve("generated-policy-model.xmi").toFile();

        ResourceSet resourceSet = UconDslToXmiParser.createConfiguredResourceSet(ecoreFile);
        EObject rootModel = UconDslToXmiParser.parsePolicyModel(ecoreFile, dslFile);
        UconDslToXmiParser.writePolicyModel(resourceSet, rootModel, outputFile);

        assertTrue(outputFile.exists());
        String xml = Files.readString(outputFile.toPath());
        assertTrue(xml.contains("predicate=\"AUTHORIZATION\""));
        assertTrue(xml.contains("phase=\"PRE\""));
        assertTrue(xml.contains("entity=\"SUBJECT\""));
        assertTrue(xml.contains("operator=\"EQUALS\""));

        Resource generated = resourceSet.getResource(URI.createFileURI(outputFile.getAbsolutePath()), true);
        generated.load(Collections.emptyMap());
        assertTrue(generated.getErrors().isEmpty());
        assertFalse(generated.getContents().isEmpty());
    }

    @Test
    void parserShouldRejectDuplicatePolicyIds() throws Exception {
        String duplicateDsl = """
                policy P_DUP {
                    predicate: AUTHORIZATION
                    phase: PRE
                    updateTiming: NONE
                    targetAction: REGISTER
                    effect: PERMIT
                    priority: 10
                    description: "Duplicate test 1"
                    subjectType: "Student"
                    objectType: "ClassSection"
                    source: "Test"
                    version: "1.0"
                    policyStatus: ACTIVE
                    uconVariant: "preA0"
                    denyReason: "TEST_DENY"

                    condition: subject.tuitionPaid == true
                }

                policy P_DUP {
                    predicate: AUTHORIZATION
                    phase: PRE
                    updateTiming: NONE
                    targetAction: REGISTER
                    effect: PERMIT
                    priority: 9
                    description: "Duplicate test 2"
                    subjectType: "Student"
                    objectType: "ClassSection"
                    source: "Test"
                    version: "1.0"
                    policyStatus: ACTIVE
                    uconVariant: "preA0"
                    denyReason: "TEST_DENY"

                    condition: subject.tuitionPaid == true
                }
                """;

        File tempDslFile = tempDir.resolve("duplicate-policy.dsl").toFile();
        Files.writeString(tempDslFile.toPath(), duplicateDsl);

        File ecoreFile = Path.of("..", "metamodel", "ucon.ecore").normalize().toFile();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> UconDslToXmiParser.parsePolicyModel(ecoreFile, tempDslFile));
        assertTrue(ex.getMessage().contains("Duplicate policyId"));
    }

    @Test
    void parserShouldExposeReadableDslSyntaxErrors() throws Exception {
        String invalidDsl = """
                policy P_INVALID {
                    predicate: AUTHORIZATION
                    phase: PRE
                    updateTiming: NONE
                    targetAction: REGISTER
                    effect: PERMIT
                    priority: 10
                    description: "Invalid"
                    subjectType: "Student"
                    objectType: "ClassSection"
                    source: "Test"
                    version: "1.0"
                    policyStatus: ACTIVE
                    uconVariant: "preA0"
                    denyReason: "TEST_DENY"

                    condition: subject.tuitionPaid ==
                }
                """;

        File tempDslFile = tempDir.resolve("invalid-policy.dsl").toFile();
        Files.writeString(tempDslFile.toPath(), invalidDsl);

        File ecoreFile = Path.of("..", "metamodel", "ucon.ecore").normalize().toFile();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> UconDslToXmiParser.parsePolicyModel(ecoreFile, tempDslFile));
        assertTrue(ex.getMessage().contains("Invalid DSL syntax"));
    }
}
