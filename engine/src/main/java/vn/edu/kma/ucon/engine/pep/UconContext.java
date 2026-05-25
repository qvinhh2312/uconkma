package vn.edu.kma.ucon.engine.pep;

import java.util.ArrayList;
import java.util.List;

import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.PhaseTrace;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.session.UsageSession;

/**
 * Mutable execution context that travels through the PRE, ONGOING and POST
 * UCON workflow.
 */
public class UconContext {

    private final UconRequest request;
    private final Student student;
    private final ClassSection classSection;
    private Environment preEnvironment;
    private Environment ongoingEnvironment;
    private UsageSession usageSession;
    private final List<PhaseTrace> traces = new ArrayList<>();

    public UconContext(UconRequest request,
                       Student student,
                       ClassSection classSection,
                       Environment preEnvironment) {
        this.request = request;
        this.student = student;
        this.classSection = classSection;
        this.preEnvironment = preEnvironment;
    }

    public UconRequest getRequest() {
        return request;
    }

    public Student getStudent() {
        return student;
    }

    public ClassSection getClassSection() {
        return classSection;
    }

    public Environment getPreEnvironment() {
        return preEnvironment;
    }

    public void setPreEnvironment(Environment preEnvironment) {
        this.preEnvironment = preEnvironment;
    }

    public Environment getOngoingEnvironment() {
        return ongoingEnvironment;
    }

    public void setOngoingEnvironment(Environment ongoingEnvironment) {
        this.ongoingEnvironment = ongoingEnvironment;
    }

    public UsageSession getUsageSession() {
        return usageSession;
    }

    public void setUsageSession(UsageSession usageSession) {
        this.usageSession = usageSession;
    }

    public List<PhaseTrace> getTraces() {
        return traces;
    }
}
