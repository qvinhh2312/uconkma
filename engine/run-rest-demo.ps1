param([string]$mode = "all")

$ErrorActionPreference = "Stop"
$baseUri = "http://localhost:8080"

function Write-Title($text) {
    Write-Host ""
    Write-Host $text -ForegroundColor Cyan
}

function Read-ErrorBody($errorRecord) {
    if ($errorRecord.ErrorDetails -and $errorRecord.ErrorDetails.Message) {
        return $errorRecord.ErrorDetails.Message
    }

    $exception = $errorRecord.Exception
    if ($exception.Response -and $exception.Response.GetResponseStream()) {
        $reader = New-Object System.IO.StreamReader($exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        $reader.Close()
        if ($body) { return $body }
    }
    return $exception.Message
}

function Show-State($label, $studentId, $classId) {
    Write-Title "[STATE] $label"
    $state = Invoke-RestMethod -Uri "$baseUri/api/demo/state?studentId=$studentId&classId=$classId" -Method GET
    $state | ConvertTo-Json -Depth 6
}

function Invoke-DemoRequest($label, $uri, $body) {
    Write-Title "[REQUEST] $label"
    Write-Host ($body | ConvertTo-Json -Depth 4)
    try {
        $response = Invoke-RestMethod -Uri $uri -Method POST -Body ($body | ConvertTo-Json) -ContentType "application/json"
        Write-Host "[HTTP RESPONSE] $response" -ForegroundColor Green
        return $response
    } catch {
        $errorBody = Read-ErrorBody $_
        Write-Host "[HTTP RESPONSE] $errorBody" -ForegroundColor Yellow
        return $errorBody
    }
}

function Run-RegisterSuccess() {
    $studentId = "SV001"
    $classId = "CS102_01"
    Show-State "Truoc REGISTER thanh cong" $studentId $classId
    Invoke-DemoRequest "REGISTER thanh cong" "$baseUri/api/register" @{
        requestId = "demo-register-success"
        studentId = $studentId
        classId = $classId
    } | Out-Null
    Show-State "Sau REGISTER thanh cong" $studentId $classId
}

function Run-RegisterTuitionDeny() {
    $studentId = "SV002"
    $classId = "CS102_01"
    Show-State "Truoc REGISTER bi tu choi do hoc phi" $studentId $classId
    Invoke-DemoRequest "REGISTER bi tu choi do hoc phi" "$baseUri/api/register" @{
        requestId = "demo-register-deny-tuition"
        studentId = $studentId
        classId = $classId
    } | Out-Null
    Show-State "Sau REGISTER bi tu choi do hoc phi" $studentId $classId
}

function Run-DropSuccess() {
    $studentId = "SV001"
    $classId = "CS102_01"
    Show-State "Truoc DROP thanh cong" $studentId $classId
    Invoke-DemoRequest "DROP thanh cong" "$baseUri/api/drop" @{
        requestId = "demo-drop-success"
        studentId = $studentId
        classId = $classId
    } | Out-Null
    Show-State "Sau DROP thanh cong" $studentId $classId
}

Write-Title "[INFO] Script nay gia dinh app dang chay tren http://localhost:8080"
Write-Host "[INFO] Neu app vua moi start, DB runtime se o trang thai seed tu data.sql" -ForegroundColor DarkGray

switch ($mode.ToLower()) {
    "1" { Run-RegisterSuccess }
    "2" { Run-RegisterTuitionDeny }
    "3" { Run-DropSuccess }
    "all" {
        Run-RegisterSuccess
        Run-RegisterTuitionDeny
        Run-DropSuccess
    }
    default {
        Write-Host "Cach dung:" -ForegroundColor Yellow
        Write-Host "  .\run-rest-demo.ps1 1    # Case 1: REGISTER thanh cong"
        Write-Host "  .\run-rest-demo.ps1 2    # Case 2: REGISTER bi tu choi do hoc phi"
        Write-Host "  .\run-rest-demo.ps1 3    # Case 3: DROP thanh cong"
        Write-Host "  .\run-rest-demo.ps1 all  # Chay lien tiep ca 3 case"
        exit 1
    }
}
