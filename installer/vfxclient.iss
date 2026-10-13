; Instalador do VfxClient (Inno Setup 6).
; A CI compila com:  iscc /DAppVersion=0.1.0 installer\vfxclient.iss
; Pré-requisito: launcher\build\jpackage\VfxClient\ gerado por "gradlew :launcher:jpackageImage".

#ifndef AppVersion
  #define AppVersion "0.0.0"
#endif

#define AppName "VfxClient"
#define AppExe "VfxClient.exe"

[Setup]
AppId={{63A7A140-03B1-45A0-A42C-F31A13187055}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppName}
; Instala por usuário (sem pedir admin) para o auto-update conseguir sobrescrever os arquivos.
PrivilegesRequired=lowest
DefaultDirName={localappdata}\Programs\{#AppName}
DisableProgramGroupPage=yes
OutputDir=..\dist
OutputBaseFilename={#AppName}-Setup-{#AppVersion}
SetupIconFile=..\branding\generated\vfxclient.ico
UninstallDisplayIcon={app}\{#AppExe}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "..\launcher\build\jpackage\VfxClient\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; O OptiFine só aparece em {app}\optifine\ no instalador gerado no PC (build-installer.ps1 copia o
; jar de installer\local\ para a imagem do app). A CI/GitHub nunca recebe o OptiFine.

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent
