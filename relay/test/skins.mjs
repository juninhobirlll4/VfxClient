// Teste das skins: reivindicar um nick, baixar, tentar com outra chave e recusar arquivo errado.
// Uso: npm run dev (outro terminal) e depois: node test/skins.mjs [http://localhost:8787]
import { randomBytes } from "node:crypto";
import { deflateSync, crc32 } from "node:zlib";

const base = process.argv[2] || "http://localhost:8787";
const name = "T" + randomBytes(5).toString("hex");
const keyA = randomBytes(32).toString("hex");
const keyB = randomBytes(32).toString("hex");

/** PNG RGBA de uma cor só, no tamanho pedido. */
function png(width, height, rgba) {
	const row = Buffer.alloc(1 + width * 4);
	for (let x = 0; x < width; x++) {
		Buffer.from(rgba).copy(row, 1 + x * 4);
	}
	const raw = Buffer.concat(Array.from({ length: height }, () => row));
	const chunk = (type, data) => {
		const body = Buffer.concat([Buffer.from(type), data]);
		const out = Buffer.alloc(8 + data.length + 4);
		out.writeUInt32BE(data.length, 0);
		body.copy(out, 4);
		out.writeUInt32BE(crc32(body) >>> 0, 8 + data.length);
		return out;
	};
	const header = Buffer.alloc(13);
	header.writeUInt32BE(width, 0);
	header.writeUInt32BE(height, 4);
	header[8] = 8;
	header[9] = 6;
	return Buffer.concat([
		Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
		chunk("IHDR", header),
		chunk("IDAT", deflateSync(raw)),
		chunk("IEND", Buffer.alloc(0)),
	]);
}

async function call(method, path, { key, body } = {}) {
	const response = await fetch(`${base}${path}`, {
		method,
		headers: key ? { "x-vfx-key": key } : {},
		body,
	});
	return { status: response.status, response };
}

function expect(label, actual, wanted) {
	const ok = actual === wanted;
	console.log(`${ok ? "ok  " : "ERRO"} ${label}: ${actual}${ok ? "" : ` (esperado ${wanted})`}`);
	if (!ok) {
		process.exitCode = 1;
	}
}

const skin = png(64, 64, [200, 40, 120, 255]);
expect("GET sem skin", (await call("GET", `/skin/${name}`)).status, 404);
expect("PUT inicial (reivindica o nick)", (await call("PUT", `/skin/${name}?model=slim`, { key: keyA, body: skin })).status, 200);

const got = await call("GET", `/skin/${name.toUpperCase()}`);
expect("GET (nick em maiúsculas)", got.status, 200);
expect("modelo", got.response.headers.get("x-vfx-model"), "slim");
expect("bytes iguais", Buffer.from(await got.response.arrayBuffer()).equals(skin), true);

expect("PUT com outra chave", (await call("PUT", `/skin/${name}`, { key: keyB, body: skin })).status, 403);
expect("DELETE com outra chave", (await call("DELETE", `/skin/${name}`, { key: keyB })).status, 403);
expect("PUT logo em seguida (limite de tempo)", (await call("PUT", `/skin/${name}`, { key: keyA, body: skin })).status, 429);
expect("PNG de tamanho errado", (await call("PUT", `/skin/${name}x`, { key: keyA, body: png(32, 32, [0, 0, 0, 255]) })).status, 400);
expect("arquivo que não é PNG", (await call("PUT", `/skin/${name}y`, { key: keyA, body: Buffer.from("oi") })).status, 400);
expect("nick inválido", (await call("GET", "/skin/nick%20ruim!")).status, 400);
expect("DELETE do dono", (await call("DELETE", `/skin/${name}`, { key: keyA })).status, 200);
expect("GET depois de apagar", (await call("GET", `/skin/${name}`)).status, 404);
console.log(process.exitCode ? "FALHOU" : "OK");
