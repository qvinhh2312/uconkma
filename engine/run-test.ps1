param([string]$id = "all")

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$mvn = Join-Path $scriptRoot "apache-maven-3.9.6\bin\mvn.cmd"
$cls = "UconEngineApplicationTests"

$cases = @{
    "T01" = @{
        Method = "test01_RegisterSuccess_UpdatesStateBillingTransactionAndAudit"
        Title = "Dang ky hoc phan thanh cong"
        Goal = "Chung minh request REGISTER hop le di qua du PRE, ONGOING, POST va thuc thi day du updates/obligations."
        Phase = "PRE -> ONGOING -> POST"
        Policies = "P19, P20, P11, P12"
        Checks = @(
            "Gui request REGISTER hop le cho SV001 vao lop CS102_01"
            "Kiem tra registerAttemptCount tang len 1 sau PRE update"
            "Kiem tra enrolled tang len 5"
            "Kiem tra currentCredits tang len 4"
            "Kiem tra tuitionDebt tang len 4000000"
            "Kiem tra Registration duoc tao"
            "Kiem tra Audit log ghi ALLOW"
        )
        Result = @(
            "Request duoc cho phep"
            "PRE update va ONGOING update duoc ap dung an toan"
            "State hoc tap va hoc phi duoc cap nhat"
            "Registration va audit log duoc tao"
        )
    }
    "T02" = @{
        Method = "test02_RegisterDenied_WhenTuitionNotPaid"
        Title = "Tu choi dang ky khi chua dong hoc phi"
        Goal = "Chung minh UCON chan request o pha PRE truoc khi hanh dong xay ra."
        Phase = "PRE"
        Policies = "P01"
        Checks = @(
            "Tao sinh vien chua dong hoc phi"
            "Gui request REGISTER vao lop mo"
            "Kiem tra response tra ve TUITION_NOT_PAID"
            "Kiem tra khong tao Registration"
            "Kiem tra Audit log ghi DENY"
        )
        Result = @(
            "Request bi chan tai pha PRE"
            "Khong co cap nhat state nao xay ra"
            "Audit log van duoc ghi de truy vet"
        )
    }
    "T03" = @{
        Method = "test03_RegisterDenied_OutsideTransactionWindow"
        Title = "Tu choi dang ky ngoai khung thoi gian giao dich"
        Goal = "Chung minh policy moi truong gioi han hanh dong theo pha va thoi gian hop le."
        Phase = "PRE"
        Policies = "P02"
        Checks = @(
            "Dung environment co registrationPhase khong hop le"
            "Dat currentDateTime nam ngoai khoang openTime-closeTime"
            "Danh gia PRE truc tiep tai PolicyEngine"
            "Kiem tra failed code la OUTSIDE_TRANSACTION_WINDOW"
        )
        Result = @(
            "Request khong duoc phep thuc hien ngoai transaction window"
            "Policy moi truong duoc thuc thi dung"
        )
    }
    "T04" = @{
        Method = "test04_RegisterDenied_WhenClassLocksBetweenPhases"
        Title = "Tu choi dang ky khi lop bi khoa giua hai pha"
        Goal = "Chung minh ongoing check cua UCON khi state doi sau PRE."
        Phase = "PRE -> ONGOING"
        Policies = "P03, P09"
        Checks = @(
            "Danh gia PRE khi lop dang OPEN va request hop le"
            "Doi trang thai lop sang LOCKED truoc commit"
            "Danh gia lai ONGOING"
            "Kiem tra failed code la CLASS_STATUS_CHANGED"
        )
        Result = @(
            "PRE pass nhung ONGOING fail"
            "Quyen tam thoi bi thu hoi do state thay doi"
        )
    }
    "T05" = @{
        Method = "test05_RegisterDenied_WhenRepositoryShowsExistingRegistration"
        Title = "Tu choi dang ky trung theo repository"
        Goal = "Chung minh duplicate check dua tren RegistrationRepository."
        Phase = "PRE"
        Policies = "P04"
        Checks = @(
            "Tao san mot Registration trong repository"
            "Dam bao Student khong can co registeredClassIds"
            "Gui lai request REGISTER cung class"
            "Kiem tra response tra ve ALREADY_REGISTERED"
        )
        Result = @(
            "Duplicate duoc phat hien bang repository"
            "Logic policy khong phu thuoc state CSV trong Student"
        )
    }
    "T06" = @{
        Method = "test06_RegisterDenied_WhenCreditLimitExceeded"
        Title = "Tu choi dang ky khi vuot tran tin chi"
        Goal = "Chung minh policy han muc tin chi duoc kiem tra truoc khi dang ky."
        Phase = "PRE"
        Policies = "P05"
        Checks = @(
            "Dat currentCredits = 12 cho sinh vien"
            "Gui request dang ky mon 4 tin chi"
            "Kiem tra tong tin chi vuot maxCreditsEffective"
            "Kiem tra response tra ve CREDIT_LIMIT_EXCEEDED"
        )
        Result = @(
            "Dang ky bi chan do vuot tran tin chi"
            "Khong co state nao bi thay doi"
        )
    }
    "T07" = @{
        Method = "test07_RegisterDenied_WhenPrerequisiteMissing"
        Title = "Tu choi dang ky khi thieu mon tien quyet"
        Goal = "Chung minh policy hoc vu kiem tra prerequisite truoc khi cho dang ky."
        Phase = "PRE"
        Policies = "P06"
        Checks = @(
            "Xoa danh sach mon da hoan thanh cua sinh vien"
            "Gui request dang ky mon yeu cau CS101"
            "Kiem tra response tra ve PREREQUISITE_NOT_MET"
        )
        Result = @(
            "Sinh vien khong du dieu kien tien quyet"
            "Request bi tu choi tai PRE"
        )
    }
    "T08" = @{
        Method = "test08_RegisterDenied_WhenScheduleConflicts"
        Title = "Tu choi dang ky khi trung lich hoc"
        Goal = "Chung minh policy conflict lich hoc ngan dang ky lop bi chong lich."
        Phase = "PRE"
        Policies = "P07"
        Checks = @(
            "Gan registeredScheduleSlots trung voi lich lop can dang ky"
            "Gui request REGISTER"
            "Kiem tra response tra ve SCHEDULE_CONFLICT"
        )
        Result = @(
            "Request bi tu choi do trung lich"
            "Lich hoc hien tai cua sinh vien duoc bao toan"
        )
    }
    "T09" = @{
        Method = "test09_RegisterDenied_WhenStudentOnHold"
        Title = "Tu choi dang ky khi sinh vien dang bi hold"
        Goal = "Chung minh ONGOING co the dung request dua tren trang thai hoc vu."
        Phase = "ONGOING"
        Policies = "P10"
        Checks = @(
            "Gan hold DISCIPLINARY_HOLD cho sinh vien"
            "Gui request REGISTER"
            "Kiem tra response tra ve STUDENT_ON_HOLD"
            "Kiem tra khong tao Registration"
        )
        Result = @(
            "Request bi chan do sinh vien dang co hold"
            "Khong co post-update nao duoc thuc thi"
        )
    }
    "T10" = @{
        Method = "test10_OnlyOneStudentCanClaimLastSeat"
        Title = "Chi mot sinh vien co the lay suat cuoi cung"
        Goal = "Chung minh ONGOING ket hop reserved seat va optimistic locking de chong race condition."
        Phase = "ONGOING"
        Policies = "P08, P20"
        Checks = @(
            "Tao 2 thread dang ky dong thoi cho 1 cho cuoi cung"
            "Cho hai request bat dau cung luc"
            "Kiem tra chi 1 request thanh cong va 1 request that bai"
            "Kiem tra enrolled khong vuot capacity"
        )
        Result = @(
            "He thong giu duoc bat bien suc chua lop"
            "Khong co tinh trang overbook"
        )
    }
    "T11" = @{
        Method = "test11_MaintenanceBlocksRequest_AndPreservesState"
        Title = "Maintenance chan request va giu nguyen state"
        Goal = "Chung minh UCON re-check gan commit va chong mutation khi he thong bao tri."
        Phase = "PRE -> ONGOING"
        Policies = "P13a, P13"
        Checks = @(
            "Danh gia PRE khi maintenance = false va request hop le"
            "Danh gia ONGOING khi maintenance = true"
            "Gui request qua controller khi maintenance dang bat"
            "Kiem tra khong co Registration, currentCredits, enrolled bi thay doi"
            "Kiem tra Audit log ghi DENY"
        )
        Result = @(
            "Request bi chan boi maintenance"
            "Toan bo state duoc bao toan"
            "Audit log van duoc ghi lai de truy vet"
        )
    }
    "T12" = @{
        Method = "test12_DropRestoresState_RemovesTransaction_AndRefundsDebt"
        Title = "DROP hoan tra state va hoan no hoc phi"
        Goal = "Chung minh DROP di qua PRE va POST de hoan tac trang thai sau khi huy lop."
        Phase = "PRE -> POST"
        Policies = "P16, P14, P12"
        Checks = @(
            "Dang ky thanh cong mot lop truoc khi DROP"
            "Dat tuitionPaid = false de xac nhan DROP van duoc phep"
            "Gui request DROP"
            "Kiem tra Registration bi xoa"
            "Kiem tra currentCredits, tuitionDebt, enrolled duoc hoan tra"
            "Kiem tra lich hoc va classId da bi go bo"
        )
        Result = @(
            "DROP thanh cong va state duoc phuc hoi"
            "Cong no hoc phi duoc tru lai"
            "Registration bi xoa khoi he thong"
        )
    }
    "T13" = @{
        Method = "test13_RegisterDenied_WhenRegulationNotConfirmed"
        Title = "Tu choi dang ky khi chua xac nhan quy che"
        Goal = "Chung minh obligation PRE buoc sinh vien xac nhan quy che truoc khi dang ky."
        Phase = "PRE"
        Policies = "P17"
        Checks = @(
            "Tao request REGISTER hop le"
            "Dat confirmedRegistrationRule = false"
            "Gui request qua controller"
            "Kiem tra response tra ve REGULATION_NOT_CONFIRMED"
        )
        Result = @(
            "Request bi chan boi obligation PRE"
            "He thong bat buoc nguoi dung xac nhan quy che dang ky"
        )
    }
    "T14" = @{
        Method = "test14_RegisterDenied_WhenOverrideReasonMissing"
        Title = "Tu choi dang ky khi override khong co ly do"
        Goal = "Chung minh obligation PRE yeu cau ly do khi request dung co che override hoc vu."
        Phase = "PRE"
        Policies = "P18"
        Checks = @(
            "Tao request REGISTER hop le"
            "Dat adminOverride = true va overrideReason rong"
            "Gui request qua controller"
            "Kiem tra response tra ve OVERRIDE_REASON_REQUIRED"
        )
        Result = @(
            "Request bi chan boi obligation PRE"
            "Override hoc vu phai co giai trinh hop le"
        )
    }
    "T15" = @{
        Method = "test15_ValidatorRejectsRequestManagedFieldUpdates"
        Title = "Validator chan cap nhat vao field duoc quan ly boi request"
        Goal = "Chung minh semantic validation bao ve model khoi update khong hop le."
        Phase = "MODEL VALIDATION"
        Policies = "Validator"
        Checks = @(
            "Sao chep policy model hien tai"
            "Sua target cua UpdateStatement thanh REQUEST.decision"
            "Chay semantic validator"
            "Kiem tra validator nem loi updates REQUEST path"
        )
        Result = @(
            "Policy model sai bi chan truoc khi runtime"
            "REQUEST duoc xem la immutable trong update semantics"
        )
    }
    "T16" = @{
        Method = "test16_ValidatorRejectsMalformedAuditLogStatements"
        Title = "Validator chan cau truc AuditLog sai"
        Goal = "Chung minh statement schema duoc kiem tra truoc khi policy duoc nap vao PDP."
        Phase = "MODEL VALIDATION"
        Policies = "Validator"
        Checks = @(
            "Sao chep policy model hien tai"
            "Xoa bot mot doi so cua AuditLogStatement"
            "Chay semantic validator"
            "Kiem tra loi create AuditLog(...) must have exactly 5 arguments"
        )
        Result = @(
            "Audit statement sai schema bi tu choi"
            "PDP chi chap nhan policy model hop le"
        )
    }
    "T17" = @{
        Method = "test17_PolicyDecisionPointFailsFast_WhenValidationFails"
        Title = "PDP fail-fast khi semantic validation that bai"
        Goal = "Chung minh he thong khong khoi dong voi policy model khong dang tin cay."
        Phase = "PDP STARTUP"
        Policies = "PolicyDecisionPoint"
        Checks = @(
            "Tao PolicyDecisionPoint voi validator gia lap luon nem loi"
            "Goi ham init"
            "Kiem tra PDP startup failed va co root cause ro rang"
        )
        Result = @(
            "PDP dung khoi dong ngay khi validation loi"
            "He thong tranh trang thai thuc thi voi policy model khong hop le"
        )
    }
    "T18" = @{
        Method = "test18_ValidatorRejectsInvalidPath_Arity_AndPhase"
        Title = "Validator chan path, arity va phase khong hop le"
        Goal = "Chung minh semantic validator bao ve model khoi cac loi path typo va function call sai."
        Phase = "MODEL VALIDATION"
        Policies = "Validator"
        Checks = @(
            "Sua VariableAccess thanh SUBJECT.maxCreditEffecitve"
            "Them mot doi so du vao function isEmpty"
            "Dat function checkExistsRegistration vao POST"
            "Kiem tra validator bat duoc ca 3 nhom loi"
        )
        Result = @(
            "Path sai bi chan som"
            "Function call sai arity bi chan"
            "Function dung sai phase bi chan"
        )
    }
    "T19" = @{
        Method = "test19_PolicyValidatorRejectsEnvironmentImmutableUpdate"
        Title = "PolicyValidator chan cap nhat vao ENVIRONMENT immutable"
        Goal = "Chung minh attribute-schema.yml va PolicyValidator cung bao ve mutability rules."
        Phase = "MODEL VALIDATION"
        Policies = "PolicyValidator, attribute-schema.yml"
        Checks = @(
            "Sao chep policy model hien tai"
            "Sua target cua UpdateStatement thanh ENVIRONMENT.isMaintenance"
            "Chay PolicyValidator"
            "Kiem tra loi immutable ENVIRONMENT update"
        )
        Result = @(
            "Environment duoc xem la immutable"
            "Policy sai bi chan boi tang schema + validator"
        )
    }
    "T20" = @{
        Method = "test20_PolicyAnalyzerWarnsWhenAuditTraceMissing"
        Title = "PolicyAnalyzer canh bao khi policy audit bi thieu"
        Goal = "Chung minh he thong khong chi validate dung/sai ma con phan tich chat luong policy set."
        Phase = "POLICY ANALYSIS"
        Policies = "PolicyAnalyzer"
        Checks = @(
            "Sao chep policy model hien tai"
            "Xoa policy P12_AuditAndTrace_PostB3"
            "Chay PolicyAnalyzer"
            "Kiem tra warning MISSING_AUDIT"
        )
        Result = @(
            "Analyzer phat hien policy set thieu trace obligation"
            "Co the dung de ho tro bao cao va danh gia chat luong policy"
        )
    }
    "T21" = @{
        Method = "test21_DenyResponseContainsDecisionTrace"
        Title = "Response DENY tra ve decision trace"
        Goal = "Chung minh runtime REST khong chi tra DENY ma con giai thich request fail o dau."
        Phase = "TRACEABILITY"
        Policies = "DecisionTrace"
        Checks = @(
            "Tao sinh vien SV002 chua dong hoc phi"
            "Gui request REGISTER qua controller"
            "Doc response JSON"
            "Kiem tra decisionTrace co phase PRE va failedPolicy P01_TuitionPaid_PreA0"
        )
        Result = @(
            "Response REST co trace giai thich quyet dinh"
            "Nguoi dung co the thay phase va policy gay DENY"
        )
    }
    "T22" = @{
        Method = "test22_DirectOngoingMaintenanceMarksSessionRevoked"
        Title = "UsageSession bi REVOKED khi ongoing condition fail"
        Goal = "Chung minh continuity qua UsageSession khi request dang ACTIVE bi maintenance huy o ONGOING."
        Phase = "ONGOING"
        Policies = "P13, UsageSession"
        Checks = @(
            "Tao UsageSession ACTIVE cho request REGISTER"
            "Danh gia ONGOING CONDITION voi maintenance = true"
            "Mark session thanh REVOKED"
            "Kiem tra status trong repository la REVOKED"
        )
        Result = @(
            "UsageSession luu duoc vong doi quyen su dung"
            "Continuity cua UCON duoc the hien qua session status"
        )
    }
    "T23" = @{
        Method = "test23_Race_10Students_3Slots_PreservesInvariants"
        Title = "Stress race 10 sinh vien tranh 3 cho va giu invariant"
        Goal = "Chung minh stress concurrency khong lam vuot capacity va reservedSeats duoc don sach."
        Phase = "ONGOING / INVARIANT"
        Policies = "P08, P20, InvariantChecker"
        Checks = @(
            "Dat lop con 3 cho trong"
            "Cho 10 sinh vien dang ky dong thoi"
            "Kiem tra tong request xu ly du 10"
            "Kiem tra enrolled nam trong [4,7]"
            "Kiem tra reservedSeats ve 0"
            "Kiem tra registration count = enrolled - 4"
        )
        Result = @(
            "Khong co overbook"
            "Invariant suc chua va reserved seat duoc bao toan duoi stress"
        )
    }
    "T24" = @{
        Method = "test24_ReserveSeatRollback_RestoresReservedSeats"
        Title = "Rollback ongoing-update phuc hoi reservedSeats"
        Goal = "Chung minh onA2 co rollback ro rang cho reserved seat."
        Phase = "ONGOING -> ROLLBACK"
        Policies = "P20, RollbackManager"
        Checks = @(
            "Build ongoing update plan cho request REGISTER"
            "Apply P20 de tang reservedSeats len 1"
            "Build rollback plan va apply rollback"
            "Kiem tra reservedSeats quay ve 0"
        )
        Result = @(
            "Ongoing update co rollback dung nhu dac ta"
            "Reserved seat khong bi leak sau khi huy bo"
        )
    }
    "T25" = @{
        Method = "test25_RegisterDenied_WhenMaxRegisterAttemptsReached"
        Title = "Tu choi dang ky khi vuot gioi han so lan thu"
        Goal = "Chung minh policy su dung P25 de gioi han so lan register attempt trong mot dot."
        Phase = "PRE"
        Policies = "P25"
        Checks = @(
            "Dat registerAttemptCount = 5 cho SV001"
            "Gui request REGISTER moi"
            "Kiem tra response tra ve MAX_REGISTER_ATTEMPTS_EXCEEDED"
            "Kiem tra failedPolicy la P25_MaxRegisterAttempts_PreA0"
        )
        Result = @(
            "Request bi chan do vuot nguong so lan thu"
            "Rang buoc usage-count cua UCON duoc kich hoat"
        )
    }
    "T26" = @{
        Method = "test26_DropDenied_WhenMaxDropTimesReached"
        Title = "Tu choi DROP khi vuot gioi han so lan huy"
        Goal = "Chung minh policy su dung P26 de gioi han so lan DROP trong hoc ky."
        Phase = "PRE"
        Policies = "P26"
        Checks = @(
            "Dang ky thanh cong mot lop de tao state DROP hop le"
            "Dat dropCountForSemester = 2 cho SV001"
            "Gui request DROP"
            "Kiem tra response tra ve MAX_DROP_TIMES_EXCEEDED"
            "Kiem tra failedPolicy la P26_MaxDropTimes_PreA0"
        )
        Result = @(
            "Request DROP bi chan do vuot nguong so lan huy"
            "Rang buoc usage-count cho DROP duoc ap dung"
        )
    }
    "T27" = @{
        Method = "test27_RegisterDenied_WhenSessionLeaseExpiresDuringOngoing"
        Title = "Tu choi request khi ongoing obligation ve session lease that bai"
        Goal = "Chung minh onB0 co the revoke request khi usage session khong con hop le trong luc dang xu ly."
        Phase = "ONGOING"
        Policies = "P27, UsageSession"
        Checks = @(
            "Tao request REGISTER hop le nhung dat sessionLeaseValid = false"
            "Cho request di qua PRE va tao UsageSession ACTIVE"
            "Kiem tra fail o ONGOING OBLIGATION"
            "Kiem tra failedPolicy la P27_SessionLease_OnB0"
            "Kiem tra sessionStatus = REVOKED va khong tao Registration"
        )
        Result = @(
            "Coverage UCON duoc bo sung them onB0"
            "Usage session co the bi revoke do nghia vu ongoing khong con duoc dap ung"
        )
    }
}

$aliases = @{
    "P01" = "T02"
    "P02" = "T03"
    "P03" = "T04"
    "P04" = "T05"
    "P05" = "T06"
    "P06" = "T07"
    "P07" = "T08"
    "P08" = "T10"
    "P09" = "T04"
    "P10" = "T09"
    "P11" = "T01"
    "P12" = "T01"
    "P13" = "T11"
    "P13A" = "T11"
    "P17" = "T13"
    "P18" = "T14"
    "P19" = "T01"
    "P20" = "T01"
    "P21" = "T26"
    "P23" = "T26"
    "P25" = "T25"
    "P26" = "T26"
    "P27" = "T27"
    "P14" = "T12"
    "P16" = "T12"
}

function Write-Section($title, $color = "Cyan") {
    Write-Host ""
    Write-Host $title -ForegroundColor $color
}

function Write-Bullets($prefix, $items, $color = "Gray") {
    foreach ($item in $items) {
        Write-Host "$prefix $item" -ForegroundColor $color
    }
}

function Show-CaseInfo($displayId, $canonicalId, $case) {
    Write-Section "[$displayId]$($case.Title)"
    Write-Host "Muc tieu: $($case.Goal)" -ForegroundColor Yellow
    Write-Host "Pha UCON: $($case.Phase)" -ForegroundColor Yellow
    Write-Host "Policy/Test lien quan: $($case.Policies) / $canonicalId" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "[RUNNING CHECK]" -ForegroundColor Green
    Write-Bullets "-" $case.Checks "Gray"
}

function Run-Maven($arguments) {
    $env:MAVEN_OPTS = "-Dmaven.repo.local=e:\UCON_KMA\.m2\repository"
    $env:MAVEN_USER_HOME = "e:\UCON_KMA\.m2"
    $env:USERPROFILE = "e:\UCON_KMA"
    $env:HOME = "e:\UCON_KMA"
    Push-Location $scriptRoot
    try {
        & $mvn @arguments 2>&1 | ForEach-Object { $_.ToString() }
    }
    finally {
        Pop-Location
    }
}

function Show-MavenSummary($output) {
    $output | Select-String "Running vn\.edu\.kma\.ucon\.engine\.UconEngineApplicationTests|Tests run:|Results:|BUILD SUCCESS|BUILD FAILURE"
}

function New-ReportRow($rowType, $displayId, $caseId, $case) {
    [PSCustomObject]@{
        Loai             = $rowType
        Ma               = $displayId
        TestCase         = $caseId
        TestMethod       = $case.Method
        TieuDe           = $case.Title
        PhaUCON          = $case.Phase
        PolicyLienQuan   = $case.Policies
        MucTieu          = $case.Goal
        KichBanTest      = ($case.Checks -join " | ")
        KetQuaMongDoi    = ($case.Result -join " | ")
        SoBuocKiemTra    = $case.Checks.Count
        SoKetQuaKyVong   = $case.Result.Count
    }
}

function Show-TestReport($cases, $aliases) {
    Write-Section "[REPORT] Bao cao tong hop test policy va UCON"
    Write-Host "Nguon du lieu: run-test.ps1 + UconEngineApplicationTests + ucon_policy.dsl" -ForegroundColor Yellow
    Write-Host "Tong so test case goc: $($cases.Count)" -ForegroundColor Yellow
    Write-Host "Tong so policy alias: $($aliases.Count)" -ForegroundColor Yellow
    Write-Host ""

    $testRows = foreach ($caseId in ($cases.Keys | Sort-Object)) {
        New-ReportRow "TEST_CASE" $caseId $caseId $cases[$caseId]
    }

    $policyRows = foreach ($policyId in ($aliases.Keys | Sort-Object)) {
        $caseId = $aliases[$policyId]
        New-ReportRow "POLICY" $policyId $caseId $cases[$caseId]
    }

    Write-Host "[BANG 1] TEST CASE GOC" -ForegroundColor Cyan
    $testRows |
        Sort-Object TestCase |
        Format-Table TestCase, TestMethod, PhaUCON, PolicyLienQuan, TieuDe -AutoSize -Wrap

    Write-Host ""
    Write-Host "[BANG 2] POLICY -> TEST MAPPING" -ForegroundColor Cyan
    $policyRows |
        Sort-Object Ma |
        Format-Table Ma, TestCase, TestMethod, PhaUCON, TieuDe -AutoSize -Wrap

    Write-Host ""
    Write-Host "[BANG 3] NOI DUNG KIEM THU DAY DU" -ForegroundColor Cyan
    $policyRows |
        Sort-Object Ma |
        Format-Table Ma, TestCase, MucTieu, KichBanTest, KetQuaMongDoi -AutoSize -Wrap

    Write-Host ""
    Write-Host "[TONG KET]" -ForegroundColor Magenta
    Write-Bullets "-" @(
        "Cac kich ban test duoc xay dung tu policy DSL va semantics UCON."
        "Moi policy duoc map toi mot test case cu the hoac mot test case bao phu nhieu policy lien quan."
        "Bang 3 la bang dung de dua vao bao cao hoac thuyet trinh vi co du muc tieu, kich ban va ket qua mong doi."
    ) "Gray"
}

$id = $id.ToUpper()

if ($id -eq "REPORT" -or $id -eq "TABLE") {
    Show-TestReport $cases $aliases
}
elseif ($id -eq "ALL") {
    Write-Section "[ALL] Chay toan bo bo test UCON"
    Write-Host "Tong quan: Bo 27 test bao phu business policy, session, update, validator, concurrency, usage-count, onB0 va trace." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "[RUNNING CHECK]" -ForegroundColor Green
    Write-Bullets "-" @(
        "Core business rules cho REGISTER va DROP"
        "Obligation PRE, ongoing update va race condition"
        "Semantic validator va fail-fast startup"
        "Attribute schema, policy analyzer va decision trace"
    ) "Gray"
    Write-Host ""
    Write-Host "[RUN] Dang thuc thi Maven..." -ForegroundColor Cyan
    $output = Run-Maven @("clean", "test")
    $exitCode = $LASTEXITCODE
    Show-MavenSummary $output
    Write-Host ""
    if ($exitCode -eq 0) {
        Write-Host "[RESULT]" -ForegroundColor Magenta
        Write-Bullets "-" @(
            "Toan bo policy engine van hoat dong dung tren mo hinh PRE/ONGOING/POST moi"
            "Khong co regression trong business logic, validator va trace runtime"
        ) "Gray"
    }
    else {
        Write-Host "[RESULT]" -ForegroundColor Red
        Write-Bullets "-" @(
            "Test khong dat ky vong, can kiem tra policy/test mapping hoac logic enforcement."
        ) "Gray"
        exit $exitCode
    }
}
elseif ($cases.ContainsKey($id)) {
    $case = $cases[$id]
    Show-CaseInfo $id $id $case
    Write-Host ""
    Write-Host "[RUN] Dang thuc thi $($case.Method)..." -ForegroundColor Cyan
    $output = Run-Maven @("test", "-Dtest=${cls}#$($case.Method)")
    $exitCode = $LASTEXITCODE
    Show-MavenSummary $output
    Write-Host ""
    if ($exitCode -eq 0) {
        Write-Host "[RESULT]" -ForegroundColor Magenta
        Write-Bullets "-" $case.Result "Gray"
    }
    else {
        Write-Host "[RESULT]" -ForegroundColor Red
        Write-Bullets "-" @(
            "Test khong dat ky vong, can kiem tra policy/test mapping hoac logic enforcement."
        ) "Gray"
        exit $exitCode
    }
}
elseif ($aliases.ContainsKey($id)) {
    $canonicalId = $aliases[$id]
    $case = $cases[$canonicalId]
    Show-CaseInfo $id $canonicalId $case
    Write-Host ""
    Write-Host "[RUN] Dang thuc thi $($case.Method)..." -ForegroundColor Cyan
    $output = Run-Maven @("test", "-Dtest=${cls}#$($case.Method)")
    $exitCode = $LASTEXITCODE
    Show-MavenSummary $output
    Write-Host ""
    if ($exitCode -eq 0) {
        Write-Host "[RESULT]" -ForegroundColor Magenta
        Write-Bullets "-" $case.Result "Gray"
    }
    else {
        Write-Host "[FAIL]" -ForegroundColor Red
        Write-Bullets "-" @(
            "Test khong dat ky vong, can kiem tra policy/test mapping hoac logic enforcement."
        ) "Gray"
        exit $exitCode
    }
}
else {
    Write-Host "`n[X] Khong tim thay ID '$id'" -ForegroundColor Red
    Write-Host ""
    Write-Host "Cach dung: .\run-test.ps1 <ID>"
    Write-Host ""
    Write-Host "  T01..T27"
    Write-Host "  P01..P27"
    Write-Host "  REPORT    (xem bang tong hop test/policy)"
    Write-Host "  all       (chay toan bo 27 tests)"
}
