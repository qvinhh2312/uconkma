package vn.edu.kma.ucon.engine.pep;

import java.util.Map;
import java.util.Set;

/**
 * Centralizes deny reason text and revocation semantics to avoid scattering
 * policy-code strings across the API layer.
 */
public final class DenyReasonCatalog {

    public static final String TUITION_NOT_PAID = "TUITION_NOT_PAID";
    public static final String OUTSIDE_TRANSACTION_WINDOW = "OUTSIDE_TRANSACTION_WINDOW";
    public static final String CLASS_NOT_OPEN = "CLASS_NOT_OPEN";
    public static final String ALREADY_REGISTERED = "ALREADY_REGISTERED";
    public static final String CREDIT_LIMIT_EXCEEDED = "CREDIT_LIMIT_EXCEEDED";
    public static final String PREREQUISITE_NOT_MET = "PREREQUISITE_NOT_MET";
    public static final String REGULATION_NOT_CONFIRMED = "REGULATION_NOT_CONFIRMED";
    public static final String OVERRIDE_REASON_REQUIRED = "OVERRIDE_REASON_REQUIRED";
    public static final String SCHEDULE_CONFLICT = "SCHEDULE_CONFLICT";
    public static final String CLASS_FULL_ON_COMMIT = "CLASS_FULL_ON_COMMIT";
    public static final String NO_SEAT_TO_RESERVE = "NO_SEAT_TO_RESERVE";
    public static final String CLASS_STATUS_CHANGED = "CLASS_STATUS_CHANGED";
    public static final String STUDENT_ON_HOLD = "STUDENT_ON_HOLD";
    public static final String SYSTEM_UNDER_MAINTENANCE = "SYSTEM_UNDER_MAINTENANCE";
    public static final String USAGE_SESSION_EXPIRED = "USAGE_SESSION_EXPIRED";
    public static final String NOT_REGISTERED = "NOT_REGISTERED";

    private static final Set<String> REVOCATION_CODES = Set.of(
            SYSTEM_UNDER_MAINTENANCE,
            CLASS_STATUS_CHANGED,
            USAGE_SESSION_EXPIRED);

    private static final Map<String, String> EXPLANATIONS = Map.ofEntries(
            Map.entry(TUITION_NOT_PAID, "Sinh vien chua hoan tat hoc phi nen request bi chan truoc khi dang ky xay ra."),
            Map.entry(OUTSIDE_TRANSACTION_WINDOW, "Thoi diem hien tai nam ngoai khung giao dich hop le cua dot dang ky."),
            Map.entry(CLASS_NOT_OPEN, "Lop hoc phan khong o trang thai OPEN nen khong the dang ky."),
            Map.entry(ALREADY_REGISTERED, "Sinh vien da co giao dich dang ky hop le cho lop hoc phan nay."),
            Map.entry(CREDIT_LIMIT_EXCEEDED, "Tong so tin chi sau khi dang ky vuot qua gioi han tin chi hieu luc."),
            Map.entry(PREREQUISITE_NOT_MET, "Sinh vien chua hoan thanh day du mon tien quyet cua hoc phan."),
            Map.entry(REGULATION_NOT_CONFIRMED, "Sinh vien chua xac nhan da doc quy che dang ky nen khong duoc tiep tuc request."),
            Map.entry(OVERRIDE_REASON_REQUIRED, "Request co su dung override hoc vu nhung khong cung cap ly do hop le."),
            Map.entry(SCHEDULE_CONFLICT, "Lich hoc cua lop moi bi trung voi lich hoc da dang ky."),
            Map.entry(CLASS_FULL_ON_COMMIT, "Tai thoi diem gan commit, lop da het cho nen request bi tu choi."),
            Map.entry(NO_SEAT_TO_RESERVE, "Khong the giu tam cho o pha ongoing vi so cho trong khong con du."),
            Map.entry(CLASS_STATUS_CHANGED, "Trang thai lop da thay doi giua PRE va ONGOING nen request khong con hop le."),
            Map.entry(STUDENT_ON_HOLD, "Sinh vien dang co hold hoc vu/ky luat nen khong duoc thuc hien giao dich."),
            Map.entry(SYSTEM_UNDER_MAINTENANCE, "He thong da chuyen sang trang thai maintenance trong luc giao dich dang duoc xu ly."),
            Map.entry(USAGE_SESSION_EXPIRED, "Usage session khong con hop le trong qua trinh xu ly nen request bi revoke o ONGOING."),
            Map.entry(NOT_REGISTERED, "Khong ton tai giao dich dang ky hop le de thuc hien thao tac DROP."));

    private DenyReasonCatalog() {
    }

    public static boolean isRevocationCode(String denyReason) {
        return REVOCATION_CODES.contains(denyReason);
    }

    public static String explanationFor(String denyReason) {
        return EXPLANATIONS.getOrDefault(denyReason, "Policy da tu choi request o pha hien tai.");
    }
}
