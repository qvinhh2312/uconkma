package vn.edu.kma.ucon.engine.pep;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Course;
import vn.edu.kma.ucon.engine.pip.entity.AuditLog;
import vn.edu.kma.ucon.engine.pip.entity.Registration;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.entity.StudentGrade;
import vn.edu.kma.ucon.engine.pip.repository.AuditLogRepository;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.CourseRepository;
import vn.edu.kma.ucon.engine.pip.repository.RegistrationRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentGradeRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;
import vn.edu.kma.ucon.engine.session.UsageSession;
import vn.edu.kma.ucon.engine.session.UsageSessionRepository;

@RestController
@RequestMapping("/api")
public class StudentPortalController {

    private final AuthService authService;
    private final StudentRepository studentRepository;
    private final StudentGradeRepository gradeRepository;
    private final ClassSectionRepository classSectionRepository;
    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UsageSessionRepository usageSessionRepository;

    public StudentPortalController(AuthService authService,
                                   StudentRepository studentRepository,
                                   StudentGradeRepository gradeRepository,
                                   ClassSectionRepository classSectionRepository,
                                   CourseRepository courseRepository,
                                   RegistrationRepository registrationRepository,
                                   AuditLogRepository auditLogRepository,
                                   UsageSessionRepository usageSessionRepository) {
        this.authService = authService;
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
        this.classSectionRepository = classSectionRepository;
        this.courseRepository = courseRepository;
        this.registrationRepository = registrationRepository;
        this.auditLogRepository = auditLogRepository;
        this.usageSessionRepository = usageSessionRepository;
    }

    @GetMapping("/students")
    public List<Map<String, Object>> listStudents(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authService.requireAdmin(authorizationHeader);
        return studentRepository.findAll().stream().map(this::studentSummary).toList();
    }

    @GetMapping("/students/{studentId}")
    public Map<String, Object> studentDetail(
            @PathVariable String studentId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthPrincipal principal = requireSameStudentOrAdmin(studentId, authorizationHeader);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        Map<String, Object> detail = studentDetail(student);
        detail.put("viewerRole", principal.role().name());
        return detail;
    }

    @GetMapping("/students/me")
    public Map<String, Object> myProfile(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthPrincipal principal = authService.requireAuthenticated(authorizationHeader);
        if (principal.studentId() == null || principal.studentId().isBlank()) {
            throw new IllegalArgumentException("Logged-in account is not linked to a student profile.");
        }
        return studentDetail(principal.studentId(), authorizationHeader);
    }

    @PatchMapping("/students/me/profile")
    public Map<String, Object> updateMyProfile(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthPrincipal principal = requireStudent(authorizationHeader);
        Student student = studentRepository.findById(principal.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + principal.studentId()));
        if (request.containsKey("email")) {
            String email = textValue(request.get("email"), "email");
            if (!email.contains("@")) {
                throw new IllegalArgumentException("email must be valid.");
            }
            student.setEmail(email);
        }
        if (request.containsKey("dateOfBirth")) {
            student.setDateOfBirth(textValue(request.get("dateOfBirth"), "dateOfBirth"));
        }
        if (request.containsKey("gender")) {
            student.setGender(textValue(request.get("gender"), "gender"));
        }
        studentRepository.save(student);
        Map<String, Object> response = studentDetail(student);
        response.put("message", "Profile updated.");
        return response;
    }

    @GetMapping("/students/me/dashboard")
    public Map<String, Object> myDashboard(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthPrincipal principal = requireStudent(authorizationHeader);
        Student student = studentRepository.findById(principal.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + principal.studentId()));
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("profile", studentDetail(student));
        dashboard.put("registeredClasses", registeredClassesFor(student.getStudentId()));
        dashboard.put("availableClasses", classSectionRepository.findAll().stream()
                .map(this::classSummary)
                .toList());
        dashboard.put("recentHistory", auditLogRepository.findTop20ByStudentIdOrderByIdDesc(student.getStudentId()).stream()
                .map(this::auditSummary)
                .toList());
        dashboard.put("sessions", usageSessionRepository.findTop20BySubjectIdOrderByStartedAtDesc(student.getStudentId()).stream()
                .map(this::sessionSummary)
                .toList());
        return dashboard;
    }

    @GetMapping("/students/{studentId}/grades")
    public List<StudentGrade> studentGrades(
            @PathVariable String studentId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireSameStudentOrAdmin(studentId, authorizationHeader);
        return gradeRepository.findByStudentIdOrderBySemesterDescCourseIdAsc(studentId);
    }

    @GetMapping("/students/me/grades")
    public List<StudentGrade> myGrades(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthPrincipal principal = authService.requireAuthenticated(authorizationHeader);
        if (principal.studentId() == null || principal.studentId().isBlank()) {
            throw new IllegalArgumentException("Logged-in account is not linked to a student profile.");
        }
        return gradeRepository.findByStudentIdOrderBySemesterDescCourseIdAsc(principal.studentId());
    }

    @GetMapping("/students/me/registered-classes")
    public List<Map<String, Object>> myRegisteredClasses(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthPrincipal principal = requireStudent(authorizationHeader);
        return registeredClassesFor(principal.studentId());
    }

    @GetMapping("/students/me/history")
    public List<Map<String, Object>> myHistory(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthPrincipal principal = requireStudent(authorizationHeader);
        return auditLogRepository.findTop20ByStudentIdOrderByIdDesc(principal.studentId()).stream()
                .map(this::auditSummary)
                .toList();
    }

    @GetMapping("/students/me/sessions")
    public List<Map<String, Object>> mySessions(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthPrincipal principal = requireStudent(authorizationHeader);
        return usageSessionRepository.findTop20BySubjectIdOrderByStartedAtDesc(principal.studentId()).stream()
                .map(this::sessionSummary)
                .toList();
    }

    @GetMapping("/classes")
    public List<Map<String, Object>> listClasses() {
        return classSectionRepository.findAll().stream().map(this::classSummary).toList();
    }

    private AuthPrincipal requireSameStudentOrAdmin(String studentId, String authorizationHeader) {
        AuthPrincipal principal = authService.requireAuthenticated(authorizationHeader);
        if (principal.isAdmin()) {
            return principal;
        }
        if (principal.studentId() != null && principal.studentId().equals(studentId)) {
            return principal;
        }
        throw new IllegalArgumentException("Students can only access their own profile.");
    }

    private AuthPrincipal requireStudent(String authorizationHeader) {
        AuthPrincipal principal = authService.requireAuthenticated(authorizationHeader);
        if (principal.studentId() == null || principal.studentId().isBlank()) {
            throw new IllegalArgumentException("Logged-in account is not linked to a student profile.");
        }
        return principal;
    }

    private Map<String, Object> studentSummary(Student student) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentId", student.getStudentId());
        data.put("fullName", safe(student.getFullName()));
        data.put("email", safe(student.getEmail()));
        data.put("dateOfBirth", safe(student.getDateOfBirth()));
        data.put("gender", safe(student.getGender()));
        data.put("major", safe(student.getMajor()));
        data.put("cohort", safe(student.getCohort()));
        data.put("currentCredits", student.getCurrentCredits());
        data.put("tuitionPaid", student.isTuitionPaid());
        data.put("tuitionDebt", student.getTuitionDebt());
        data.put("holds", safe(student.getHolds()));
        return data;
    }

    private Map<String, Object> studentDetail(Student student) {
        Map<String, Object> data = studentSummary(student);
        data.put("academicWarning", student.isAcademicWarning());
        data.put("maxCreditsEffective", student.getMaxCreditsEffective());
        data.put("completedCourses", safe(student.getCompletedCourses()));
        data.put("completedCredits", completedCredits(student));
        data.put("registeredClassIds", safe(student.getRegisteredClassIds()));
        data.put("registeredScheduleSlots", safe(student.getRegisteredScheduleSlots()));
        data.put("registerAttemptCount", student.getRegisterAttemptCount());
        data.put("dropCountForSemester", student.getDropCountForSemester());
        return data;
    }

    private Map<String, Object> classSummary(ClassSection classSection) {
        Course course = classSection.getCourse();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("classId", classSection.getClassId());
        data.put("status", safe(classSection.getStatus()));
        data.put("capacity", classSection.getCapacity());
        data.put("enrolled", classSection.getEnrolled());
        data.put("reservedSeats", classSection.getReservedSeats());
        data.put("scheduleSlots", safe(classSection.getScheduleSlots()));
        data.put("semester", "2026_FALL");
        data.put("courseId", course != null ? course.getCourseId() : null);
        data.put("courseName", course != null ? safe(course.getCourseName()) : "");
        data.put("credits", course != null ? course.getCredits() : null);
        data.put("tuitionFee", course != null ? course.getTuitionFee() : null);
        data.put("prerequisites", course != null ? safe(course.getPrerequisites()) : "");
        return data;
    }

    private List<Map<String, Object>> registeredClassesFor(String studentId) {
        return registrationRepository.findByStudentIdAndSemesterOrderByIdDesc(studentId, "2026_FALL").stream()
                .map(this::registeredClassSummary)
                .toList();
    }

    private Map<String, Object> registeredClassSummary(Registration registration) {
        ClassSection classSection = classSectionRepository.findById(registration.getClassId()).orElse(null);
        Map<String, Object> data = classSection != null ? classSummary(classSection) : new LinkedHashMap<>();
        data.put("classId", registration.getClassId());
        data.put("semester", registration.getSemester());
        data.put("registrationStatus", registration.getActionType());
        data.put("registeredAt", registration.getRegisteredAt());
        return data;
    }

    private Map<String, Object> auditSummary(AuditLog auditLog) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", auditLog.getId());
        data.put("requestId", auditLog.getRequestId());
        data.put("action", inferAction(auditLog.getRequestId()));
        data.put("classId", auditLog.getClassId());
        data.put("decision", auditLog.getDecision());
        data.put("failedPolicy", safe(auditLog.getFailedPolicyCodes()));
        data.put("denyReason", safe(auditLog.getFailedPolicyCodes()));
        data.put("createdAt", auditLog.getCreatedAt());
        data.put("sessionStatus", sessionStatusFor(auditLog.getRequestId()));
        return data;
    }

    private Map<String, Object> sessionSummary(UsageSession session) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getSessionId());
        data.put("requestId", session.getRequestId());
        data.put("action", session.getRightName());
        data.put("classId", session.getObjectId());
        data.put("status", session.getStatus());
        data.put("startedAt", session.getStartedAt());
        data.put("lastCheckedAt", session.getLastCheckedAt());
        data.put("revokeReason", safe(session.getRevokeReason()));
        return data;
    }

    private String sessionStatusFor(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return "";
        }
        return usageSessionRepository.findAll().stream()
                .filter(session -> requestId.equals(session.getRequestId()))
                .findFirst()
                .map(session -> session.getStatus().name())
                .orElse("");
    }

    private String inferAction(String requestId) {
        if (requestId == null) {
            return "";
        }
        return requestId.toUpperCase().contains("DROP") ? "DROP" : "REGISTER";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String textValue(Object value, String fieldName) {
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.toString().trim();
    }

    private int completedCredits(Student student) {
        String completedCourses = student.getCompletedCourses();
        if (completedCourses == null || completedCourses.isBlank()) {
            return 0;
        }
        int total = 0;
        for (String courseId : completedCourses.split(",")) {
            String normalizedCourseId = courseId.trim();
            if (!normalizedCourseId.isBlank()) {
                total += courseRepository.findById(normalizedCourseId)
                        .map(Course::getCredits)
                        .orElse(0);
            }
        }
        return total;
    }
}
