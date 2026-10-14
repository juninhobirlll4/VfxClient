package com.vfxclient.launcher.game;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Http;
import com.vfxclient.launcher.util.Json;

/**
 * Instala o Legacy Fabric (o carregador de mods do VfxClient) para uma versão do Minecraft.
 *
 * <p>Usa a API oficial do Legacy Fabric para pegar o loader estável mais novo; sem internet,
 * reaproveita a última instalação.</p>
 */
public final class LegacyFabric {

	private static final String META = "https://meta.legacyfabric.net/v2/versions/loader/";

	private LegacyFabric() {
	}

	/**
	 * @param minecraftVersion ex.: {@code 1.8.9}
	 * @param progress         andamento
	 * @return id da versão do Fabric (ex.: {@code fabric-loader-0.19.5-1.8.9}), pronta para o {@link VersionResolver}
	 * @throws IOException se não estiver instalado e não der para baixar
	 */
	public static String install(String minecraftVersion, Progress progress) throws IOException {
		progress.update("Verificando o Legacy Fabric", -1, null);
		try {
			String loader = latestStableLoader(minecraftVersion);
			String profileUrl = META + minecraftVersion + "/" + loader + "/profile/json";
			JsonObject profile = JsonParser.parseString(Http.getString(profileUrl)).getAsJsonObject();
			String id = Json.string(profile, "id");
			Json.write(VersionResolver.jsonPath(id), profile);
			return id;
		} catch (IOException | RuntimeException e) {
			String cached = findInstalled(minecraftVersion);
			if (cached != null) {
				return cached;
			}
			throw new IOException("Não foi possível instalar o Legacy Fabric (" + e.getMessage() + ")", e);
		}
	}

	private static String latestStableLoader(String minecraftVersion) throws IOException {
		JsonArray loaders = JsonParser.parseString(Http.getString(META + minecraftVersion)).getAsJsonArray();
		String first = null;
		for (JsonElement element : loaders) {
			JsonObject loader = element.getAsJsonObject().getAsJsonObject("loader");
			String version = Json.string(loader, "version");
			if (first == null) {
				first = version;
			}
			if (loader.has("stable") && loader.get("stable").getAsBoolean()) {
				return version;
			}
		}

		if (first == null) {
			throw new IOException("Nenhum loader disponível para " + minecraftVersion);
		}

		return first;
	}

	private static String findInstalled(String minecraftVersion) {
		if (!Files.isDirectory(LauncherPaths.versions())) {
			return null;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(LauncherPaths.versions(), "fabric-loader-*-" + minecraftVersion)) {
			for (Path dir : stream) {
				String id = dir.getFileName().toString();
				if (Files.exists(VersionResolver.jsonPath(id))) {
					return id;
				}
			}
		} catch (IOException ignored) {
			// Sem cópia local.
		}
		return null;
	}
}
