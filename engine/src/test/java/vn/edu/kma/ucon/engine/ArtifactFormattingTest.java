package vn.edu.kma.ucon.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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
            "docs/test-result.md",
            "docs/final_code_quality_checklist.md",
            "docs/final_release_checklist.md");

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
        Path repoRoot = findRepoRoot();
        Path artifact = repoRoot.resolve(path);
        assumeTrue(Files.exists(artifact), "Repository artifact is unavailable in this test checkout: " + path);
        return artifact.toFile();
    }

    private Path findRepoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("README.md"))
                    && Files.exists(current.resolve("dsl/UconPolicy.g4"))
                    && Files.exists(current.resolve("engine/pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }

        assumeTrue(false, "Full repository checkout is required for artifact formatting checks");
        return Path.of(".");
    }
}
