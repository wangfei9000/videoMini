$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$gradleUserHome = Join-Path $projectRoot ".gradle-build"
$androidUserHome = Join-Path $projectRoot ".android-build"

$env:GRADLE_USER_HOME = $gradleUserHome
$env:ANDROID_USER_HOME = $androidUserHome

Write-Host "Project: $projectRoot"
Write-Host "Building debug APK..."

$gradleWrapper = Join-Path $projectRoot "gradlew.bat"
$gradleExe = $null

if (Test-Path $gradleWrapper) {
    $gradleExe = $gradleWrapper
} else {
    $gradleCandidates = @(
        "C:\Users\wf\.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3\bin\gradle.bat",
        "C:\Gradle\gradle-8.14.3\bin\gradle.bat"
    )

    $gradleExe = $gradleCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}

if (-not $gradleExe) {
    $gradleCommand = Get-Command gradle -ErrorAction SilentlyContinue
    if ($gradleCommand) {
        $gradleExe = $gradleCommand.Source
    }
}

if (-not $gradleExe) {
    throw "Gradle was not found. Install Gradle or update the Gradle path in this script."
}

Push-Location $projectRoot
try {
    & $gradleExe assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "APK build failed. Gradle exit code: $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

$apk = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    throw "Gradle completed, but the APK was not found: $apk"
}

Write-Host ""
Write-Host "APK build succeeded:" -ForegroundColor Green
Write-Host $apk -ForegroundColor Cyan
