package vn.edu.kma.ucon.engine.session.monitor;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pdp.AuthDecision;
import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;
import vn.edu.kma.ucon.engine.pdp.PolicyEngine;
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

@Service
public class SessionRecheckService {

    private static final String MONITOR_SEMESTER = "2026_FALL";
    private static final String MONITOR_PHASE = "NORMAL";
    private static final String MONITOR_DATE = "2026-03-27";
    private static final String MONITOR_OPEN = "2026-01-01";
    private static final String MONITOR_CLOSE = "2026-12-31";

    private final UsageSessionRepository usageSessionRepository;
    private final UsageSessionService usageSessionService;
    private final StudentRepository studentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PolicyEngine policyEngine;
    private final MaintenanceFlag maintenanceFlag;

    public SessionRecheckService(UsageSessionRepository usageSessionRepository,
                                 UsageSessionService usageSessionService,
                                 StudentRepository studentRepository,
                                 ClassSectionRepository classSectionRepository,
                                 AuditLogRepository auditLogRepository,
                                 PolicyEngine policyEngine,
                                 MaintenanceFlag maintenanceFlag) {
        this.usageSessionRepository = usageSessionRepository;
        this.usageSessionService = usageSessionService;
        this.studentRepository = studentRepository;
        this.classSectionRepository = classSectionRepository;
        this.auditLogRepository = auditLogRepository;
        this.policyEngine = policyEngine;
        this.maintenanceFlag = maintenanceFlag;
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
        Environment env = new Environment(MONITOR_PHASE, MONITOR_DATE);
        env.setOpenTime(MONITOR_OPEN);
        env.setCloseTime(MONITOR_CLOSE);
        env.setSemester(MONITOR_SEMESTER);
        env.setIsMaintenance(maintenanceFlag.isActive());
        env.setMaxRegisterAttempts(5);
        env.setMaxDropTimes(2);
        return env;
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
