# Gera o instalador do VfxClient no seu PC (o mesmo que a CI publica a cada tag).
#
# Uso (na pasta do repositório):
#   powershell -ExecutionPolicy Bypass -File installer\build-installer.ps1
#
# Resultado em dist\:
#   VfxClient-Setup-<versão>.exe     instalador
#   VfxClient-<versão>-portable.zip  versão portátil (é só extrair e abrir VfxClient.exe)
#   vfxcore-<versão>.jar             o mod sozinho (para instalar no Legacy Fabric manualmente)
#
# Requisitos: JDK 21 (JAVA_HOME) e Inno Setup 6 (winget install JRSoftware.InnoSetup).

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$version = (Select-String -Path gradle.properties -Pattern '^client_version=(.+)$').Matches[0].Groups[1].Value.Trim()
$numeric = ($version -split '-')[0]
Write-Host "VfxClient $version"

& .\gradlew.bat build :launcher:jpackageImage --console=plain
if ($LASTEXITCODE -ne 0) { throw "O build falhou." }

New-Item -ItemType Directory -Force dist | Out-Null
Copy-Item "core\build\libs\vfxcore-$version.jar" dist\ -Force
$zip = "dist\VfxClient-$version-portable.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path "launcher\build\jpackage\VfxClient" -DestinationPath $zip

$iscc = @(
    "$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe",
    "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
    "$env:ProgramFiles\Inno Setup 6\ISCC.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $iscc) { throw "Inno Setup não encontrado. Instale com: winget install JRSoftware.InnoSetup" }

& $iscc "/DAppVersion=$numeric" "installer\vfxclient.iss"
if ($LASTEXITCODE -ne 0) { throw "O Inno Setup falhou." }
if ($version -ne $numeric) {
    Move-Item "dist\VfxClient-Setup-$numeric.exe" "dist\VfxClient-Setup-$version.exe" -Force
}

Write-Host ""
Write-Host "Pronto! Arquivos em dist\:"
Get-ChildItem dist | ForEach-Object { Write-Host ("  " + $_.Name + "  (" + [math]::Round($_.Length / 1MB, 1) + " MB)") }
