package vn.edu.kma.ucon.engine.pep;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
/**
 * Thin API facade for registration/drop endpoints. All policy decisions are
 * delegated to the application service and UCON workflow.
 */
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiDecisionResponse> register(@RequestBody UconRequest req) {
        return registrationService.register(req);
    }

    @PostMapping("/drop")
    public ResponseEntity<ApiDecisionResponse> drop(@RequestBody UconRequest req) {
        return registrationService.drop(req);
    }
}
