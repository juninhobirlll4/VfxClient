package com.vfxclient.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Informações gravadas no launcher durante o build (versão do client e do Minecraft).
 *
 * @param clientVersion    versão do VfxClient, ex.: {@code 0.1.0}
 * @param minecraftVersion versão do Minecraft suportada, ex.: {@code 1.8.9}
 * @param servicesUrl      servidor do VfxClient na Cloudflare (emotes e skins), vazio se não houver
 */
public record BuildInfo(String clientVersion, String minecraftVersion, String servicesUrl) {

	private static final String RESOURCE = "build-info.properties";

	/**
	 * Lê o {@code build-info.properties} empacotado no jar.
	 *
	 * @return as informações de build; campos ausentes viram {@code "dev"}
	 */
	public static BuildInfo load() {
		Properties props = new Properties();
		try (InputStream in = BuildInfo.class.getResourceAsStream(RESOURCE)) {
			if (in != null) {
				props.load(in);
			}
		} catch (IOException ignored) {
			// Sem o arquivo, seguimos com os valores padrão.
		}
		return new BuildInfo(
				props.getProperty("client.version", "dev"),
				props.getProperty("minecraft.version", "1.8.9"),
				cleanUrl(props.getProperty("services.url", "")));
	}

	/** Só aceita {@code https://...}; um valor não preenchido pelo Gradle vira vazio. */
	private static String cleanUrl(String url) {
		String value = url.trim();
		boolean valid = value.startsWith("https://") && !value.contains("${");
		return valid ? value.replaceAll("/+$", "") : "";
	}
}
