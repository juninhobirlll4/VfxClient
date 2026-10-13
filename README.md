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

Guia completo (build, instalador, pastas, testes, login Microsoft): **[INSTRUCOES.txt](INSTRUCOES.txt)**.


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

## Site de download

A pasta `site/` é publicada no GitHub Pages pela CI (`.github/workflows/pages.yml`) e sempre aponta para os arquivos
da última versão em GitHub Releases. Para ativar: no GitHub, **Settings > Pages > Source = GitHub Actions**.
O repositório precisa ser **público** para o site e os downloads funcionarem para quem não é colaborador.

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

### Feito
- [x] **v0.1 – Base:** Click GUI (Right Shift), config JSON com hot-reload, HUD (FPS, ping, coordenadas, relógio), editor de HUD, brilho total
- [x] **v0.2 – Geral + HUD + Visual:** logo na tela inicial, tecla do menu configurável, comandos `/vfx`; CPS, teclas, armadura, poções, texto; zoom, fogo baixo, sem tremor; 1ª leva de desempenho
- [x] **v0.3 – Desempenho 2ª leva + perfis de FPS:** perfis Qualidade / Equilibrado / Máximo FPS / PC fraco (ajustam o VfxClient e as opções gráficas do Minecraft), culling de entidades atrás de blocos, distância de baús/placas, texturas animadas, chuva, céu e brilho de encantamento desligáveis, itens no chão como um só modelo, limite de tempo de chunks por frame

- [x] **v0.4 – Utilidades + opções estilo OptiFine:** sprint automático e agachar alternado, mira (5 formatos), contorno de bloco, cor do hit, chat com horário e mensagens agrupadas, placar sem números, hora e clima locais, nome próprio no F5; opções gráficas do Minecraft dentro do menu (distância, FPS, vsync, VBO, mipmap, luz...), sem névoa, sem vinheta, animação por tipo, partículas por tipo; site de download

- [x] **v0.5 – Launcher completo + personalização:** launcher jogável (versões oficiais e perfil VfxClient com Legacy Fabric, download de versão/bibliotecas/assets/Java 8 da Mojang, barra de progresso, status do jogo), login Microsoft por código, conta offline, skin (upload oficial ou local) e capa, RAM por perfil com recomendação, diagnóstico do PC, limpeza de cache, atualização pelo GitHub, instalador .exe; no mod: seletor de cor RGB/HEX/alfa, teclas totalmente configuráveis, HUD compacta/avançada, perfis de configuração, presets Baixo/Equilibrado/Alto com otimização automática, skin local
- [x] **v0.5.1 – Loaders no launcher:** botões VfxClient / OptiFine / Forge para o 1.8.9 (OptiFine escolhido pelo jogador por causa da licença; Forge baixado sozinho; Forge + OptiFine como mod)
- [x] **v0.6 – HUD nova + loaders prontos:** Forge embutido no launcher e OptiFine encontrado sozinho no PC; HUD com âncoras, cor principal/secundária, valores destacados, cantos 0–10 px, cores por módulo e layouts prontos (Minimal, PvP, Survival, Content Creator, Competitivo); FPS e ping coloridos por faixa, CPS com média de 5 s, bioma e Nether/Overworld, barra de durabilidade, efeitos com ícone e cor, relógio com segundos, Servidor e Tempo de jogo; ping em número na Tab, chat com até 1000 linhas, Auto GG, print copiado; freelook, motion blur, saturação; cache da HUD, FPS reduzido parado (AFK); exportar/importar config em JSON

- [x] **v0.7 – VfxClient + OptiFine e visual novo:** perfil VfxClient com OptiFine junto (Legacy OptiFabric), HUD plana (cantos retos, fundo 35%), menu com cartões de contorno, busca e engrenagem, prévia ao vivo da mira

### Próximas partes
- [ ] **v0.8 – Social:** waypoints (criar, cor, distância, compartilhar no chat)
- [ ] **v0.8 – Contas e skins VfxClient:** cadastro próprio; skin enviada pelo client e vista por todos que usam o VfxClient (ver abaixo)
- [ ] **v0.9 – Perfis de mods e auto-update** via GitHub Releases
- [ ] **v1.0 – Polimento:** arte final, Javadoc completo, testes com o grupo

### Desempenho: como os ganhos são feitos

Implementações **nossas** das técnicas conhecidas para ganhar FPS na 1.8.9. Cada uma é um toggle e os perfis ligam várias de uma vez:

| Técnica | Módulo | Ganho típico |
|---|---|---|
| Não desenhar entidades atrás de blocos | Culling de entidades | Alto em lobbies e farms |
| Distância máxima de entidades e de baús/placas | Distância de entidades / de baús | Alto em lobbies cheios |
| Menos partículas | Partículas | Alto em PvP (crits, poções) |
| Texturas animadas, céu, chuva, brilho e nuvens desligáveis | Ajustes de render / Sem nuvens | Médio, maior em PCs fracos |
| Itens no chão como um só modelo | Ajustes de render | Médio com muitos drops |
| Limite de tempo de chunks por frame | Ajustes de render | Menos travadas ao andar |
| Gráficos rápidos, VBO, sem sombras, sem vsync, distância menor | Perfil de FPS | Alto, varia por placa de vídeo |
| FPS baixo com a janela sem foco | FPS sem foco | Libera o PC fora do jogo |

HUD em cache: desde a v0.6 o texto da HUD é recalculado no máximo 20 vezes por segundo. Próxima ideia: culling de baús atrás de blocos.
### Contas e skins (v0.8)

- **Conta VfxClient:** e-mail + senha, com nick único reservado. Entra em servidores **não-premium**
  (offline-mode). Servidores premium continuam exigindo conta Microsoft; isso é regra do Minecraft.
- **Skins:** o jogador envia a skin pelo client. O mod consulta nossa API pelo nick e troca a textura
  localmente. Quem tem o VfxClient vê a skin de quem também tem; quem não tem vê a skin padrão.
- **Backend:** precisa de um serviço nosso para contas e skins (decisão pendente, ver abaixo).

## Decisões pendentes

- [ ] Backend de contas/skins: Firebase (sem servidor para manter) ou servidor próprio
- [ ] authlib-injector: com o modelo de skins acima ele deixa de ser necessário; proposta é removê-lo da stack
- [ ] Contas Microsoft também podem usar skin VfxClient?

## Terceiros
- [Legacy OptiFabric](https://github.com/tildejustin/optifabric) (MPL-2.0): embutido sem modificação no launcher para carregar o OptiFine junto com o VfxClient.
- Forge 1.8.9 (LGPL): instalador oficial embutido.
- OptiFine: não vai para o GitHub; só no instalador gerado no PC para o grupo.
