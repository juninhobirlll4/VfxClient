<p align="center">
  <img src="branding/generated/wordmark.png" alt="VfxClient" width="480">
</p>

<p align="center">
  <b>Client de Minecraft 1.8.9 feito por nós, para nós.</b><br>
  Menu próprio, HUD personalizável, emotes, skins para os amigos, otimizações de FPS e OptiFine no mesmo jogo.
</p>

<p align="center">
  <img alt="Versão" src="https://img.shields.io/badge/vers%C3%A3o-0.15.0-7B2FF7">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.8.9-62B47A">
  <img alt="Windows" src="https://img.shields.io/badge/Windows-10%20%7C%2011-0078D6">
  <img alt="Build" src="https://github.com/juninhobirlll4/VfxClient/actions/workflows/build.yml/badge.svg">
</p>

---

## Sumário

- [O que é](#o-que-é)
- [Novidades da v0.15](#novidades-da-v015)
- [Instalar e jogar](#instalar-e-jogar)
- [Emotes e skins para quem usa o VfxClient](#emotes-e-skins-para-quem-usa-o-vfxclient)
- [Recursos](#recursos)
- [Desempenho](#desempenho)
- [Para desenvolvedores](#para-desenvolvedores)
- [Histórico de versões](#histórico-de-versões)
- [Próximos passos](#próximos-passos)
- [Terceiros e licenças](#terceiros-e-licenças)

## O que é

O VfxClient tem duas partes:

| Parte | O que faz |
|---|---|
| **Launcher** (`VfxClient.exe`) | Contas (Microsoft ou offline), skin, RAM, diagnóstico do PC, download do jogo e do Java, atualização automática. |
| **Mod** (`vfxcore`) | Roda dentro do jogo: menu (Right Shift), HUD, utilidades, visual e otimizações. |

No launcher você escolhe o loader do 1.8.9:

| Loader | O que abre |
|---|---|
| **VfxClient** (padrão) | VfxClient **+ OptiFine** no mesmo jogo |
| OptiFine | Só o Minecraft com OptiFine |
| Forge | Forge 1.8.9 para os seus mods (com OptiFine opcional) |

## Novidades da v0.16

- **Motor de FPS** (Desempenho, desligado por padrão): *FPS Boost* (corta enfeites sozinho quando o FPS cai), *World Optimization*
  (sem sombras de entidades), *Lazy Chunk Loading* (chunks aos poucos) e *GPU Fencing* (menos atraso de entrada). Funciona junto com o OptiFine.
- **Cartão do servidor** no HUD com o ícone do servidor, o endereço, o ping e os jogadores, com o ícone esmaecido no fundo; o aviso do
  pacote de textura usa o mesmo cartão.
- A logo do VfxClient fica só ao lado do nome (não ao lado da vida).

## Novidades da v0.15

- **Roda de emotes** como a do Lunar: segure **B**, aponte e solte. São **56 emotes em 7 páginas** (a rodinha do mouse troca
  de página) e dá para criar um **atalho de tecla** para qualquer um deles. Quem usa o VfxClient no mesmo servidor vê os seus emotes.
- **Logo do VfxClient** ao lado do nick de quem usa o client, em cima do jogador e na Tab.
- **Skins online**: os amigos que usam o client veem a sua skin (contas offline, com chave secreta por nick).
- **Zoom suave**: entra e sai com animação e a rodinha do mouse ajusta a aproximação.
- **Mira estilo CS** (ponto no centro, contorno, abrir ao andar e atacar) e um **editor para desenhar a sua mira**.
- **Aviso do pacote de textura** com a logo, o nome completo e a descrição.

<details>
<summary><b>Novidades da v0.13</b></summary>

- **Telas do jogo no visual do VfxClient**, com as letras Montserrat (títulos e botões) e Inter (textos):
  - **tela inicial** redesenhada, com a logo grande e os botões no centro;
  - **menu de pausa** com o mundo desfocado atrás, botões grandes e etiquetas de servidor, ping, jogadores e tempo de sessão;
  - **multijogador** com cartões, ping colorido, favoritos (estrela), abas Meus/Favoritos/LAN e busca;
  - **Opções, Vídeo, Controles** e as demais telas do jogo (inclusive as do OptiFine) com botões planos e fundo escuro.
- Tudo volta ao original em **Geral > Telas do VfxClient**.
- Código reorganizado (imports, métodos menores), sem mudança de comportamento.

</details>

<details>
<summary><b>Novidades da v0.7</b></summary>

- **VfxClient + OptiFine juntos:** o perfil VfxClient já abre com o OptiFine, sem precisar escolher outro loader.
  Dá para desligar na tela Início ("OptiFine junto com o VfxClient").
- **Visual novo, plano e escuro:**
  - HUD com fundo translúcido, cantos retos e texto branco com sombra;
  - teclas (WASD e cliques) quadradas, que ficam brancas ao pressionar;
  - menu com cartões de contorno (contorno branco = ligado), iniciais de cada módulo e engrenagem para as opções.
- **Busca no menu:** com o menu aberto, é só digitar o nome do módulo.
- **Prévia da mira:** nas opções da Mira, a mira aparece ao vivo sobre o céu e sobre um fundo escuro, ampliada e em tamanho real.
- **Zoom sem conflito:** com o OptiFine junto, vale o zoom do VfxClient (o do OptiFine é desligado se usar a mesma tecla).
- Quem já tinha config salva passa para o visual novo uma vez, sem perder posições nem módulos ligados.

</details>

<details>
<summary><b>Novidades da v0.6</b></summary>

- Forge embutido no launcher e OptiFine encontrado sozinho no PC.
- HUD presa a âncoras (continua no lugar ao mudar o tamanho da janela) e redimensionável no editor (arraste o canto ou use a rodinha).
- Layouts prontos: Minimal, PvP, Survival, Content Creator e Competitivo.
- FPS e ping coloridos (opcional), CPS com média de 5 s, coordenadas com bioma e conversão Nether/Overworld, armadura com barra de durabilidade, efeitos com ícone e cor, relógio com segundos.
- Módulos novos: Servidor, Tempo de jogo, ping em número na Tab, chat com até 1000 linhas, Auto GG, print copiado para a área de transferência, Freelook, Motion blur e Saturação.
- Desempenho: cache da HUD e FPS reduzido quando você fica parado (AFK).
- Exportar e importar todas as opções em JSON.

</details>

## Instalar e jogar

1. Baixe e rode `VfxClient-Setup-<versão>.exe` (instala por usuário, sem pedir administrador).
2. Abra o **VfxClient** pelo atalho.
3. Aba **Conta**: entre com Microsoft ou crie uma conta offline.
4. Aba **Início**: deixe o loader **VfxClient** e clique em **INICIAR**.
   Na primeira vez ele baixa cerca de 300 MB (Minecraft, Java 8 e Legacy Fabric).
5. No jogo, aperte **Right Shift** para abrir o menu.

**OptiFine:** a licença do OptiFine não permite publicá-lo. O instalador público sai sem ele; o launcher procura sozinho
um `OptiFine_1.8.9_*.jar` na Área de Trabalho, em Downloads ou no `.minecraft`. Também dá para usar
**Selecionar OptiFine...** na tela Início ou baixar no [site oficial](https://optifine.net/downloads).

Comandos no chat: `/vfx`, `/vfx menu`, `/vfx hud` (editor de HUD) e `/vfx reset`.

## Emotes e skins para quem usa o VfxClient

Emotes, skins e a logo ao lado do nick aparecem **para quem também usa o VfxClient** no mesmo servidor de Minecraft.
Quem joga com outro client ou com o jogo normal não vê nada disso (e também não atrapalha).

| Recurso | Quem vê | Como funciona |
|---|---|---|
| **Emotes** (tecla **B**) | Quem usa o VfxClient no mesmo servidor | Segure **B**, aponte e solte. São 56 emotes em 7 páginas (a rodinha do mouse troca de página). Com o mouse sobre um emote, apertar uma tecla cria o atalho dele. |
| **Logo ao lado do nick** | Quem usa o VfxClient no mesmo servidor | Aparece em cima do jogador e na lista de jogadores (Tab). |
| **Skin** | Quem usa o VfxClient, em **servidor offline** | No launcher, aba **Conta**, escolha o PNG da skin. Ela é enviada ao servidor do VfxClient e os amigos passam a vê-la. |

- Tudo isso passa por um servidor do próprio VfxClient na Cloudflare (pasta [`relay/`](relay/README.md)). O client manda só um código
  gerado a partir do endereço do servidor de Minecraft, nunca o endereço em si, e nada de emote fica guardado.
- **Skins:** valem para **contas offline** (servidores sem verificação). Quem tem conta original (Microsoft) continua com a
  skin oficial. O primeiro envio de um nick o "reivindica" com uma chave secreta guardada no seu launcher; só você troca a sua skin,
  e em outro PC o nick aparece como ocupado.
- Para desligar: **Social > Emotes** (ver os emotes dos outros, mostrar os seus, logo ao lado do nick) e
  **Geral > Skins do VfxClient** (ver a skin dos amigos).

## Recursos

### HUD
| Módulo | Destaques |
|---|---|
| FPS | menor valor dos últimos segundos; cor por faixa (opcional) |
| Ping | cor por qualidade da conexão (opcional) |
| Coordenadas | direção, chunk, bioma, conversão Nether/Overworld |
| Relógio | 12 h / 24 h, segundos |
| CPS | esquerdo, direito, os dois ou a soma; último segundo ou média de 5 s |
| Teclas | WASD, cliques com CPS, espaço, Shift; estilos, cores e tamanho |
| Armadura | durabilidade em número ou %, barra e cor por estado |
| Efeitos | ícone, nível, tempo, cor do efeito, pisca no fim |
| Servidor | cartão com o ícone do servidor, endereço, ping e jogadores |
| Tempo de jogo | sessão, hoje ou total |
| Texto livre | qualquer texto na tela |
| Pacote de textura | ícone, nome inteiro do arquivo e descrição do pacote em uso, ao entrar no mundo ou trocar de pacote (ou sempre) |

- **Editor de HUD:** arraste para mover; arraste o canto ou use a rodinha para mudar o tamanho; botão direito volta ao padrão.
- **Estilo global:** cor do texto, cor de destaque, fundo, cantos (0 a 10 px), sombra, modo Compacto/Avançado.
- **Layouts prontos:** Minimal, PvP, Survival, Content Creator, Competitivo.

### Utilidades e visual
- Sprint automático e agachar alternado, contorno de bloco, cor do hit.
- **Mira:** formatos prontos com as opções de uma mira de CS (espaço, espessura, ponto no centro, contorno, abrir ao andar e
  atacar) e um **editor para desenhar a sua** (grade 17x17, 11 cores, espelho, desfazer, predefinições e prévia).
- **Zoom suave:** entra e sai com animação; com a tecla segurada, a rodinha do mouse ajusta a aproximação.
- Chat com horário, mensagens repetidas agrupadas e histórico de até 1000 linhas; placar sem números; ping em número na Tab.
- Auto GG (desligado por padrão), print copiado para a área de transferência.
- Brilho total, fogo baixo, sem tremor ao levar dano, hora e clima locais.
- Freelook (desligado por padrão; **alguns servidores proíbem**), Motion blur e Saturação de cor.

### Geral
- Perfis de configuração (ex.: "PvP" e "Construção"), exportar/importar em JSON.
- Seletor de cor com RGB, alfa, HEX e paleta; valores digitáveis; opções avançadas no modo Completo.
- Skin para conta offline, vista também pelos amigos que usam o VfxClient (veja acima).

> Nenhum módulo dá vantagem injusta (nada de reach, killaura, X-ray etc.). Respeite as regras de cada servidor.

## Desempenho

Implementações **nossas** das técnicas conhecidas para ganhar FPS na 1.8.9, somadas ao OptiFine no perfil VfxClient.
Cada uma é um toggle, e os presets **Baixo / Equilibrado / Alto** ligam várias de uma vez (com otimização automática
se o FPS ficar abaixo da meta).

| Técnica | Módulo | Ganho típico |
|---|---|---|
| Não desenhar entidades atrás de blocos | Culling de entidades | Alto em lobbies e farms |
| Distância máxima de entidades e de baús/placas | Distância de entidades / de baús | Alto em lobbies cheios |
| Esconder suportes de armadura, molduras, XP e fogos | Esconder decorações | Alto em lobbies com decoração |
| Jogadores longe sem capa, armadura, item na mão e camadas da skin | Jogadores longe simples | Médio em PvP com muita gente |
| Texto das placas só de perto; banners e cabeças por distância | Placas e blocos | Médio |
| Menos partículas (por tipo) | Partículas | Alto em PvP (crits, poções) |
| Texturas animadas, céu, chuva, brilho e nuvens desligáveis | Ajustes de render / Sem nuvens | Médio, maior em PCs fracos |
| Itens no chão como um só modelo | Ajustes de render | Médio com muitos drops |
| Limite de tempo de chunks por frame | Ajustes de render | Menos travadas ao andar |
| Opções gráficas do Minecraft dentro do menu | Opções gráficas / Preset | Alto, varia por placa de vídeo |
| Texto da HUD recalculado no máximo 20x por segundo | Estilo da HUD | Pequeno, sempre ligado |
| FPS limitado sem foco e parado (AFK) | FPS dinâmico | Libera o PC fora do jogo |
| Corta enfeites sozinho quando o FPS cai (FPS Boost) | Motor de FPS | Médio, só quando precisa |
| Sem sombras de entidades (World Optimization) | Motor de FPS | Pequeno a médio |
| Chunks aos poucos (Lazy Chunk Loading) | Motor de FPS | Menos travadas ao andar |
| Espera a placa terminar o quadro (GPU Fencing) | Motor de FPS | Menos atraso de entrada |

As novas (decorações, jogadores longe, placas) vêm **desligadas**. O módulo **Teste de FPS** mede 30 segundos e mostra a média, os
1% mais lentos e o pior segundo no chat, para comparar antes e depois de ligar uma opção.

## Para desenvolvedores

| | |
|---|---|
| Mod | Legacy Fabric Loader 0.18 (sem Legacy Fabric API), Legacy Yarn, Mixin, **Java 8** |
| Launcher | Java 21 + JavaFX 21, jpackage, instalador Inno Setup |
| Build | Gradle 9 (wrapper) + Fabric Loom / Legacy Looming |
| CI | GitHub Actions: build a cada push, release a cada tag `v*`, site no GitHub Pages |

```
VfxClient/
├── core/                 # mod "vfxcore" (id: vfx)
├── launcher/             # launcher JavaFX
├── relay/                # servidor de emotes e skins (Cloudflare Workers)
├── installer/            # Inno Setup + build-installer.ps1
├── site/                 # página de download (GitHub Pages)
├── branding/             # logo (SVG fonte, PNG/ICO gerados) e paleta
└── .github/workflows/    # build.yml, release.yml, pages.yml
```

Requisito: **JDK 21**. O Gradle baixa o Java 8 do Minecraft de desenvolvimento sozinho.

```bat
gradlew build                 :: compila tudo (o que a CI roda)
gradlew :launcher:run         :: abre o launcher
gradlew :core:runClient       :: Minecraft 1.8.9 só com o mod
gradlew :core:genSources      :: código do Minecraft navegável (nomes Yarn)

:: modo linha de comando do launcher
gradlew :launcher:run --args="--cli launch --offline Nick --join localhost:25565"

:: instalador e zip portátil em dist\
powershell -ExecutionPolicy Bypass -File installer\build-installer.ps1
```

Guia completo (pastas, testes, login Microsoft, instalador com OptiFine para o grupo): **[INSTRUCOES.txt](INSTRUCOES.txt)**.
Regras de trabalho: **[CONTRIBUTING.md](CONTRIBUTING.md)**.

### Publicar uma versão

Suba o número em `client_version` (`gradle.properties`) e o texto em `launcher/src/main/resources/com/vfxclient/launcher/changelog.txt`
e dê push:

```bat
git push origin main
```

Se a versão `v<client_version>` ainda não existir, a CI publica no GitHub Releases o instalador, o zip portátil e o jar do mod
(sem OptiFine). Se já existir, nada é publicado: por isso **sempre suba o número** antes do push. Tags com sufixo
(`v0.8.0-beta`) saem como *pre-release*. O launcher instalado avisa quando há versão nova.

O servidor de emotes e skins é publicado à parte, com `npx wrangler deploy` dentro de `relay/` (veja [relay/README.md](relay/README.md)).

## Histórico de versões

| Versão | Principais mudanças |
|---|---|
| **0.16** | Motor de FPS (FPS Boost, sombras, chunks aos poucos, GPU Fencing), cartão do servidor e do pacote de textura com ícone |
| 0.15 | Roda de emotes com 56 emotes e atalhos, logo no nick e na Tab, zoom suave, mira estilo CS com editor, aviso do pacote de textura |
| 0.14 | Emotes (tecla B) e skins vistos por quem usa o VfxClient, opções de desempenho novas, teste de FPS, launcher com as fontes do jogo |
| 0.13 | Tela inicial, menu de pausa (com desfoque), multijogador e Opções no visual novo; fontes Montserrat e Inter |
| 0.12 | Contorno verde/vermelho no menu, tecla de atalho em todo módulo, Hitbox, FOV, cor das partículas, launcher na bandeja |
| 0.11 | Atualização dentro do launcher (Atualizar agora / Mais tarde) |
| 0.10 | Launcher mais simples e animado, arrastar a skin |
| 0.9 | Primeira tela inicial nova, Texto automático, chat anti-spam, cabeças na Tab, galeria de prints |
| 0.8 | Launcher minimalista, OptiFine junto sem opções na tela |
| 0.7 | VfxClient + OptiFine no mesmo perfil, visual plano, menu com cartões e busca, prévia da mira |
| 0.6 | Forge embutido, HUD com âncoras e layouts, redimensionar no editor, módulos novos (Servidor, Tempo de jogo, Freelook, Auto GG...) |
| 0.5.1 | Botões de loader: VfxClient / OptiFine / Forge |
| 0.5 | Launcher completo (contas, skin, RAM, diagnóstico, atualização, instalador), seletor de cor, perfis de config, presets de FPS |
| 0.4 | Utilidades (sprint, mira, chat, placar) e opções gráficas no menu; site de download |
| 0.3 | 2ª leva de desempenho e perfis de FPS |
| 0.2 | Logo na tela inicial, comandos `/vfx`, CPS, teclas, armadura, poções, zoom |
| 0.1 | Menu, config JSON, HUD básica, editor de HUD |

## Próximos passos

- [x] **Emotes**, **skins online** e **logo no nick** (v0.15)
- [x] Tela inicial do jogo e launcher no mesmo visual novo
- [x] Zoom suave, mira estilo CS com editor e aviso do pacote de textura (v0.15)
- [ ] **Cosméticos** (itens visuais para os amigos que usam o client)
- [ ] **Social:** waypoints (criar, cor, distância, compartilhar no chat) e minimapa opcional
- [ ] **Contas VfxClient:** cadastro próprio (as skins online já funcionam para contas offline, sem cadastro)
- [ ] **v1.0 – Polimento:** arte final, testes com o grupo

## Terceiros e licenças

- **[Legacy OptiFabric](https://github.com/tildejustin/optifabric)** (MPL-2.0): vai sem modificação dentro do launcher para carregar o OptiFine junto com o VfxClient.
- **Servidor de emotes** (`relay/`): Cloudflare Workers; veja [relay/README.md](relay/README.md).
- **Certificados raiz** da Google Trust Services e do Let's Encrypt (públicos) vão no mod para a conexão segura funcionar no Java 8 do jogo.
- **Forge 1.8.9** (LGPL): instalador oficial vai dentro do launcher.
- **Fontes [Montserrat](https://github.com/JulietaUla/Montserrat) e [Inter](https://github.com/rsms/inter)** (SIL OFL 1.1): vão dentro do mod, com as licenças em `core/src/main/resources/assets/vfx/fonts/`.
- **OptiFine:** não faz parte deste repositório nem das releases públicas (a licença não permite redistribuir).
- Minecraft é marca da Mojang/Microsoft. O VfxClient não é afiliado à Mojang nem à Microsoft.

Licença do código do VfxClient: veja [LICENSE](LICENSE).
