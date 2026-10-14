package com.vfxclient.launcher.game;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Http;
import com.vfxclient.launcher.util.Json;

/**
 * Instala o Java certo para cada versão usando os runtimes oficiais da Mojang
 * (o mesmo que o launcher oficial usa). A 1.8.9 pede {@code jre-legacy} (Java 8).
 *
 * <p>Sem internet, procura um Java instalado no computador com a versão certa.</p>
 */
public final class JavaRuntimeInstaller {

	private static final String ALL_RUNTIMES =
			"https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";

	private JavaRuntimeInstaller() {
	}

	/**
	 * @param version  versão resolvida
	 * @param progress andamento
	 * @return caminho do executável do Java ({@code javaw.exe} no Windows)
	 * @throws IOException se não houver como conseguir o Java
	 */
	public static Path install(VersionResolver.Resolved version, Progress progress) throws IOException {
		JsonObject javaVersion = Json.object(version.json(), "javaVersion");
		String component = javaVersion == null ? "jre-legacy" : Json.string(javaVersion, "component");
		int major = javaVersion == null || !javaVersion.has("majorVersion") ? 8 : javaVersion.get("majorVersion").getAsInt();
		Path dir = LauncherPaths.runtime().resolve(component);
		try {
			downloadRuntime(component, dir, progress);
			return executable(dir);
		} catch (IOException e) {
			if (Files.exists(executable(dir))) {
				// Já instalado antes: segue offline.
				return executable(dir);
			}
			Path local = findInstalledJava(major);
			if (local != null) {
				progress.update("Usando o Java " + major + " instalado no computador", 1, local.toString());
				return local;
			}
			throw new IOException("Não foi possível baixar o Java " + major + " (" + e.getMessage() + ")", e);
		}
	}

	private static void downloadRuntime(String component, Path dir, Progress progress) throws IOException {
		String platform = platformKey();
		JsonObject all = Http.getJson(ALL_RUNTIMES);
		JsonObject forPlatform = Json.object(all, platform);
		JsonArray candidates = forPlatform == null ? null : forPlatform.getAsJsonArray(component);
		if (candidates == null || candidates.isEmpty()) {
			throw new IOException("A Mojang não tem o Java " + component + " para " + platform);
		}
		JsonObject manifestRef = candidates.get(0).getAsJsonObject().getAsJsonObject("manifest");
		JsonObject manifest = Http.getJson(Json.string(manifestRef, "url"));

		List<Downloader.Task> tasks = new ArrayList<>();
		for (Map.Entry<String, JsonElement> entry : manifest.getAsJsonObject("files").entrySet()) {
			JsonObject file = entry.getValue().getAsJsonObject();
			Path target = dir.resolve(entry.getKey());
			String type = Json.string(file, "type");
			if ("directory".equals(type)) {
				Files.createDirectories(target);
			} else if ("file".equals(type)) {
				JsonObject raw = Json.object(Json.object(file, "downloads"), "raw");
				tasks.add(new Downloader.Task(Json.string(raw, "url"), target, Json.string(raw, "sha1"), raw.get("size").getAsLong()));
			}
		}
		Downloader.run("Java " + component, tasks, progress);
	}

	private static Path executable(Path runtimeDir) {
		boolean windows = Rules.osName().equals("windows");
		return runtimeDir.resolve("bin").resolve(windows ? "javaw.exe" : "java");
	}

	private static String platformKey() {
		String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
		switch (Rules.osName()) {
			case "windows":
				return arch.contains("aarch64") ? "windows-arm64" : arch.contains("64") ? "windows-x64" : "windows-x86";
			case "osx":
				return arch.contains("aarch64") ? "mac-os-arm64" : "mac-os";
			default:
				return arch.contains("64") ? "linux" : "linux-i386";
		}
	}

	/**
	 * Procura um Java já instalado (JAVA_HOME e pastas comuns de instalação no Windows).
	 *
	 * @param major versão principal desejada (8, 17, 21...)
	 * @return executável encontrado, ou {@code null}
	 */
	static Path findInstalledJava(int major) {
		List<Path> roots = new ArrayList<>();
		String javaHome = System.getenv("JAVA_HOME");
		if (javaHome != null) {
			roots.add(Paths.get(javaHome));
		}

		for (String base : new String[]{"C:\\Program Files\\Eclipse Adoptium", "C:\\Program Files\\Java",
				"C:\\Program Files\\Zulu", "C:\\Program Files\\Microsoft", "C:\\Program Files (x86)\\Java"}) {
			Path dir = Paths.get(base);
			if (Files.isDirectory(dir)) {
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
					for (Path child : stream) {
						roots.add(child);
					}
				} catch (IOException ignored) {
					// Pasta inacessível: ignora.
				}
			}
		}

		for (Path root : roots) {
			String name = root.getFileName() == null ? "" : root.getFileName().toString().toLowerCase(Locale.ROOT);
			boolean matches = major == 8
					? name.contains("1.8") || name.matches(".*(jdk|jre)-?8.*")
					: name.matches(".*(jdk|jre)-?" + major + "([.\\-].*)?");
			Path exe = executable(root);
			if (matches && Files.exists(exe)) {
				return exe;
			}
		}

		return null;
	}
}
