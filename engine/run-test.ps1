param([string]$id = "all")

$mvn = ".\apache-maven-3.9.6\bin\mvn.cmd"
$cls = "UconEngineApplicationTests"

$map = @{
    "T01" = "test01_RegisterSuccess_UpdatesStateBillingTransactionAndAudit"
    "T02" = "test02_RegisterDenied_WhenTuitionNotPaid"
    "T03" = "test03_RegisterDenied_OutsideTransactionWindow"
    "T04" = "test04_RegisterDenied_WhenClassLocksBetweenPhases"
    "T05" = "test05_RegisterDenied_WhenRepositoryShowsExistingRegistration"
    "T06" = "test06_RegisterDenied_WhenCreditLimitExceeded"
    "T07" = "test07_RegisterDenied_WhenPrerequisiteMissing"
    "T08" = "test08_RegisterDenied_WhenScheduleConflicts"
    "T09" = "test09_RegisterDenied_WhenStudentOnHold"
    "T10" = "test10_OnlyOneStudentCanClaimLastSeat"
    "T11" = "test11_MaintenanceBlocksRequest_AndPreservesState"
    "T12" = "test12_DropRestoresState_RemovesTransaction_AndRefundsDebt"
    "T13" = "test13_ValidatorRejectsRequestManagedFieldUpdates"
    "T14" = "test14_ValidatorRejectsMalformedAuditLogStatements"
    "T15" = "test15_PolicyDecisionPointFailsFast_WhenValidationFails"
    "T16" = "test16_ValidatorRejectsInvalidPath_Arity_AndPhase"
    "P01" = "test02_RegisterDenied_WhenTuitionNotPaid"
    "P02" = "test03_RegisterDenied_OutsideTransactionWindow"
    "P03" = "test04_RegisterDenied_WhenClassLocksBetweenPhases"
    "P04" = "test05_RegisterDenied_WhenRepositoryShowsExistingRegistration"
    "P05" = "test06_RegisterDenied_WhenCreditLimitExceeded"
    "P06" = "test07_RegisterDenied_WhenPrerequisiteMissing"
    "P07" = "test08_RegisterDenied_WhenScheduleConflicts"
    "P08" = "test10_OnlyOneStudentCanClaimLastSeat"
    "P09" = "test04_RegisterDenied_WhenClassLocksBetweenPhases"
    "P10" = "test09_RegisterDenied_WhenStudentOnHold"
    "P11" = "test01_RegisterSuccess_UpdatesStateBillingTransactionAndAudit"
    "P12" = "test01_RegisterSuccess_UpdatesStateBillingTransactionAndAudit"
    "P13" = "test11_MaintenanceBlocksRequest_AndPreservesState"
    "P13A" = "test11_MaintenanceBlocksRequest_AndPreservesState"
    "P14" = "test12_DropRestoresState_RemovesTransaction_AndRefundsDebt"
    "P16" = "test12_DropRestoresState_RemovesTransaction_AndRefundsDebt"
}

$id = $id.ToUpper()

if ($id -eq "ALL") {
    Write-Host "`n>> Running ALL 16 tests...`n" -ForegroundColor Cyan
    & $mvn clean test 2>&1 | Select-String "Tests run|BUILD SUCCESS|BUILD FAILURE"
}
elseif ($map.ContainsKey($id)) {
    $method = $map[$id]
    Write-Host "`n>> Running [$id] -> $method`n" -ForegroundColor Cyan
    & $mvn test "-Dtest=${cls}#${method}" 2>&1 | Select-String "Tests run|BUILD SUCCESS|BUILD FAILURE"
}
else {
    Write-Host "`n[X] Khong tim thay ID '$id'" -ForegroundColor Red
    Write-Host @"

Cach dung: .\run-test.ps1 <ID>

  T01..T16
  P01..P14, P13A, P16
  all       (chay toan bo 16 tests)
"@
}
