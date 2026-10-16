<#
.SYNOPSIS
    Groups benchmark runs by version, loader, backend and tag and prints medians as a Markdown table.

.EXAMPLE
    .\scripts\bench-summary.ps1
    .\scripts\bench-summary.ps1 -Scenario rdswitch -Tag vanilla,smooth
#>
param(
    [string] $File = (Join-Path (Split-Path -Parent $PSScriptRoot) "run\hinexoptimize\bench\summary-v2.csv"),
    [ValidateSet("spin", "rdswitch")] [string] $Scenario = "spin",
    [string[]] $Tag
)

$inv = [Globalization.CultureInfo]::InvariantCulture
function Median([double[]] $values) {
    $sorted = $values | Sort-Object
    $n = $sorted.Count
    if ($n -eq 0) { return [double]::NaN }
    if ($n % 2) { return $sorted[($n - 1) / 2] }
    return ($sorted[$n / 2 - 1] + $sorted[$n / 2]) / 2
}
function Fmt($value, $digits = 1) { $value.ToString("F$digits", $inv) }

$rows = Import-Csv $File | Where-Object { $_.scenario -eq $Scenario }
if ($Tag) { $rows = $rows | Where-Object { $Tag -contains $_.tag } }

# name = column, decimals, divisor
$metrics = if ($Scenario -eq "rdswitch") {
    [ordered]@{
        "max frame ms"        = @("max_ms", 1, 1)
        "frames > 50 ms"      = @("stutters_50ms", 0, 1)
        "p99 ms"              = @("p99_ms", 2, 1)
        "FPS avg"             = @("avg_fps", 1, 1)
        "sections rebuilt"    = @("section_compiles", 0, 1)
        "rebuild time s"      = @("settle_ms", 1, 1000)
        "longest step s"      = @("max_settle_ms", 1, 1000)
    }
} else {
    [ordered]@{
        "FPS avg"             = @("avg_fps", 1, 1)
        "1% low"              = @("low1_fps", 1, 1)
        "0.1% low"            = @("low01_fps", 1, 1)
        "p99 ms"              = @("p99_ms", 2, 1)
        "draws/frame"         = @("draw_calls_pf", 0, 1)
        "uniform binds/frame" = @("uniform_binds_pf", 0, 1)
        "descriptor upd/frame" = @("descriptor_updates_pf", 0, 1)
        "startup s"           = @("startup_ms", 1, 1000)
        "startup GLSL ms"     = @("startup_glsl_ms", 0, 1)
        "startup pipeline ms" = @("startup_pipeline_ms", 0, 1)
        "reload ms"           = @("reload_ms", 0, 1)
        "reload GLSL ms"      = @("reload_glsl_ms", 0, 1)
    }
}

$groups = $rows | Group-Object { "$($_.minecraft) | $($_.loader) | $($_.backend) | $($_.tag)" }
"| version | loader | backend | tag | runs | " + (($metrics.Keys) -join " | ") + " |"
"|" + ("---|" * (5 + $metrics.Count))
foreach ($g in $groups) {
    $parts = $g.Name -split ' \| '
    $cells = foreach ($m in $metrics.GetEnumerator()) {
        $values = foreach ($r in $g.Group) { $v = $r.($m.Value[0]); if ($v) { [double]::Parse($v, $inv) } }
        Fmt ((Median $values) / $m.Value[2]) $m.Value[1]
    }
    "| " + ($parts -join " | ") + " | $($g.Count) | " + ($cells -join " | ") + " |"
}
