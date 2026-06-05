export const friendlyPolicyMessages: Record<string, string> = {
  P17_AgreeRegistrationRule_PreB0: "Cần tuân thủ quy định trước khi đăng kí.",
  P01_TuitionPaid_PreA0: "Bạn còn vướng điều kiện học phí.",
  P06_Prerequisite_PreA0: "Bạn chưa học xong môn tiên quyết.",
  P08_CapacityRecheck_OnA0: "Lớp đã hết chỗ.",
  P20_ReserveSeat_OnA2: "Không giữ được chỗ cho lớp này.",
  P16_DropOnlyIfRegistered_PreA0: "Bạn chưa đăng kí lớp này nên không thể hủy.",
  P13_EmergencyMaintenance_OnC0: "Hệ thống đang bảo trì, vui lòng thử lại sau.",
  P13a_EmergencyMaintenance_PreC0: "Hệ thống đang bảo trì, vui lòng thử lại sau.",
  P27_SessionLease_OnB0: "Phiên xử lý đã hết hạn, vui lòng thử lại.",
  TUITION_NOT_PAID: "Bạn còn vướng điều kiện học phí.",
  REGULATION_NOT_CONFIRMED: "Cần tuân thủ quy định trước khi đăng kí.",
  PREREQUISITE_NOT_MET: "Bạn chưa học xong môn tiên quyết.",
  CLASS_FULL: "Lớp đã hết chỗ.",
  CLASS_FULL_ON_COMMIT: "Lớp vừa hết chỗ khi xử lý đăng kí.",
  NO_SEAT_TO_RESERVE: "Không còn chỗ trống để giữ.",
  SCHEDULE_CONFLICT: "Lịch học bị trùng với lớp đã đăng kí.",
  NOT_REGISTERED: "Bạn chưa đăng kí lớp này nên không thể hủy.",
  SYSTEM_UNDER_MAINTENANCE: "Hệ thống đang bảo trì, vui lòng thử lại sau.",
};

export function friendlyMessage(policyOrReason?: string) {
  if (!policyOrReason) {
    return "Khong co loi chinh sach.";
  }
  return friendlyPolicyMessages[policyOrReason] ?? "Chưa đủ điều kiện để thực hiện thao tác này.";
}
