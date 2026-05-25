package vn.edu.kma.ucon.engine.session.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event-driven monitor for UCON continuity. Domain events trigger re-evaluation
 * of ONGOING policies against ACTIVE usage sessions, allowing sessions to be
 * revoked when mutable attributes change after initial access.
 */
@Component
public class OngoingMonitor {

    private static final Logger log = LoggerFactory.getLogger(OngoingMonitor.class);

    private final SessionRecheckService sessionRecheckService;

    public OngoingMonitor(SessionRecheckService sessionRecheckService) {
        this.sessionRecheckService = sessionRecheckService;
    }

    @EventListener
    public void onMaintenanceEnabled(MaintenanceEnabledEvent event) {
        SessionRecheckResult result = sessionRecheckService.recheckAllActiveSessions(
                event.active() ? "MAINTENANCE_ENABLED" : "MAINTENANCE_CHANGED");
        log.info("[MONITOR] maintenance active={} checkedSessions={} revokedSessions={}",
                event.active(), result.checkedSessions(), result.revokedSessions());
    }

    @EventListener
    public void onClassStatusChanged(ClassStatusChangedEvent event) {
        SessionRecheckResult result = sessionRecheckService.recheckActiveSessionsForClass(
                event.classId(), "CLASS_STATUS_CHANGED:" + event.newStatus());
        log.info("[MONITOR] classId={} newStatus={} checkedSessions={} revokedSessions={}",
                event.classId(), event.newStatus(), result.checkedSessions(), result.revokedSessions());
    }

    @EventListener
    public void onStudentHoldAdded(StudentHoldAddedEvent event) {
        SessionRecheckResult result = sessionRecheckService.recheckActiveSessionsForStudent(
                event.studentId(), "STUDENT_HOLD_ADDED:" + event.holdCode());
        log.info("[MONITOR] studentId={} holdCode={} checkedSessions={} revokedSessions={}",
                event.studentId(), event.holdCode(), result.checkedSessions(), result.revokedSessions());
    }
}
