package vn.edu.kma.ucon.engine.pep;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Policy enforcement point (PEP) entry that delegates all decisions to the
 * canonical UCON execution workflow.
 */
@Service
public class UconPepService {

    private final UconExecutionWorkflow workflow;

    public UconPepService(UconExecutionWorkflow workflow) {
        this.workflow = workflow;
    }

    public ResponseEntity<ApiDecisionResponse> enforce(UconContext context,
                                                       String successMessage,
                                                       String successExplanation) {
        UconWorkflowResult result = workflow.execute(context, successMessage, successExplanation);
        return ResponseEntity.status(result.status()).body(result.response());
    }
}
