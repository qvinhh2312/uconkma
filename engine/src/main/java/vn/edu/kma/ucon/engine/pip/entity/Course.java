package vn.edu.kma.ucon.engine.pip.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Course {

    @Id
    private String courseId;
    private int credits;
    private String prerequisites;
    private int tuitionFee;

    public Course() {}

    public Course(String courseId, int credits, String prerequisites) {
        this.courseId = courseId;
        this.credits = credits;
        this.prerequisites = prerequisites;
    }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getPrerequisites() { return prerequisites; }
    public void setPrerequisites(String prerequisites) { this.prerequisites = prerequisites; }

    public int getTuitionFee() { return tuitionFee; }
    public void setTuitionFee(int tuitionFee) { this.tuitionFee = tuitionFee; }
}
