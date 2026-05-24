package vn.edu.kma.ucon.engine.session;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.kma.ucon.engine.pep.UconRequest;

@Service
public class UsageSessionService {

    private final UsageSessionRepository repository;

    public UsageSessionService(UsageSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UsageSession createActive(UconRequest request) {
        UsageSession session = new UsageSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setRequestId(request.getRequestId());
        session.setSubjectId(request.getStudentId());
        session.setObjectId(request.getClassId());
        session.setRightName(request.getActionType());
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session.setLastCheckedAt(LocalDateTime.now());
        session.setRevokeReason(null);
        return repository.save(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UsageSession touch(UsageSession session) {
        session.setLastCheckedAt(LocalDateTime.now());
        return repository.save(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UsageSession markCommitted(UsageSession session) {
        session.setStatus(SessionStatus.COMMITTED);
        session.setLastCheckedAt(LocalDateTime.now());
        session.setRevokeReason(null);
        return repository.save(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UsageSession markRevoked(UsageSession session, String reason) {
        session.setStatus(SessionStatus.REVOKED);
        session.setLastCheckedAt(LocalDateTime.now());
        session.setRevokeReason(reason);
        return repository.save(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UsageSession markFailed(UsageSession session, String reason) {
        session.setStatus(SessionStatus.FAILED);
        session.setLastCheckedAt(LocalDateTime.now());
        session.setRevokeReason(reason);
        return repository.save(session);
    }
}
