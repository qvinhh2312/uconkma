// pre authorization

policy P01_TuitionPaid_Pre {
    type: PRE_AUTHORIZATION
    targetAction: ANY
    effect: PERMIT
    priority: 100
    description: "Chỉ cho phép SV đã hoàn tất học phí"
    denyReason: "TUITION_NOT_PAID"
    
    condition: subject.tuitionPaid == true
}

policy P02_RegistrationWindow_Pre {
    type: PRE_AUTHORIZATION
    targetAction: ANY
    effect: PERMIT
    priority: 90
    description: "Chỉ cho đăng ký trong đợt và giờ hợp lệ"
    denyReason: "OUTSIDE_REGISTRATION_WINDOW"
    
    condition: environment.registrationPhase IN ["NORMAL", "LATE"] 
               AND environment.currentDateTime >= environment.openTime 
               AND environment.currentDateTime <= environment.closeTime
}

policy P03_ClassStatusOpen_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 80
    description: "Chỉ lớp đang mở thực sự mới được đăng ký"
    denyReason: "CLASS_NOT_OPEN"
    
    condition: object.status == "OPEN"
}

policy P04_NotAlreadyRegistered_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 70
    description: "Không cho đăng ký trùng cùng lớp"
    denyReason: "ALREADY_REGISTERED"
    
    condition: NOT checkExistsRegistration(subject.studentId, object.classId, environment.semester)
}

policy P05_CreditLimit_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 60
    description: "Không vượt trần hạn mức tín chỉ thực tế"
    denyReason: "CREDIT_LIMIT_EXCEEDED"
    
    condition: (subject.currentCredits + object.course.credits) <= subject.maxCreditsEffective
}

policy P06_Prerequisite_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 50
    description: "Đảm bảo đã hoàn tất môn học tiên quyết"
    denyReason: "PREREQUISITE_NOT_MET"
    
    condition: object.course.prerequisites SUBSET_OF subject.completedCourses
}

policy P07_ScheduleConflict_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 40
    description: "Tránh trùng lịch học với các môn đã chọn"
    denyReason: "SCHEDULE_CONFLICT"
    
    condition: NOT (object.scheduleSlots OVERLAPS subject.registeredScheduleSlots)
}

// ongoing

policy P08_CapacityRecheck_On {
    type: ONGOING_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 30
    description: "Chống race condition ở slot cuối cùng"
    denyReason: "CLASS_FULL_ON_COMMIT"
    
    condition: object.enrolled < object.capacity
}

policy P09_ClassStatusRecheck_On {
    type: ONGOING_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 20
    description: "Kiểm tra lại trạng thái lớp phòng khi Admin khóa đột xuất"
    denyReason: "CLASS_STATUS_CHANGED"
    
    condition: object.status == "OPEN"
}

policy P10_StudentHoldRecheck_On {
    type: ONGOING_AUTHORIZATION
    targetAction: ANY
    effect: PERMIT
    priority: 10
    description: "Kiểm tra tình trạng cầm chân kỷ luật của SV trước khi tạo mốc"
    denyReason: "STUDENT_ON_HOLD"
    
    condition: isEmpty(subject.holds)
}

policy P13_EmergencyMaintenance_On {
    type: ONGOING_AUTHORIZATION
    targetAction: ANY
    effect: PERMIT
    priority: 50
    description: "Ngắt toàn bộ giao dịch đang lơ lửng nếu Admin kích hoạt cờ Bảo trì khẩn cấp"
    denyReason: "SYSTEM_UNDER_MAINTENANCE"
    
    condition: environment.isMaintenance == false
}

// post update

policy P11_RegisterStateUpdate_Post {
    type: POST_UPDATE
    targetAction: REGISTER
    effect: PERMIT
    priority: 8
    description: "Atomic Update trạng thái Object và Subject sau khi Đăng ký"
    
    condition: true
    
    postUpdates:
       create Transaction(subject.studentId, object.classId, environment.semester, "REGISTER")
       object.enrolled ADD_ASSIGN 1
       subject.currentCredits ADD_ASSIGN object.course.credits
       subject.registeredScheduleSlots APPEND object.scheduleSlots
       subject.registeredClassIds APPEND object.classId
}

policy P14_DropStateRevert_Post {
    type: POST_UPDATE
    targetAction: DROP
    effect: PERMIT
    priority: 8
    description: "Hoàn trả dữ liệu: Giảm sĩ số, trừ tín chỉ, xóa lịch, xóa classId, hủy Record"
    
    condition: true
    
    postUpdates:
       delete Transaction(subject.studentId, object.classId, environment.semester)
       object.enrolled SUB_ASSIGN 1
       subject.currentCredits SUB_ASSIGN object.course.credits
       subject.registeredScheduleSlots REMOVE object.scheduleSlots
       subject.registeredClassIds REMOVE object.classId
}

policy P15a_RegisterBilling_Post {
    type: POST_UPDATE
    targetAction: REGISTER
    effect: PERMIT
    priority: 5
    description: "Cộng dồn công nợ học phí ngay sau khi đăng ký lớp thành công"
    
    condition: true
    
    postUpdates:
       subject.tuitionDebt ADD_ASSIGN object.course.tuitionFee
}

policy P15b_DropRefund_Post {
    type: POST_UPDATE
    targetAction: DROP
    effect: PERMIT
    priority: 5
    description: "Hoàn trả lại công nợ học phí ngay sau khi hủy lớp thành công"
    
    condition: true
    
    postUpdates:
       subject.tuitionDebt SUB_ASSIGN object.course.tuitionFee
}

policy P12_AuditAndTrace_Post {
    type: POST_UPDATE
    targetAction: ANY
    effect: PERMIT
    priority: 1
    description: "Ghi dấu vết Audit Log cho bất kỳ request nào"
    
    condition: true
    
    postUpdates:
       create AuditLog(request.id, subject.studentId, object.classId, request.decision, request.failedPolicyCodes)
}
