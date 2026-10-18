// Teste rápido do servidor de emotes: presença (quem usa o VfxClient), emote e saída.
// Uso: npm run dev (em outro terminal) e depois: node test/smoke.mjs [ws://localhost:8787/ws]
import { createHash, randomBytes } from "node:crypto";

const base = process.argv[2] || "ws://localhost:8787/ws";
// Uma sala nova a cada execução, para não depender de testes anteriores.
const room = createHash("sha256").update("teste-" + randomBytes(6).toString("hex")).digest("hex");

/** Abre um client na sala; as mensagens recebidas ficam numa fila. */
function open(uuid, name) {
	return new Promise((resolve, reject) => {
		const ws = new WebSocket(`${base}?room=${room}`);
		const queue = [];
		const waiting = [];
		ws.addEventListener("message", (event) => {
			const message = JSON.parse(event.data);
			const waiter = waiting.shift();
			if (waiter) {
				waiter(message);
			} else {
				queue.push(message);
			}
		});
		ws.addEventListener("open", () => {
			ws.send(JSON.stringify({ t: "hello", v: 1, uuid, name }));
			resolve({
				ws,
				next: () => new Promise((resolveNext, rejectNext) => {
					if (queue.length) {
						resolveNext(queue.shift());
						return;
					}
					const timer = setTimeout(() => rejectNext(new Error("nada chegou em 3 s")), 3000);
					waiting.push((message) => {
						clearTimeout(timer);
						resolveNext(message);
					});
				}),
			});
		});
		ws.addEventListener("error", reject);
	});
}

function expect(label, ok) {
	console.log(`${ok ? "ok  " : "ERRO"} ${label}`);
	if (!ok) {
		process.exitCode = 1;
	}
}

const alice = await open("11111111-1111-1111-1111-111111111111", "Alice");
expect("Alice recebe a lista vazia ao entrar", (await alice.next()).players?.length === 0);

const bob = await open("22222222-2222-2222-2222-222222222222", "Bob");
const roster = await bob.next();
expect("Bob recebe a lista com a Alice", roster.t === "roster" && roster.players.length === 1 && roster.players[0].name === "Alice");

const join = await alice.next();
expect("Alice é avisada de que o Bob entrou", join.t === "join" && join.name === "Bob");

alice.ws.send(JSON.stringify({ t: "emote", id: "acenar" }));
const emote = await bob.next();
expect("Bob recebe o emote da Alice", emote.t === "emote" && emote.name === "Alice" && emote.id === "acenar");

alice.ws.close();
const stop = await bob.next();
const leave = await bob.next();
expect("Bob recebe o stop da Alice ao ela sair", stop.t === "stop" && stop.name === "Alice");
expect("Bob é avisado de que a Alice saiu", leave.t === "leave" && leave.name === "Alice");

bob.ws.close();
console.log(process.exitCode ? "FALHOU" : "OK");
