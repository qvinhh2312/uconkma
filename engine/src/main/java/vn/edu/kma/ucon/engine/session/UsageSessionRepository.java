package vn.edu.kma.ucon.engine.session;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageSessionRepository extends JpaRepository<UsageSession, String> {
    List<UsageSession> findByStatus(SessionStatus status);
    List<UsageSession> findByStatusAndObjectId(SessionStatus status, String objectId);
    List<UsageSession> findByStatusAndSubjectId(SessionStatus status, String subjectId);
    List<UsageSession> findTop20BySubjectIdOrderByStartedAtDesc(String subjectId);
}
