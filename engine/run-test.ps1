# ============================================================
# UCON Policy Engine — Test Runner
# Usage: .\run-test.ps1 <ID>
#
# ID có thể là:
#   T01 — T11       : theo số thứ tự test
#   P01, P02, P03   : theo policy ID
#   P04, P05, P06
#   P07, P08, P09
#   P10, P11, P12
#   all             : chạy toàn bộ 11 tests
# ============================================================

param([string]$id = "all")

$mvn = ".\apache-maven-3.9.6\bin\mvn.cmd"
$cls = "UconEngineApplicationTests"

$map = @{
    "T01" = "test01_HappyPath_SuccessfulRegistration"
    "T02" = "test02_P01_TuitionNotPaid_ShouldDeny"
    "T03" = "test03_P03_ClassNotOpen_ShouldDeny"
    "T04" = "test04_P04_AlreadyRegistered_ShouldDeny"
    "T05" = "test05_P05_MaxCreditLimit_ShouldDeny"
    "T06" = "test06_P06_Prerequisite_ShouldDeny"
    "T07" = "test07_P07_ScheduleConflict_ShouldDeny"
    "T08" = "test08_P10_StudentOnHold_ShouldDeny"
    "T09" = "test09_P08_RaceCondition_OptimisticLocking"
    "T10" = "test10_P02_OutsideRegistrationWindow_ShouldDeny"
    "T11" = "test11_P09_ClassStatusChangedOngoing_ShouldDeny"
    # Alias theo Policy ID
    "P11" = "test01_HappyPath_SuccessfulRegistration"  # P11+P12 Happy Path
    "P12" = "test01_HappyPath_SuccessfulRegistration"
    "P01" = "test02_P01_TuitionNotPaid_ShouldDeny"
    "P03" = "test03_P03_ClassNotOpen_ShouldDeny"
    "P04" = "test04_P04_AlreadyRegistered_ShouldDeny"
    "P05" = "test05_P05_MaxCreditLimit_ShouldDeny"
    "P06" = "test06_P06_Prerequisite_ShouldDeny"
    "P07" = "test07_P07_ScheduleConflict_ShouldDeny"
    "P10" = "test08_P10_StudentOnHold_ShouldDeny"
    "P08" = "test09_P08_RaceCondition_OptimisticLocking"
    "P02" = "test10_P02_OutsideRegistrationWindow_ShouldDeny"
    "P09" = "test11_P09_ClassStatusChangedOngoing_ShouldDeny"
}

$id = $id.ToUpper()

if ($id -eq "ALL") {
    Write-Host "`n🚀 Running ALL 11 tests...`n" -ForegroundColor Cyan
    & $mvn clean test 2>&1 | Select-String "TEST 0|TEST 1|->|PASSED|Tests run|BUILD"
}
elseif ($map.ContainsKey($id)) {
    $method = $map[$id]
    Write-Host "`n🚀 Running [$id] → $method`n" -ForegroundColor Cyan
    & $mvn test "-Dtest=${cls}#${method}" 2>&1 | Select-String "TEST 0|TEST 1|->|PASSED|FAILED|Tests run|BUILD"
}
else {
    Write-Host "`n❌ Không tìm thấy ID '$id'" -ForegroundColor Red
    Write-Host @"

Cách dùng: .\run-test.ps1 <ID>

  ID hợp lệ:
    T01  T02  T03  T04  T05  T06
    T07  T08  T09  T10  T11
    P01  P02  P03  P04  P05  P06
    P07  P08  P09  P10  P11  P12
    all  (chạy toàn bộ)
"@
}
