// PRE phase

policy P01_TuitionPaid_PreA0 {
    predicate: AUTHORIZATION
    phase: PRE
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 100
    description: "Chi cho phep SV da hoan tat hoc phi"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "TUITION_NOT_PAID"

    condition: subject.tuitionPaid == true
}

policy P13a_EmergencyMaintenance_PreC0 {
    predicate: CONDITION
    phase: PRE
    updateTiming: NONE
    targetAction: ANY
    effect: PERMIT
    priority: 95
    description: "Chi cho giao dich khi he thong khong o trang thai bao tri"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "SYSTEM_UNDER_MAINTENANCE"

    condition: environment.isMaintenance == false
}

policy P02_TransactionWindow_PreC0 {
    predicate: CONDITION
    phase: PRE
    updateTiming: NONE
    targetAction: ANY
    effect: PERMIT
    priority: 90
    description: "Chi cho giao dich trong dot va gio hop le"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "OUTSIDE_TRANSACTION_WINDOW"

    condition: environment.registrationPhase IN ["NORMAL", "LATE"]
               AND environment.currentDateTime >= environment.openTime
               AND environment.currentDateTime <= environment.closeTime
}

policy P03_ClassStatusOpen_PreA0 {
    predicate: AUTHORIZATION
    phase: PRE
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 80
    description: "Chi lop dang mo thuc su moi duoc dang ky"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "CLASS_NOT_OPEN"

    condition: object.status == "OPEN"
}

policy P04_NotAlreadyRegistered_PreA0 {
    predicate: AUTHORIZATION
    phase: PRE
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 70
    description: "Khong cho dang ky trung cung lop"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "ALREADY_REGISTERED"

    condition: NOT checkExistsRegistration(subject.studentId, object.classId, environment.semester)
}

policy P16_DropOnlyIfRegistered_PreA0 {
    predicate: AUTHORIZATION
    phase: PRE
    updateTiming: NONE
    targetAction: DROP
    effect: PERMIT
    priority: 65
    description: "Chi cho huy lop khi SV da co giao dich dang ky hop le"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "NOT_REGISTERED"

    condition: checkExistsRegistration(subject.studentId, object.classId, environment.semester)
}

policy P05_CreditLimit_PreA0 {
    predicate: AUTHORIZATION
    phase: PRE
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 60
    description: "Khong vuot tran han muc tin chi thuc te"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "CREDIT_LIMIT_EXCEEDED"

    condition: (subject.currentCredits + object.course.credits) <= subject.maxCreditsEffective
}

policy P06_Prerequisite_PreA0 {
    predicate: AUTHORIZATION
    phase: PRE
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 50
    description: "Dam bao da hoan tat mon hoc tien quyet"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "PREREQUISITE_NOT_MET"

    condition: object.course.prerequisites SUBSET_OF subject.completedCourses
}

policy P17_AgreeRegistrationRule_PreB0 {
    predicate: OBLIGATION
    phase: PRE
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 45
    description: "Sinh vien phai xac nhan quy che dang ky truoc khi gui request"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "REGULATION_NOT_CONFIRMED"

    condition: request.confirmedRegistrationRule == true
}

policy P07_ScheduleConflict_PreA0 {
    predicate: AUTHORIZATION
    phase: PRE
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 40
    description: "Tranh trung lich hoc voi cac mon da chon"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "SCHEDULE_CONFLICT"

    condition: NOT (object.scheduleSlots OVERLAPS subject.registeredScheduleSlots)
}

policy P18_AdminOverrideReason_PreB0 {
    predicate: OBLIGATION
    phase: PRE
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 35
    description: "Neu dung override hoc vu thi phai co ly do"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "OVERRIDE_REASON_REQUIRED"

    condition: request.adminOverride == false
               OR request.overrideReason != ""
}

policy P19_RegisterAttempt_PreA1 {
    predicate: AUTHORIZATION
    phase: PRE
    updateTiming: PRE
    targetAction: REGISTER
    effect: PERMIT
    priority: 5
    description: "Tang so lan thu dang ky cho moi request hop le o PRE"
    subjectType: "Student"
    objectType: "ClassSection"

    condition: true

    preUpdates:
       subject.registerAttemptCount ADD_ASSIGN 1
}

// ONGOING phase

policy P13_EmergencyMaintenance_OnC0 {
    predicate: CONDITION
    phase: ONGOING
    updateTiming: NONE
    targetAction: ANY
    effect: PERMIT
    priority: 50
    description: "Ngat giao dich dang lo lung neu Admin kich hoat bao tri khan cap"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "SYSTEM_UNDER_MAINTENANCE"

    condition: environment.isMaintenance == false
}

policy P08_CapacityRecheck_OnA0 {
    predicate: AUTHORIZATION
    phase: ONGOING
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 30
    description: "Chong race condition o slot cuoi cung"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "CLASS_FULL_ON_COMMIT"

    condition: object.enrolled < object.capacity
}

policy P20_ReserveSeat_OnA2 {
    predicate: AUTHORIZATION
    phase: ONGOING
    updateTiming: ONGOING
    targetAction: REGISTER
    effect: PERMIT
    priority: 25
    description: "Giu tam mot cho truoc khi commit dang ky"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "NO_SEAT_TO_RESERVE"

    condition: (object.enrolled + object.reservedSeats) < object.capacity

    ongoingUpdates:
       object.reservedSeats ADD_ASSIGN 1

    rollbackUpdates:
       object.reservedSeats SUB_ASSIGN 1
}

policy P09_ClassStatusRecheck_OnA0 {
    predicate: AUTHORIZATION
    phase: ONGOING
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 20
    description: "Kiem tra lai trang thai lop phong khi Admin khoa dot xuat"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "CLASS_STATUS_CHANGED"

    condition: object.status == "OPEN"
}

policy P10_StudentHoldRecheck_OnA0 {
    predicate: AUTHORIZATION
    phase: ONGOING
    updateTiming: NONE
    targetAction: REGISTER
    effect: PERMIT
    priority: 10
    description: "Kiem tra tinh trang hold cua SV truoc commit dang ky"
    subjectType: "Student"
    objectType: "ClassSection"
    denyReason: "STUDENT_ON_HOLD"

    condition: isEmpty(subject.holds)
}

// POST phase

policy P11_RegisterStateUpdate_PostA3 {
    predicate: AUTHORIZATION
    phase: POST
    updateTiming: POST
    targetAction: REGISTER
    effect: PERMIT
    priority: 8
    description: "Commit dang ky: transaction, state subject object, tuition debt"
    subjectType: "Student"
    objectType: "ClassSection"

    condition: true

    postUpdates:
       create Transaction(subject.studentId, object.classId, environment.semester, "REGISTER")
       object.reservedSeats SUB_ASSIGN 1
       object.enrolled ADD_ASSIGN 1
       subject.currentCredits ADD_ASSIGN object.course.credits
       subject.registeredScheduleSlots APPEND object.scheduleSlots
       subject.registeredClassIds APPEND object.classId
       subject.tuitionDebt ADD_ASSIGN object.course.tuitionFee
}

policy P14_DropStateRevert_PostA3 {
    predicate: AUTHORIZATION
    phase: POST
    updateTiming: POST
    targetAction: DROP
    effect: PERMIT
    priority: 8
    description: "Commit huy lop: xoa transaction, hoan state va cap nhat settlement"
    subjectType: "Student"
    objectType: "ClassSection"

    condition: true

    postUpdates:
       delete Transaction(subject.studentId, object.classId, environment.semester)
       object.enrolled SUB_ASSIGN 1
       subject.currentCredits SUB_ASSIGN object.course.credits
       subject.registeredScheduleSlots REMOVE object.scheduleSlots
       subject.registeredClassIds REMOVE object.classId
       subject.tuitionDebt SUB_ASSIGN object.course.tuitionFee
}

policy P12_AuditAndTrace_PostB3 {
    predicate: OBLIGATION
    phase: POST
    updateTiming: POST
    targetAction: ANY
    effect: PERMIT
    priority: 1
    description: "Ghi audit log cho moi request"
    subjectType: "Student"
    objectType: "ClassSection"

    condition: true

    postUpdates:
       create AuditLog(request.requestId, subject.studentId, object.classId, request.decision, request.failedPolicyCodes)
}
