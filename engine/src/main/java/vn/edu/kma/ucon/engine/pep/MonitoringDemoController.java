package vn.edu.kma.ucon.engine.pep;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;
import vn.edu.kma.ucon.engine.session.monitor.SessionRecheckResult;
import vn.edu.kma.ucon.engine.session.monitor.SessionRecheckService;

@RestController
@RequestMapping("/api/demo/monitor")
public class MonitoringDemoController {

    private static final Set<String> VALID_CLASS_STATUSES = Set.of("OPEN", "LOCKED", "CLOSED", "CANCELLED");

    private final MaintenanceFlag maintenanceFlag;
    private final StudentRepository studentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SessionRecheckService sessionRecheckService;

    public MonitoringDemoController(MaintenanceFlag maintenanceFlag,
                                    StudentRepository studentRepository,
                                    ClassSectionRepository classSectionRepository,
                                    SessionRecheckService sessionRecheckService) {
        this.maintenanceFlag = maintenanceFlag;
        this.studentRepository = studentRepository;
        this.classSectionRepository = classSectionRepository;
        this.sessionRecheckService = sessionRecheckService;
    }

    @PostMapping("/maintenance")
    public ResponseEntity<Map<String, Object>> maintenance(@RequestParam boolean active) {
        maintenanceFlag.setActive(active);
        SessionRecheckResult result = sessionRecheckService.recheckAllActiveSessions(
                active ? "MAINTENANCE_ENABLED" : "MAINTENANCE_DISABLED");
        return ResponseEntity.ok(response("maintenance",
                Map.of(
                        "active", active,
                        "checkedSessions", result.checkedSessions(),
                        "revokedSessions", result.revokedSessions(),
                        "message", "Maintenance recheck completed.")));
    }

    @PostMapping("/class-status")
    public ResponseEntity<Map<String, Object>> classStatus(@RequestParam String classId, @RequestParam String status) {
        String normalizedStatus = normalizeClassStatus(status);
        ClassSection classSection = classSectionRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("ClassSection not found: " + classId));
        classSection.setStatus(normalizedStatus);
        classSectionRepository.save(classSection);
        SessionRecheckResult result = sessionRecheckService.recheckActiveSessionsForClass(
                classId,
                "CLASS_STATUS_CHANGED:" + normalizedStatus);
        return ResponseEntity.ok(response("class-status",
                Map.of(
                        "classId", classId,
                        "status", normalizedStatus,
                        "checkedSessions", result.checkedSessions(),
                        "revokedSessions", result.revokedSessions(),
                        "message", "Class status changed and related active sessions were rechecked.")));
    }

    @PostMapping("/student-hold")
    public ResponseEntity<Map<String, Object>> studentHold(@RequestParam String studentId, @RequestParam String holdCode) {
        String normalizedHoldCode = normalizeRequiredValue("holdCode", holdCode);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        student.setHolds(appendUniqueHold(student.getHolds(), normalizedHoldCode));
        studentRepository.save(student);
        SessionRecheckResult result = sessionRecheckService.recheckActiveSessionsForStudent(
                studentId,
                "STUDENT_HOLD_ADDED:" + normalizedHoldCode);
        return ResponseEntity.ok(response("student-hold",
                Map.of(
                        "studentId", studentId,
                        "holdCode", normalizedHoldCode,
                        "checkedSessions", result.checkedSessions(),
                        "revokedSessions", result.revokedSessions(),
                        "message", "Student hold changed and related active sessions were rechecked.")));
    }

    @PostMapping("/recheck")
    public ResponseEntity<Map<String, Object>> recheckActiveSessions() {
        var result = sessionRecheckService.recheckAllActiveSessions("MANUAL_RECHECK");
        return ResponseEntity.ok(response("recheck", Map.of(
                "checkedSessions", result.checkedSessions(),
                "revokedSessions", result.revokedSessions(),
                "message", "Manual active-session recheck completed.")));
    }

    private Map<String, Object> response(String action, Map<String, Object> details) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("action", action);
        response.putAll(details);
        return response;
    }

    private String normalizeClassStatus(String status) {
        String normalizedStatus = normalizeRequiredValue("status", status).toUpperCase();
        if (!VALID_CLASS_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid class status: " + status);
        }
        return normalizedStatus;
    }

    private String normalizeRequiredValue(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private String appendUniqueHold(String current, String holdCode) {
        Set<String> holds = new LinkedHashSet<>();
        if (current != null && !current.isBlank()) {
            Arrays.stream(current.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(holds::add);
        }
        holds.add(holdCode);
        return String.join(",", holds);
    }
}
