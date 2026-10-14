package com.vfxclient.core.gui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.VfxPaths;

/** Servidores favoritos (★), guardados pelo endereço em {@code vfxclient/config/favorites.txt}. */
public final class ServerFavorites {

	private static Set<String> addresses;

	private ServerFavorites() {
	}

	/** @return se o endereço está nos favoritos */
	public static boolean contains(String address) {
		return load().contains(normalize(address));
	}

	/** Marca ou desmarca o endereço como favorito e salva. */
	public static void toggle(String address) {
		String key = normalize(address);
		Set<String> set = load();
		if (!set.remove(key)) {
			set.add(key);
		}
		save();
	}

	private static Set<String> load() {
		if (addresses == null) {
			addresses = new LinkedHashSet<String>();
			try {
				Path file = file();
				if (Files.exists(file)) {
					for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
						if (!line.trim().isEmpty()) {
							addresses.add(normalize(line));
						}
					}
				}
			} catch (IOException e) {
				VfxCore.LOGGER.warn("Não deu para ler os favoritos: {}", e.toString());
			}
		}
		return addresses;
	}

	private static void save() {
		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			Files.write(file, new ArrayList<String>(load()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			VfxCore.LOGGER.warn("Não deu para salvar os favoritos: {}", e.toString());
		}
	}

	private static Path file() {
		return VfxPaths.config().resolve("favorites.txt");
	}

	private static String normalize(String address) {
		return address == null ? "" : address.trim().toLowerCase();
	}
}