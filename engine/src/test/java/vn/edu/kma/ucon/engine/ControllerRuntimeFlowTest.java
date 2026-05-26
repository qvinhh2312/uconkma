package vn.edu.kma.ucon.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import vn.edu.kma.ucon.engine.pdp.DecisionTrace;
import vn.edu.kma.ucon.engine.pdp.PolicyTraceEntry;
import vn.edu.kma.ucon.engine.pep.ApiDecisionResponse;
import vn.edu.kma.ucon.engine.pep.UconRequest;

/**
 * Regression tests for the public controller flow. These tests lock the API to
 * the PRE -> ONGOING -> POST workflow and prevent policy hard-coding from
 * returning through the controller.
 */
class ControllerRuntimeFlowTest extends AbstractUconIntegrationTest {

    @Test
    @DisplayName("Controller register returns a DecisionTrace")
    void controller_register_shouldReturnDecisionTrace() {
        ResponseEntity<ApiDecisionResponse> response = registrationController.register(registerRequest());

        assertEquals(200, response.getStatusCode().value());
        assertEquals("POST", response.getBody().getPhase());
        assertEquals("AUTHORIZATION", response.getBody().getPredicate());
        assertEquals("COMMITTED", response.getBody().getSessionStatus());
        DecisionTrace trace = response.getBody().getDecisionTrace();
        assertNotNull(trace);
        assertEquals("ALLOW", trace.decision());
        assertEquals("COMMITTED", trace.sessionStatus());
        assertFalse(trace.phases().isEmpty());
        assertNotNull(trace.snapshotBefore());
        assertNotNull(trace.snapshotAfter());
    }

    @Test
    @DisplayName("Controller drop returns a DecisionTrace")
    void controller_drop_shouldReturnDecisionTrace() {
        assertEquals(200, registrationController.register(registerRequest()).getStatusCode().value());

        ResponseEntity<ApiDecisionResponse> response = registrationController.drop(dropRequest());

        assertEquals(200, response.getStatusCode().value());
        DecisionTrace trace = response.getBody().getDecisionTrace();
        assertNotNull(trace);
        assertEquals("ALLOW", trace.decision());
        assertEquals("COMMITTED", trace.sessionStatus());
        assertTrue(trace.phases().stream().anyMatch(phase -> "POST".equals(phase.phase())));
    }

    @Test
    @DisplayName("Controller register uses PRE, ONGOING and POST workflow")
    void controller_register_shouldUsePreOngoingPostWorkflow() {
        ResponseEntity<ApiDecisionResponse> response = registrationController.register(registerRequest());

        List<String> phases = response.getBody().getDecisionTrace().phases().stream()
                .map(phase -> phase.phase())
                .toList();
        assertTrue(phases.contains("PRE"));
        assertTrue(phases.contains("ONGOING"));
        assertTrue(phases.contains("POST"));
        assertFalse(phases.contains("PRE" + "_AUTHORIZATION"));
        assertFalse(phases.contains("ONGOING" + "_AUTHORIZATION"));
        assertFalse(phases.contains("POST" + "_UPDATE"));
    }

    @Test
    @DisplayName("Controller drop not-registered denial is produced by P16 policy")
    void controller_drop_shouldBeDeniedByPolicyNotHardCode() {
        ResponseEntity<ApiDecisionResponse> response = registrationController.drop(dropRequest());

        assertEquals(403, response.getStatusCode().value());
        assertEquals("DENY", response.getBody().getDecision());
        assertEquals("PRE", response.getBody().getPhase());
        assertEquals("AUTHORIZATION", response.getBody().getPredicate());
        assertEquals("FAILED", response.getBody().getSessionStatus());
        assertEquals("P16_DropOnlyIfRegistered_PreA0", response.getBody().getFailedPolicy());
        assertEquals("NOT_REGISTERED", response.getBody().getDenyReason());
        assertTrue(response.getBody().getDecisionTrace().phases().stream()
                .anyMatch(phase -> "PRE".equals(phase.phase())
                        && "AUTHORIZATION".equals(phase.predicate())
                        && "P16_DropOnlyIfRegistered_PreA0".equals(phase.failedPolicy())));
    }

    @Test
    @DisplayName("Controller decision trace includes policy metadata")
    void controller_decisionTrace_shouldIncludePolicyMetadata() {
        ResponseEntity<ApiDecisionResponse> response = registrationController.drop(dropRequest());

        PolicyTraceEntry entry = response.getBody().getDecisionTrace().phases().stream()
                .flatMap(phase -> phase.policies().stream())
                .filter(policy -> "P16_DropOnlyIfRegistered_PreA0".equals(policy.policyId()))
                .findFirst()
                .orElseThrow();

        assertEquals("Quy che dang ky hoc phan KMA", entry.source());
        assertEquals("1.0", entry.version());
        assertEquals("preA0", entry.uconVariant());
        assertEquals("ACTIVE", entry.policyStatus());
    }

    @Test
    @DisplayName("Controller validation failure still returns ApiDecisionResponse object")
    void controller_validationFailure_shouldReturnDecisionObject() {
        UconRequest request = new UconRequest();
        request.setRequestId("bad-request-1");
        request.setStudentId("SV001");

        ResponseEntity<ApiDecisionResponse> response = registrationController.register(request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("bad-request-1", response.getBody().getRequestId());
        assertEquals("REGISTER", response.getBody().getAction());
        assertEquals("DENY", response.getBody().getDecision());
        assertEquals("VALIDATION", response.getBody().getPhase());
        assertEquals("REQUEST", response.getBody().getPredicate());
        assertEquals("BAD_REQUEST", response.getBody().getDenyReason());
        assertEquals("FAILED", response.getBody().getSessionStatus());
        assertNotNull(response.getBody().getDecisionTrace());
        assertEquals("DENY", response.getBody().getDecisionTrace().decision());
    }
}
