// pre authorization

policy P01_TuitionPaid_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 100
    description: "Chi cho phep SV da hoan tat hoc phi"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "TUITION_NOT_PAID"

    condition: subject.tuitionPaid == true
}

policy P13a_EmergencyMaintenance_Pre {
    type: PRE_AUTHORIZATION
    targetAction: ANY
    effect: PERMIT
    priority: 95
    description: "Chi cho giao dich khi he thong khong o trang thai bao tri"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "SYSTEM_UNDER_MAINTENANCE"

    condition: environment.isMaintenance == false
}

policy P02_TransactionWindow_Pre {
    type: PRE_AUTHORIZATION
    targetAction: ANY
    effect: PERMIT
    priority: 90
    description: "Chi cho giao dich trong dot va gio hop le"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "OUTSIDE_TRANSACTION_WINDOW"

    condition: environment.registrationPhase IN ["NORMAL", "LATE"]
               AND environment.currentDateTime >= environment.openTime
               AND environment.currentDateTime <= environment.closeTime
}

policy P03_ClassStatusOpen_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 80
    description: "Chi lop dang mo thuc su moi duoc dang ky"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "CLASS_NOT_OPEN"

    condition: object.status == "OPEN"
}

policy P04_NotAlreadyRegistered_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 70
    description: "Khong cho dang ky trung cung lop"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "ALREADY_REGISTERED"

    condition: NOT checkExistsRegistration(subject.studentId, object.classId, environment.semester)
}

policy P16_DropOnlyIfRegistered_Pre {
    type: PRE_AUTHORIZATION
    targetAction: DROP
    effect: PERMIT
    priority: 65
    description: "Chi cho huy lop khi SV da co giao dich dang ky hop le"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "NOT_REGISTERED"

    condition: checkExistsRegistration(subject.studentId, object.classId, environment.semester)
}

policy P05_CreditLimit_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 60
    description: "Khong vuot tran han muc tin chi thuc te"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "CREDIT_LIMIT_EXCEEDED"

    condition: (subject.currentCredits + object.course.credits) <= subject.maxCreditsEffective
}

policy P06_Prerequisite_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 50
    description: "Dam bao da hoan tat mon hoc tien quyet"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "PREREQUISITE_NOT_MET"

    condition: object.course.prerequisites SUBSET_OF subject.completedCourses
}

policy P07_ScheduleConflict_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 40
    description: "Tranh trung lich hoc voi cac mon da chon"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "SCHEDULE_CONFLICT"

    condition: NOT (object.scheduleSlots OVERLAPS subject.registeredScheduleSlots)
}

// ongoing

policy P08_CapacityRecheck_On {
    type: ONGOING_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 30
    description: "Chong race condition o slot cuoi cung"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "CLASS_FULL_ON_COMMIT"

    condition: object.enrolled < object.capacity
}

policy P09_ClassStatusRecheck_On {
    type: ONGOING_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 20
    description: "Kiem tra lai trang thai lop phong khi Admin khoa dot xuat"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "CLASS_STATUS_CHANGED"

    condition: object.status == "OPEN"
}

policy P10_StudentHoldRecheck_On {
    type: ONGOING_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 10
    description: "Kiem tra tinh trang hold cua SV truoc commit dang ky"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "STUDENT_ON_HOLD"

    condition: isEmpty(subject.holds)
}

policy P13_EmergencyMaintenance_On {
    type: ONGOING_AUTHORIZATION
    targetAction: ANY
    effect: PERMIT
    priority: 50
    description: "Ngat giao dich dang lo lung neu Admin kich hoat bao tri khan cap"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "SYSTEM_UNDER_MAINTENANCE"

    condition: environment.isMaintenance == false
}

// post update

policy P11_RegisterStateUpdate_Post {
    type: POST_UPDATE
    targetAction: REGISTER
    effect: PERMIT
    priority: 8
    description: "Commit dang ky: transaction, state subject object, tuition debt"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: MUTATION

    condition: true

    postUpdates:
       create Transaction(subject.studentId, object.classId, environment.semester, "REGISTER")
       object.enrolled ADD_ASSIGN 1
       subject.currentCredits ADD_ASSIGN object.course.credits
       subject.registeredScheduleSlots APPEND object.scheduleSlots
       subject.registeredClassIds APPEND object.classId
       subject.tuitionDebt ADD_ASSIGN object.course.tuitionFee
}

policy P14_DropStateRevert_Post {
    type: POST_UPDATE
    targetAction: DROP
    effect: PERMIT
    priority: 8
    description: "Commit huy lop: xoa transaction, hoan state va cap nhat settlement"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: MUTATION

    condition: true

    postUpdates:
       delete Transaction(subject.studentId, object.classId, environment.semester)
       object.enrolled SUB_ASSIGN 1
       subject.currentCredits SUB_ASSIGN object.course.credits
       subject.registeredScheduleSlots REMOVE object.scheduleSlots
       subject.registeredClassIds REMOVE object.classId
       subject.tuitionDebt SUB_ASSIGN object.course.tuitionFee
}

policy P12_AuditAndTrace_Post {
    type: POST_UPDATE
    targetAction: ANY
    effect: PERMIT
    priority: 1
    description: "Ghi audit log cho moi request"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: TRACE

    condition: true

    postUpdates:
       create AuditLog(request.requestId, subject.studentId, object.classId, request.decision, request.failedPolicyCodes)
}
