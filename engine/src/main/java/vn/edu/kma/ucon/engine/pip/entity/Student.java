package vn.edu.kma.ucon.engine.pip.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Version;

@Entity
public class Student {

    @Id
    private String studentId;
    
    private int currentCredits;
    private boolean tuitionPaid;
    private boolean academicWarning;
    private int maxCreditsEffective;
    private int tuitionDebt;

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

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
