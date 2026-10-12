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
  - Visual: `zoom`, `fullbright`, `lowFire`, `noHurtCam`.
  - Desempenho: `fpsLimiter`, `entityDistance`, `particleLimit`, `noClouds`.
- `hud/`: `HudRenderer` (desenha a HUD), `ClickTracker` (CPS, alimentado por `KeyBinding.onKeyPressed`, códigos -100/-99).
- `command/VfxCommands`: `/vfx`, `/vfx menu`, `/vfx hud`, `/vfx reset` (interceptados antes de ir ao servidor).
- `config/ConfigManager`: salva com debounce de 1 s, hot-reload por data de modificação, arquivo corrompido vira `.broken`.
  Migra a antiga chave `clickGuiKey` (v0.1) para o módulo `menu`.
- `gui/`: `ClickGuiScreen`, `HudEditorScreen`, `Render2D`, `Theme`, `Anim`, `Logo` (ícone e wordmark carregados do classpath).
- `mixin/`: `InGameHud` (HUD), `KeyBinding` (teclas/cliques), `MinecraftClient` (tick + FPS sem foco), `TitleScreen` (logo e versão),
  `GameRenderer` (zoom, sem tremor, sem nuvens), `HeldItemRenderer` (fogo baixo), `ParticleManager` (limite),
  `EntityRenderDispatcher` (distância de entidades), `ClientPlayerEntity` (comandos).

## Estado atual (v0.2.0)
- Build ok. `runClient` sobe sem erro (todos os Mixins de inicialização aplicam). O de `ClientPlayerEntity` só carrega ao entrar num mundo.
- Click GUI, HUD e editor confirmados pelo usuário na v0.1. Os módulos da v0.2 ainda precisam ser testados por ele no jogo.
- O log do jogo em dev quase não aparece: o log4j antigo do 1.8.9 não entende a config que o Loom gera. Para depurar, use `System.out`
  temporariamente ou procure crash reports em `core/run/crash-reports`.

## Próximos passos (ver roadmap no README)
1. Usuário testar os módulos da v0.2 e reportar.
2. v0.3: Social → waypoints.
3. v0.4: launcher jogável offline (baixa MC 1.8.9 da Mojang, Legacy Fabric, Java 8 e o `vfxcore`).

## Decisões pendentes (perguntar ao usuário)
- Backend de contas/skins (v0.9–v0.10): Firebase (recomendado) ou servidor próprio.
- Remover authlib-injector da stack (proposto; o modelo de skins via API própria não precisa dele).
- Contas Microsoft também usam skin VfxClient?
- Performance (v0.6): o usuário quer ganhos "como o Lunar" → implementar técnicas equivalentes com código próprio, nunca copiar.

## Comandos
```bat
gradlew build              :: compila tudo (o que a CI roda)
gradlew :core:runClient    :: Minecraft 1.8.9 com o mod
gradlew :launcher:run      :: launcher
gradlew :core:genSources   :: código do Minecraft navegável (nomes Yarn)
```
O usuário usa Windows (cmd). Ele testa o jogo e relata; você não vê a janela do Minecraft.
