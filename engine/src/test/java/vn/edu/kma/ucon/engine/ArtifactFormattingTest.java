package vn.edu.kma.ucon.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lightweight repository presentation checks for central academic artifacts.
 */
class ArtifactFormattingTest {

    private static final List<String> IMPORTANT_ARTIFACTS = List.of(
            "README.md",
            ".github/workflows/maven.yml",
            "dsl/UconPolicy.g4",
            "dsl/ucon_policy.dsl",
            "engine/src/main/java/vn/edu/kma/ucon/engine/pep/RegistrationController.java",
            "engine/src/main/java/vn/edu/kma/ucon/engine/pep/RegistrationService.java",
            "engine/src/main/java/vn/edu/kma/ucon/engine/pep/UconPepService.java",
            "engine/src/main/java/vn/edu/kma/ucon/engine/pep/UconExecutionWorkflow.java",
            "xmi/ucon_policy.xmi",
            "metamodel/ucon.ecore",
            "docs/ucon_mapping.md",
            "docs/ucon_coverage_report.md",
            "docs/policy_catalog.md",
            "docs/validation_rules.md",
            "docs/decision_trace_examples.md",
            "docs/test-result.md");

    @Test
    @DisplayName("Important artifacts are readable and not minified into one line")
    void importantArtifacts_shouldNotBeMinified() throws Exception {
        for (String path : IMPORTANT_ARTIFACTS) {
            File file = resolve(path);
            long lines = Files.lines(file.toPath()).count();
            assertTrue(lines > 5, path + " should be formatted into multiple readable lines");
        }
    }

    @Test
    @DisplayName("Repository artifacts do not contain legacy phase names")
    void artifacts_shouldNotContainLegacyPhaseNames() throws Exception {
        List<String> legacyTokens = List.of(
                "PRE" + "_AUTHORIZATION",
                "ONGOING" + "_AUTHORIZATION",
                "POST" + "_UPDATE",
                "DENIED" + "_PREAUTH");
        for (String path : IMPORTANT_ARTIFACTS) {
            String content = Files.readString(resolve(path).toPath());
            for (String token : legacyTokens) {
                assertFalse(content.contains(token), path + " must not contain legacy token " + token);
            }
        }
    }

    private File resolve(String path) {
        File fromModule = new File("../" + path);
        if (fromModule.exists()) {
            return fromModule;
        }
        File fromRoot = new File(path);
        if (fromRoot.exists()) {
            return fromRoot;
        }
        throw new IllegalStateException("Cannot resolve artifact: " + path);
    }
}
