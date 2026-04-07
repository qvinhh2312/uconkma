package vn.edu.kma.ucon.engine.pip.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Version;

@Entity
public class ClassSection {

    @Id
    private String classId;
    
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;
    
    private int capacity;
    private int enrolled;
    private String status; // e.g. "OPEN", "CLOSED", "CANCELLED"
    
    // Comma-separated slots e.g. "T2_1-3,T4_4-6"
    private String scheduleSlots;

    @Version
    private Long version; // Optimistic Locking

    public ClassSection() {}

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getEnrolled() { return enrolled; }
    public void setEnrolled(int enrolled) { this.enrolled = enrolled; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getScheduleSlots() { return scheduleSlots; }
    public void setScheduleSlots(String scheduleSlots) { this.scheduleSlots = scheduleSlots; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
