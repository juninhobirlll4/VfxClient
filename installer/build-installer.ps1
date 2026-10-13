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
#
# OptiFine junto (só para o instalador do grupo, gerado no seu PC): deixe o
# OptiFine_1.8.9_*.jar em installer\local\ (ou na Área de Trabalho) e rode o script.
# Ele vai para a pasta optifine\ do VfxClient instalado e o launcher importa sozinho.
# Para gerar sem ele:  build-installer.ps1 -SemOptiFine
# installer\local\ e jars do OptiFine são ignorados pelo git e nunca vão para o GitHub.

param([switch]$SemOptiFine)

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

# OptiFine local (opcional)
$optifine = $null
if (-not $SemOptiFine) {
    New-Item -ItemType Directory -Force "installer\local" | Out-Null
    $optifine = Get-ChildItem "installer\local" -Filter "OptiFine_1.8.9*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $optifine) {
        $desktop = [Environment]::GetFolderPath("Desktop")
        $found = Get-ChildItem $desktop -Filter "OptiFine_1.8.9*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            Copy-Item $found.FullName "installer\local\" -Force
            $optifine = Get-Item ("installer\local\" + $found.Name)
        }
    }
}
$appOptiFine = "launcher\build\jpackage\VfxClient\optifine"
if (Test-Path $appOptiFine) { Remove-Item -Recurse -Force $appOptiFine }
if ($optifine) {
    Write-Host "OptiFine incluído: $($optifine.Name)"
    New-Item -ItemType Directory -Force $appOptiFine | Out-Null
    Copy-Item $optifine.FullName $appOptiFine -Force
} else {
    Write-Host "OptiFine não incluído (coloque o jar em installer\local\ para incluir)."
}
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
