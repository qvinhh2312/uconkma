export const friendlyPolicyMessages: Record<string, string> = {
  P17_AgreeRegistrationRule_PreB0: "Ban can xac nhan quy che dang ky truoc khi tiep tuc.",
  P01_TuitionPaid_PreA0: "Ban chua du dieu kien hoc phi de dang ky hoc phan.",
  P06_Prerequisite_PreA0: "Ban chua dat hoc phan tien quyet.",
  P08_CapacityRecheck_OnA0: "Lop hoc phan da het cho.",
  P20_ReserveSeat_OnA2: "He thong khong the giu cho cho lop nay.",
  P16_DropOnlyIfRegistered_PreA0: "Ban chua dang ky lop nay nen khong the huy.",
  P13_EmergencyMaintenance_OnC0: "He thong dang bao tri, thao tac bi tam dung.",
  P13a_EmergencyMaintenance_PreC0: "He thong dang bao tri, thao tac bi tam dung.",
  P27_SessionLease_OnB0: "Phien xu ly khong con hop le, vui long thu lai.",
  TUITION_NOT_PAID: "Ban chua du dieu kien hoc phi de dang ky hoc phan.",
  REGULATION_NOT_CONFIRMED: "Ban can xac nhan quy che dang ky hoc phan.",
  PREREQUISITE_NOT_MET: "Ban chua dat hoc phan tien quyet.",
  CLASS_FULL: "Lop hoc phan da het cho.",
  NOT_REGISTERED: "Ban chua dang ky lop nay nen khong the huy.",
  SYSTEM_UNDER_MAINTENANCE: "He thong dang bao tri, vui long thu lai sau.",
};

export function friendlyMessage(policyOrReason?: string) {
  if (!policyOrReason) {
    return "Khong co loi chinh sach.";
  }
  return friendlyPolicyMessages[policyOrReason] ?? policyOrReason;
}
