package vn.edu.kma.ucon.engine.pip.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Persistence entity for a registration record.
 * Maps to the DSL abstraction "Transaction" used in the policy model.
 * It is created by P11 and removed by P14 in ucon_policy.dsl.
 * We name it Registration here to avoid collision with JTA's javax.transaction.Transaction.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_registration_student_class_semester",
        columnNames = {"studentId", "classId", "semester"}
))
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;
    private String classId;
    private String semester;
    private String actionType; // e.g. "REGISTER" or "DROP"

    public Registration() {}

    public Registration(String studentId, String classId, String semester, String actionType) {
        this.studentId = studentId;
        this.classId = classId;
        this.semester = semester;
        this.actionType = actionType;
    }

    public Long getId() { return id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
}
