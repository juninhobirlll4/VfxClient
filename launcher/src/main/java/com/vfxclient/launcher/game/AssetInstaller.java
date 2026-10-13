package com.vfxclient.launcher.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Json;

/** Baixa os assets (sons, idiomas...) de uma versão. */
public final class AssetInstaller {

	private static final String RESOURCES = "https://resources.download.minecraft.net/";

	/**
	 * @param indexName nome do índice (ex.: {@code 1.8})
	 * @param root      pasta a passar como {@code --assetsDir}
	 */
	public record Assets(String indexName, Path root) {
	}

	private AssetInstaller() {
	}

	/**
	 * @param version  versão resolvida
	 * @param progress andamento
	 * @return índice e pasta de assets a usar
	 * @throws IOException se algum download falhar
	 */
	public static Assets install(VersionResolver.Resolved version, Progress progress) throws IOException {
		JsonObject index = Json.object(version.json(), "assetIndex");
		String id = Json.string(index, "id");
		Path indexFile = LauncherPaths.assets().resolve("indexes").resolve(id + ".json");
		long size = index.has("size") ? index.get("size").getAsLong() : 0;
		Downloader.run("Índice de assets", List.of(new Downloader.Task(Json.string(index, "url"), indexFile, Json.string(index, "sha1"), size)), Progress.NONE);

		JsonObject indexJson = Json.readObject(indexFile);
		JsonObject objects = indexJson.getAsJsonObject("objects");
		List<Downloader.Task> tasks = new ArrayList<>();
		for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
			JsonObject object = entry.getValue().getAsJsonObject();
			String hash = Json.string(object, "hash");
			String sub = hash.substring(0, 2) + "/" + hash;
			tasks.add(new Downloader.Task(RESOURCES + sub, objectPath(hash), hash, object.get("size").getAsLong()));
		}
		Downloader.run("Assets do jogo", tasks, progress);

		boolean virtual = indexJson.has("virtual") && indexJson.get("virtual").getAsBoolean();
		boolean mapToResources = indexJson.has("map_to_resources") && indexJson.get("map_to_resources").getAsBoolean();
		if (virtual || mapToResources) {
			// Versões muito antigas leem os assets por nome, não por hash.
			Path virtualRoot = LauncherPaths.assets().resolve("virtual").resolve(id);
			for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
				Path target = virtualRoot.resolve(entry.getKey());
				if (!Files.exists(target)) {
					Files.createDirectories(target.getParent());
					Files.copy(objectPath(Json.string(entry.getValue().getAsJsonObject(), "hash")), target, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			return new Assets(id, virtualRoot);
		}
		return new Assets(id, LauncherPaths.assets());
	}

	private static Path objectPath(String hash) {
		return LauncherPaths.assets().resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
	}
}
