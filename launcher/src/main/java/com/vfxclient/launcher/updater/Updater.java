package com.vfxclient.launcher.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vfxclient.launcher.BuildInfo;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Http;
import com.vfxclient.launcher.util.Json;

/**
 * Atualização automática via GitHub Releases: compara a versão do launcher com a última
 * release e baixa o instalador novo (que atualiza o launcher e o mod juntos).
 */
public final class Updater {

	/** Repositório onde as versões são publicadas. */
	public static final String REPO = "juninhobirlll4/VfxClient";

	/**
	 * Atualização disponível.
	 *
	 * @param version      versão nova (sem o "v")
	 * @param installerUrl link do instalador .exe
	 * @param size         tamanho do instalador
	 * @param notes        notas da versão
	 */
	public record Update(String version, String installerUrl, long size, String notes) {
	}

	private Updater() {
	}

	/**
	 * @return a atualização, ou {@code null} se já está na última versão
	 * @throws IOException sem internet ou repositório privado
	 */
	public static Update check() throws IOException {
		JsonObject release = Http.getJson("https://api.github.com/repos/" + REPO + "/releases/latest");
		String tag = Json.string(release, "tag_name");
		if (tag == null) {
			return null;
		}
		String latest = tag.startsWith("v") ? tag.substring(1) : tag;
		if (compare(latest, BuildInfo.load().clientVersion()) <= 0) {
			return null;
		}
		for (JsonElement element : release.getAsJsonArray("assets")) {
			JsonObject asset = element.getAsJsonObject();
			String name = Json.string(asset, "name");
			if (name != null && name.endsWith(".exe") && name.contains("Setup")) {
				return new Update(latest, Json.string(asset, "browser_download_url"), asset.get("size").getAsLong(),
						Json.string(release, "body"));
			}
		}
		return null;
	}

	/**
	 * Baixa o instalador e o executa (o launcher deve fechar logo em seguida).
	 *
	 * @param update atualização
	 * @throws IOException se o download falhar
	 */
	public static void downloadAndRun(Update update) throws IOException {
		Path file = LauncherPaths.cache().resolve("VfxClient-Setup-" + update.version() + ".exe");
		Files.createDirectories(file.getParent());
		Http.download(update.installerUrl(), file);
		new ProcessBuilder(file.toString(), "/SILENT").start();
	}

	/**
	 * Compara versões numéricas ({@code 0.10.0} &gt; {@code 0.9.2}).
	 *
	 * @param a versão
	 * @param b outra versão
	 * @return negativo, zero ou positivo
	 */
	public static int compare(String a, String b) {
		String[] pa = a.split("[.\\-+]");
		String[] pb = b.split("[.\\-+]");
		for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
			int x = i < pa.length ? number(pa[i]) : 0;
			int y = i < pb.length ? number(pb[i]) : 0;
			if (x != y) {
				return Integer.compare(x, y);
			}
		}
		return 0;
	}

	private static int number(String part) {
		try {
			return Integer.parseInt(part);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
