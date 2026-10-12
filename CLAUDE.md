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
- `module/`: `Module` (liga/desliga + `Setting`s + JSON), `HudModule` (texto arrastável com fundo/sombra/cor/tamanho), `Category`.
  Módulos atuais: HUD (`fps`, `ping`, `coordinates`, `clock`), Visual (`fullbright`).
- `config/ConfigManager`: salva com debounce de 1 s, hot-reload por data de modificação, arquivo corrompido vira `.broken`.
- `gui/`: `ClickGuiScreen` (Right Shift), `HudEditorScreen` (drag-and-drop), `Render2D` (rounded rect, degradê, scissor), `Theme` (paleta), `Anim`, `Logo`.
- `mixin/`: `InGameHudMixin` (desenha HUD), `KeyBindingMixin` (tecla do menu), `MinecraftClientMixin` (tick), `TitleScreenMixin`.

## Estado atual (v0.1.0)
- Build e `runClient` funcionam; HUD do FPS aparece no jogo.
- **Em teste:** abrir o menu no Right Shift. Corrigido: o menu abria e fechava no mesmo evento de tecla; agora `onKeyPressed`
  só marca o pedido e a tela abre no fim do tick (`VfxCore.onClientTick`).
- **A confirmar com o usuário:** se as Coordenadas aparecem abaixo do FPS (deveriam; Ping só em servidor; Relógio começa desligado).

## Próximos passos (ver roadmap no README)
1. Confirmar Click GUI, Coordenadas e editor de HUD funcionando no jogo.
2. v0.2: logo na tela de título/splash, `/vfx reset`, trocar a tecla do menu pela GUI.
3. v0.3: mais HUD (CPS, armadura, poções, keystrokes, texto custom).
4. v0.4: launcher jogável offline (baixa MC 1.8.9 da Mojang, Legacy Fabric, Java 8 e o `vfxcore`).

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
