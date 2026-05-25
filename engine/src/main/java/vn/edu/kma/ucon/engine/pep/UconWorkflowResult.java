package vn.edu.kma.ucon.engine.pep;

import org.springframework.http.HttpStatus;

/**
 * Transport-neutral workflow result that lets the controller stay thin while
 * preserving HTTP semantics at the edge.
 */
public record UconWorkflowResult(HttpStatus status, ApiDecisionResponse response) {
}
