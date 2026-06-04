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
        Course cs101 = upsertCourse("CS101", 3, "", 3000000);
        Course cs102 = upsertCourse("CS102", 4, "CS101", 4000000);
        Course cs201 = upsertCourse("CS201", 3, "CS101", 3500000);
        Course net201 = upsertCourse("NET201", 3, "CS101", 3600000);
        Course sec301 = upsertCourse("SEC301", 4, "CS102,NET201", 4800000);

        upsertClassSection("CS101_01", cs101, 30, 10, "T2_1-3", "OPEN");
        upsertClassSection("CS102_01", cs102, 5, 4, "T3_1-3,T5_4-6", "OPEN");
        upsertClassSection("CS201_01", cs201, 25, 12, "T4_1-3", "OPEN");
        upsertClassSection("NET201_01", net201, 20, 18, "T2_7-9,T6_1-3", "OPEN");
        upsertClassSection("SEC301_01", sec301, 15, 14, "T5_7-9,T7_1-3", "OPEN");
    }

    private void seedStudents() {
        upsertStudent("SV001", "Nguyen Van An", "An toan thong tin", "K2023", true, "CS101", "", 15);
        upsertStudent("SV002", "Tran Thi Binh", "Cong nghe thong tin", "K2023", false, "CS101", "", 15);
        upsertStudent("SV003", "Le Minh Chau", "An toan thong tin", "K2022", true, "CS101,CS102", "", 18);
        upsertStudent("SV004", "Pham Quoc Dung", "Ky thuat phan mem", "K2022", true, "CS101", "ACADEMIC_HOLD", 15);
        upsertStudent("SV005", "Hoang Ngoc Ha", "Mang may tinh", "K2024", true, "", "", 12);
        upsertStudent("SV006", "Do Tuan Kiet", "An toan thong tin", "K2021", true, "CS101,CS102,NET201", "", 21);
        upsertStudent("SV007", "Bui Lan Nhi", "Cong nghe thong tin", "K2024", false, "", "TUITION_HOLD", 12);
        upsertStudent("SV008", "Vu Thanh Phong", "Ky thuat may tinh", "K2023", true, "CS101,CS102", "", 18);
        upsertStudent("SV009", "Dang My Linh", "An toan thong tin", "K2022", true, "CS101,NET201", "", 18);
        upsertStudent("SV010", "Nguyen Hai Nam", "He thong thong tin", "K2021", true, "CS101,CS102,CS201", "", 21);
    }

    private void seedAccounts() {
        createAccountIfMissing("admin", "admin123", "Quan tri dao tao", AccountRole.ADMIN, null);
        createAccountIfMissing("sv001", "student123", "Nguyen Van An", AccountRole.STUDENT, "SV001");
        createAccountIfMissing("sv002", "student123", "Tran Thi Binh", AccountRole.STUDENT, "SV002");
        createAccountIfMissing("sv003", "student123", "Le Minh Chau", AccountRole.STUDENT, "SV003");
        createAccountIfMissing("sv004", "student123", "Pham Quoc Dung", AccountRole.STUDENT, "SV004");
        createAccountIfMissing("sv005", "student123", "Hoang Ngoc Ha", AccountRole.STUDENT, "SV005");
        createAccountIfMissing("sv006", "student123", "Do Tuan Kiet", AccountRole.STUDENT, "SV006");
        createAccountIfMissing("sv007", "student123", "Bui Lan Nhi", AccountRole.STUDENT, "SV007");
        createAccountIfMissing("sv008", "student123", "Vu Thanh Phong", AccountRole.STUDENT, "SV008");
        createAccountIfMissing("sv009", "student123", "Dang My Linh", AccountRole.STUDENT, "SV009");
        createAccountIfMissing("sv010", "student123", "Nguyen Hai Nam", AccountRole.STUDENT, "SV010");
    }

    private void seedGrades() {
        createGradeIfMissing("SV001", "CS101", "Lap trinh co ban", "2025_FALL", 8.0, 8.5, 8.3, "B+");
        createGradeIfMissing("SV001", "MATH01", "Toan roi rac", "2025_FALL", 7.5, 8.0, 7.8, "B");
        createGradeIfMissing("SV002", "CS101", "Lap trinh co ban", "2025_FALL", 6.5, 7.0, 6.8, "C+");
        createGradeIfMissing("SV003", "CS101", "Lap trinh co ban", "2025_FALL", 8.8, 9.0, 8.9, "A");
        createGradeIfMissing("SV003", "CS102", "Cau truc du lieu", "2026_SPRING", 8.0, 8.2, 8.1, "B+");
        createGradeIfMissing("SV004", "CS101", "Lap trinh co ban", "2025_FALL", 5.8, 6.2, 6.0, "C");
        createGradeIfMissing("SV005", "MATH01", "Toan roi rac", "2025_FALL", 7.0, 7.3, 7.2, "B");
        createGradeIfMissing("SV006", "NET201", "Mang may tinh", "2026_SPRING", 8.5, 8.8, 8.7, "A");
        createGradeIfMissing("SV008", "CS102", "Cau truc du lieu", "2026_SPRING", 7.8, 8.1, 8.0, "B+");
        createGradeIfMissing("SV009", "NET201", "Mang may tinh", "2026_SPRING", 8.2, 8.0, 8.1, "B+");
        createGradeIfMissing("SV010", "CS201", "Lap trinh huong doi tuong", "2026_SPRING", 8.7, 9.1, 8.9, "A");
    }

    private Course upsertCourse(String courseId, int credits, String prerequisites, int tuitionFee) {
        Course course = courseRepository.findById(courseId).orElseGet(Course::new);
        course.setCourseId(courseId);
        course.setCredits(credits);
        course.setPrerequisites(prerequisites);
        course.setTuitionFee(tuitionFee);
        return courseRepository.save(course);
    }

    private void upsertClassSection(String classId,
                                    Course course,
                                    int capacity,
                                    int enrolled,
                                    String scheduleSlots,
                                    String status) {
        ClassSection section = classSectionRepository.findById(classId).orElseGet(ClassSection::new);
        section.setClassId(classId);
        section.setCourse(course);
        section.setCapacity(capacity);
        section.setEnrolled(Math.max(section.getEnrolled(), enrolled));
        section.setReservedSeats(Math.max(section.getReservedSeats(), 0));
        section.setScheduleSlots(scheduleSlots);
        section.setStatus(defaultText(section.getStatus(), status));
        classSectionRepository.save(section);
    }

    private void upsertStudent(String studentId,
                               String fullName,
                               String major,
                               String cohort,
                               boolean tuitionPaid,
                               String completedCourses,
                               String holds,
                               int maxCredits) {
        Student student = studentRepository.findById(studentId).orElseGet(Student::new);
        student.setStudentId(studentId);
        student.setFullName(defaultText(student.getFullName(), fullName));
        student.setEmail(defaultText(student.getEmail(), studentId.toLowerCase() + "@kma.edu.vn"));
        student.setMajor(defaultText(student.getMajor(), major));
        student.setCohort(defaultText(student.getCohort(), cohort));
        student.setTuitionPaid(tuitionPaid);
        student.setCompletedCourses(defaultText(student.getCompletedCourses(), completedCourses));
        student.setHolds(defaultText(student.getHolds(), holds));
        student.setMaxCreditsEffective(student.getMaxCreditsEffective() == 0 ? maxCredits : student.getMaxCreditsEffective());
        studentRepository.save(student);
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
