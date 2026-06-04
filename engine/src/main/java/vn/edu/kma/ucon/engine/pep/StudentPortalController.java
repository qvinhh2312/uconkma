package vn.edu.kma.ucon.engine.pep;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Course;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.entity.StudentGrade;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.CourseRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentGradeRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;

@RestController
@RequestMapping("/api")
public class StudentPortalController {

    private final AuthService authService;
    private final StudentRepository studentRepository;
    private final StudentGradeRepository gradeRepository;
    private final ClassSectionRepository classSectionRepository;
    private final CourseRepository courseRepository;

    public StudentPortalController(AuthService authService,
                                   StudentRepository studentRepository,
                                   StudentGradeRepository gradeRepository,
                                   ClassSectionRepository classSectionRepository,
                                   CourseRepository courseRepository) {
        this.authService = authService;
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
        this.classSectionRepository = classSectionRepository;
        this.courseRepository = courseRepository;
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

    private Map<String, Object> studentSummary(Student student) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentId", student.getStudentId());
        data.put("fullName", safe(student.getFullName()));
        data.put("email", safe(student.getEmail()));
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
        data.put("courseId", course != null ? course.getCourseId() : null);
        data.put("credits", course != null ? course.getCredits() : null);
        data.put("tuitionFee", course != null ? course.getTuitionFee() : null);
        data.put("prerequisites", course != null ? safe(course.getPrerequisites()) : "");
        return data;
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
