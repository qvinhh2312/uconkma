package vn.edu.kma.ucon.engine.pdp;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;

/**
 * Runtime safety net for mutable domain invariants after UCON updates and
 * commit/rollback steps.
 */
@Component
public class DomainInvariantChecker {

    public List<ValidationError> validate(Student student, ClassSection cls) {
        List<ValidationError> errors = new ArrayList<>();

        if (cls.getEnrolled() < 0) {
            errors.add(new ValidationError("WFR-C2", "OBJECT.enrolled", "object.enrolled must stay >= 0."));
        }
        if (cls.getReservedSeats() < 0) {
            errors.add(new ValidationError("WFR-C2B", "OBJECT.reservedSeats", "object.reservedSeats must stay >= 0."));
        }
        if (cls.getEnrolled() > cls.getCapacity()) {
            errors.add(new ValidationError("WFR-C1", "OBJECT.enrolled", "object.enrolled must not exceed object.capacity."));
        }
        if (cls.getEnrolled() + cls.getReservedSeats() > cls.getCapacity()) {
            errors.add(new ValidationError("WFR-C1B", "OBJECT.reservedSeats", "enrolled + reservedSeats must not exceed capacity."));
        }
        if (student.getCurrentCredits() < 0) {
            errors.add(new ValidationError("WFR-C3", "SUBJECT.currentCredits", "subject.currentCredits must stay >= 0."));
        }
        if (student.getCurrentCredits() > student.getMaxCreditsEffective()) {
            errors.add(new ValidationError("WFR-C4", "SUBJECT.currentCredits", "subject.currentCredits must not exceed subject.maxCreditsEffective."));
        }
        if (student.getTuitionDebt() < 0) {
            errors.add(new ValidationError("WFR-C5", "SUBJECT.tuitionDebt", "subject.tuitionDebt must stay >= 0."));
        }

        return errors;
    }

    public void assertValid(Student student, ClassSection cls) {
        List<ValidationError> errors = validate(student, cls);
        if (!errors.isEmpty()) {
            ValidationError first = errors.get(0);
            throw new IllegalStateException(first.code() + " at " + first.location() + ": " + first.message());
        }
    }
}
