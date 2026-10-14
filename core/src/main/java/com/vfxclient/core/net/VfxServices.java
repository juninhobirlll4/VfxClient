package com.vfxclient.core.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Endereço do servidor do VfxClient na Cloudflare (emotes e skins, pasta {@code relay/}).
 *
 * <p>O endereço vem do {@code services_url} do {@code gradle.properties}, o mesmo que o launcher
 * usa; assim só existe um lugar para trocar.</p>
 */
public final class VfxServices {

	private static final String RESOURCE = "/assets/vfx/services.properties";
	private static String baseUrl;

	private VfxServices() {
	}

	/** @return endereço {@code https://...} sem barra no final, ou vazio se o build não trouxe um */
	public static synchronized String baseUrl() {
		if (baseUrl == null) {
			baseUrl = readBaseUrl();
		}
		return baseUrl;
	}

	/** @return endereço do WebSocket de emotes, ex.: {@code wss://.../ws} */
	public static String emotesUrl() {
		String base = baseUrl();
		return base.isEmpty() ? "" : base.replaceFirst("^http", "ws") + "/ws";
	}

	/**
	 * @param name nick do jogador
	 * @return endereço da skin desse nick
	 */
	public static String skinUrl(String name) {
		return baseUrl() + "/skin/" + name;
	}

	private static String readBaseUrl() {
		Properties properties = new Properties();
		try (InputStream in = VfxServices.class.getResourceAsStream(RESOURCE)) {
			if (in != null) {
				properties.load(in);
			}
		} catch (IOException ignored) {
			// Sem o arquivo, os recursos online ficam desligados.
		}

		String url = properties.getProperty("url", "").trim();
		boolean valid = url.startsWith("https://") && !url.contains("${");
		return valid ? url.replaceAll("/+$", "") : "";
	}
}
