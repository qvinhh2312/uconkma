package vn.edu.kma.ucon.engine.pip;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.MaintenanceFlag;

/**
 * Shared demo environment state used by PIP, monitoring and admin UI controls.
 * This keeps registration-window changes visible to the UCON runtime instead of
 * leaving phase/date values hard-coded in the PIP.
 */
@Service
public class EnvironmentStateService {

    private final MaintenanceFlag maintenanceFlag;
    private String registrationPhase = "NORMAL";
    private String currentDateTime = "2026-03-27";
    private String openTime = "2026-01-01";
    private String closeTime = "2026-12-31";
    private String semester = "2026_FALL";
    private int maxRegisterAttempts = 5;
    private int maxDropTimes = 2;

    public EnvironmentStateService(MaintenanceFlag maintenanceFlag) {
        this.maintenanceFlag = maintenanceFlag;
    }

    public synchronized Environment buildEnvironment() {
        Environment environment = new Environment(registrationPhase, currentDateTime);
        environment.setOpenTime(openTime);
        environment.setCloseTime(closeTime);
        environment.setSemester(semester);
        environment.setIsMaintenance(maintenanceFlag.isActive());
        environment.setMaxRegisterAttempts(maxRegisterAttempts);
        environment.setMaxDropTimes(maxDropTimes);
        return environment;
    }

    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("semester", semester);
        environment.put("maintenance", maintenanceFlag.isActive());
        environment.put("registrationPhase", registrationPhase);
        environment.put("currentDateTime", currentDateTime);
        environment.put("openTime", openTime);
        environment.put("closeTime", closeTime);
        environment.put("maxRegisterAttempts", maxRegisterAttempts);
        environment.put("maxDropTimes", maxDropTimes);
        return environment;
    }

    public synchronized Map<String, Object> openRegistrationWindow() {
        this.registrationPhase = "NORMAL";
        this.currentDateTime = "2026-03-27";
        this.openTime = "2026-01-01";
        this.closeTime = "2026-12-31";
        maintenanceFlag.setActive(false);
        return snapshot();
    }

    public synchronized Map<String, Object> closeRegistrationWindow() {
        this.registrationPhase = "CLOSED";
        this.currentDateTime = "2026-03-27";
        return snapshot();
    }
}
