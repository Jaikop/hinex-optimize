<#
.SYNOPSIS
    Groups benchmark runs by version, loader, backend and tag and prints medians as a Markdown table.

.EXAMPLE
    .\scripts\bench-summary.ps1
    .\scripts\bench-summary.ps1 -Tag baseline,spirv-cache
#>
param(
    [string] $File = (Join-Path (Split-Path -Parent $PSScriptRoot) "run\hinexoptimize\bench\summary-v1.csv"),
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
function Num($row, $name) { [double]::Parse($row.$name, $inv) }
function Fmt($value, $digits = 1) { $value.ToString("F$digits", $inv) }

$rows = Import-Csv $File
if ($Tag) { $rows = $rows | Where-Object { $Tag -contains $_.tag } }

$metrics = [ordered]@{
    "FPS avg"            = @("avg_fps", 1)
    "1% low"             = @("low1_fps", 1)
    "0.1% low"           = @("low01_fps", 1)
    "p99 ms"             = @("p99_ms", 2)
    "draws/frame"        = @("draw_calls_pf", 0)
    "uniform binds/frame" = @("uniform_binds_pf", 0)
    "descriptor upd/frame" = @("descriptor_updates_pf", 0)
    "startup s"          = @("startup_ms", 1)
    "startup GLSL ms"    = @("startup_glsl_ms", 0)
    "startup pipeline ms" = @("startup_pipeline_ms", 0)
    "reload ms"          = @("reload_ms", 0)
    "reload GLSL ms"     = @("reload_glsl_ms", 0)
}

$groups = $rows | Group-Object { "$($_.minecraft) | $($_.loader) | $($_.backend) | $($_.tag)" }
$header = "| version | loader | backend | tag | runs | " + (($metrics.Keys) -join " | ") + " |"
$header
"|" + ("---|" * (5 + $metrics.Count))
foreach ($g in $groups) {
    $parts = $g.Name -split ' \| '
    $cells = foreach ($m in $metrics.GetEnumerator()) {
        $values = foreach ($r in $g.Group) { $v = $r.($m.Value[0]); if ($v -ne '') { [double]::Parse($v, $inv) } }
        $median = Median $values
        if ($m.Value[0] -eq "startup_ms") { $median = $median / 1000 }
        Fmt $median $m.Value[1]
    }
    "| " + ($parts -join " | ") + " | $($g.Count) | " + ($cells -join " | ") + " |"
}
