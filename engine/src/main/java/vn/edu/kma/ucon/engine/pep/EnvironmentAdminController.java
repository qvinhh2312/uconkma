package vn.edu.kma.ucon.engine.pep;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.kma.ucon.engine.pip.EnvironmentStateService;

@RestController
@RequestMapping("/api/demo/environment")
public class EnvironmentAdminController {

    private final AuthService authService;
    private final EnvironmentStateService environmentStateService;

    public EnvironmentAdminController(AuthService authService, EnvironmentStateService environmentStateService) {
        this.authService = authService;
        this.environmentStateService = environmentStateService;
    }

    @GetMapping("/state")
    public Map<String, Object> state() {
        return environmentStateService.snapshot();
    }

    @PostMapping("/open-registration")
    public Map<String, Object> openRegistration(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdminIfAuthenticated(authorizationHeader);
        return environmentStateService.openRegistrationWindow();
    }

    @PostMapping("/close-registration")
    public Map<String, Object> closeRegistration(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdminIfAuthenticated(authorizationHeader);
        return environmentStateService.closeRegistrationWindow();
    }

    private void requireAdminIfAuthenticated(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return;
        }
        authService.requireAdmin(authorizationHeader);
    }
}
