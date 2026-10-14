# CLAUDE.md – VfxClient

Instruções para o Claude Code neste repositório. Leia antes de qualquer tarefa.

## O projeto
Client de Minecraft **1.8.9** próprio de um grupo fechado de amigos: mod in-game (`core`) + launcher desktop (`launcher`).
Tudo é nosso: nome, logo, cores (roxo → branco), código e assets.

## Regras (não negociáveis)
- **Um commit por funcionalidade**; `gradlew build` precisa passar em todo commit.
- **Nada de outro client no código**: nenhum nome, pacote, classe, texto ou asset de Lunar, Badlion, CMClient etc.
  Não fazer engenharia reversa nem extrair código/assets de outros clients. Referência externa = só comportamento observado jogando, descrito em issue.
- Assets novos começam como **placeholder versionado** em `branding/svg/` (SVG → PNG via `branding/render_icons.py`).
- **Dúvida de escopo: perguntar antes de implementar.**
- Javadoc, comentários, docs e textos da interface em **pt-BR**.
- Stack não muda sem perguntar.

## Stack
- Minecraft 1.8.9 + **Legacy Fabric** Loader 0.18.x, **Legacy Yarn** (build em `gradle.properties`), Mixin. **Sem** Legacy Fabric API (removida de propósito: tudo via Mixins próprios).
- Mod compila para **Java 8** (`options.release = 8`): sem `var`, `record`, text blocks, APIs pós-Java 8. Gson do jogo é antigo (2.2.4): use `new JsonParser().parse(...)`.
- Launcher: Java 21 + JavaFX 21, empacotado com jpackage (app-image) + Inno Setup.
- Gradle 9 (wrapper) + Fabric Loom/Legacy Looming 1.16. Rodar Gradle com JDK 21; `runClient` usa JDK 8 (`run_java_version`).
- CI: `.github/workflows/build.yml` (push/PR) e `release.yml` (tag `v*` → instalador, zip portátil e jar do mod).

## Convenções
- Pacote `com.vfxclient.core` / `com.vfxclient.launcher`. Mod id e namespace de assets: `vfx`.
- Dados do jogador: `.minecraft/vfxclient/` (config em `config/modules.json`).
- Membros adicionados por Mixin: prefixo `vfx$` + `@Unique`.
- Mod resources em `assets/vfx/` **não** passam pelo ResourceManager do 1.8.9 (namespace não registrado):
  carregue imagens direto do classpath (ver `gui/Logo.java`).
- Nomes Yarn do 1.8.9 diferem do MCP. Exemplos já verificados: `MinecraftClient.getCurrentFps()` (static), `client.player.x/y/z/yaw`,
  `Screen.keyPressed(char,int)`, `mouseClicked/mouseReleased/mouseDragged`, `Screen.handleMouse()` (**sem** `throws IOException`),
  `DrawableHelper.fill` (static), `TextRenderer.draw(String,float,float,int,boolean)`, `Window(client).getWidth()` (escalado),
  `KeyBinding.onKeyPressed(int)` (static), `InGameHud.render(F)V`, `MinecraftClient.tick()`.
  Na dúvida, rode `gradlew :core:genSources` e confira o código-fonte gerado.

## Arquitetura do core
- `VfxCore` (entrypoint `client`): cria `ModuleManager`, carrega `ConfigManager`, recebe tick e teclas dos Mixins.
  `VfxCore.module(Classe.class)` é o atalho que os Mixins usam (devolve `null` antes da inicialização).
- `module/`: `Module` (liga/desliga + `Setting`s + JSON; `isAlwaysOn()` esconde a chave), `Category` (Geral, HUD, Visual, Desempenho, Social).
  - `HudModule`: base de tudo que aparece na HUD (posição, fundo, escala); subclasses informam tamanho e desenham a partir de (0,0).
    `TextHudModule` para texto de uma ou várias linhas.
  - Settings: `BooleanSetting`, `NumberSetting` (slider), `TextSetting` (caixa de texto), `KeySetting` (captura de tecla/botão do mouse).
  - Geral: `menu` (tecla do menu, sempre ligado), `titleScreen`.
  - HUD: `fps`, `ping`, `coordinates`, `clock`, `cps`, `keystrokes`, `armor`, `potions`, `customText`.
  - Utilidades: `toggleSprint` (HUD de estado, força `KeyBinding.isPressed`), `crosshair`, `blockOutline`, `hitColor`, `chat`,
    `scoreboard`, `timeChanger`, `weatherChanger`, `ownNametag`.
  - Visual: `zoom`, `fullbright`, `lowFire`, `noHurtCam`.
  - Desempenho: `performanceProfile` (perfis que ajustam módulos + GameOptions vanilla), `renderTweaks`, `entityCulling`,
    `vanillaOptions` (GameOptions dentro da GUI; lê no `onSettingsOpened`), `entityDistance`, `blockEntityDistance`,
    `particleLimit` (por tipo), `noClouds`, `fpsLimiter`. Não dá para usar OptiFine (não existe para Fabric 1.8.9 e a licença
    proíbe redistribuir): as otimizações são nossas.
  - Settings também: `ChoiceSetting` (ciclo de opções). `Module.onSettingChanged` é chamado pela GUI após mudança manual;
    mudar algo em Desempenho marca o perfil como "Personalizado".
- `hud/`: `HudRenderer` (desenha a HUD), `ClickTracker` (CPS, alimentado por `KeyBinding.onKeyPressed`, códigos -100/-99).
- `command/VfxCommands`: `/vfx`, `/vfx menu`, `/vfx hud`, `/vfx reset` (interceptados antes de ir ao servidor).
- `config/ConfigManager`: salva com debounce de 1 s, hot-reload por data de modificação, arquivo corrompido vira `.broken`.
  Migra a antiga chave `clickGuiKey` (v0.1) para o módulo `menu`.
- `gui/`: `ClickGuiScreen`, `HudEditorScreen`, `Render2D`, `Theme`, `Anim`, `Logo` (ícone e wordmark carregados do classpath).
- `mixin/`: `InGameHud` (HUD), `KeyBinding` (teclas/cliques), `MinecraftClient` (tick + FPS sem foco), `TitleScreen` (logo e versão),
  `GameRenderer` (zoom, sem tremor, sem nuvens), `HeldItemRenderer` (fogo baixo), `ParticleManager` (limite),
  `EntityRenderDispatcher` (distância + culling de entidades), `ClientPlayerEntity` (comandos).
  `mixin/perf/`: `SpriteAtlasTexture` (animações), `WorldRenderer` (céu, tempo de chunks), `ItemRenderer`/`ArmorFeatureRenderer` (brilho),
  `ItemEntityRenderer` (`method_10222` = cópias do modelo do item), `BlockEntityRenderDispatcher` (distância de baús).
- **Não** coloque classes comuns dentro de `com.vfxclient.core.mixin`: o Mixin proíbe carregar classes do pacote de mixins.

## Launcher (v0.5)
- Pacotes em `launcher/src/main/java/com/vfxclient/launcher/`: `auth` (Microsoft device code → XBL → XSTS → MC; offline; `MinecraftServices`
  skin/capa), `profiles`, `settings` (`SystemInfo` RAM), `mods` (`ModInstaller`: vfxcore embutido em resources/mods/vfxcore.jar pelo
  processResources), `game` (`Downloader` paralelo com SHA-1, `VersionManifest`, `VersionResolver` com inheritsFrom, `LibraryInstaller`
  + nativos, `AssetInstaller`, `JavaRuntimeInstaller` = runtimes oficiais Mojang, `LegacyFabric` meta API, `GameLauncher`),
  `optimization` (`Diagnostics`, `CacheCleaner`, `JvmFlags`), `updater`, `ui` (Home/Account/Settings + `SkinPreview`), `cli`.
- Dados em `%APPDATA%\.vfxclient` (`-Dvfx.home=` muda). Mod recebe `-Dvfx.profile` e `-Dvfx.preset=low|balanced|high`.
- LWJGL do Legacy Fabric: lib Maven com `natives` e sem `downloads` (só existe o jar `natives-windows`).
- Testes sem GUI: `gradlew :launcher:run --args="--cli launch --offline Nick --wait"` (baixa e abre o jogo de verdade);
  `-Dvfx.snapshot=<pasta>` no gradlew salva PNG de cada tela e fecha (revisão visual).
- Instalador: `installer\build-installer.ps1` (jpackage app-image + Inno Setup instalado via winget). `jpackage --add-modules`
  inclui jdk.crypto.ec (HTTPS). O app-image não tem `java.exe` solto; teste com `VfxClient.exe --cli ...` e o código de saída.
- Login Microsoft precisa de Client ID do Azure (settings.json) e aprovação da Mojang; sem isso, conta offline.
- Loaders (v0.5.1): `Profile.Kind` VFX / OPTIFINE / FORGE / VANILLA; perfis fixos `vfxclient-1.8.9`, `optifine-1.8.9`, `forge-1.8.9`.
  `Forge` lê `install_profile.json` do instalador oficial (pula libs com clientreq=false); o instalador vem embutido em
  `launcher/src/main/resources/com/vfxclient/launcher/loaders/` (LGPL), conferido por SHA-1, com fallback no Maven.
  OptiFine (decisão do usuário, v0.6): vai SÓ no instalador gerado localmente (`build-installer.ps1` copia o jar de
  `installer/local/` ou da Área de Trabalho para `{app}\optifine\`); nunca no git/CI/release pública (licença; `.gitignore`
  bloqueia jars e `installer/local/`). `OptiFine.autoDetect()` procura primeiro em `{app}\optifine\`, depois em Desktop,
  Downloads e .minecraft e importa para `launcher/optifine/`; a versão usa LaunchWrapper + `optifine.OptiFineTweaker`
  (Patcher do OptiFine). Loaders pré-instalados em segundo plano ao abrir o launcher. Forge, OptiFine e Forge+OptiFine
  testados abrindo via CLI (v0.6). CLI `--join host:porta` entra direto num servidor (`-Dvfx.join`).
  OptiFine/Forge rodam SEM os recursos do VfxClient (decisão do usuário: opção 1; port para Forge fica para depois).

## v0.9
- `gui/VfxTitleScreen` (estende TitleScreen; `super.init()` + `buttons.clear()`; fundo via `TitleScreenAccessor` @Invoker
  renderBackground). Troca no `MinecraftClientMixin` @Inject HEAD do setScreen (TitleScreen exato ou null sem mundo → cancela e
  abre a nossa). NÃO usar ModifyVariable no PUTFIELD (a tela original ia para currentScreen sem init → NPE).
- `AutoTextModule` (6 tecla+texto, cooldown 1,5 s, via `VfxCore.onKeyPressed`); `ChatModule` anti-spam/bloqueio/cópia
  (`hud/ChatLines` implementado no `ChatHudMixin`, `ChatScreenMixin` Ctrl+clique); `TabListModule.alwaysHeads` (redirect
  `ClientConnection.isEncrypted` no PlayerListHud.render); placar compacto (push/pop em renderScoreboardObjective HEAD/RETURN).
- Launcher: aba Prints (`ScreenshotsView`). Testes: `-Dvfx.titleShot=true` no gradlew :launcher:run (repassado ao jogo)
  salva `screenshots/vfx-teste-inicio.png` e fecha o jogo sozinho.

## v0.8
- Launcher minimalista: `HomeView` = cartão (logo, JOGAR, seletor de versão com só a versão no botão, "+"), painel Novidades
  (`resources/.../changelog.txt`, linhas com `#` = versão) e rodapé com preset/RAM. Sem botões de loader nem opções de OptiFine.
  Sem OptiFine no PC: aviso + "Baixar OptiFine" (abre a página oficial; `Timeline` chama `OptiFine.autoDetect()` a cada 2 s).
  NÃO baixar o OptiFine automaticamente do site (raspagem bloqueada pela permissão do ambiente).
- `-Dvfx.snapshot` agora desenha as telas sem mostrar a janela.
- Editor de HUD: botão "X" no elemento em destaque desliga o módulo.

## v0.7
- Perfil VFX carrega o OptiFine junto: `OptiFine.syncFabricMods` põe o jar do OptiFine + Legacy OptiFabric (embutido em
  `resources/.../loaders/optifabric-legacy.jar`, MPL-2.0) em `mods/` (`Profile.optifineWithVfx`, padrão true). Testado: abre e entra
  no mundo sem erro de mixin. `ZoomModule` desliga o `of.key.zoom` se usar a mesma tecla.
- Visual plano (`Theme` neutro, `OUTLINE_ON/OFF`, `Render2D.outlinedRect/gear`); `HudStyleModule.styleVersion=2` migra configs
  antigas uma vez (`applyFlatLook`). Menu: cartões com contorno, iniciais, engrenagem, busca (`search`). Mira com prévia.
- Teste visual SEM teclado/mouse: `-Dvfx.openMenu=<id|qualquer>` (extraJvmArgs do perfil) abre o menu e salva
  `screenshots/vfx-teste.png`. NÃO automatizar teclado/mouse nem trazer a janela para frente: o jogo de teste rouba o foco
  e recebe o que o usuário digita.

## HUD (v0.6)
- `HudModule`: 9 âncoras (`anchor` no JSON; sem âncora = canto superior esquerdo, configs antigas), `screenX/screenY`,
  `moveTo` (editor escolhe a âncora pelo terço da tela), `place` (layouts). Marcadores `VALUE`/`TEXT` no texto pintam valores
  com a cor principal (`HudStyleModule.accentColor`) ou secundária (`isSecondary()` → `secondaryColor`). Cores próprias por módulo.
- `TextHudModule` faz cache das linhas (`HudStyleModule.cacheMillis`, padrão 50 ms) fora do editor.
- `HudLayoutModule`: layouts prontos; instalação nova aplica o PvP no primeiro tick (`ConfigManager` sem arquivo).
- Novos: `ServerIpModule`, `PlaytimeModule`, `TabListModule` (+`PlayerListHudMixin`), `AutoGgModule` (via `ChatHudMixin`),
  `ScreenshotModule` (AWT clipboard), `FreelookModule` (redirect de `increaseTransforms` + troca de yaw/pitch em `transformCamera`),
  `MotionBlurModule`/`SaturationModule` (`render/PostEffect` com shaders vanilla phosphor/color_convolve + `ShaderEffectAccessor`),
  `FpsLimiterModule` com AFK. Discord Rich Presence foi feito e removido (reprovado pelo usuário na v0.6).
- Teste em mundo: servidor oficial 1.8.9 offline local + `--cli launch --join localhost:25565`; prints da janela via .NET.

## Estado atual (v0.5.0)
- Build ok; launcher baixa e abre o 1.8.9 + Legacy Fabric + VfxClient (testado via CLI, conta offline).
- Mod: seletor de cor (`ColorSetting` ARGB, JSON `#AARRGGBB`, migra índices antigos), `ButtonSetting`, `ChoiceSetting` dinâmica,
  opções avançadas (`addAdvanced`, modo Simples/Completo em Geral > Menu), prévia ao vivo de `HudModule` no menu,
  `HudStyleModule` (compacto/avançado), perfis de config (`config/profiles/*.json`), presets Baixo/Equilibrado/Alto/Personalizado
  com otimização automática e `SystemSpecs`, `LocalSkinModule` + `SkinMixin`.
- Falta teste do usuário dentro do jogo para os módulos novos e para o login Microsoft (depende do Client ID dele).

## Próximos passos
1. v0.7: Social → waypoints (e minimapa opcional, desligado em PvP).
2. v0.8: servidor de contas/skins VfxClient (backend: usuário avaliando VPS; Hetzner CX23 recomendado, ou Supabase).
## Decisões pendentes (perguntar ao usuário)
- Backend de contas/skins (v0.8): usuário não quer Firebase; opções apresentadas: Supabase, Cloudflare (Workers + D1 + R2) ou VPS própria.
- Remover authlib-injector da stack (proposto; o modelo de skins via API própria não precisa dele).
- Decidido: contas Microsoft usam a skin original (oficial); skin VfxClient só para contas VfxClient.
- Performance (v0.6): o usuário quer ganhos "como o Lunar" → implementar técnicas equivalentes com código próprio, nunca copiar.

## Comandos
```bat
gradlew build              :: compila tudo (o que a CI roda)
gradlew :core:runClient    :: Minecraft 1.8.9 com o mod
gradlew :launcher:run      :: launcher
gradlew :core:genSources   :: código do Minecraft navegável (nomes Yarn)
```
O usuário usa Windows (cmd). Ele testa o jogo e relata; você não vê a janela do Minecraft.
