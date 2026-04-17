package vn.edu.kma.ucon.engine.pdp;

import org.springframework.stereotype.Component;

@Component
public class MaintenanceFlag {
    private volatile boolean active = false;

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
