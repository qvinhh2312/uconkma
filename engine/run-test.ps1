param([string]$id = "all")

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$mvn = Join-Path $scriptRoot "apache-maven-3.9.6\bin\mvn.cmd"
$cls = "UconEngineApplicationTests"

$cases = @{
    "T01" = @{
        Method = "test01_RegisterSuccess_UpdatesStateBillingTransactionAndAudit"
        Title = "Dang ky hoc phan thanh cong"
        Goal = "Chung minh request REGISTER hop le duoc permit va post-update chay day du."
        Phase = "PRE -> ONGOING -> POST_UPDATE"
        Policies = "P11, P12"
        Checks = @(
            "Gui request REGISTER hop le cho SV001 vao lop CS102_01"
            "Kiem tra enrolled tang len 5"
            "Kiem tra currentCredits tang len 4"
            "Kiem tra tuitionDebt tang len 4000000"
            "Kiem tra Registration duoc tao"
            "Kiem tra Audit log ghi ALLOW"
        )
        Result = @(
            "Request duoc cho phep"
            "State hoc tap va hoc phi duoc cap nhat"
            "Registration va audit log duoc tao"
        )
    }
    "T02" = @{
        Method = "test02_RegisterDenied_WhenTuitionNotPaid"
        Title = "Tu choi dang ky khi chua dong hoc phi"
        Goal = "Chung minh UCON pre-authorization chan request truoc khi hanh dong xay ra."
        Phase = "PRE_AUTHORIZATION"
        Policies = "P01"
        Checks = @(
            "Tao sinh vien chua dong hoc phi"
            "Gui request REGISTER vao lop mo"
            "Kiem tra response tra ve TUITION_NOT_PAID"
            "Kiem tra khong tao Registration"
            "Kiem tra Audit log ghi DENY"
        )
        Result = @(
            "Request bi chan tai pha PRE_AUTHORIZATION"
            "Khong co cap nhat state nao xay ra"
            "Audit log van duoc ghi de truy vet"
        )
    }
    "T03" = @{
        Method = "test03_RegisterDenied_OutsideTransactionWindow"
        Title = "Tu choi dang ky ngoai khung thoi gian giao dich"
        Goal = "Chung minh policy moi truong gioi han hanh dong theo pha va thoi gian hop le."
        Phase = "PRE_AUTHORIZATION"
        Policies = "P02"
        Checks = @(
            "Dung environment co registrationPhase khong hop le"
            "Dat currentDateTime nam ngoai khoang openTime-closeTime"
            "Danh gia PRE_AUTHORIZATION truc tiep tai PolicyEngine"
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
        Goal = "Chung minh ongoing-authorization cua UCON khi state doi sau PRE."
        Phase = "PRE_AUTHORIZATION -> ONGOING_AUTHORIZATION"
        Policies = "P03, P09"
        Checks = @(
            "Danh gia PRE khi lop dang OPEN va request hop le"
            "Doi trang thai lop sang LOCKED truoc commit"
            "Danh gia lai ONGOING_AUTHORIZATION"
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
        Phase = "PRE_AUTHORIZATION"
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
        Phase = "PRE_AUTHORIZATION"
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
        Phase = "PRE_AUTHORIZATION"
        Policies = "P06"
        Checks = @(
            "Xoa danh sach mon da hoan thanh cua sinh vien"
            "Gui request dang ky mon yeu cau CS101"
            "Kiem tra response tra ve PREREQUISITE_NOT_MET"
        )
        Result = @(
            "Sinh vien khong du dieu kien tien quyet"
            "Request bi tu choi tai PRE_AUTHORIZATION"
        )
    }
    "T08" = @{
        Method = "test08_RegisterDenied_WhenScheduleConflicts"
        Title = "Tu choi dang ky khi trung lich hoc"
        Goal = "Chung minh policy conflict lich hoc ngan dang ky lop bi chong lich."
        Phase = "PRE_AUTHORIZATION"
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
        Goal = "Chung minh ONGOING_AUTHORIZATION co the dung request dua tren trang thai hoc vu."
        Phase = "ONGOING_AUTHORIZATION"
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
        Goal = "Chung minh ONGOING_AUTHORIZATION ket hop optimistic locking de chong race condition."
        Phase = "ONGOING_AUTHORIZATION"
        Policies = "P08"
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
        Goal = "Chung minh UCON re-check lien tuc gan commit va chong mutation khi he thong bao tri."
        Phase = "PRE_AUTHORIZATION -> ONGOING_AUTHORIZATION"
        Policies = "P13A, P13"
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
        Goal = "Chung minh post-update co the cap nhat va dao nguoc trang thai sau khi huy lop."
        Phase = "POST_UPDATE"
        Policies = "P14, P16"
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
        Method = "test13_ValidatorRejectsRequestManagedFieldUpdates"
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
    "T14" = @{
        Method = "test14_ValidatorRejectsMalformedAuditLogStatements"
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
    "T15" = @{
        Method = "test15_PolicyDecisionPointFailsFast_WhenValidationFails"
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
    "T16" = @{
        Method = "test16_ValidatorRejectsInvalidPath_Arity_AndPhase"
        Title = "Validator chan path, arity va phase khong hop le"
        Goal = "Chung minh semantic validator bao ve model khoi cac loi path typo va function call sai."
        Phase = "MODEL VALIDATION"
        Policies = "Validator"
        Checks = @(
            "Sua VariableAccess thanh SUBJECT.maxCreditEffecitve"
            "Them mot doi so du vao function isEmpty"
            "Dat function checkExistsRegistration vao POST_UPDATE"
            "Kiem tra validator bat duoc ca 3 nhom loi"
        )
        Result = @(
            "Path sai bi chan som"
            "Function call sai arity bi chan"
            "Function dung sai phase bi chan"
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

function Shorten-Text($text, $maxLength) {
    if ($null -eq $text) {
        return ""
    }
    if ($text.Length -le $maxLength) {
        return $text
    }
    return $text.Substring(0, $maxLength - 3) + "..."
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
        Select-Object `
            @{Name="Test"; Expression={$_.TestCase}}, `
            @{Name="Pha"; Expression={Shorten-Text $_.PhaUCON 28}}, `
            @{Name="Policy"; Expression={Shorten-Text $_.PolicyLienQuan 18}}, `
            @{Name="Method"; Expression={Shorten-Text $_.TestMethod 42}} |
        Format-Table -AutoSize

    Write-Host ""
    Write-Host "[BANG 2] POLICY -> TEST MAPPING" -ForegroundColor Cyan
    $policyRows |
        Sort-Object Ma |
        Select-Object `
            @{Name="Policy"; Expression={$_.Ma}}, `
            @{Name="Test"; Expression={$_.TestCase}}, `
            @{Name="Pha"; Expression={Shorten-Text $_.PhaUCON 28}}, `
            @{Name="Method"; Expression={Shorten-Text $_.TestMethod 42}} |
        Format-Table -AutoSize

    Write-Host ""
    Write-Host "[BANG 3] NOI DUNG KIEM THU DAY DU" -ForegroundColor Cyan
    foreach ($row in ($policyRows | Sort-Object Ma)) {
        $case = $cases[$row.TestCase]
        Write-Host ""
        Write-Host "$($row.Ma) -> $($row.TestCase)" -ForegroundColor Green
        Write-Host "  Tieu de      : $($row.TieuDe)" -ForegroundColor Gray
        Write-Host "  Test method  : $($row.TestMethod)" -ForegroundColor Gray
        Write-Host "  Pha UCON     : $($row.PhaUCON)" -ForegroundColor Gray
        Write-Host "  Muc tieu     : $($row.MucTieu)" -ForegroundColor Gray
        Write-Host "  Chinh sach   : $($row.PolicyLienQuan)" -ForegroundColor Gray
        Write-Host "  Kich ban test:" -ForegroundColor Yellow
        foreach ($check in $case.Checks) {
            Write-Host "    - $check" -ForegroundColor Gray
        }
        Write-Host "  Ket qua mong doi:" -ForegroundColor Magenta
        foreach ($result in $case.Result) {
            Write-Host "    - $result" -ForegroundColor Gray
        }
    }

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
    Write-Host "Tong quan: Bo 16 test bao phu business policy, concurrency va validator." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "[RUNNING CHECK]" -ForegroundColor Green
    Write-Bullets "-" @(
        "Core business rules cho REGISTER va DROP"
        "Ongoing authorization va race condition"
        "Semantic validator va fail-fast startup"
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
            "Toan bo policy engine van hoat dong dung"
            "Khong co regression trong business logic va validator"
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
        Write-Host "[RESULT KHI PASS]" -ForegroundColor Magenta
        Write-Bullets "-" $case.Result "Gray"
    }
    else {
        Write-Host "[RESULT KHI FAIL]" -ForegroundColor Red
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
    Write-Host "  T01..T16"
    Write-Host "  P01..P16"
    Write-Host "  REPORT    (xem bang tong hop test/policy)"
    Write-Host "  all       (chay toan bo 16 tests)"
}
