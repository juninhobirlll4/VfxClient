package com.vfxclient.core.emote;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.net.VfxUsers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

/**
 * Ligação com o servidor de emotes: entra na "sala" do servidor de Minecraft em que você está,
 * manda os seus emotes e recebe os dos outros jogadores do VfxClient.
 *
 * <p>A sala é o SHA-256 do endereço do servidor; o endereço em si nunca sai do PC. Em mundo
 * local não conecta.</p>
 */
public final class EmoteRelay {

	private static final long PING_EVERY_MS = 30_000L;
	private static final long RETRY_MIN_MS = 5_000L;
	private static final long RETRY_MAX_MS = 60_000L;

	private final Queue<String> incoming = new ConcurrentLinkedQueue<String>();

	private volatile RelaySocket socket;
	private volatile boolean connecting;
	private volatile String room;
	private long retryAt;
	private long retryDelay = RETRY_MIN_MS;
	private long lastPing;

	/**
	 * Chamado a cada tick: conecta, troca de sala, reconecta e entrega o que chegou.
	 *
	 * @param client     instância do Minecraft
	 * @param url        endereço do servidor de emotes (vazio = desligado)
	 * @param showOthers se os emotes dos outros aparecem
	 */
	public void tick(MinecraftClient client, String url, boolean showOthers) {
		String wanted = url.isEmpty() ? null : roomFor(client);
		if (wanted == null ? room != null : !wanted.equals(room)) {
			disconnect();
			room = wanted;
			retryAt = 0;
			retryDelay = RETRY_MIN_MS;
		}

		long now = System.currentTimeMillis();
		if (room != null && socket == null && !connecting && now >= retryAt) {
			connectAsync(url, room, client);
		}
		if (socket != null && now - lastPing > PING_EVERY_MS) {
			lastPing = now;
			send(null);
		}

		for (String text = incoming.poll(); text != null; text = incoming.poll()) {
			handle(client, text, showOthers);
		}
	}

	/** Avisa os outros que você começou um emote. */
	public void sendEmote(Emote emote) {
		JsonObject message = new JsonObject();
		message.addProperty("t", "emote");
		message.addProperty("id", emote.id());
		send(message.toString());
	}

	/** Avisa os outros que o seu emote parou (você se mexeu). */
	public void sendStop() {
		send("{\"t\":\"stop\"}");
	}

	public void disconnect() {
		RelaySocket current = socket;
		socket = null;
		if (current != null) {
			current.close();
		}
		EmoteState.clear();
		VfxUsers.clear();
	}

	/** @param text mensagem, ou {@code null} para só mandar um ping */
	private void send(String text) {
		RelaySocket current = socket;
		if (current == null) {
			return;
		}
		try {
			if (text == null) {
				current.ping();
			} else {
				current.sendText(text);
			}
		} catch (IOException e) {
			current.close();
		}
	}

	private void connectAsync(String url, String forRoom, MinecraftClient client) {
		if (client.player == null) {
			return;
		}
		connecting = true;
		String uuid = client.player.getUuid().toString();
		String name = client.player.getGameProfile().getName();

		Thread thread = new Thread(() -> connect(url, forRoom, uuid, name), "VfxClient emotes (conexão)");
		thread.setDaemon(true);
		thread.start();
	}

	private void connect(String url, String forRoom, String uuid, String name) {
		try {
			URI uri = new URI(url + (url.contains("?") ? "&" : "?") + "room=" + forRoom);
			final RelaySocket[] self = new RelaySocket[1];
			RelaySocket opened = RelaySocket.connect(uri, new RelaySocket.Listener() {
				@Override
				public void onText(String text) {
					incoming.add(text);
				}

				@Override
				public void onClosed(String reason) {
					connectionLost(self[0], reason);
				}
			});
			self[0] = opened;

			JsonObject hello = new JsonObject();
			hello.addProperty("t", "hello");
			hello.addProperty("v", 1);
			hello.addProperty("uuid", uuid);
			hello.addProperty("name", name);
			opened.sendText(hello.toString());

			if (!forRoom.equals(room)) {
				// Trocou de servidor enquanto conectava: esta conexão é da sala antiga.
				opened.close();
				return;
			}
			socket = opened;
			retryDelay = RETRY_MIN_MS;
			VfxUsers.add(name);
			VfxCore.LOGGER.info("Emotes: conectado ao servidor de emotes.");
		} catch (IOException | URISyntaxException e) {
			VfxCore.LOGGER.warn("Emotes: não deu para conectar ({}). Tentando de novo em {} s.", e.getMessage(), retryDelay / 1000);
			scheduleRetry();
		} finally {
			connecting = false;
		}
	}

	/** Só a conexão atual conta: uma antiga fechando não derruba a nova. */
	private void connectionLost(RelaySocket closed, String reason) {
		if (closed != null && socket == closed) {
			socket = null;
			VfxCore.LOGGER.info("Emotes: conexão caiu ({}).", reason);
			scheduleRetry();
		}
	}

	private void scheduleRetry() {
		retryAt = System.currentTimeMillis() + retryDelay;
		retryDelay = Math.min(RETRY_MAX_MS, retryDelay * 2);
	}

	private static void handle(MinecraftClient client, String text, boolean showOthers) {
		JsonObject message;
		try {
			message = new JsonParser().parse(text).getAsJsonObject();
		} catch (RuntimeException e) {
			return;
		}

		String type = message.has("t") ? message.get("t").getAsString() : "";
		if ("roster".equals(type)) {
			handleRoster(message);
			return;
		}

		UUID uuid;
		try {
			uuid = UUID.fromString(message.get("uuid").getAsString());
		} catch (RuntimeException e) {
			return;
		}
		if (client.player != null && uuid.equals(client.player.getUuid())) {
			return;
		}

		String name = message.has("name") ? message.get("name").getAsString() : null;
		if ("join".equals(type)) {
			VfxUsers.add(name);
		} else if ("leave".equals(type)) {
			VfxUsers.remove(name);
			EmoteState.stop(uuid);
		} else if ("stop".equals(type)) {
			EmoteState.stop(uuid);
		} else if ("emote".equals(type) && showOthers) {
			Emote emote = Emotes.byId(message.has("id") ? message.get("id").getAsString() : "");
			if (emote != null) {
				EmoteState.play(uuid, name, emote);
			}
		}
	}

	/** Lista de quem já usa o VfxClient neste servidor (chega logo depois de conectar). */
	private static void handleRoster(JsonObject message) {
		if (!message.has("players") || !message.get("players").isJsonArray()) {
			return;
		}
		for (JsonElement player : message.getAsJsonArray("players")) {
			if (player.isJsonObject() && player.getAsJsonObject().has("name")) {
				VfxUsers.add(player.getAsJsonObject().get("name").getAsString());
			}
		}
	}
	/** @return a sala do servidor atual, ou {@code null} em mundo local / fora de um mundo */
	private static String roomFor(MinecraftClient client) {
		if (client.world == null || client.isInSingleplayer()) {
			return null;
		}
		String address = serverAddress(client);
		return address == null ? null : sha256(normalize(address));
	}

	private static String serverAddress(MinecraftClient client) {
		ServerInfo entry = client.getCurrentServerEntry();
		if (entry != null && entry.address != null) {
			return entry.address;
		}
		if (client.getNetworkHandler() == null) {
			return null;
		}
		SocketAddress socketAddress = client.getNetworkHandler().getClientConnection().getAddress();
		if (socketAddress instanceof InetSocketAddress) {
			InetSocketAddress inet = (InetSocketAddress) socketAddress;
			return inet.getHostString() + ":" + inet.getPort();
		}
		return null;
	}

	/** "Server.com" e "server.com:25565" viram a mesma sala. */
	static String normalize(String address) {
		String value = address.trim().toLowerCase(Locale.ROOT);
		if (value.endsWith(".")) {
			value = value.substring(0, value.length() - 1);
		}
		if (value.lastIndexOf(':') < 0) {
			value += ":25565";
		}
		return value;
	}

	private static String sha256(String text) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(64);
			for (byte b : hash) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
