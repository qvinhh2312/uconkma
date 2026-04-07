package vn.edu.kma.ucon.engine.pip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.ucon.engine.pip.entity.Registration;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
}
