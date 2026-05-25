param(
    [string]$Owner = "qvinhh2312",
    [string]$Repo = "uconkma",
    [string]$Branch = "master",
    [int]$MinimumLines = 6
)

$ErrorActionPreference = "Stop"

$files = @(
    "dsl/UconPolicy.g4",
    "dsl/ucon_policy.dsl",
    ".github/workflows/maven.yml",
    "README.md",
    "docs/test-result.md",
    "docs/ucon_mapping.md",
    "docs/ucon_coverage_report.md",
    "docs/policy_catalog.md",
    "docs/validation_rules.md",
    "docs/decision_trace_examples.md",
    "docs/benchmark_result.md",
    "engine/src/main/java/vn/edu/kma/ucon/engine/pep/RegistrationController.java",
    "engine/src/main/java/vn/edu/kma/ucon/engine/pep/RegistrationService.java",
    "engine/src/main/java/vn/edu/kma/ucon/engine/pep/UconPepService.java",
    "engine/src/main/java/vn/edu/kma/ucon/engine/pep/UconExecutionWorkflow.java",
    "engine/src/main/java/vn/edu/kma/ucon/engine/pdp/PolicyEngine.java",
    "engine/src/main/java/vn/edu/kma/ucon/engine/pdp/PolicyValidator.java",
    "engine/src/main/java/vn/edu/kma/ucon/engine/pdp/PolicyAnalyzer.java",
    "engine/src/main/java/vn/edu/kma/ucon/engine/update/UpdateManager.java",
    "engine/src/main/java/vn/edu/kma/ucon/engine/update/RollbackManager.java"
)

$cacheBust = Get-Date -Format "yyyyMMddHHmmss"
$failures = @()

foreach ($file in $files) {
    $url = "https://raw.githubusercontent.com/{0}/{1}/{2}/{3}?cb={4}" -f $Owner, $Repo, $Branch, $file, $cacheBust
    $content = (Invoke-WebRequest -Uri $url -UseBasicParsing).Content
    $lineCount = ($content -split "`n").Count
    "{0}: {1}" -f $file, $lineCount

    if ($lineCount -lt $MinimumLines) {
        $failures += "$file has only $lineCount raw lines"
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host "Raw GitHub formatting verification passed."
