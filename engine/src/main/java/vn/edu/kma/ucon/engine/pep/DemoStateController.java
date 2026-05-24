package vn.edu.kma.ucon.engine.pep;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
import vn.edu.kma.ucon.engine.pip.entity.AuditLog;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Registration;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.AuditLogRepository;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.RegistrationRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;

@RestController
@RequestMapping("/api/demo")
public class DemoStateController {

    private static final String DEMO_SEMESTER = "2026_FALL";

    private final StudentRepository studentRepo;
    private final ClassSectionRepository classRepo;
    private final RegistrationRepository registrationRepo;
    private final AuditLogRepository auditRepo;
    private final MaintenanceFlag maintenanceFlag;

    public DemoStateController(StudentRepository studentRepo,
                               ClassSectionRepository classRepo,
                               RegistrationRepository registrationRepo,
                               AuditLogRepository auditRepo,
                               MaintenanceFlag maintenanceFlag) {
        this.studentRepo = studentRepo;
        this.classRepo = classRepo;
        this.registrationRepo = registrationRepo;
        this.auditRepo = auditRepo;
        this.maintenanceFlag = maintenanceFlag;
    }

    @GetMapping("/state")
    public Map<String, Object> state(@RequestParam String studentId, @RequestParam String classId) {
        Student student = studentRepo.findById(studentId).orElse(null);
        ClassSection cls = classRepo.findById(classId).orElse(null);
        Registration registration = registrationRepo.findByStudentIdAndClassIdAndSemester(studentId, classId, DEMO_SEMESTER)
                .orElse(null);
        AuditLog latestAudit = auditRepo.findTopByStudentIdAndClassIdOrderByIdDesc(studentId, classId).orElse(null);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("environment", environmentSnapshot());
        response.put("student", studentSnapshot(student));
        response.put("classSection", classSnapshot(cls));
        response.put("registration", registrationSnapshot(registration));
        response.put("latestAudit", auditSnapshot(latestAudit));
        response.put("totals", totalsSnapshot());
        return response;
    }

    private Map<String, Object> environmentSnapshot() {
        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("semester", DEMO_SEMESTER);
        environment.put("maintenance", maintenanceFlag.isActive());
        environment.put("registrationPhase", "NORMAL");
        environment.put("openTime", "2026-01-01");
        environment.put("closeTime", "2026-12-31");
        environment.put("maxRegisterAttempts", 5);
        environment.put("maxDropTimes", 2);
        return environment;
    }

    private Map<String, Object> studentSnapshot(Student student) {
        Map<String, Object> state = new LinkedHashMap<>();
        if (student == null) {
            state.put("exists", false);
            return state;
        }
        state.put("exists", true);
        state.put("studentId", student.getStudentId());
        state.put("currentCredits", student.getCurrentCredits());
        state.put("tuitionPaid", student.isTuitionPaid());
        state.put("tuitionDebt", student.getTuitionDebt());
        state.put("registerAttemptCount", student.getRegisterAttemptCount());
        state.put("dropCountForSemester", student.getDropCountForSemester());
        state.put("holds", safe(student.getHolds()));
        state.put("completedCourses", safe(student.getCompletedCourses()));
        state.put("registeredClassIds", safe(student.getRegisteredClassIds()));
        state.put("registeredScheduleSlots", safe(student.getRegisteredScheduleSlots()));
        return state;
    }

    private Map<String, Object> classSnapshot(ClassSection cls) {
        Map<String, Object> state = new LinkedHashMap<>();
        if (cls == null) {
            state.put("exists", false);
            return state;
        }
        state.put("exists", true);
        state.put("classId", cls.getClassId());
        state.put("status", safe(cls.getStatus()));
        state.put("enrolled", cls.getEnrolled());
        state.put("reservedSeats", cls.getReservedSeats());
        state.put("capacity", cls.getCapacity());
        state.put("scheduleSlots", safe(cls.getScheduleSlots()));
        state.put("courseId", cls.getCourse() != null ? safe(cls.getCourse().getCourseId()) : "null");
        state.put("credits", cls.getCourse() != null ? cls.getCourse().getCredits() : null);
        state.put("tuitionFee", cls.getCourse() != null ? cls.getCourse().getTuitionFee() : null);
        return state;
    }

    private Map<String, Object> registrationSnapshot(Registration registration) {
        Map<String, Object> state = new LinkedHashMap<>();
        if (registration == null) {
            state.put("exists", false);
            return state;
        }
        state.put("exists", true);
        state.put("id", registration.getId());
        state.put("studentId", registration.getStudentId());
        state.put("classId", registration.getClassId());
        state.put("semester", registration.getSemester());
        state.put("actionType", registration.getActionType());
        return state;
    }

    private Map<String, Object> auditSnapshot(AuditLog auditLog) {
        Map<String, Object> state = new LinkedHashMap<>();
        if (auditLog == null) {
            state.put("exists", false);
            return state;
        }
        state.put("exists", true);
        state.put("id", auditLog.getId());
        state.put("requestId", auditLog.getRequestId());
        state.put("studentId", auditLog.getStudentId());
        state.put("classId", auditLog.getClassId());
        state.put("decision", auditLog.getDecision());
        state.put("failedPolicyCodes", safe(auditLog.getFailedPolicyCodes()));
        return state;
    }

    private Map<String, Object> totalsSnapshot() {
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("students", studentRepo.count());
        totals.put("classes", classRepo.count());
        totals.put("registrations", registrationRepo.count());
        totals.put("auditLogs", auditRepo.count());
        return totals;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "<empty>" : value;
    }
}
