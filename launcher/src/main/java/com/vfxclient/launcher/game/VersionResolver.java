package com.vfxclient.launcher.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Json;

/**
 * Carrega o JSON de uma versão (baixando se preciso) e resolve a herança
 * ({@code inheritsFrom}), como o perfil do Legacy Fabric que herda da 1.8.9.
 */
public final class VersionResolver {

	/**
	 * Versão pronta para instalar e abrir.
	 *
	 * @param id    id da versão final (ex.: {@code fabric-loader-0.19.5-1.8.9})
	 * @param jarId id da versão cujo jar do cliente é usado (ex.: {@code 1.8.9})
	 * @param json  JSON já mesclado com o pai
	 */
	public record Resolved(String id, String jarId, JsonObject json) {
	}

	private VersionResolver() {
	}

	/**
	 * @param id       versão
	 * @param progress andamento
	 * @return versão resolvida
	 * @throws IOException se o JSON não existir nem puder ser baixado
	 */
	public static Resolved resolve(String id, Progress progress) throws IOException {
		JsonObject json = loadJson(id, progress);
		String parentId = Json.string(json, "inheritsFrom");
		if (parentId == null) {
			return new Resolved(id, id, json);
		}
		Resolved parent = resolve(parentId, progress);
		return new Resolved(id, parent.jarId(), merge(parent.json(), json));
	}

	/**
	 * @param id versão
	 * @return caminho do JSON salvo da versão
	 */
	public static Path jsonPath(String id) {
		return LauncherPaths.versions().resolve(id).resolve(id + ".json");
	}

	private static JsonObject loadJson(String id, Progress progress) throws IOException {
		Path file = jsonPath(id);
		VersionManifest.Entry entry = VersionManifest.find(id);
		if (entry != null) {
			progress.update("Verificando a versão " + id, -1, null);
			Downloader.run("Versão " + id, java.util.List.of(new Downloader.Task(entry.url(), file, entry.sha1(), 0)), Progress.NONE);
		}
		if (!Files.exists(file)) {
			throw new IOException("A versão " + id + " não está instalada e não foi possível baixá-la");
		}
		return Json.readObject(file);
	}

	/** Junta filho e pai: bibliotecas do filho têm prioridade (mesmo grupo:artefato). */
	private static JsonObject merge(JsonObject parent, JsonObject child) {
		JsonObject merged = parent.deepCopy();
		for (String key : child.keySet()) {
			if (!key.equals("libraries") && !key.equals("arguments") && !key.equals("inheritsFrom")) {
				merged.add(key, child.get(key).deepCopy());
			}
		}

		JsonArray libraries = new JsonArray();
		Set<String> seen = new HashSet<>();
		for (JsonArray source : new JsonArray[]{child.getAsJsonArray("libraries"), parent.getAsJsonArray("libraries")}) {
			if (source == null) {
				continue;
			}
			for (JsonElement element : source) {
				String key = libraryKey(element.getAsJsonObject());
				if (seen.add(key)) {
					libraries.add(element.deepCopy());
				}
			}
		}
		merged.add("libraries", libraries);

		JsonObject childArgs = Json.object(child, "arguments");
		if (childArgs != null) {
			JsonObject args = Json.object(merged, "arguments");
			if (args == null) {
				args = new JsonObject();
				merged.add("arguments", args);
			}
			for (String side : new String[]{"game", "jvm"}) {
				if (childArgs.has(side)) {
					JsonArray list = args.has(side) ? args.getAsJsonArray(side) : new JsonArray();
					list.addAll(childArgs.getAsJsonArray(side));
					args.add(side, list);
				}
			}
		}
		return merged;
	}

	/**
	 * @param library biblioteca do JSON
	 * @return {@code grupo:artefato[:classificador]} (sem a versão)
	 */
	static String libraryKey(JsonObject library) {
		String[] parts = Json.string(library, "name").split(":");
		String key = parts[0] + ":" + parts[1];
		return parts.length > 3 ? key + ":" + parts[3] : key;
	}
}
