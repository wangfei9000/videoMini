$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:GRADLE_USER_HOME = Join-Path $projectRoot ".gradle-build"
$env:ANDROID_USER_HOME = Join-Path $projectRoot ".android-build"

$gradle = Join-Path $projectRoot "gradlew.bat"
if (-not (Test-Path $gradle)) {
    $gradle = "C:\Users\wf\.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3\bin\gradle.bat"
}
if (-not (Test-Path $gradle)) {
    throw "Gradle was not found. Update the Gradle path in this script."
}

Push-Location $projectRoot
try {
    & $gradle assembleRelease
    if ($LASTEXITCODE -ne 0) {
        throw "Release APK build failed. Gradle exit code: $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

$apk = Join-Path $projectRoot "app\build\outputs\apk\release\app-release-unsigned.apk"
if (-not (Test-Path $apk)) {
    $apk = Join-Path $projectRoot "app\build\outputs\apk\release\app-release.apk"
}
if (-not (Test-Path $apk)) {
    throw "Release build completed, but no release APK was found."
}

Write-Host "Release APK generated:" -ForegroundColor Green
Write-Host $apk -ForegroundColor Cyan
