package vn.edu.kma.ucon.engine.pep;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import vn.edu.kma.ucon.engine.pip.entity.AccountRole;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Course;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.entity.StudentGrade;
import vn.edu.kma.ucon.engine.pip.entity.UserAccount;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.CourseRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentGradeRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;
import vn.edu.kma.ucon.engine.pip.repository.UserAccountRepository;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final AuthService authService;
    private final UserAccountRepository accountRepository;
    private final CourseRepository courseRepository;
    private final ClassSectionRepository classSectionRepository;
    private final StudentRepository studentRepository;
    private final StudentGradeRepository gradeRepository;

    public DemoDataSeeder(AuthService authService,
                          UserAccountRepository accountRepository,
                          CourseRepository courseRepository,
                          ClassSectionRepository classSectionRepository,
                          StudentRepository studentRepository,
                          StudentGradeRepository gradeRepository) {
        this.authService = authService;
        this.accountRepository = accountRepository;
        this.courseRepository = courseRepository;
        this.classSectionRepository = classSectionRepository;
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
    }

    @Override
    public void run(String... args) {
        seedCoursesAndClasses();
        seedStudents();
        seedAccounts();
        seedGrades();
    }

    private void seedCoursesAndClasses() {
        Course cs101 = courseRepository.findById("CS101").orElseGet(Course::new);
        cs101.setCourseId("CS101");
        cs101.setCredits(3);
        cs101.setPrerequisites("");
        cs101.setTuitionFee(3000000);
        courseRepository.save(cs101);

        Course cs102 = courseRepository.findById("CS102").orElseGet(Course::new);
        cs102.setCourseId("CS102");
        cs102.setCredits(4);
        cs102.setPrerequisites("CS101");
        cs102.setTuitionFee(4000000);
        courseRepository.save(cs102);

        ClassSection cs101Class = classSectionRepository.findById("CS101_01").orElseGet(ClassSection::new);
        cs101Class.setClassId("CS101_01");
        cs101Class.setCourse(cs101);
        cs101Class.setCapacity(30);
        cs101Class.setEnrolled(Math.max(cs101Class.getEnrolled(), 10));
        cs101Class.setReservedSeats(Math.max(cs101Class.getReservedSeats(), 0));
        cs101Class.setScheduleSlots("T2_1-3");
        cs101Class.setStatus(defaultText(cs101Class.getStatus(), "OPEN"));
        classSectionRepository.save(cs101Class);

        ClassSection cs102Class = classSectionRepository.findById("CS102_01").orElseGet(ClassSection::new);
        cs102Class.setClassId("CS102_01");
        cs102Class.setCourse(cs102);
        cs102Class.setCapacity(5);
        cs102Class.setEnrolled(Math.max(cs102Class.getEnrolled(), 4));
        cs102Class.setReservedSeats(Math.max(cs102Class.getReservedSeats(), 0));
        cs102Class.setScheduleSlots("T3_1-3,T5_4-6");
        cs102Class.setStatus(defaultText(cs102Class.getStatus(), "OPEN"));
        classSectionRepository.save(cs102Class);
    }

    private void seedStudents() {
        Student sv001 = studentRepository.findById("SV001").orElseGet(Student::new);
        sv001.setStudentId("SV001");
        sv001.setFullName(defaultText(sv001.getFullName(), "Nguyen Van An"));
        sv001.setEmail(defaultText(sv001.getEmail(), "sv001@kma.edu.vn"));
        sv001.setMajor(defaultText(sv001.getMajor(), "An toan thong tin"));
        sv001.setCohort(defaultText(sv001.getCohort(), "K2023"));
        sv001.setTuitionPaid(true);
        sv001.setCompletedCourses(defaultText(sv001.getCompletedCourses(), "CS101"));
        sv001.setMaxCreditsEffective(sv001.getMaxCreditsEffective() == 0 ? 15 : sv001.getMaxCreditsEffective());
        studentRepository.save(sv001);

        Student sv002 = studentRepository.findById("SV002").orElseGet(Student::new);
        sv002.setStudentId("SV002");
        sv002.setFullName(defaultText(sv002.getFullName(), "Tran Thi Binh"));
        sv002.setEmail(defaultText(sv002.getEmail(), "sv002@kma.edu.vn"));
        sv002.setMajor(defaultText(sv002.getMajor(), "Cong nghe thong tin"));
        sv002.setCohort(defaultText(sv002.getCohort(), "K2023"));
        sv002.setTuitionPaid(false);
        sv002.setCompletedCourses(defaultText(sv002.getCompletedCourses(), "CS101"));
        sv002.setMaxCreditsEffective(sv002.getMaxCreditsEffective() == 0 ? 15 : sv002.getMaxCreditsEffective());
        studentRepository.save(sv002);
    }

    private void seedAccounts() {
        createAccountIfMissing("admin", "admin123", "Quan tri dao tao", AccountRole.ADMIN, null);
        createAccountIfMissing("sv001", "student123", "Nguyen Van An", AccountRole.STUDENT, "SV001");
        createAccountIfMissing("sv002", "student123", "Tran Thi Binh", AccountRole.STUDENT, "SV002");
    }

    private void seedGrades() {
        createGradeIfMissing("SV001", "CS101", "Lap trinh co ban", "2025_FALL", 8.0, 8.5, 8.3, "B+");
        createGradeIfMissing("SV001", "MATH01", "Toan roi rac", "2025_FALL", 7.5, 8.0, 7.8, "B");
        createGradeIfMissing("SV002", "CS101", "Lap trinh co ban", "2025_FALL", 6.5, 7.0, 6.8, "C+");
    }

    private void createAccountIfMissing(String username,
                                        String password,
                                        String displayName,
                                        AccountRole role,
                                        String studentId) {
        if (accountRepository.existsByUsername(username)) {
            return;
        }
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(authService.hashPassword(password));
        account.setDisplayName(displayName);
        account.setRole(role);
        account.setStudentId(studentId);
        account.setEnabled(true);
        accountRepository.save(account);
    }

    private void createGradeIfMissing(String studentId,
                                      String courseId,
                                      String courseName,
                                      String semester,
                                      double processScore,
                                      double finalScore,
                                      double totalScore,
                                      String letterGrade) {
        if (gradeRepository.existsByStudentIdAndCourseIdAndSemester(studentId, courseId, semester)) {
            return;
        }
        StudentGrade grade = new StudentGrade();
        grade.setStudentId(studentId);
        grade.setCourseId(courseId);
        grade.setCourseName(courseName);
        grade.setSemester(semester);
        grade.setProcessScore(processScore);
        grade.setFinalScore(finalScore);
        grade.setTotalScore(totalScore);
        grade.setLetterGrade(letterGrade);
        gradeRepository.save(grade);
    }

    private String defaultText(String current, String fallback) {
        return current == null || current.isBlank() ? fallback : current;
    }
}
