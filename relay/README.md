# Servidor de emotes do VfxClient

Repassa os emotes entre quem usa o VfxClient no mesmo servidor de Minecraft. Roda na Cloudflare
(Workers + Durable Objects), no plano gratuito.

- Cada servidor de Minecraft vira uma sala. O client manda só o **hash SHA-256** do endereço, nunca o
  endereço em si.
- Nada fica guardado: a sala existe enquanto tem alguém conectado.
- Limites: mensagens de até 256 bytes, um emote a cada 0,4 s por jogador e até 200 jogadores por sala.

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

O endereço que o client usa é esse, com `wss://` no começo e `/ws` no fim:

```
wss://vfx-emotes.SEU-NOME.workers.dev/ws
```

Ele vai em `EmotesModule.DEFAULT_RELAY`
(`core/src/main/java/com/vfxclient/core/module/social/EmotesModule.java`). Depois de trocar, publique uma
versão nova do client. Dá também para testar sem publicar: no jogo, **Social > Emotes > Servidor de emotes**
(opção avançada).

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

O teste conecta dois clients na mesma sala e confere se o emote e o "parou" chegam.

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
