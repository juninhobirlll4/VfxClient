<p align="center">
  <img src="branding/generated/wordmark.png" alt="VfxClient" width="480">
</p>

# VfxClient

Client de Minecraft **1.8.9** feito por nós, para nós. Nosso nome, nossa logo, nossas cores e nosso código.

| | |
|---|---|
| Jogo | Minecraft Java Edition 1.8.9 |
| Mod | Legacy Fabric Loader + Legacy Fabric API, Legacy Yarn, Mixin, **Java 8** |
| Launcher | JavaFX 21 empacotado com jpackage, instalador Inno Setup |
| Build | Gradle 9 (wrapper) + Fabric Loom / Legacy Looming |
| CI | GitHub Actions: build a cada push, release a cada tag `v*` |

## Estrutura

```
VfxClient/
├── core/                 # mod "vfxcore" (id: vfx) que roda dentro do jogo
├── launcher/             # launcher desktop JavaFX
├── branding/             # SVGs da marca (fonte), PNG/ICO gerados e paleta
├── installer/            # script do Inno Setup
├── .github/workflows/    # build.yml (verificação) e release.yml (publicação)
└── CONTRIBUTING.md       # regras de trabalho
```

Dados do client no jogador: `.minecraft/vfxclient/` (config em `.minecraft/vfxclient/config/*.json`).

## Como rodar

Requisitos: **JDK 21** (roda o Gradle e o launcher) e **JDK 8** (roda o Minecraft de desenvolvimento).

```bat
:: abre o Minecraft 1.8.9 com o VfxClient carregado
gradlew :core:runClient

:: abre o launcher
gradlew :launcher:run

:: compila tudo (é o que a CI roda)
gradlew build

:: gera launcher\build\jpackage\VfxClient\VfxClient.exe
gradlew :launcher:jpackageImage
```

A primeira execução demora: o Gradle baixa o Minecraft, os mappings e remapeia o jogo.
Se o Gradle não encontrar o JDK 8, ele baixa um automaticamente. Para rodar em Java 21,
troque `run_java_version` em `gradle.properties`.

**Abrir no IntelliJ:** *File → Open* na pasta do repo e aceitar o import do Gradle.
Depois rode `gradlew :core:genSources` para ter o código do Minecraft navegável.

## Release

```bat
git tag v0.1.0
git push origin v0.1.0
```

A CI publica no GitHub Releases o instalador, o zip portátil e o jar do mod.
Tags com sufixo (`v0.2.0-beta`) saem como *pre-release*.

---

## Roadmap

### M0 – Scaffold ✅
- [x] Monorepo Gradle com `core` e `launcher`
- [x] Mod carrega no 1.8.9 e registra no log (valida Mixin)
- [x] Launcher abre com a nossa identidade, versões visíveis e botão Jogar (desativado)
- [x] CI de build e de release (instalador + portátil + jar do mod)
- [x] Marca placeholder (SVG → PNG/ICO) versionada

### MVP – "dá pra jogar com a nossa cara"
- [x] **v0.1 – Click GUI + base:** menu no **Right Shift** com categorias (HUD, Visual, Desempenho, Social), cartões de módulos, opções por módulo e animações
- [x] **v0.1 – Config:** `.minecraft/vfxclient/config/modules.json`, salva sozinha, hot-reload ao editar o arquivo, "Restaurar padrão" por módulo
- [x] **v0.1 – HUD:** FPS, ping, coordenadas e relógio, com fundo, sombra, cor da marca e tamanho configuráveis
- [x] **v0.1 – Editor de HUD:** arrastar e soltar, grudar nas bordas/centro, botão direito volta à posição padrão
- [x] **v0.1 – Visual:** Brilho total
- [ ] **v0.2 – Identidade in-game:** logo na tela de título e splash; comando `/vfx reset`; trocar a tecla do menu pela GUI
- [ ] **v0.3 – Mais HUD:** CPS, armadura, efeitos de poção, teclas (keystrokes), widget de texto custom
- [ ] **v0.4 – Launcher jogável (offline):** baixa o Minecraft 1.8.9 dos servidores da Mojang, o Legacy Fabric, um runtime Java 8 e o `vfxcore`; botão Jogar e status de download
- [ ] **v0.5 – Login Microsoft:** contas premium
- [ ] **v0.6 – Performance (FPS):** módulos de otimização próprios (ver abaixo)

### v1 – "client completo"
- [ ] **v0.7 – Partículas:** toggles geral e por tipo, vanilla e custom
- [ ] **v0.8 – Waypoints:** criar, renomear, cor, distância, compartilhar via comando/chat
- [ ] **v0.9 – Contas VfxClient:** cadastro e login próprios (ver abaixo)
- [ ] **v0.10 – Skins VfxClient:** cada jogador envia a skin pelo client; todos com o VfxClient veem
- [ ] **v0.11 – Perfis e auto-update:** perfis performance / partículas / social; atualização via GitHub Releases
- [ ] **v1.0 – Polimento:** arte final, Javadoc completo, testes com o grupo

### Performance (v0.6)

Implementações **nossas** das técnicas que clients de PvP usam para ganhar FPS na 1.8.9.
Cada uma vira um módulo com toggle e é medida com benchmark antes/depois:

- Culling de entidades e tile entities fora da visão ou atrás de blocos
- Distância máxima de render para entidades, baús, placas e banners
- Limite e culling de partículas
- HUD/texto em cache, redesenhado só quando muda
- FPS limitado em menus e com a janela sem foco
- Limite de atualizações de chunk por frame
- Opções para desligar névoa, nuvens, céu e texturas animadas

### Contas e skins (v0.9–v0.10)

- **Conta VfxClient:** e-mail + senha, com nick único reservado. Entra em servidores **não-premium**
  (offline-mode). Servidores premium continuam exigindo conta Microsoft; isso é regra do Minecraft.
- **Skins:** o jogador envia a skin pelo client. O mod consulta nossa API pelo nick e troca a textura
  localmente. Quem tem o VfxClient vê a skin de quem também tem; quem não tem vê a skin padrão.
- **Backend:** precisa de um serviço nosso para contas e skins (decisão pendente, ver abaixo).

## Decisões pendentes

- [ ] Backend de contas/skins: Firebase (sem servidor para manter) ou servidor próprio
- [ ] authlib-injector: com o modelo de skins acima ele deixa de ser necessário; proposta é removê-lo da stack
- [ ] Contas Microsoft também podem usar skin VfxClient?
