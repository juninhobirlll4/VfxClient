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

## v0.14
- Emotes: `emote/` (`Emotes` com 8 animações por ângulos; `EmoteState` quem faz o quê + `applyPose` no TAIL de
  PlayerEntityModel.setAngles; corpo alinhado à cabeça no tick; braço em 1ª pessoa ignorado via EmoteArmMixin),
  `RelaySocket` (WebSocket mínimo Java 8), `RelayTls` (raízes GTS/ISRG em `assets/vfx/certs/roots.pem` + checagem de nome),
  `EmoteRelay` (sala = SHA-256 de host:porta normalizado). `module/social/EmotesModule` (tecla B, câmera, URL avançada;
  `DEFAULT_RELAY` = URL publicada). Roda: `gui/EmoteWheelScreen`.
- Convenção dos ângulos (1.8.9): braço pitch -90 frente, -180 cima; yaw negativo = braço direito para dentro (~20° junta
  as mãos); roll positivo = braço direito para fora ABAIXO do ombro e para dentro acima (`Emotes.outward`).
- `relay/`: Worker + Durable Object (SQLite, plano grátis). `npm run dev` + `npm test`; publicar com `npx wrangler deploy`.

## v0.13.1 (publicado junto na 0.14)
- Desempenho (desligados por padrão): `HideDecorationsModule` (EntityRenderDispatcher.shouldRender), `FarPlayersModule`
  (redirect FeatureRenderer.render em LivingEntityRenderer.renderFeatures, setModelPose TAIL, EntityRenderer.renderLabelIfPresent),
  `BlockDetailsModule` (placas: flag no HEAD + redirect TextRenderer.draw; banners/cabeças no BlockEntityRenderDispatcher; tampa
  do baú zerada), `FpsTestModule` (contagem no HEAD de GameRenderer.render(FJ)V).
- Launcher com Montserrat/Inter: build copia as fontes do core; nomes internos dos arquivos são "Montserrat Thin Black",
  "Montserrat Thin ExtraBold", "Inter Medium" e "Inter" (só bold) — use esses nomes no CSS.
- `VanillaStyle`: slider com trilha embaixo, caixas de texto (TextFieldWidget fill ordinal 0/1). `-Dvfx.openMenu=mundo`.
- Teste em mundo com entidades: servidor de teste com RCON (porta 25575) + `execute Teste ~ ~ ~ summon ...`.
  Cuidado: o jogo de teste pega o foco e recebe o que o usuário digita.

## v0.13
- Fontes Montserrat/Inter (OFL, `assets/vfx/fonts/`) desenhadas por `gui/font/` (`GlyphAtlas` AWT → textura, `FontFamily`
  com cache de até 6 tamanhos: NÃO variar o tamanho de texto por frame; encolher com matriz). `Fonts.setPixelsPerUnit`/`useGuiScale`.
- `gui/Canvas` (referência 1920x1080), `gui/Ui` (botões `buttonFrame`/`buttonLabel`, ícones, `pill`), `gui/ClickArea`.
- `gui/ScreenSwap` troca TitleScreen/null → `VfxTitleScreen`, GameMenuScreen → `VfxPauseScreen`, MultiplayerScreen →
  `VfxServerScreen` (módulo `TitleScreenModule` = "Telas do VfxClient"). Pausa/servidores estendem as telas do jogo e
  reaproveitam as ações pelos ids dos botões originais.
- Desfoque: `BlurModule` + `BlurredScreen` (PostEffect `blur.json` depois do mundo). `InGameHudMixin` esconde a HUD sob
  `BlurredScreen` mas PRECISA chamar `gameRenderer.setupHudMatrixMode()` antes de cancelar (sem isso a tela some).
- `gui/VanillaStyle` + mixins `ButtonWidget`, `OptionSliderWidget`/`SliderWidget` (pegador), `ScreenBackground`
  (terra → fundo escuro), `DrawableHelper` (título = texto branco centralizado com y<=20), `ListWidget` (scissor, sem faixas),
  `TextureManager` (terra das listas → véu 1x1; o OptiFine muda o método que desenha esse fundo, por isso é na textura).
- `TestShots`: `-Dvfx.titleShot=true|servidores|opcoes|video|controles`, `-Dvfx.openMenu=pausa|<id>`.
- Código: imports em ordem (java/javax, depois alfabético), sem nome de classe completo no meio do código, métodos curtos.

## v0.12
- Menu: `Theme.MODULE_ON/OFF` (verde/vermelho), entrada dos cartões em sequência (`listChangedAt`), opções deslizando
  (`settingsOpenedAt`). Toda módulo desligável ganha `toggleKey` (via `ModuleManager.register` → `Module.enableToggleKey`);
  `VfxCore.onKeyPressed` alterna e chama `ToggleNotificationModule.notify`.
- Novos: `BlockCounterModule`, `HitboxModule` (+EntityRenderDispatcherMixin renderHitbox/drawBox), `ParticleColorModule`
  (redirect createParticle, ids 9/10), `FovModule` (getFov + updateMovementFovMultiplier), `ViewModelModule`
  (applyEquipAndSwingOffset TAIL), `ScrollTooltipsModule` (`ScreenTooltipMixin`), `HotbarModule` (redirect drawTexture
  ordinal 0/1 em renderHotbar). Launcher: `ui/TrayManager` (bandeja, `Platform.setImplicitExit(false)`, fechar janela = sair).
- Testes de menu (`-Dvfx.openMenu`) e tela inicial (`-Dvfx.titleShot`) fecham o jogo sozinhos depois do print.

## v0.11
- Atualização: `ui/UpdateNotifier` (checa ao abrir e a cada 30 min; janela "Atualizar agora / Mais tarde"; notificação do Windows
  via SystemTray se minimizado; "Mais tarde" deixa faixa na Home). `Updater.download` (Downloader com progresso) + `Updater.run`
  (`/SILENT /CLOSEAPPLICATIONS`), depois `System.exit(0)`; o .iss reabre o app com `Check: WizardSilent` e usa `CloseApplications=force`.
- CI `release.yml`: push no main publica se `v<client_version>` ainda não existe (senão SKIP); tag v* continua valendo.
  Fluxo de versão nova: subir `client_version` + changelog.txt, commit, `git push`. Repositório precisa ser público para a API.
- Teste: `-Dvfx.fakeUpdate=X` (com `-Dvfx.snapshot` salva `atualizacao.png`).

## v0.10
- Launcher simples e animado: `ui/Motion` (enter, hoverGrow, toast, progress; desligado com `-Dvfx.snapshot`), troca de aba com
  fade em `VfxLauncherApp.show`. `AccountView`: linhas de conta (clique escolhe, x remove), nick + Entrar, Microsoft como link,
  skin com arrastar e soltar (`drop-zone`), capa só em conta Microsoft. `SettingsView`: memória (GB), desempenho, ao abrir o
  jogo, avisos; resto em `TitledPane` "Avançado". Salva sozinho. `ui/Faces` recorta o rosto da skin.

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
