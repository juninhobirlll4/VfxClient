package com.vfxclient.launcher;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Pastas do launcher. Tudo fica em {@code %APPDATA%\.vfxclient} (no Windows), separado do
 * {@code .minecraft} do launcher oficial para não misturar versões e mods.
 *
 * <pre>
 * .vfxclient/
 *   launcher/      settings.json, profiles.json, accounts.json, logs do launcher
 *   versions/      JSON e jar de cada versão (+ natives extraídos)
 *   libraries/     bibliotecas do Minecraft e do Legacy Fabric
 *   assets/        índices e objetos (sons, idiomas...)
 *   runtime/       Javas baixados da Mojang (jre-legacy = Java 8 para a 1.8.9)
 *   instances/     uma pasta de jogo por perfil (mods, saves, config do VfxClient)
 *   cache/         downloads temporários
 * </pre>
 */
public final class LauncherPaths {

	private static final Path ROOT = resolveRoot();

	private LauncherPaths() {
	}

	private static Path resolveRoot() {
		String override = System.getProperty("vfx.home");
		if (override != null && !override.isBlank()) {
			return Paths.get(override);
		}
		String appData = System.getenv("APPDATA");
		if (appData != null && !appData.isBlank()) {
			return Paths.get(appData, ".vfxclient");
		}
		return Paths.get(System.getProperty("user.home"), ".vfxclient");
	}

	/** @return pasta raiz de dados */
	public static Path root() {
		return ROOT;
	}

	/** @return configurações do próprio launcher */
	public static Path launcher() {
		return ROOT.resolve("launcher");
	}

	/** @return JSONs e jars das versões */
	public static Path versions() {
		return ROOT.resolve("versions");
	}

	/** @return bibliotecas */
	public static Path libraries() {
		return ROOT.resolve("libraries");
	}

	/** @return assets (sons, idiomas, texturas extras) */
	public static Path assets() {
		return ROOT.resolve("assets");
	}

	/** @return Javas baixados */
	public static Path runtime() {
		return ROOT.resolve("runtime");
	}

	/** @return pastas de jogo de cada perfil */
	public static Path instances() {
		return ROOT.resolve("instances");
	}

	/** @return arquivos temporários */
	public static Path cache() {
		return ROOT.resolve("cache");
	}
}
