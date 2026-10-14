/**
 * Servidor de emotes do VfxClient (Cloudflare Worker + Durable Object).
 *
 * Cada servidor de Minecraft vira uma "sala" (o client manda o hash SHA-256 do endereço, nunca o
 * endereço em si). Quem está na mesma sala recebe os emotes dos outros. Nada é guardado: quando
 * todo mundo sai, a sala some.
 *
 * Mensagens (JSON, uma por quadro de WebSocket):
 *   client -> servidor  {"t":"hello","v":1,"uuid":"...","name":"..."}
 *                       {"t":"emote","id":"acenar"}
 *                       {"t":"stop"}
 *   servidor -> client  {"t":"emote","uuid":"...","name":"...","id":"acenar"}
 *                       {"t":"stop","uuid":"...","name":"..."}
 */
import { DurableObject } from "cloudflare:workers";

const ROOM = /^[0-9a-f]{64}$/;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const NAME = /^[A-Za-z0-9_]{1,16}$/;
const EMOTE = /^[a-z_]{1,24}$/;

const MAX_MESSAGE = 256;
const MIN_INTERVAL_MS = 400;
const MAX_PER_ROOM = 200;

export default {
	async fetch(request, env) {
		const url = new URL(request.url);
		if (url.pathname !== "/ws") {
			return new Response("VfxClient emotes\n", { headers: { "content-type": "text/plain; charset=utf-8" } });
		}
		if (request.headers.get("Upgrade") !== "websocket") {
			return new Response("Use WebSocket.\n", { status: 426 });
		}

		const room = url.searchParams.get("room") || "";
		if (!ROOM.test(room)) {
			return new Response("Sala inválida.\n", { status: 400 });
		}
		const stub = env.ROOMS.get(env.ROOMS.idFromName(room));
		return stub.fetch(request);
	},
};

/** Uma sala = os jogadores do VfxClient num mesmo servidor de Minecraft. */
export class EmoteRoom extends DurableObject {

	async fetch(request) {
		if (this.ctx.getWebSockets().length >= MAX_PER_ROOM) {
			return new Response("Sala cheia.\n", { status: 503 });
		}

		const pair = new WebSocketPair();
		const [client, server] = Object.values(pair);
		// Com hibernação a sala não fica cobrando enquanto ninguém manda nada.
		this.ctx.acceptWebSocket(server);
		server.serializeAttachment({ uuid: null, name: null, last: 0 });
		return new Response(null, { status: 101, webSocket: client });
	}

	async webSocketMessage(ws, data) {
		if (typeof data !== "string" || data.length > MAX_MESSAGE) {
			return;
		}
		let message;
		try {
			message = JSON.parse(data);
		} catch {
			return;
		}

		const player = ws.deserializeAttachment();
		switch (message.t) {
			case "hello":
				this.hello(ws, player, message);
				break;
			case "emote":
				this.emote(ws, player, message);
				break;
			case "stop":
				if (player.uuid) {
					this.broadcast(ws, { t: "stop", uuid: player.uuid, name: player.name });
				}
				break;
			default:
				break;
		}
	}

	hello(ws, player, message) {
		if (!UUID.test(String(message.uuid)) || !NAME.test(String(message.name))) {
			ws.close(1008, "Identificação inválida");
			return;
		}
		player.uuid = message.uuid;
		player.name = message.name;
		ws.serializeAttachment(player);
	}

	emote(ws, player, message) {
		if (!player.uuid || !EMOTE.test(String(message.id))) {
			return;
		}
		const now = Date.now();
		if (now - player.last < MIN_INTERVAL_MS) {
			return;
		}
		player.last = now;
		ws.serializeAttachment(player);
		this.broadcast(ws, { t: "emote", uuid: player.uuid, name: player.name, id: message.id });
	}

	async webSocketClose(ws) {
		const player = ws.deserializeAttachment();
		if (player && player.uuid) {
			this.broadcast(ws, { t: "stop", uuid: player.uuid, name: player.name });
		}
	}

	/** Manda para todos da sala, menos quem enviou. */
	broadcast(sender, message) {
		const text = JSON.stringify(message);
		for (const ws of this.ctx.getWebSockets()) {
			if (ws !== sender) {
				try {
					ws.send(text);
				} catch {
					// Conexão fechando; o webSocketClose dela cuida do resto.
				}
			}
		}
	}
}
