package vn.edu.kma.ucon.engine.pdp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import vn.edu.kma.ucon.engine.pip.repository.RegistrationRepository;

@Component
public class PolicyFunctionExecutor {

    private final RegistrationRepository registrationRepository;

    public PolicyFunctionExecutor(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    public boolean isEmpty(Object argVal) {
        return argVal == null || argVal.toString().trim().isEmpty();
    }

    public boolean checkExistsRegistration(String studentId, String classId, String semester) {
        if (studentId == null || classId == null || semester == null) {
            return false;
        }
        return registrationRepository.findByStudentIdAndClassIdAndSemester(
                studentId.trim(),
                classId.trim(),
                semester.trim()
        ).isPresent();
    }

    public Collection<?> asList(Object val) {
        if (val == null) return Collections.emptyList();
        if (val instanceof Collection) return (Collection<?>) val;
        return Arrays.stream(val.toString().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
