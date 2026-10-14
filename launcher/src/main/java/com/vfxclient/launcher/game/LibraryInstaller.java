package com.vfxclient.launcher.game;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Json;

/**
 * Bibliotecas de uma versão: monta a lista de downloads, o classpath e extrai os
 * arquivos nativos (DLLs do LWJGL etc.).
 */
public final class LibraryInstaller {

	private static final String MOJANG_LIBRARIES = "https://libraries.minecraft.net/";

	/**
	 * Resultado da preparação.
	 *
	 * @param downloads  arquivos a baixar/verificar
	 * @param classpath  jars do classpath, na ordem
	 * @param nativeJars jars de nativos a extrair
	 */
	public record Plan(List<Downloader.Task> downloads, List<Path> classpath, List<NativeJar> nativeJars) {
	}

	/**
	 * Um jar de nativos.
	 *
	 * @param file     caminho do jar
	 * @param excludes prefixos que não devem ser extraídos (ex.: {@code META-INF/})
	 */
	public record NativeJar(Path file, List<String> excludes) {
	}

	private LibraryInstaller() {
	}

	/**
	 * @param version versão resolvida
	 * @return plano com downloads, classpath e nativos
	 */
	public static Plan plan(VersionResolver.Resolved version) {
		List<Downloader.Task> downloads = new ArrayList<>();
		List<Path> classpath = new ArrayList<>();
		List<NativeJar> natives = new ArrayList<>();

		JsonArray libraries = version.json().getAsJsonArray("libraries");
		for (JsonElement element : libraries) {
			JsonObject library = element.getAsJsonObject();
			if (!Rules.allows(library.getAsJsonArray("rules"))) {
				continue;
			}
			// Formato antigo do Forge: bibliotecas só do servidor vêm marcadas com clientreq=false.
			if (library.has("clientreq") && !library.get("clientreq").getAsBoolean()) {
				continue;
			}
			String name = Json.string(library, "name");
			JsonObject downloadsJson = Json.object(library, "downloads");
			JsonObject nativesMap = Json.object(library, "natives");
			List<String> excludes = excludes(library);

			if (downloadsJson != null) {
				// Formato da Mojang: artefato e classificadores com URL e hash.
				JsonObject artifact = Json.object(downloadsJson, "artifact");
				if (artifact != null && nativesMap == null) {
					Path file = LauncherPaths.libraries().resolve(Json.string(artifact, "path"));
					downloads.add(task(artifact, file));
					classpath.add(file);
				}
				if (nativesMap != null) {
					String classifier = nativeClassifier(nativesMap);
					JsonObject classifiers = Json.object(downloadsJson, "classifiers");
					JsonObject nativeArtifact = classifier == null ? null : Json.object(classifiers, classifier);
					if (nativeArtifact != null) {
						Path file = LauncherPaths.libraries().resolve(Json.string(nativeArtifact, "path"));
						downloads.add(task(nativeArtifact, file));
						natives.add(new NativeJar(file, excludes));
					}
				}
				continue;
			}

			// Formato Maven (Fabric/Legacy Fabric): nome + repositório.
			String repo = Json.string(library, "url");
			if (repo == null) {
				repo = MOJANG_LIBRARIES;
			}
			if (!repo.endsWith("/")) {
				repo += "/";
			}
			String sha1 = Json.string(library, "sha1");
			long size = library.has("size") ? library.get("size").getAsLong() : 0;
			if (nativesMap != null || isPlatformNatives(name, nativesMap)) {
				// Nativos em formato Maven (ex.: LWJGL do Legacy Fabric, que só publica os nativos).
				String classifier = nativesMap != null ? nativeClassifier(nativesMap) : "natives-" + Rules.osName();
				if (classifier == null) {
					continue;
				}
				String path = mavenPath(name, classifier);
				Path file = LauncherPaths.libraries().resolve(path);
				downloads.add(new Downloader.Task(repo + path, file, null, 0));
				natives.add(new NativeJar(file, excludes.isEmpty() ? List.of("META-INF/") : excludes));
				continue;
			}
			String path = mavenPath(name, null);
			Path file = LauncherPaths.libraries().resolve(path);
			downloads.add(new Downloader.Task(repo + path, file, sha1, size));
			classpath.add(file);
		}

		return new Plan(downloads, classpath, natives);
	}

	/**
	 * Extrai os nativos para a pasta da versão.
	 *
	 * @param jars nativos
	 * @param dir  destino (limpo antes)
	 * @throws IOException em erro de disco
	 */
	public static void extractNatives(List<NativeJar> jars, Path dir) throws IOException {
		Files.createDirectories(dir);
		for (NativeJar jar : jars) {
			try (ZipFile zip = new ZipFile(jar.file().toFile())) {
				var entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (entry.isDirectory() || excluded(entry.getName(), jar.excludes())) {
						continue;
					}
					Path target = dir.resolve(entry.getName()).normalize();
					if (!target.startsWith(dir)) {
						continue; // Proteção contra caminhos maliciosos (zip slip).
					}
					Files.createDirectories(target.getParent());
					try (InputStream in = zip.getInputStream(entry)) {
						Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}
	}

	/**
	 * Caminho Maven de um artefato.
	 *
	 * @param name       {@code grupo:artefato:versão[:classificador]}
	 * @param classifier classificador extra (ou {@code null})
	 * @return caminho relativo, ex.: {@code net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar}
	 */
	public static String mavenPath(String name, String classifier) {
		String[] parts = name.split(":");
		String group = parts[0].replace('.', '/');
		String artifact = parts[1];
		String version = parts[2];
		String suffix = classifier != null ? "-" + classifier : parts.length > 3 ? "-" + parts[3] : "";
		return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + suffix + ".jar";
	}

	private static boolean isPlatformNatives(String name, JsonObject nativesMap) {
		return nativesMap == null && name.split(":")[1].endsWith("-platform");
	}

	private static String nativeClassifier(JsonObject nativesMap) {
		String classifier = Json.string(nativesMap, Rules.osName());
		return classifier == null ? null : classifier.replace("${arch}", Rules.archBits());
	}

	private static List<String> excludes(JsonObject library) {
		List<String> list = new ArrayList<>();
		JsonObject extract = Json.object(library, "extract");
		if (extract != null && extract.has("exclude")) {
			for (JsonElement e : extract.getAsJsonArray("exclude")) {
				list.add(e.getAsString());
			}
		}
		return list;
	}

	private static boolean excluded(String entry, List<String> excludes) {
		for (String prefix : excludes) {
			if (entry.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private static Downloader.Task task(JsonObject artifact, Path file) {
		long size = artifact.has("size") ? artifact.get("size").getAsLong() : 0;
		return new Downloader.Task(Json.string(artifact, "url"), file, Json.string(artifact, "sha1"), size);
	}
}
