# Baut dist\PhishingForCompliments.exe – eine einzelne Datei zum Weitergeben (kein Java nötig).
#
#   powershell -ExecutionPolicy Bypass -File packaging\build-exe.ps1 [-SkipTests] [-Jdk <Pfad>]
#
# Ablauf:
#   1. Maven-Build (inkl. Tests) -> modulares JAR
#   2. jpackage -> App-Abbild mit eigener, abgespeckter Java-Laufzeit (target\jpackage)
#   3. Selbsttest des App-Abbilds (--selftest, öffnet kein Fenster)
#   4. App-Abbild zippen und in den C#-Starter einbetten (Windows-eigener csc.exe)
#   5. Selbsttest der fertigen .exe in einem Test-Verzeichnis
#
# Benötigt ein JDK 21+ mit jpackage (Standard: neuestes in %USERPROFILE%\.jdks oder $env:JPACKAGE_JDK).
param(
    [switch]$SkipTests,
    [string]$Jdk = ""
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$appName = "PhishingForCompliments"

function Invoke-Native {
    param([string]$Exe, [string[]]$Arguments, [string]$Step)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & $Exe @Arguments
    $code = $LASTEXITCODE
    $ErrorActionPreference = $previous
    if ($code -ne 0) { throw "$Step fehlgeschlagen (Exit-Code $code)" }
}

function Invoke-SelfTest {
    param([string]$Exe, [string]$Step)
    $p = Start-Process -FilePath $Exe -ArgumentList "--selftest" -Wait -PassThru
    $log = Join-Path $env:TEMP "phishingforcompliments-selftest.log"
    if ($p.ExitCode -ne 0) {
        if (Test-Path $log) { Get-Content $log | Write-Output }
        throw "$Step fehlgeschlagen (Exit-Code $($p.ExitCode))"
    }
    Write-Output "  $Step ok"
}

# --- JDK mit jpackage finden ------------------------------------------------
if (-not $Jdk) { $Jdk = $env:JPACKAGE_JDK }
if (-not $Jdk) {
    $Jdk = Get-ChildItem (Join-Path $env:USERPROFILE ".jdks") -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName "bin\jpackage.exe") } |
        Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $Jdk -or -not (Test-Path (Join-Path $Jdk "bin\jpackage.exe"))) {
    throw "Kein JDK mit jpackage gefunden. JDK 21+ installieren (z.B. in %USERPROFILE%\.jdks) oder -Jdk angeben."
}
$env:JAVA_HOME = $Jdk
Write-Output "JDK: $Jdk"

$csc = Join-Path $env:WINDIR "Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if (-not (Test-Path $csc)) { throw "C#-Compiler nicht gefunden: $csc" }

$icon = Join-Path $PSScriptRoot "app.ico"
if (-not (Test-Path $icon)) { & (Join-Path $PSScriptRoot "make-icon.ps1") }

# --- 1. Maven-Build -----------------------------------------------------------
Write-Output "1/5 Maven-Build ..."
$mvnArgs = @("-q", "-B", "clean", "package")
if ($SkipTests) { $mvnArgs += "-DskipTests" }
Push-Location $root
try { Invoke-Native (Join-Path $root "mvnw.cmd") $mvnArgs "Maven-Build" } finally { Pop-Location }

$pom = [xml](Get-Content (Join-Path $root "pom.xml"))
$fxVersion = $pom.project.properties.'javafx.version'
$projectVersion = $pom.project.version
$appVersion = ($projectVersion -replace '[^0-9.].*$', '').TrimEnd('.')
if (-not $appVersion) { $appVersion = "1.0" }

$appJar = Join-Path $root "target\$appName-$projectVersion.jar"
if (-not (Test-Path $appJar)) { throw "JAR nicht gefunden: $appJar" }
$m2 = Join-Path $env:USERPROFILE ".m2\repository\org\openjfx"
$modulePath = @($appJar)
foreach ($m in "base", "graphics", "controls") {
    $jar = Join-Path $m2 "javafx-$m\$fxVersion\javafx-$m-$fxVersion-win.jar"
    if (-not (Test-Path $jar)) { throw "JavaFX-Bibliothek fehlt: $jar" }
    $modulePath += $jar
}

# --- 2. jpackage --------------------------------------------------------------
Write-Output "2/5 jpackage (App mit eigener Java-Laufzeit) ..."
$imageDest = Join-Path $root "target\jpackage"
if (Test-Path $imageDest) { Remove-Item -Recurse -Force $imageDest -Confirm:$false }
Invoke-Native (Join-Path $Jdk "bin\jpackage.exe") @(
    "--type", "app-image",
    "--name", $appName,
    "--app-version", $appVersion,
    "--vendor", "Phishing for Compliments",
    "--description", "Lernspiel zum Erkennen von Phishing-Mails",
    "--icon", $icon,
    "--module-path", ($modulePath -join ";"),
    "--module", "com.example.phishingforcompliments/com.example.phishingforcompliments.Launcher",
    "--jlink-options", "--strip-debug --no-header-files --no-man-pages --strip-native-commands",
    "--dest", $imageDest
) "jpackage"
$image = Join-Path $imageDest $appName

# --- 3. Selbsttest des App-Abbilds ---------------------------------------------
Write-Output "3/5 Selbsttest des App-Abbilds ..."
Invoke-SelfTest (Join-Path $image "$appName.exe") "Selbsttest App-Abbild"

# --- 4. Zippen und Starter kompilieren -----------------------------------------
Write-Output "4/5 Einzeldatei bauen ..."
$zip = Join-Path $root "target\app.zip"
if (Test-Path $zip) { Remove-Item -Force $zip -Confirm:$false }
Invoke-Native (Join-Path $Jdk "bin\jar.exe") @("--create", "--file", $zip, "--no-manifest", "-C", $image, ".") "Zippen"

$dist = Join-Path $root "dist"
New-Item -ItemType Directory -Force $dist | Out-Null
$out = Join-Path $dist "$appName.exe"
if (Test-Path $out) { Remove-Item -Force $out -Confirm:$false }
Invoke-Native $csc @(
    "/nologo", "/optimize+", "/target:winexe", "/platform:anycpu",
    "/win32icon:$icon",
    "/resource:$zip,app.zip",
    "/r:System.IO.Compression.dll", "/r:System.Windows.Forms.dll", "/r:System.Drawing.dll",
    "/out:$out",
    (Join-Path $PSScriptRoot "launcher\PfcLauncher.cs")
) "C#-Starter kompilieren"

# --- 5. Selbsttest der fertigen .exe (in einem Test-Verzeichnis) ----------------
Write-Output "5/5 Selbsttest der fertigen .exe ..."
$testHome = Join-Path $root "target\exe-selftest"
if (Test-Path $testHome) { Remove-Item -Recurse -Force $testHome -Confirm:$false }
$env:PFC_HOME = $testHome
try {
    Invoke-SelfTest $out "Selbsttest Einzeldatei (inkl. Entpacken)"
} finally {
    Remove-Item Env:\PFC_HOME
}

$sizeMb = [Math]::Round((Get-Item $out).Length / 1MB, 1)
Write-Output ""
Write-Output "Fertig: $out ($sizeMb MB)"
