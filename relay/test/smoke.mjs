// Teste rápido do servidor de emotes: dois clients na mesma sala, um manda emote, o outro recebe.
// Uso: npm run dev (em outro terminal) e depois: node test/smoke.mjs [ws://localhost:8787/ws]
import { createHash } from "node:crypto";

const base = process.argv[2] || "ws://localhost:8787/ws";
const room = createHash("sha256").update("localhost:25565").digest("hex");

function open(uuid, name) {
	return new Promise((resolve, reject) => {
		const ws = new WebSocket(`${base}?room=${room}`);
		ws.addEventListener("open", () => {
			ws.send(JSON.stringify({ t: "hello", v: 1, uuid, name }));
			resolve(ws);
		});
		ws.addEventListener("error", reject);
	});
}

function next(ws) {
	return new Promise((resolve, reject) => {
		const timer = setTimeout(() => reject(new Error("nada chegou em 3 s")), 3000);
		ws.addEventListener("message", (event) => {
			clearTimeout(timer);
			resolve(JSON.parse(event.data));
		}, { once: true });
	});
}

const alice = await open("11111111-1111-1111-1111-111111111111", "Alice");
const bob = await open("22222222-2222-2222-2222-222222222222", "Bob");
await new Promise((r) => setTimeout(r, 200));

alice.send(JSON.stringify({ t: "emote", id: "acenar" }));
const got = await next(bob);
console.log("Bob recebeu:", got);
if (got.t !== "emote" || got.name !== "Alice" || got.id !== "acenar") {
	throw new Error("mensagem errada");
}

const stop = next(bob);
alice.close();
console.log("Bob recebeu ao Alice sair:", await stop);
bob.close();
console.log("OK");
