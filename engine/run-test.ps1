# ============================================================
# UCON Policy Engine — Test Runner
# Usage: .\run-test.ps1 <ID>
#
# T01 = P01  T02 = P02  ...  T11 = P11+P12
# all        : chạy toàn bộ 11 tests
# ============================================================

param([string]$id = "all")

$mvn = ".\apache-maven-3.9.6\bin\mvn.cmd"
$cls = "UconEngineApplicationTests"

$map = @{

    "T01" = "test02_P01_TuitionNotPaid_ShouldDeny"
    "T02" = "test10_P02_OutsideRegistrationWindow_ShouldDeny"
    "T03" = "test03_P03_ClassNotOpen_ShouldDeny"
    "T04" = "test04_P04_AlreadyRegistered_ShouldDeny"
    "T05" = "test05_P05_MaxCreditLimit_ShouldDeny"
    "T06" = "test06_P06_Prerequisite_ShouldDeny"
    "T07" = "test07_P07_ScheduleConflict_ShouldDeny"
    "T08" = "test09_P08_RaceCondition_OptimisticLocking"
    "T09" = "test11_P09_ClassStatusChangedOngoing_ShouldDeny"
    "T10" = "test08_P10_StudentOnHold_ShouldDeny"
    "T11" = "test01_HappyPath_SuccessfulRegistration"
    # Alias trực tiếp theo Policy ID
    "P01" = "test02_P01_TuitionNotPaid_ShouldDeny"
    "P02" = "test10_P02_OutsideRegistrationWindow_ShouldDeny"
    "P03" = "test03_P03_ClassNotOpen_ShouldDeny"
    "P04" = "test04_P04_AlreadyRegistered_ShouldDeny"
    "P05" = "test05_P05_MaxCreditLimit_ShouldDeny"
    "P06" = "test06_P06_Prerequisite_ShouldDeny"
    "P07" = "test07_P07_ScheduleConflict_ShouldDeny"
    "P08" = "test09_P08_RaceCondition_OptimisticLocking"
    "P09" = "test11_P09_ClassStatusChangedOngoing_ShouldDeny"
    "P10" = "test08_P10_StudentOnHold_ShouldDeny"
    "P11" = "test01_HappyPath_SuccessfulRegistration"
    "P12" = "test01_HappyPath_SuccessfulRegistration"
}

$id = $id.ToUpper()

if ($id -eq "ALL") {
    Write-Host "`n>> Running ALL 11 tests...`n" -ForegroundColor Cyan
    & $mvn clean test 2>&1 | Select-String "TEST 0|TEST 1|->|PASSED|Tests run|BUILD"
}
elseif ($map.ContainsKey($id)) {
    $method = $map[$id]
    Write-Host "`n>> Running [$id] -> $method`n" -ForegroundColor Cyan
    & $mvn test "-Dtest=${cls}#${method}" 2>&1 | Select-String "TEST 0|TEST 1|->|PASSED|FAILED|Tests run|BUILD"
}
else {
    Write-Host "`n[X] Khong tim thay ID '$id'" -ForegroundColor Red
    Write-Host @"

Cach dung: .\run.bat <ID>

  T01=P01  T02=P02  T03=P03  T04=P04
  T05=P05  T06=P06  T07=P07  T08=P08
  T09=P09  T10=P10  T11=P11+P12
  all  (chay toan bo 11 tests)
"@
}
