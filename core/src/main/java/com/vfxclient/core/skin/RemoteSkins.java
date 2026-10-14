package com.vfxclient.core.skin;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.net.VfxServices;
import com.vfxclient.core.net.VfxTls;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Skins dos outros jogadores que usam o VfxClient, baixadas do servidor do VfxClient.
 *
 * <p>Só vale para jogadores de servidor offline (UUID gerado a partir do nick): quem tem conta
 * original já tem skin oficial, e assim ninguém troca a skin oficial de outra pessoa
 * reivindicando o nick dela.</p>
 */
public final class RemoteSkins {

	private static final int MAX_BYTES = 32 * 1024;
	private static final int TIMEOUT_MS = 6000;
	private static final long REFRESH_MS = 10 * 60_000L;
	private static final long RETRY_MS = 5 * 60_000L;
	private static final long RETRY_ERROR_MS = 60_000L;

	/** Estado da skin de um nick. */
	private static final class Entry {
		volatile boolean loading;
		/** Quando consultar o servidor de novo (epoch em ms; 0 = nunca consultou). */
		volatile long nextCheck;
		/** Imagem baixada, esperando a thread do jogo criar a textura. */
		volatile BufferedImage pending;
		volatile String pendingModel = "default";
		volatile boolean pendingNone;
		/** {@code true} depois de saber que o servidor não tem skin desse nick. */
		boolean none;
		Identifier texture;
		NativeImageBackedTexture backing;
		String model = "default";
	}

	private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<String, Entry>();
	private static final ExecutorService DOWNLOADS = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "VfxClient skins");
		thread.setDaemon(true);
		return thread;
	});

	private RemoteSkins() {
	}

	/**
	 * @param player jogador que o jogo vai desenhar
	 * @return textura da skin dele no servidor do VfxClient, ou {@code null} para usar a normal
	 */
	public static Identifier textureFor(AbstractClientPlayerEntity player) {
		Entry entry = entryFor(player);
		return entry == null ? null : entry.texture;
	}

	/** @return {@code "slim"} ou {@code "default"}; só vale se {@link #textureFor} não for nulo */
	public static String modelFor(AbstractClientPlayerEntity player) {
		Entry entry = entryFor(player);
		return entry == null ? "default" : entry.model;
	}

	private static Entry entryFor(AbstractClientPlayerEntity player) {
		String name = player.getGameProfile().getName();
		if (name == null || !isOfflineAccount(player.getUuid(), name) || VfxServices.baseUrl().isEmpty()) {
			return null;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		boolean isSelf = client.player != null && player.getUuid().equals(client.player.getUuid());
		if (isSelf && !Boolean.getBoolean("vfx.remoteSelf")) {
			// A sua própria skin vem do arquivo do launcher (LocalSkinModule); -Dvfx.remoteSelf é só para testes.
			return null;
		}

		String key = name.toLowerCase(Locale.ROOT);
		Entry entry = ENTRIES.get(key);
		if (entry == null) {
			entry = new Entry();
			ENTRIES.put(key, entry);
		}
		applyPending(entry, key);
		scheduleIfNeeded(entry, name);
		return entry;
	}

	/** Cria (ou atualiza) a textura na thread do jogo, que é a única que pode mexer no OpenGL. */
	private static void applyPending(Entry entry, String key) {
		if (entry.pendingNone) {
			entry.pendingNone = false;
			entry.none = true;
			entry.texture = null;
		}

		BufferedImage image = entry.pending;
		if (image == null) {
			return;
		}
		entry.pending = null;
		entry.none = false;
		entry.model = entry.pendingModel;

		NativeImageBackedTexture backing = entry.backing;
		if (backing != null && sameSize(backing, image)) {
			image.getRGB(0, 0, image.getWidth(), image.getHeight(), backing.getPixels(), 0, image.getWidth());
			backing.upload();
			return;
		}
		backing = new NativeImageBackedTexture(image);
		entry.backing = backing;
		entry.texture = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("vfx_skin_" + key, backing);
	}

	private static boolean sameSize(NativeImageBackedTexture texture, BufferedImage image) {
		return texture.getPixels().length == image.getWidth() * image.getHeight();
	}

	private static void scheduleIfNeeded(Entry entry, String name) {
		if (entry.loading) {
			return;
		}
		if (System.currentTimeMillis() < entry.nextCheck) {
			return;
		}
		entry.loading = true;
		DOWNLOADS.execute(() -> download(entry, name));
	}

	private static void download(Entry entry, String name) {
		try {
			HttpsURLConnection connection = (HttpsURLConnection) new URL(VfxServices.skinUrl(name)).openConnection();
			connection.setSSLSocketFactory(VfxTls.socketFactory());
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			connection.setRequestProperty("User-Agent", "VfxClient");

			int status = connection.getResponseCode();
			long now = System.currentTimeMillis();
			if (status == 404) {
				entry.pendingNone = true;
				entry.nextCheck = now + RETRY_MS;
			} else if (status == 200) {
				BufferedImage image = ImageIO.read(new ByteArrayInputStream(readLimited(connection.getInputStream())));
				if (image != null && image.getWidth() == 64 && (image.getHeight() == 64 || image.getHeight() == 32)) {
					String model = "slim".equals(connection.getHeaderField("X-Vfx-Model")) ? "slim" : "default";
					entry.pendingModel = model;
					entry.pending = image;
				}
				entry.nextCheck = now + REFRESH_MS;
			} else {
				entry.nextCheck = now + RETRY_ERROR_MS;
			}
		} catch (IOException | RuntimeException e) {
			VfxCore.LOGGER.warn("Skins: não deu para baixar a skin de {} ({}). Tenta de novo em 1 min.", name, e.toString());
			entry.nextCheck = System.currentTimeMillis() + RETRY_ERROR_MS;
		} finally {
			entry.loading = false;
		}
	}

	private static byte[] readLimited(InputStream in) throws IOException {
		try (InputStream stream = in) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			for (int read = stream.read(buffer); read >= 0; read = stream.read(buffer)) {
				out.write(buffer, 0, read);
				if (out.size() > MAX_BYTES) {
					throw new IOException("Skin grande demais.");
				}
			}
			return out.toByteArray();
		}
	}

	/** Servidores offline dão a cada nick o UUID de {@code "OfflinePlayer:<nick>"}. */
	private static boolean isOfflineAccount(UUID uuid, String name) {
		UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
		return offline.equals(uuid);
	}
}
