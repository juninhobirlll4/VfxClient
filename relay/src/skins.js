/**
 * Skins do VfxClient.
 *
 *   GET /skin/<nick>                 -> PNG da skin (404 se o nick não tem skin)
 *   PUT /skin/<nick>?model=default|slim, cabeçalho X-Vfx-Key: <64 hex>, corpo = PNG
 *   DELETE /skin/<nick>, cabeçalho X-Vfx-Key
 *
 * O primeiro PUT de um nick "reivindica" o nick: o servidor guarda só o SHA-256 da chave. Depois,
 * só quem mandar a mesma chave troca ou apaga a skin. A chave é gerada pelo launcher de cada pessoa.
 */
import { DurableObject } from "cloudflare:workers";

const NAME = /^[A-Za-z0-9_]{1,16}$/;
const KEY = /^[0-9a-f]{64}$/;
const MODELS = new Set(["default", "slim"]);

const MAX_BYTES = 24 * 1024;
const MIN_INTERVAL_MS = 10_000;
const MAX_SKINS = 5000;
const PNG_SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];

export class SkinStore extends DurableObject {

	constructor(ctx, env) {
		super(ctx, env);
		this.sql = ctx.storage.sql;
		this.sql.exec(
			"CREATE TABLE IF NOT EXISTS skins (" +
			"name TEXT PRIMARY KEY, key_hash TEXT NOT NULL, model TEXT NOT NULL, " +
			"png BLOB NOT NULL, updated INTEGER NOT NULL)");
	}

	async fetch(request) {
		const url = new URL(request.url);
		const name = decodeURIComponent(url.pathname.slice("/skin/".length));
		if (!NAME.test(name)) {
			return text("Nick inválido.", 400);
		}
		const id = name.toLowerCase();

		switch (request.method) {
			case "GET":
				return this.get(id);
			case "PUT":
				return this.put(id, url, request);
			case "DELETE":
				return this.remove(id, request);
			default:
				return text("Método não aceito.", 405);
		}
	}

	get(id) {
		const row = this.sql.exec("SELECT model, png FROM skins WHERE name = ?", id).toArray()[0];
		if (!row) {
			return text("Sem skin.", 404);
		}
		return new Response(row.png, {
			headers: {
				"content-type": "image/png",
				"x-vfx-model": row.model,
				"cache-control": "public, max-age=60",
			},
		});
	}

	async put(id, url, request) {
		const key = request.headers.get("x-vfx-key") || "";
		const model = url.searchParams.get("model") || "default";
		if (!KEY.test(key) || !MODELS.has(model)) {
			return text("Chave ou modelo inválido.", 400);
		}

		const png = new Uint8Array(await request.arrayBuffer());
		const problem = checkPng(png);
		if (problem) {
			return text(problem, 400);
		}

		const hash = await sha256(key);
		const existing = this.sql.exec("SELECT key_hash, updated FROM skins WHERE name = ?", id).toArray()[0];
		const now = Date.now();
		if (existing) {
			if (!sameText(existing.key_hash, hash)) {
				return text("Este nick já pertence a outra pessoa.", 403);
			}
			if (now - existing.updated < MIN_INTERVAL_MS) {
				return text("Espere alguns segundos para trocar de novo.", 429);
			}
		} else if (this.sql.exec("SELECT COUNT(*) AS n FROM skins").one().n >= MAX_SKINS) {
			return text("Servidor de skins cheio.", 507);
		}

		this.sql.exec(
			"INSERT INTO skins (name, key_hash, model, png, updated) VALUES (?, ?, ?, ?, ?) " +
			"ON CONFLICT(name) DO UPDATE SET model = excluded.model, png = excluded.png, updated = excluded.updated",
			id, hash, model, png, now);
		return text("OK", 200);
	}

	async remove(id, request) {
		const key = request.headers.get("x-vfx-key") || "";
		const existing = this.sql.exec("SELECT key_hash FROM skins WHERE name = ?", id).toArray()[0];
		if (!existing) {
			return text("Sem skin.", 404);
		}
		if (!KEY.test(key) || !sameText(existing.key_hash, await sha256(key))) {
			return text("Este nick já pertence a outra pessoa.", 403);
		}
		this.sql.exec("DELETE FROM skins WHERE name = ?", id);
		return text("OK", 200);
	}
}

/** @returns uma mensagem de erro, ou null se o arquivo parece uma skin (PNG 64x64 ou 64x32) */
function checkPng(bytes) {
	if (bytes.length === 0 || bytes.length > MAX_BYTES) {
		return "Skin vazia ou grande demais (máximo 24 KB).";
	}
	if (bytes.length < 33 || PNG_SIGNATURE.some((b, i) => bytes[i] !== b)) {
		return "O arquivo não é um PNG.";
	}
	const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
	const header = String.fromCharCode(bytes[12], bytes[13], bytes[14], bytes[15]);
	const width = view.getUint32(16);
	const height = view.getUint32(20);
	if (header !== "IHDR" || width !== 64 || (height !== 64 && height !== 32)) {
		return "A skin precisa ter 64x64 (ou 64x32).";
	}
	return null;
}

async function sha256(value) {
	const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
	return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** Compara sem parar no primeiro caractere diferente. */
function sameText(a, b) {
	if (a.length !== b.length) {
		return false;
	}
	let difference = 0;
	for (let i = 0; i < a.length; i++) {
		difference |= a.charCodeAt(i) ^ b.charCodeAt(i);
	}
	return difference === 0;
}

function text(body, status) {
	return new Response(body + "\n", { status, headers: { "content-type": "text/plain; charset=utf-8" } });
}
