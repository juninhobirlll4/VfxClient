# Servidor do VfxClient (emotes e skins)

Um só Worker na Cloudflare (Workers + Durable Objects, plano gratuito) que faz duas coisas para quem usa o
VfxClient:

**Emotes** (`/ws`): repassa os emotes entre quem está no mesmo servidor de Minecraft.

- Cada servidor de Minecraft vira uma sala. O client manda só o **hash SHA-256** do endereço, nunca o
  endereço em si.
- Nada fica guardado: a sala existe enquanto tem alguém conectado.
- Limites: mensagens de até 256 bytes, um emote a cada 0,4 s por jogador e até 200 jogadores por sala.

**Skins** (`/skin/<nick>`): guarda a skin de quem usa conta offline para os amigos verem.

- `GET /skin/<nick>` baixa o PNG (cabeçalho `X-Vfx-Model`: `default` ou `slim`).
- `PUT /skin/<nick>?model=default|slim` envia a skin, com o cabeçalho `X-Vfx-Key`.
- `DELETE /skin/<nick>` apaga, também com a chave.
- A chave (64 caracteres hex) é criada pelo launcher de cada pessoa no primeiro envio. O servidor guarda só
  o hash dela: quem envia primeiro "reivindica" o nick, e só a mesma chave troca ou apaga a skin depois.
  Em outro PC (outra chave), o nick fica como ocupado.
- Só aceita PNG 64x64 ou 64x32 de até 24 KB, uma troca a cada 10 s por nick e até 5000 skins.
- No jogo, a skin só vale para jogadores de servidor offline (UUID gerado do nick). Assim ninguém troca a
  skin oficial de quem tem conta original reivindicando o nick dela.

## Publicar (uma vez)

Precisa do [Node.js](https://nodejs.org) (LTS) e de uma conta grátis na Cloudflare
(<https://dash.cloudflare.com/sign-up>).

```bat
cd relay
npm install
npx wrangler login
npx wrangler deploy
```

- `wrangler login` abre o navegador para autorizar a sua conta.
- Na primeira publicação, a Cloudflare pede para escolher o seu subdomínio `workers.dev`.
- No fim, o deploy mostra o endereço, algo como `https://vfx-emotes.SEU-NOME.workers.dev`.

O endereço fica em `services_url` no `gradle.properties` (o mod e o launcher usam o mesmo). O atual é
`https://vfx-emotes.vfxclient-sand.workers.dev`. Se trocar, publique uma versão nova do client. Dá também
para testar sem publicar: no jogo, **Social > Emotes > Servidor de emotes** (opção avançada; vazio = o do
VfxClient).

Para atualizar o servidor depois de mudar `src/index.js`, rode `npx wrangler deploy` de novo.

## Testar no PC

```bat
cd relay
npm install
npm run dev
```

Em outro terminal, também na pasta `relay`:

```bat
npm test
```

O teste conecta dois clients na mesma sala e confere se o emote e o "parou" chegam, e depois testa as
skins (reivindicar o nick, baixar, recusar outra chave e arquivos errados). Dá para rodar contra o
servidor publicado: `node test/skins.mjs https://vfx-emotes.vfxclient-sand.workers.dev`.

Para o jogo usar o servidor local:

```bat
gradlew :launcher:run -Dvfx.emoteRelay=ws://localhost:8787/ws --args="--cli launch --offline Teste --join localhost:25565"
```

## Mensagens

| Quem manda | JSON |
|---|---|
| client | `{"t":"hello","v":1,"uuid":"...","name":"Nick"}` (logo ao conectar) |
| client | `{"t":"emote","id":"acenar"}` |
| client | `{"t":"stop"}` |
| servidor | `{"t":"emote","uuid":"...","name":"Nick","id":"acenar"}` |
| servidor | `{"t":"stop","uuid":"...","name":"Nick"}` (também quando alguém sai) |

Os ids dos emotes ficam em `core/.../emote/Emotes.java`. Não troque um id já publicado: quem estiver na
versão antiga deixaria de ver o emote.

## Limitação conhecida

Em servidores offline (pirata), dá para alguém se passar por outro jogador e fazer um emote "no nome" dele.
O dano é só esse (um emote falso), e o servidor não guarda nada.
