package com.vfxclient.launcher.profiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.settings.SystemInfo;
import com.vfxclient.launcher.util.Json;

/** Perfis de jogo salvos em {@code launcher/profiles.json}. */
public final class ProfileStore {

	/** Id do perfil padrão do VfxClient. */
	public static final String DEFAULT_ID = "vfxclient-1.8.9";

	private static final Path FILE = LauncherPaths.launcher().resolve("profiles.json");

	/** Formato do arquivo. */
	private static final class Data {
		List<Profile> profiles = new ArrayList<>();
		String selected = DEFAULT_ID;
	}

	private Data data = new Data();

	/** Lê o arquivo e garante que o perfil padrão do VfxClient exista. */
	public void load() {
		try {
			Data loaded = Json.read(FILE, Data.class);
			if (loaded != null && loaded.profiles != null) {
				data = loaded;
			}
		} catch (IOException e) {
			data = new Data();
		}
		if (find(DEFAULT_ID) == null) {
			Profile profile = new Profile();
			profile.id = DEFAULT_ID;
			profile.name = "VfxClient";
			profile.kind = Profile.Kind.VFX;
			profile.minecraftVersion = "1.8.9";
			profile.ramMb = SystemInfo.recommendedRamMb();
			data.profiles.add(0, profile);
		}
	}

	/**
	 * Grava o arquivo.
	 *
	 * @throws IOException em erro de disco
	 */
	public void save() throws IOException {
		Json.write(FILE, data);
	}

	/** @return perfis */
	public List<Profile> all() {
		return data.profiles;
	}

	/**
	 * @param id identificador
	 * @return perfil ou {@code null}
	 */
	public Profile find(String id) {
		for (Profile profile : data.profiles) {
			if (profile.id.equals(id)) {
				return profile;
			}
		}
		return null;
	}

	/** @return perfil escolhido (ou o padrão) */
	public Profile selected() {
		Profile profile = find(data.selected);
		return profile != null ? profile : data.profiles.get(0);
	}

	/**
	 * @param profile perfil a usar
	 */
	public void select(Profile profile) {
		data.selected = profile.id;
	}

	/**
	 * Cria um perfil de Minecraft oficial (sem mods) para uma versão.
	 *
	 * @param version versão do Minecraft
	 * @return perfil novo (já adicionado e selecionado)
	 */
	public Profile createVanilla(String version) {
		Profile profile = new Profile();
		profile.kind = Profile.Kind.VANILLA;
		profile.minecraftVersion = version;
		profile.name = "Minecraft " + version;
		profile.id = uniqueId("vanilla-" + version.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.\\-]", ""));
		profile.ramMb = SystemInfo.recommendedRamMb();
		profile.preset = "custom";
		data.profiles.add(profile);
		select(profile);
		return profile;
	}

	/**
	 * Remove um perfil (o padrão do VfxClient não pode ser removido).
	 *
	 * @param profile perfil
	 * @return {@code true} se removeu
	 */
	public boolean remove(Profile profile) {
		if (profile.id.equals(DEFAULT_ID)) {
			return false;
		}
		boolean removed = data.profiles.remove(profile);
		if (profile.id.equals(data.selected)) {
			data.selected = DEFAULT_ID;
		}
		return removed;
	}

	private String uniqueId(String base) {
		String id = base;
		int n = 2;
		while (find(id) != null) {
			id = base + "-" + n++;
		}
		return id;
	}
}
