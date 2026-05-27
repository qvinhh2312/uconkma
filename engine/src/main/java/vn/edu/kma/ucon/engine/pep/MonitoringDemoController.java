package vn.edu.kma.ucon.engine.pep;

import java.util.LinkedHashMap;
import java.util.Map;

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
        ClassSection classSection = classSectionRepository.findById(classId).orElse(null);
        if (classSection == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "ClassSection not found", "classId", classId));
        }
        classSection.setStatus(status);
        classSectionRepository.save(classSection);
        SessionRecheckResult result = sessionRecheckService.recheckActiveSessionsForClass(
                classId,
                "CLASS_STATUS_CHANGED:" + status);
        return ResponseEntity.ok(response("class-status",
                Map.of(
                        "classId", classId,
                        "status", status,
                        "checkedSessions", result.checkedSessions(),
                        "revokedSessions", result.revokedSessions(),
                        "message", "Class status changed and related active sessions were rechecked.")));
    }

    @PostMapping("/student-hold")
    public ResponseEntity<Map<String, Object>> studentHold(@RequestParam String studentId, @RequestParam String holdCode) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Student not found", "studentId", studentId));
        }
        String current = student.getHolds();
        student.setHolds(current == null || current.isBlank() ? holdCode : current + "," + holdCode);
        studentRepository.save(student);
        SessionRecheckResult result = sessionRecheckService.recheckActiveSessionsForStudent(
                studentId,
                "STUDENT_HOLD_ADDED:" + holdCode);
        return ResponseEntity.ok(response("student-hold",
                Map.of(
                        "studentId", studentId,
                        "holdCode", holdCode,
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
}
