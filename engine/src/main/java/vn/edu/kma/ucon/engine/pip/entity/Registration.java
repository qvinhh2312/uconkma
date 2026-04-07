package vn.edu.kma.ucon.engine.pip.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Persistence entity for a Registration transaction.
 * Maps to the "create Transaction(...)" statement in P11 of ucon_policy.dsl.
 * In the DSL pseudo-code (chapter_3_logic.md), this entity is called "Transaction".
 * We name it Registration here to avoid collision with JTA's javax.transaction.Transaction.
 */
@Entity
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
