param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$xmiPath = Join-Path $Root "xmi/ucon_policy.xmi"
$dslPath = Join-Path $Root "dsl/ucon_policy.dsl"
$outDir = Join-Path $Root "docs/generated"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

[xml]$xmi = Get-Content -Raw -Encoding UTF8 $xmiPath
$policies = $xmi.SelectNodes("//*[local-name()='policies']")
$policySets = $xmi.SelectNodes("//*[local-name()='policySets']")

function Write-Utf8NoBom($path, $content) {
    $encoding = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText($path, ($content -join [Environment]::NewLine), $encoding)
}

$policyLines = @(
    "# Generated Policy Catalog",
    "",
    "Generated from `xmi/ucon_policy.xmi`.",
    "",
    "| Policy | Predicate | Phase | UpdateTiming | Action | Effect | Variant | Status |",
    "| --- | --- | --- | --- | --- | --- | --- | --- |"
)
foreach ($policy in $policies) {
    $policyLines += "| $($policy.policyId) | $($policy.predicate) | $($policy.phase) | $($policy.updateTiming) | $($policy.targetAction) | $($policy.effect) | $($policy.uconVariant) | $($policy.policyStatus) |"
}
Write-Utf8NoBom (Join-Path $outDir "policies.md") $policyLines

$coverage = [ordered]@{}
foreach ($policy in $policies) {
    $variant = $policy.uconVariant
    if (-not $coverage.Contains($variant)) {
        $coverage[$variant] = @()
    }
    $coverage[$variant] += $policy.policyId
}
$coverageLines = @(
    "# Generated UCON Coverage",
    "",
    "Generated from policy metadata in `xmi/ucon_policy.xmi`.",
    "",
    "| UCON variant | Policies |",
    "| --- | --- |"
)
foreach ($variant in ($coverage.Keys | Sort-Object)) {
    $coverageLines += "| $variant | $($coverage[$variant] -join ', ') |"
}
Write-Utf8NoBom (Join-Path $outDir "ucon_coverage.md") $coverageLines

$dslPolicyIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($line in (Get-Content -Encoding UTF8 $dslPath)) {
    if ($line -match '^\s*policy\s+([A-Za-z0-9_]+)\s*\{') {
        [void]$dslPolicyIds.Add($Matches[1])
    }
}
$xmiPolicyIds = [System.Collections.Generic.HashSet[string]]::new()
$missingRequired = @()
foreach ($policy in $policies) {
    [void]$xmiPolicyIds.Add($policy.policyId)
    foreach ($attr in @("policyId", "predicate", "phase", "updateTiming", "targetAction", "effect", "policyStatus", "uconVariant")) {
        if ([string]::IsNullOrWhiteSpace($policy.$attr)) {
            $missingRequired += "$($policy.policyId): missing $attr"
        }
    }
}
$missingInXmi = @($dslPolicyIds | Where-Object { -not $xmiPolicyIds.Contains($_) })
$validationLines = @(
    "# Generated Validation Report",
    "",
    "Generated from `dsl/ucon_policy.dsl` and `xmi/ucon_policy.xmi`.",
    "",
    "- DSL policies: $($dslPolicyIds.Count)",
    "- XMI policies: $($xmiPolicyIds.Count)",
    "- PolicySets: $($policySets.Count)",
    "- Missing DSL policies in XMI: $($missingInXmi.Count)",
    "- Missing required policy attributes: $($missingRequired.Count)",
    ""
)
if ($missingInXmi.Count -gt 0) {
    $validationLines += "## Missing DSL policies in XMI"
    $validationLines += $missingInXmi | ForEach-Object { "- $_" }
}
if ($missingRequired.Count -gt 0) {
    $validationLines += "## Missing required attributes"
    $validationLines += $missingRequired | ForEach-Object { "- $_" }
}
Write-Utf8NoBom (Join-Path $outDir "validation_report.md") $validationLines
