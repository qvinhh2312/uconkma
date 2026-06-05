package vn.edu.kma.ucon.engine.pip.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Version;

@Entity
public class Student {

    @Id
    private String studentId;

    private String fullName;
    private String email;
    private String dateOfBirth;
    private String gender;
    private String major;
    private String cohort;

    private int currentCredits;
    private boolean tuitionPaid;
    private boolean academicWarning;
    private int maxCreditsEffective;
    private int tuitionDebt;
    private int registerAttemptCount;
    private int dropCountForSemester;

    @Column(length = 1000)
    private String completedCourses;

    @Column(length = 1000)
    private String registeredScheduleSlots;

    @Column(length = 1000)
    private String registeredClassIds;

    @Column(length = 1000)
    private String holds;

    @Version
    private Long version;

    public Student() {}

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public String getCohort() { return cohort; }
    public void setCohort(String cohort) { this.cohort = cohort; }

    public int getCurrentCredits() { return currentCredits; }
    public void setCurrentCredits(int currentCredits) { this.currentCredits = currentCredits; }

    public boolean isTuitionPaid() { return tuitionPaid; }
    public void setTuitionPaid(boolean tuitionPaid) { this.tuitionPaid = tuitionPaid; }

    public boolean isAcademicWarning() { return academicWarning; }
    public void setAcademicWarning(boolean academicWarning) { this.academicWarning = academicWarning; }

    public int getMaxCreditsEffective() { return maxCreditsEffective; }
    public void setMaxCreditsEffective(int maxCreditsEffective) { this.maxCreditsEffective = maxCreditsEffective; }

    public String getCompletedCourses() { return completedCourses; }
    public void setCompletedCourses(String completedCourses) { this.completedCourses = completedCourses; }

    public String getRegisteredScheduleSlots() { return registeredScheduleSlots; }
    public void setRegisteredScheduleSlots(String registeredScheduleSlots) { this.registeredScheduleSlots = registeredScheduleSlots; }

    public String getRegisteredClassIds() { return registeredClassIds; }
    public void setRegisteredClassIds(String registeredClassIds) { this.registeredClassIds = registeredClassIds; }

    public String getHolds() { return holds; }
    public void setHolds(String holds) { this.holds = holds; }

    public int getTuitionDebt() { return tuitionDebt; }
    public void setTuitionDebt(int tuitionDebt) { this.tuitionDebt = tuitionDebt; }

    public int getRegisterAttemptCount() { return registerAttemptCount; }
    public void setRegisterAttemptCount(int registerAttemptCount) { this.registerAttemptCount = registerAttemptCount; }

    public int getDropCountForSemester() { return dropCountForSemester; }
    public void setDropCountForSemester(int dropCountForSemester) { this.dropCountForSemester = dropCountForSemester; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
