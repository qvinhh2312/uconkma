package vn.edu.kma.ucon.engine.pep;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin API facade for registration/drop endpoints. All policy decisions are
 * delegated to the application service and UCON workflow.
 */
@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final AuthService authService;

    public RegistrationController(RegistrationService registrationService, AuthService authService) {
        this.registrationService = registrationService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiDecisionResponse> register(
            @RequestBody UconRequest req,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        enforceRequesterScope(req, authorizationHeader);
        return registrationService.register(req);
    }

    public ResponseEntity<ApiDecisionResponse> register(UconRequest req) {
        return register(req, null);
    }

    @PostMapping("/drop")
    public ResponseEntity<ApiDecisionResponse> drop(
            @RequestBody UconRequest req,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        enforceRequesterScope(req, authorizationHeader);
        return registrationService.drop(req);
    }

    public ResponseEntity<ApiDecisionResponse> drop(UconRequest req) {
        return drop(req, null);
    }

    private void enforceRequesterScope(UconRequest request, String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return; // Keep legacy demo endpoints usable without login.
        }
        AuthPrincipal principal = authService.requireAuthenticated(authorizationHeader);
        if (principal.isAdmin()) {
            return;
        }
        if (principal.studentId() == null || !principal.studentId().equals(request.getStudentId())) {
            throw new IllegalArgumentException("Students can only submit REGISTER/DROP requests for themselves.");
        }
    }
}
