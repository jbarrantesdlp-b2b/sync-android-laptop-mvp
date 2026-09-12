$src = "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"
$desktop = [Environment]::GetFolderPath("Desktop")
$dest = "$desktop\SyncApp.apk"

if (Test-Path $src) {
    Copy-Item -Path $src -Destination $dest -Force
    Write-Host "✅ Instalador SyncApp.apk actualizado exitosamente en tu Escritorio:" -ForegroundColor Green
    Write-Host "   $dest" -ForegroundColor Cyan
} else {
    Write-Host "❌ No se encontró el APK compilado. Ejecutando build..." -ForegroundColor Yellow
    Set-Location $PSScriptRoot
    .\gradlew :app:assembleDebug
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $dest -Force
        Write-Host "✅ Instalador SyncApp.apk creado exitosamente en tu Escritorio:" -ForegroundColor Green
        Write-Host "   $dest" -ForegroundColor Cyan
    }
}
