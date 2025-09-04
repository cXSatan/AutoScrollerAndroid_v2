Param(
  [string]\ = 'Pixel_7a',
  [string]\ = 'com.example.autoscroller',
  [string]\ = '.MainActivity',
  [switch]\
)

# Resolve SDK paths
\C:\Users\ruivi\AppData\Local\Android\Sdk = \C:\Users\ruivi\AppData\Local\Android\Sdk; if (-not \C:\Users\ruivi\AppData\Local\Android\Sdk) { \C:\Users\ruivi\AppData\Local\Android\Sdk = "\C:\Users\ruivi\AppData\Local\Android\Sdk" }
\C:\Users\ruivi\AppData\Local\Android\Sdk\emulator\emulator.exe = Join-Path \C:\Users\ruivi\AppData\Local\Android\Sdk 'emulator\emulator.exe'
\C:\Users\ruivi\AppData\Local\Android\Sdk\platform-tools\adb.exe      = Join-Path \C:\Users\ruivi\AppData\Local\Android\Sdk 'platform-tools\adb.exe'

# Helper to run and fail fast
function Die(\) { Write-Error \; exit 1 }

# Sanity checks
if (-not (Test-Path \C:\Users\ruivi\AppData\Local\Android\Sdk\emulator\emulator.exe)) { Die "emulator.exe not found at: \C:\Users\ruivi\AppData\Local\Android\Sdk\emulator\emulator.exe" }
if (-not (Test-Path \C:\Users\ruivi\AppData\Local\Android\Sdk\platform-tools\adb.exe))      { Die "adb.exe not found at: \C:\Users\ruivi\AppData\Local\Android\Sdk\platform-tools\adb.exe" }

# Kill stale processes so we don't get 'device offline'
& \C:\Users\ruivi\AppData\Local\Android\Sdk\platform-tools\adb.exe kill-server 2>\
taskkill /IM emulator.exe /F 2>\
taskkill /IM qemu-system-x86_64.exe /F 2>\

# Build emulator args
\ = @('-avd', \, '-no-snapshot-load', '-no-boot-anim')
if (\) { \ += '-wipe-data' }

# Launch emulator detached so this terminal stays free
Start-Process -FilePath \C:\Users\ruivi\AppData\Local\Android\Sdk\emulator\emulator.exe -ArgumentList \

Write-Host "Emulator '\' launching... waiting for boot"

# Start fresh ADB and wait for full boot
& \C:\Users\ruivi\AppData\Local\Android\Sdk\platform-tools\adb.exe start-server | Out-Null
& \C:\Users\ruivi\AppData\Local\Android\Sdk\platform-tools\adb.exe wait-for-device
do { Start-Sleep -Seconds 2 } while ((& \C:\Users\ruivi\AppData\Local\Android\Sdk\platform-tools\adb.exe shell getprop sys.boot_completed).Trim() -ne '1')

# Wake/unlock
& \C:\Users\ruivi\AppData\Local\Android\Sdk\platform-tools\adb.exe shell input keyevent 82

Write-Host "Emulator ready. Building + installing debug APK..."

# Run Gradle from this script's folder
Set-Location \
if (-not (Test-Path ".\gradlew")) { Die "gradlew not found in \. Run this from your project root." }

# Build + install
& ".\gradlew" installDebug
if (\0 -ne 0) { Die "Gradle installDebug failed." }

# Launch the app
\ = "\/\"
& \C:\Users\ruivi\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -n \

if (\0 -eq 0) {
  Write-Host "? App launched: \"
  Write-Host "Tip: First run may need permissions:"
  Write-Host "  - Overlay: adb shell am start -a android.settings.MANAGE_OVERLAY_PERMISSION -d package:\"
  Write-Host "  - Accessibility: adb shell am start -a android.settings.ACCESSIBILITY_SETTINGS"
} else {
  Die "Failed to launch \"
}
