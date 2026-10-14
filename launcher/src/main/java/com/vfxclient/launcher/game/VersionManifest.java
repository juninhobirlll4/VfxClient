package com.vfxclient.launcher.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Http;
import com.vfxclient.launcher.util.Json;

/**
 * Lista oficial de versões do Minecraft (da Mojang), com cópia local para funcionar offline.
 */
public final class VersionManifest {

	private static final String URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

	/**
	 * Uma versão da lista.
	 *
	 * @param id   ex.: {@code 1.8.9}
	 * @param type {@code release}, {@code snapshot}, {@code old_beta}...
	 * @param url  endereço do JSON da versão
	 * @param sha1 hash do JSON da versão
	 */
	public record Entry(String id, String type, String url, String sha1) {
	}

	private static List<Entry> cached;

	private VersionManifest() {
	}

	/**
	 * Carrega a lista (internet primeiro; se falhar, a cópia salva).
	 *
	 * @return versões, da mais nova para a mais antiga
	 * @throws IOException se não houver internet nem cópia salva
	 */
	public static synchronized List<Entry> load() throws IOException {
		if (cached != null) {
			return cached;
		}

		Path local = LauncherPaths.launcher().resolve("version_manifest_v2.json");
		JsonObject root;
		try {
			root = Http.getJson(URL);
			Json.write(local, root);
		} catch (IOException e) {
			if (!Files.exists(local)) {
				throw new IOException("Sem internet e sem lista de versões salva", e);
			}
			root = Json.readObject(local);
		}

		List<Entry> entries = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray("versions")) {
			JsonObject v = element.getAsJsonObject();
			entries.add(new Entry(Json.string(v, "id"), Json.string(v, "type"), Json.string(v, "url"), Json.string(v, "sha1")));
		}

		cached = entries;
		return entries;
	}

	/**
	 * @param id versão
	 * @return a entrada, ou {@code null} se não existir (ou se estiver offline sem cópia)
	 */
	public static Entry find(String id) {
		try {
			for (Entry entry : load()) {
				if (entry.id().equals(id)) {
					return entry;
				}
			}
		} catch (IOException ignored) {
			// Offline: quem chamou usa o JSON salvo, se houver.
		}
		return null;
	}

	/**
	 * @return ids das versões estáveis (releases), da mais nova para a mais antiga
	 */
	public static List<String> releases() {
		List<String> ids = new ArrayList<>();
		try {
			for (Entry entry : load()) {
				if ("release".equals(entry.type())) {
					ids.add(entry.id());
				}
			}
		} catch (IOException ignored) {
			// Sem lista: a interface mostra só os perfis já existentes.
		}
		return ids;
	}
}
