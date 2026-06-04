package vn.edu.kma.ucon.engine.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.kma.ucon.engine.pep.AuthService;
import vn.edu.kma.ucon.engine.pep.AuthService.LoginRequest;
import vn.edu.kma.ucon.engine.pep.AuthService.LoginResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminPortalController {

    private final AuthService authService;
    private final AdminPortalService adminPortalService;

    public AdminPortalController(AuthService authService, AdminPortalService adminPortalService) {
        this.authService = authService;
        this.adminPortalService = adminPortalService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        if (!"ADMIN".equals(response.role())) {
            throw new IllegalArgumentException("ADMIN role is required.");
        }
        return response;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.dashboard();
    }

    @GetMapping("/policies")
    public List<Map<String, Object>> policies(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(required = false) String predicate,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword) {
        requireAdmin(authorizationHeader);
        return adminPortalService.policies(predicate, phase, status, action, keyword);
    }

    @GetMapping("/policies/{policyId}")
    public Map<String, Object> policyDetail(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String policyId) {
        requireAdmin(authorizationHeader);
        return adminPortalService.policyDetail(policyId);
    }

    @GetMapping("/policies/summary")
    public Map<String, Long> policySummary(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.policySummary();
    }

    @PostMapping("/policies/{policyId}/transition")
    public Map<String, Object> transitionPolicy(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String policyId,
            @RequestBody PolicyTransitionRequest request) {
        requireAdmin(authorizationHeader);
        return adminPortalService.transitionPolicy(policyId, request.targetStatus());
    }

    @PostMapping("/policies/reload")
    public Map<String, Object> reloadPolicies(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.reloadPolicies();
    }

    @GetMapping("/monitor/summary")
    public Map<String, Object> monitorSummary(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.monitorSummary();
    }

    @PostMapping("/monitor/maintenance")
    public Map<String, Object> maintenance(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody MaintenanceRequest request) {
        requireAdmin(authorizationHeader);
        return adminPortalService.setMaintenance(request.active());
    }

    @PostMapping("/monitor/class-status")
    public Map<String, Object> classStatus(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody ClassStatusRequest request) {
        requireAdmin(authorizationHeader);
        return adminPortalService.changeClassStatus(request.classId(), request.status());
    }

    @PostMapping("/monitor/student-hold")
    public Map<String, Object> addStudentHold(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody StudentHoldRequest request) {
        requireAdmin(authorizationHeader);
        return adminPortalService.addStudentHold(request.studentId(), request.holdCode());
    }

    @DeleteMapping("/monitor/student-hold")
    public Map<String, Object> removeStudentHold(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody StudentHoldRequest request) {
        requireAdmin(authorizationHeader);
        return adminPortalService.removeStudentHold(request.studentId(), request.holdCode());
    }

    @PostMapping("/monitor/recheck")
    public Map<String, Object> recheck(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) RecheckRequest request) {
        requireAdmin(authorizationHeader);
        return adminPortalService.recheck(request == null ? null : request.trigger());
    }

    @GetMapping("/sessions")
    public List<Map<String, Object>> sessions(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String classId) {
        requireAdmin(authorizationHeader);
        return adminPortalService.sessions(status, studentId, classId);
    }

    @GetMapping("/sessions/active")
    public List<Map<String, Object>> activeSessions(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.activeSessions();
    }

    @GetMapping("/sessions/revoked")
    public List<Map<String, Object>> revokedSessions(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.revokedSessions();
    }

    @GetMapping("/students")
    public List<Map<String, Object>> students(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.students();
    }

    @GetMapping("/students/{studentId}")
    public Map<String, Object> studentDetail(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String studentId) {
        requireAdmin(authorizationHeader);
        return adminPortalService.studentDetail(studentId);
    }

    @PatchMapping("/students/{studentId}/demo-state")
    public Map<String, Object> updateStudentState(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String studentId,
            @RequestBody Map<String, Object> request) {
        requireAdmin(authorizationHeader);
        return adminPortalService.updateStudentState(studentId, request);
    }

    @GetMapping("/classes")
    public List<Map<String, Object>> classes(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.classes();
    }

    @GetMapping("/classes/{classId}")
    public Map<String, Object> classDetail(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String classId) {
        requireAdmin(authorizationHeader);
        return adminPortalService.classDetail(classId);
    }

    @PatchMapping("/classes/{classId}/demo-state")
    public Map<String, Object> updateClassState(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String classId,
            @RequestBody Map<String, Object> request) {
        requireAdmin(authorizationHeader);
        return adminPortalService.updateClassState(classId, request);
    }

    @GetMapping("/validation")
    public Map<String, Object> validation(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.validationReport();
    }

    @GetMapping("/analyzer")
    public Map<String, Object> analyzer(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.analyzerReport();
    }

    @GetMapping("/benchmark")
    public Map<String, Object> benchmark(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return adminPortalService.benchmarkReport();
    }

    @GetMapping("/audit-logs")
    public List<Map<String, Object>> auditLogs(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String decision) {
        requireAdmin(authorizationHeader);
        return adminPortalService.auditLogs(studentId, decision);
    }

    private void requireAdmin(String authorizationHeader) {
        authService.requireAdmin(authorizationHeader);
    }

    public record PolicyTransitionRequest(String targetStatus) {}

    public record MaintenanceRequest(boolean active) {}

    public record ClassStatusRequest(String classId, String status) {}

    public record StudentHoldRequest(String studentId, String holdCode) {}

    public record RecheckRequest(String trigger) {}
}
