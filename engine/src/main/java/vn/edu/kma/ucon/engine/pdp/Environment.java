package vn.edu.kma.ucon.engine.pdp;

public class Environment {
    private String registrationPhase;
    private String currentDateTime; // Simple string for mock
    private String openTime;
    private String closeTime;
    private String semester;

    public Environment(String registrationPhase, String currentDateTime) {
        this.registrationPhase = registrationPhase;
        this.currentDateTime = currentDateTime;
    }

    public String getRegistrationPhase() {
        return registrationPhase;
    }

    public void setRegistrationPhase(String registrationPhase) {
        this.registrationPhase = registrationPhase;
    }

    public String getCurrentDateTime() {
        return currentDateTime;
    }

    public void setCurrentDateTime(String currentDateTime) {
        this.currentDateTime = currentDateTime;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(String closeTime) {
        this.closeTime = closeTime;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    private boolean isMaintenance;

    public boolean getIsMaintenance() {
        return isMaintenance;
    }

    public void setIsMaintenance(boolean isMaintenance) {
        this.isMaintenance = isMaintenance;
    }
}
