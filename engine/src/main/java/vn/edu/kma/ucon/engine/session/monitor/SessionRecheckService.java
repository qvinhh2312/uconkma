package vn.edu.kma.ucon.engine.session.monitor;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pdp.AuthDecision;
import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.PolicyEngine;
import vn.edu.kma.ucon.engine.pip.PolicyInformationPoint;
import vn.edu.kma.ucon.engine.pip.entity.AuditLog;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.AuditLogRepository;
import vn.edu.kma.ucon.engine.pip.repository.ClassSectionRepository;
import vn.edu.kma.ucon.engine.pip.repository.StudentRepository;
import vn.edu.kma.ucon.engine.session.SessionStatus;
import vn.edu.kma.ucon.engine.session.UsageSession;
import vn.edu.kma.ucon.engine.session.UsageSessionRepository;
import vn.edu.kma.ucon.engine.session.UsageSessionService;

/**
 * Re-evaluates ONGOING UCON policies for active sessions when environment,
 * object or subject attributes change outside the original request thread.
 */
@Service
public class SessionRecheckService {

    private final UsageSessionRepository usageSessionRepository;
    private final UsageSessionService usageSessionService;
    private final StudentRepository studentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PolicyEngine policyEngine;
    private final PolicyInformationPoint policyInformationPoint;

    public SessionRecheckService(UsageSessionRepository usageSessionRepository,
                                 UsageSessionService usageSessionService,
                                 StudentRepository studentRepository,
                                 ClassSectionRepository classSectionRepository,
                                 AuditLogRepository auditLogRepository,
                                 PolicyEngine policyEngine,
                                 PolicyInformationPoint policyInformationPoint) {
        this.usageSessionRepository = usageSessionRepository;
        this.usageSessionService = usageSessionService;
        this.studentRepository = studentRepository;
        this.classSectionRepository = classSectionRepository;
        this.auditLogRepository = auditLogRepository;
        this.policyEngine = policyEngine;
        this.policyInformationPoint = policyInformationPoint;
    }

    @Transactional
    public SessionRecheckResult recheckAllActiveSessions(String triggerReason) {
        return recheckSessions(usageSessionRepository.findByStatus(SessionStatus.ACTIVE), triggerReason);
    }

    @Transactional
    public SessionRecheckResult recheckActiveSessionsForClass(String classId, String triggerReason) {
        return recheckSessions(usageSessionRepository.findByStatusAndObjectId(SessionStatus.ACTIVE, classId), triggerReason);
    }

    @Transactional
    public SessionRecheckResult recheckActiveSessionsForStudent(String studentId, String triggerReason) {
        return recheckSessions(usageSessionRepository.findByStatusAndSubjectId(SessionStatus.ACTIVE, studentId), triggerReason);
    }

    private SessionRecheckResult recheckSessions(List<UsageSession> sessions, String triggerReason) {
        int checked = 0;
        int revoked = 0;
        for (UsageSession session : sessions) {
            checked++;
            if (recheckSingleSession(session, triggerReason)) {
                revoked++;
            }
        }
        return new SessionRecheckResult(checked, revoked);
    }

    private boolean recheckSingleSession(UsageSession session, String triggerReason) {
        Student student = studentRepository.findById(session.getSubjectId()).orElse(null);
        ClassSection classSection = classSectionRepository.findById(session.getObjectId()).orElse(null);
        if (student == null || classSection == null) {
            usageSessionService.markRevoked(session, "SESSION_RESOURCE_MISSING");
            writeAudit(session, "DENY", "SESSION_RESOURCE_MISSING", triggerReason);
            return true;
        }

        UconRequest request = syntheticRequest(session);
        Environment environment = buildEnvironment();
        AuthDecision decision = policyEngine.evaluatePhase("ONGOING", student, classSection, environment, request);
        if (!decision.isPermit()) {
            usageSessionService.markRevoked(session, decision.getFailedCode());
            writeAudit(session, "DENY", decision.getFailedPolicy(), triggerReason + ":" + decision.getFailedCode());
            return true;
        }

        usageSessionService.touch(session);
        return false;
    }

    private UconRequest syntheticRequest(UsageSession session) {
        UconRequest request = new UconRequest();
        request.setRequestId(session.getRequestId() != null ? session.getRequestId() : UUID.randomUUID().toString());
        request.setActionType(session.getRightName());
        request.setStudentId(session.getSubjectId());
        request.setClassId(session.getObjectId());
        request.setConfirmedRegistrationRule(Boolean.TRUE);
        request.setAdminOverride(Boolean.FALSE);
        request.setSessionLeaseValid(Boolean.TRUE);
        return request;
    }

    private Environment buildEnvironment() {
        return policyInformationPoint.buildEnvironment();
    }

    private void writeAudit(UsageSession session, String decision, String failedPolicyCodes, String triggerReason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setRequestId(session.getRequestId() != null ? session.getRequestId() : "MONITOR-" + session.getSessionId());
        auditLog.setStudentId(session.getSubjectId());
        auditLog.setClassId(session.getObjectId());
        auditLog.setDecision(decision);
        auditLog.setFailedPolicyCodes(failedPolicyCodes == null || failedPolicyCodes.isBlank()
                ? triggerReason
                : failedPolicyCodes + "|" + triggerReason);
        auditLogRepository.save(auditLog);
    }
}
