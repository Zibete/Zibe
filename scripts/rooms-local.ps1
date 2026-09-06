[CmdletBinding()]
param(
    [ValidateSet('Check', 'Start', 'Fixtures', 'Probe', 'Test', 'Stop')]
    [string]$Action = 'Check',
    [ValidateSet('demo-zibe-rooms')][string]$Project = 'demo-zibe-rooms',
    [ValidatePattern('^emulator-[0-9]+$')][string]$Serial = 'emulator-5556',
    [string]$Avd = 'Pixel_6a',
    [ValidateRange(30, 600)][int]$TimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$environmentNames = @('JAVA_HOME', 'PATH', 'FUNCTIONS_EMULATOR', 'GCLOUD_PROJECT', 'GOOGLE_CLOUD_PROJECT', 'FIREBASE_AUTH_EMULATOR_HOST', 'FIREBASE_DATABASE_EMULATOR_HOST', 'FIREBASE_STORAGE_EMULATOR_HOST', 'STORAGE_EMULATOR_HOST', 'FIREBASE_CLI_DISABLE_UPDATE_CHECK', 'GOOGLE_APPLICATION_CREDENTIALS', 'FIREBASE_TOKEN', 'ANDROID_SERIAL', 'DATABASE_URL', 'STORAGE_BUCKET_URL')
$savedEnvironment = @{}
foreach ($name in $environmentNames) { $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$repo = Split-Path $PSScriptRoot -Parent
$output = Join-Path $repo 'build/rooms-evidence/local'
$statePath = Join-Path $output 'processes.json'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$sdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
$adb = Join-Path $sdk 'platform-tools/adb.exe'
$emulator = Join-Path $sdk 'emulator/emulator.exe'
$gradleJava = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.16.8-hotspot'
$firebaseJava = 'C:/Program Files/Eclipse Adoptium/jdk-25.0.0.36-hotspot'
$firebaseCli = Join-Path $env:APPDATA 'npm/node_modules/firebase-tools/lib/bin/firebase.js'
$python = Join-Path $repo 'functions/venv/Scripts/python.exe'
$node = (Get-Command node.exe -ErrorAction Stop).Source
$ports = @(9099, 9000, 9199, 5001, 4400, 4500, 9299)

function Assert-File([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Falta requisito: $Path" }
}

function Invoke-Checked([string]$File, [string[]]$Arguments) {
    & $File @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Falló $File (exit $LASTEXITCODE)" }
}

function Save-Process($Process, [string]$Kind) {
    $entries = @()
    if (Test-Path -LiteralPath $statePath) { $entries = @(Get-Content $statePath -Raw | ConvertFrom-Json) }
    $entries += [pscustomobject]@{ id = $Process.Id; started = $Process.StartTime.ToUniversalTime().ToString('o'); kind = $Kind }
    ConvertTo-Json -InputObject @($entries) | Set-Content -LiteralPath $statePath
}

function Test-Port([int]$Port) {
    $socket = [Net.Sockets.TcpClient]::new()
    try { return $socket.ConnectAsync('127.0.0.1', $Port).Wait(500) -and $socket.Connected }
    catch { return $false }
    finally { $socket.Dispose() }
}

function Assert-Backend {
    foreach ($port in @(9099, 9000, 9199, 5001, 4400)) {
        if (-not (Test-Port $port)) { throw "Backend local ausente: 127.0.0.1:$port" }
    }
    $hub = Invoke-RestMethod 'http://127.0.0.1:4400/emulators' -TimeoutSec 5
    $expected = @{auth = 9099; database = 9000; storage = 9199; functions = 5001}
    foreach ($name in $expected.Keys) {
        if (-not $hub.PSObject.Properties[$name] -or $hub.$name.port -ne $expected[$name] -or $hub.$name.host -ne '127.0.0.1') { throw "Hub no registra endpoint local esperado: $name" }
    }
    $loadedRules = Invoke-RestMethod "http://127.0.0.1:9000/.settings/rules.json?ns=$Project-default-rtdb" -Headers @{Authorization = 'Bearer owner'} -TimeoutSec 5
    $expectedRules = Get-Content (Join-Path $repo 'database.rules.json') -Raw | ConvertFrom-Json
    if (($loadedRules | ConvertTo-Json -Depth 100 -Compress) -ne ($expectedRules | ConvertTo-Json -Depth 100 -Compress)) {
        throw 'El namespace local no tiene las Rules exactas del checkout; se aborta la prueba.'
    }
    $probe = Invoke-WebRequest "http://127.0.0.1:5001/$Project/us-central1/local_backend_probe" -Method Post -ContentType 'application/json' -Body '{"data":{}}' -SkipHttpErrorCheck -TimeoutSec 10
    if ($probe.StatusCode -ne 401 -or ($probe.Content | ConvertFrom-Json).error.status -ne 'UNAUTHENTICATED') {
        throw 'Functions no cargó el probe local autenticado; no se ejecutan pruebas sobre un backend parcial.'
    }
}

function Assert-Device {
    $deviceState = & $adb -s $Serial get-state 2>$null
    if ($LASTEXITCODE -ne 0 -or $deviceState -ne 'device') { throw "No hay dispositivo autorizado: $Serial" }
    $boot = & $adb -s $Serial shell getprop sys.boot_completed
    if ($boot.Trim() -ne '1') { throw "Boot incompleto: $Serial" }
    $currentAvd = & $adb -s $Serial emu avd name
    if ($currentAvd[0].Trim() -ne $Avd) { throw "El serial $Serial no corresponde al AVD $Avd" }
}

function Write-DeviceEvidence {
    $api = & $adb -s $Serial shell getprop ro.build.version.sdk
    $pages = & $adb -s $Serial shell getconf PAGE_SIZE
    "serial=$Serial`navd=$Avd`napi=$api`npageSize=$pages" | Set-Content (Join-Path $output 'device.txt')
}

function Start-Local {
    foreach ($port in $ports) {
        if (Test-Port $port) { throw "Puerto $port ocupado. No se adopta ni detiene un backend ajeno; ejecutá Stop para el propio." }
    }
    $env:JAVA_HOME = $firebaseJava
    $env:PATH = "$firebaseJava/bin;$repo/functions/venv/Scripts;$env:PATH"
    $env:FUNCTIONS_EMULATOR = 'true'
    $env:GCLOUD_PROJECT = $Project
    $env:GOOGLE_CLOUD_PROJECT = $Project
    $env:FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099'
    $env:FIREBASE_DATABASE_EMULATOR_HOST = '127.0.0.1:9000'
    $env:FIREBASE_STORAGE_EMULATOR_HOST = '127.0.0.1:9199'
    $env:STORAGE_EMULATOR_HOST = 'http://127.0.0.1:9199'
    $env:DATABASE_URL = "https://$Project-default-rtdb.firebaseio.com"
    $env:STORAGE_BUCKET_URL = "$Project.appspot.com"
    $env:FIREBASE_CLI_DISABLE_UPDATE_CHECK = 'true'
    # No credentials are needed for a demo emulator; never pass ambient production credentials.
    Remove-Item Env:GOOGLE_APPLICATION_CREDENTIALS -ErrorAction SilentlyContinue
    Remove-Item Env:FIREBASE_TOKEN -ErrorAction SilentlyContinue
    $backend = Start-Process -FilePath $node -ArgumentList @("`"$firebaseCli`"", 'emulators:start', '--config', 'firebase.local.json', '--project', $Project, '--only', 'auth,database,storage,functions') -WorkingDirectory $repo -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $output 'backend.stdout.log') -RedirectStandardError (Join-Path $output 'backend.stderr.log')
    Save-Process $backend 'backend'
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ($true) {
        $backend.Refresh()
        if ($backend.HasExited) { throw "Backend terminó (exit $($backend.ExitCode)); ver $output/backend.stderr.log" }
        try { Assert-Backend; break } catch {
            if ([DateTime]::UtcNow -gt $deadline) { throw "Timeout backend: $_; ver $output" }
            Start-Sleep -Milliseconds 500
        }
    }
    $devices = (& $adb devices) -join "`n"
    if ($devices -notmatch "(?m)^$([regex]::Escape($Serial))\s+device") {
        $avds = & $emulator -list-avds
        if ($Avd -notin $avds) { throw "No existe AVD $Avd. Disponibles: $($avds -join ', ')" }
        $port = $Serial.Substring('emulator-'.Length)
        $avdProcess = Start-Process -FilePath $emulator -ArgumentList @('-avd', $Avd, '-port', $port, '-no-snapshot-save', '-no-boot-anim', '-no-window', '-gpu', 'software') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $output 'avd.stdout.log') -RedirectStandardError (Join-Path $output 'avd.stderr.log')
        Save-Process $avdProcess 'avd'
    }
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ($true) {
        try { Assert-Device; break } catch {
            if ([DateTime]::UtcNow -gt $deadline) { throw "Timeout AVD: $_" }
            Start-Sleep -Milliseconds 500
        }
    }
    Write-DeviceEvidence
    Write-Output "Backend demo iniciado; $Serial listo. Evidencia: $output"
}

function Stop-Local {
    if (-not (Test-Path -LiteralPath $statePath)) { Write-Output 'No hay procesos propios registrados.'; return }
    foreach ($entry in @(Get-Content $statePath -Raw | ConvertFrom-Json)) {
        $process = Get-Process -Id $entry.id -ErrorAction SilentlyContinue
        if (-not $process) { continue }
        if ($process.StartTime.ToUniversalTime() -ne ([DateTime]$entry.started).ToUniversalTime()) {
            throw "PID $($entry.id) reutilizado; no se detiene."
        }
        $inventory = @(Get-CimInstance Win32_Process)
        $ids = [Collections.Generic.List[int]]::new()
        $ids.Add([int]$entry.id)
        for ($index = 0; $index -lt $ids.Count; $index++) {
            foreach ($child in $inventory | Where-Object { $_.ParentProcessId -eq $ids[$index] -and $_.CreationDate -ge $process.StartTime }) {
                $ids.Add([int]$child.ProcessId)
            }
        }
        $ids.Reverse()
        foreach ($idToStop in $ids) {
            $observed = $inventory | Where-Object ProcessId -EQ $idToStop | Select-Object -First 1
            $live = Get-CimInstance Win32_Process -Filter "ProcessId=$idToStop"
            if ($live -and $observed -and $live.CreationDate -eq $observed.CreationDate) {
                Stop-Process -Id $idToStop -ErrorAction SilentlyContinue
            }
        }
    }
    Remove-Item -LiteralPath $statePath
    Write-Output 'Procesos propios detenidos; no se detuvo el servidor ADB ni otros dispositivos.'
}

try {
    Push-Location $repo
    if ($Action -eq 'Stop') { Stop-Local; exit 0 }
    foreach ($file in @($adb, $emulator, "$gradleJava/bin/java.exe", "$firebaseJava/bin/java.exe", $firebaseCli, $python, "$repo/gradlew.bat")) { Assert-File $file }
    switch ($Action) {
        'Check' {
            Invoke-Checked $node @('--version')
            Invoke-Checked $python @('-c', 'import firebase_functions, firebase_admin; print("Functions SDK imports OK")')
            Invoke-Checked $emulator @('-accel-check')
            Write-Output "JDK Gradle: $gradleJava; JDK Firebase: $firebaseJava; SDK: $sdk; AVD: $Avd; serial: $Serial; proyecto: $Project"
        }
        'Start' { Start-Local }
        'Fixtures' {
            Assert-Backend
            Invoke-Checked $python @('scripts/rooms-fixtures.py', '--project', $Project)
        }
        { $_ -in 'Probe', 'Test' } {
            Assert-Backend
            Assert-Device
            Write-DeviceEvidence
            $env:ANDROID_SERIAL = $Serial
            $env:JAVA_HOME = $gradleJava
            $env:PATH = "$gradleJava/bin;$env:PATH"
            $arguments = @('-ProomsLocalTests=true', ':app:connectedLocalAndroidTest', '--no-daemon')
            if ($Action -eq 'Probe') { $arguments += '-Pandroid.testInstrumentationRunnerArguments.class=com.zibete.proyecto1.local.LocalBackendProbeAndroidTest' }
            $started = [DateTime]::UtcNow
            Invoke-Checked "$repo/gradlew.bat" $arguments
            $reports = @(Get-ChildItem "$repo/app/build/outputs/androidTest-results/connected/local" -Filter 'TEST-*.xml' -Recurse -ErrorAction SilentlyContinue | Where-Object LastWriteTimeUtc -GE $started)
            if ($reports.Count -eq 0) { throw 'No hay informes XML de instrumentation; no se considera aprobada una suite sin pruebas.' }
            $total = 0
            foreach ($report in $reports) {
                [xml]$xml = Get-Content $report.FullName
                $suite = $xml.DocumentElement
                $total += [int]$suite.GetAttribute('tests') - [int]$suite.GetAttribute('skipped')
                if ([int]$suite.GetAttribute('failures') -gt 0 -or [int]$suite.GetAttribute('errors') -gt 0) { throw "Fallos de instrumentation: $($report.FullName)" }
            }
            if ($total -lt 1) { throw 'Instrumentation ejecutó cero tests.' }
            Write-Output "Instrumentation: $total tests; informes: app/build/outputs/androidTest-results/connected/local"
        }
    }
} catch {
    Write-Error -ErrorAction Continue $_
    exit 1
} finally {
    foreach ($name in $environmentNames) { [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process') }
    Pop-Location
}
