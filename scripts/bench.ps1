<#
.SYNOPSIS
    Runs the unattended HinexOptimize benchmark in the dev client and prints the result line.

.EXAMPLE
    .\scripts\bench.ps1 -Node 26.4-fabric -Tag baseline -Vanilla
    .\scripts\bench.ps1 -Node 26.3-fabric -Backend vulkan -Tag baseline -Vanilla -Repeat 2

.NOTES
    Keep the game window visible (not minimized) while it runs. Results are appended to
    run/hinexoptimize/bench/summary-v1.csv; logs go to build/bench-<node>-<backend>-<n>.log.
#>
param(
    [string] $Node = "26.3-fabric",
    [ValidateSet("default", "opengl", "vulkan")] [string] $Backend = "default",
    [string] $Tag = "dev",
    [switch] $Vanilla,
    [int] $Seconds = 30,
    [int] $RenderDistance = 12,
    [int] $Repeat = 1
)

$root = Split-Path -Parent $PSScriptRoot
# No JAVA_HOME needed: gradle/gradle-daemon-jvm.properties makes Gradle run on Java 25

# The graphics API is read from options.txt at startup, so it is set before launching
$options = Join-Path $root "run\options.txt"
if (Test-Path $options) {
    $lines = Get-Content $options
    if ($lines -match '^preferredGraphicsBackend:') {
        $lines = $lines -replace '^preferredGraphicsBackend:.*$', "preferredGraphicsBackend:`"$Backend`""
    } else {
        $lines += "preferredGraphicsBackend:`"$Backend`""
    }
    [IO.File]::WriteAllLines($options, $lines)
} elseif ($Backend -ne "default") {
    Write-Warning "run/options.txt does not exist yet; start the game once, then choose -Backend"
}

New-Item -ItemType Directory -Force (Join-Path $root "build") | Out-Null
for ($i = 1; $i -le $Repeat; $i++) {
    $log = Join-Path $root "build\bench-$Node-$Backend-$i.log"
    $props = @(
        "-Phinexoptimize.autobench=spin",
        "-Phinexoptimize.autobench.tag=$Tag",
        "-Phinexoptimize.autobench.seconds=$Seconds",
        "-Phinexoptimize.autobench.renderDistance=$RenderDistance"
    )
    if ($Vanilla) { $props += "-Phinexoptimize.vanilla=true" }

    Write-Host "[$i/$Repeat] $Node ($Backend) tag=$Tag ..."
    # Through cmd so Gradle's stderr goes to the log instead of becoming PowerShell errors
    $gradlew = Join-Path $root "gradlew.bat"
    cmd /c "`"$gradlew`" -p `"$root`" :${Node}:runClient --console=plain $($props -join ' ') > `"$log`" 2>&1"
    $result = Select-String -Path $log -Pattern '\[HinexBench\] (RESULT|FAILED).*' | Select-Object -Last 1
    if ($result) {
        Write-Host ($result.Matches[0].Value)
    } else {
        Write-Warning "No result in $log (exit code $LASTEXITCODE)"
    }
}
