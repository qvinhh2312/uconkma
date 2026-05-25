package vn.edu.kma.ucon.engine.pep;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pip.PolicyInformationPoint;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;

/**
 * Application service for register/drop requests. It prepares a clean UCON
 * context and delegates enforcement to the PEP workflow.
 */
@Service
public class RegistrationService {

    private final PolicyInformationPoint policyInformationPoint;
    private final UconPepService uconPepService;

    public RegistrationService(PolicyInformationPoint policyInformationPoint, UconPepService uconPepService) {
        this.policyInformationPoint = policyInformationPoint;
        this.uconPepService = uconPepService;
    }

    @Transactional
    public ResponseEntity<ApiDecisionResponse> register(UconRequest request) {
        return handle(request, "REGISTER", "Successfully enrolled.",
                "Request da vuot qua PRE, ONGOING va da thuc thi day du update/obligation cua UCON.");
    }

    @Transactional
    public ResponseEntity<ApiDecisionResponse> drop(UconRequest request) {
        return handle(request, "DROP", "Successfully dropped.",
                "Request da vuot qua PRE, ONGOING va da hoan tac state qua POST update cua UCON.");
    }

    private ResponseEntity<ApiDecisionResponse> handle(UconRequest request,
                                                       String actionType,
                                                       String successMessage,
                                                       String successExplanation) {
        if (request == null) {
            return badRequest(actionType, null, null, null, "Request body is required.");
        }

        initializeRequest(request, actionType);
        if (!hasText(request.getStudentId()) || !hasText(request.getClassId())) {
            return badRequest(request.getActionType(), request.getRequestId(), request.getStudentId(), request.getClassId(),
                    "studentId and classId are required.");
        }

        Student student = policyInformationPoint.findStudent(request.getStudentId());
        ClassSection classSection = policyInformationPoint.findClassSection(request.getClassId());
        if (student == null || classSection == null) {
            return badRequest(request.getActionType(), request.getRequestId(), request.getStudentId(), request.getClassId(),
                    "Student or ClassSection not found.");
        }

        Environment preEnvironment = policyInformationPoint.buildEnvironment();
        UconContext context = new UconContext(request, student, classSection, preEnvironment);
        return uconPepService.enforce(context, successMessage, successExplanation);
    }

    private ResponseEntity<ApiDecisionResponse> badRequest(String action,
                                                           String requestId,
                                                           String studentId,
                                                           String classId,
                                                           String message) {
        ApiDecisionResponse response = new ApiDecisionResponse(
                requestId,
                action,
                "DENY",
                "VALIDATION",
                studentId,
                classId,
                null,
                "BAD_REQUEST",
                message,
                message,
                null);
        return ResponseEntity.badRequest().body(response);
    }

    private void initializeRequest(UconRequest request, String actionType) {
        request.setActionType(actionType);
        request.setStudentId(trimToNull(request.getStudentId()));
        request.setClassId(trimToNull(request.getClassId()));
        request.setRequestId(normalizeRequestId(request.getRequestId()));
        if (request.getConfirmedRegistrationRule() == null) {
            request.setConfirmedRegistrationRule(Boolean.TRUE);
        }
        if (request.getAdminOverride() == null) {
            request.setAdminOverride(Boolean.FALSE);
        }
        if (request.getSessionLeaseValid() == null) {
            request.setSessionLeaseValid(Boolean.TRUE);
        }
        request.setOverrideReason(trimToNull(request.getOverrideReason()));
    }

    private String normalizeRequestId(String requestId) {
        String normalized = trimToNull(requestId);
        return normalized != null ? normalized : UUID.randomUUID().toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
