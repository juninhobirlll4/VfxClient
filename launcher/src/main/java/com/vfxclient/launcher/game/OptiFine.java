package com.vfxclient.launcher.game;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Json;

/**
 * Suporte ao OptiFine 1.8.9.
 *
 * <p>O OptiFine não vai para o GitHub (a licença não permite distribuição pública). Ele chega
 * ao launcher de três jeitos: pelo instalador do grupo gerado no PC (pasta {@code optifine\}
 * ao lado do VfxClient.exe), achado sozinho no computador ou escolhido à mão. O launcher o
 * guarda em {@code launcher/optifine/}. A instalação imita o instalador oficial do OptiFine: cria uma
 * versão que herda do 1.8.9 e inicia pelo LaunchWrapper com o {@code OptiFineTweaker}.</p>
 */
public final class OptiFine {

	/** Página oficial de downloads. */
	public static final String DOWNLOADS_PAGE = "https://optifine.net/downloads";

	private static final String TWEAKER_CLASS = "optifine/OptiFineTweaker.class";
	private static final String PATCHER_CLASS = "optifine/Patcher.class";

	private OptiFine() {
	}

	/** @return pasta onde o jar escolhido pelo jogador fica guardado */
	public static Path storage() {
		return LauncherPaths.launcher().resolve("optifine");
	}

	/** @return o jar do OptiFine 1.8.9 já importado, ou {@code null} */
	public static Path installedJar() {
		if (!Files.isDirectory(storage())) {
			return null;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(storage(), "OptiFine*1.8.9*.jar")) {
			for (Path jar : stream) {
				return jar;
			}
		} catch (IOException ignored) {
			// Sem acesso: trata como não instalado.
		}
		return null;
	}

	/**
	 * Procura um OptiFine 1.8.9 que o jogador já tenha no computador e o importa sozinho:
	 * Área de Trabalho, Downloads e as pastas do launcher oficial (.minecraft). Assim ninguém
	 * precisa selecionar o arquivo à mão; o launcher nunca baixa nem distribui o OptiFine.
	 *
	 * @return o jar importado (ou o que já estava), ou {@code null} se não achou nenhum
	 */
	public static Path autoDetect() {
		Path current = installedJar();
		if (current != null) {
			return current;
		}
		String home = System.getProperty("user.home");
		String appData = System.getenv("APPDATA");
		List<Path> places = new ArrayList<>();
		// Instalador do grupo (gerado no PC com o OptiFine junto): pasta optifine\ ao lado do VfxClient.exe.
		Path installed = installDir();
		if (installed != null) {
			places.add(installed.resolve("optifine"));
		}
		places.add(Path.of(home, "Desktop"));
		places.add(Path.of(home, "OneDrive", "Desktop"));
		places.add(Path.of(home, "OneDrive", "Área de Trabalho"));
		places.add(Path.of(home, "Downloads"));
		if (appData != null) {
			places.add(Path.of(appData, ".minecraft", "mods"));
			places.add(Path.of(appData, ".minecraft", "libraries", "optifine", "OptiFine"));
		}
		for (Path place : places) {
			Path found = findIn(place, 2);
			if (found != null) {
				try {
					return importJar(found);
				} catch (IOException ignored) {
					// Arquivo inválido: continua procurando.
				}
			}
		}
		return null;
	}

	/** @return pasta do VfxClient instalado (onde fica o VfxClient.exe), ou {@code null} no modo de desenvolvimento */
	private static Path installDir() {
		String appPath = System.getProperty("jpackage.app-path");
		if (appPath != null) {
			return Path.of(appPath).getParent();
		}
		return ProcessHandle.current().info().command()
				.map(Path::of)
				.filter(exe -> exe.getFileName().toString().equalsIgnoreCase("VfxClient.exe"))
				.map(Path::getParent)
				.orElse(null);
	}

	private static Path findIn(Path dir, int depth) {
		if (!Files.isDirectory(dir)) {
			return null;
		}
		try (var walk = Files.walk(dir, depth)) {
			return walk.filter(p -> {
				String n = p.getFileName().toString();
				return n.matches("(?i)OptiFine[_-]1\\.8\\.9.*\\.jar") && Files.isRegularFile(p);
			}).findFirst().orElse(null);
		} catch (IOException | java.io.UncheckedIOException e) {
			return null;
		}
	}

	/**
	 * Confere e guarda o jar baixado pelo jogador (substitui o anterior).
	 *
	 * @param jar arquivo escolhido
	 * @return caminho guardado
	 * @throws IOException se não for um OptiFine 1.8.9 válido
	 */
	public static Path importJar(Path jar) throws IOException {
		String name = jar.getFileName().toString();
		if (!name.toLowerCase().endsWith(".jar") || !name.contains("1.8.9")) {
			throw new IOException("Escolha o arquivo do OptiFine para 1.8.9 (ex.: OptiFine_1.8.9_HD_U_M5.jar).");
		}
		try (ZipFile zip = new ZipFile(jar.toFile())) {
			if (zip.getEntry(TWEAKER_CLASS) == null) {
				throw new IOException("Esse arquivo não parece ser o OptiFine (falta optifine/OptiFineTweaker).");
			}
		}
		Files.createDirectories(storage());
		try (DirectoryStream<Path> old = Files.newDirectoryStream(storage(), "*.jar")) {
			for (Path file : old) {
				Files.deleteIfExists(file);
			}
		}
		Path target = storage().resolve(name);
		Files.copy(jar, target, StandardCopyOption.REPLACE_EXISTING);
		return target;
	}

	/**
	 * Cria (ou reaproveita) a versão do OptiFine.
	 *
	 * @param vanilla  versão 1.8.9 já resolvida (para o jar e os argumentos)
	 * @param java     Java 8 já instalado (usado pelo patcher do OptiFine)
	 * @param progress andamento
	 * @return id da versão do OptiFine
	 * @throws IOException se o OptiFine não tiver sido importado ou a instalação falhar
	 */
	public static String install(VersionResolver.Resolved vanilla, Path java, Progress progress) throws IOException {
		Path jar = installedJar();
		if (jar == null) {
			throw new IOException("Baixe o OptiFine 1.8.9 em optifine.net e escolha o arquivo no launcher (botão \"Selecionar OptiFine\").");
		}
		String edition = jar.getFileName().toString().replaceFirst("(?i)^OptiFine[_-]", "").replaceFirst("(?i)\\.jar$", "");
		String libraryName = "optifine:OptiFine:" + edition;
		Path library = LauncherPaths.libraries().resolve(LibraryInstaller.mavenPath(libraryName, null));
		String id = "1.8.9-OptiFine_" + edition.replaceFirst("^1\\.8\\.9_", "");

		if (!Files.exists(library) || Files.getLastModifiedTime(library).compareTo(Files.getLastModifiedTime(jar)) < 0) {
			progress.update("Preparando o OptiFine", -1, edition);
			Path vanillaJar = LauncherPaths.versions().resolve(vanilla.jarId()).resolve(vanilla.jarId() + ".jar");
			buildLibrary(jar, vanillaJar, library, java);
		}

		JsonObject version = new JsonObject();
		version.addProperty("id", id);
		version.addProperty("inheritsFrom", vanilla.id());
		version.addProperty("type", "release");
		version.addProperty("mainClass", "net.minecraft.launchwrapper.Launch");
		version.addProperty("minecraftArguments", Json.string(vanilla.json(), "minecraftArguments") + " --tweakClass optifine.OptiFineTweaker");
		JsonArray libraries = new JsonArray();
		libraries.add(library(libraryName));
		libraries.add(library("net.minecraft:launchwrapper:1.12"));
		version.add("libraries", libraries);
		Json.write(VersionResolver.jsonPath(id), version);
		return id;
	}

	/**
	 * Monta o jar de biblioteca do OptiFine. OptiFines que trazem o próprio "Patcher" geram o
	 * jar a partir do jar do jogo (como o instalador oficial); os outros já vêm prontos.
	 */
	private static void buildLibrary(Path optifine, Path vanillaJar, Path library, Path java) throws IOException {
		Files.createDirectories(library.getParent());
		boolean hasPatcher;
		try (ZipFile zip = new ZipFile(optifine.toFile())) {
			hasPatcher = zip.getEntry(PATCHER_CLASS) != null;
		}
		if (hasPatcher) {
			Path out = library.resolveSibling(library.getFileName() + ".tmp");
			Path javaExe = java.resolveSibling(java.getFileName().toString().replace("javaw", "java"));
			Process process = new ProcessBuilder(javaExe.toString(), "-cp", optifine.toString(), "optifine.Patcher",
					vanillaJar.toString(), optifine.toString(), out.toString())
					.redirectErrorStream(true)
					.redirectOutput(LauncherPaths.cache().resolve("optifine-patcher.log").toFile())
					.start();
			try {
				boolean finished = process.waitFor(2, TimeUnit.MINUTES);
				if (finished && process.exitValue() == 0 && Files.size(out) > 0) {
					Files.move(out, library, StandardCopyOption.REPLACE_EXISTING);
					return;
				}
				process.destroyForcibly();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrompido", e);
			} catch (IOException ignored) {
				// O patcher não gerou o arquivo: usa o jar como veio.
			} finally {
				Files.deleteIfExists(out);
			}
		}
		Files.copy(optifine, library, StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * Coloca (ou tira) o OptiFine da pasta de mods de um perfil Forge.
	 *
	 * @param gameDir pasta do jogo do perfil
	 * @param enabled se o OptiFine deve estar na pasta de mods
	 * @throws IOException se pedir o OptiFine e ele não estiver importado
	 */
	public static void syncForgeMod(Path gameDir, boolean enabled) throws IOException {
		Path mods = gameDir.resolve("mods");
		Files.createDirectories(mods);
		try (DirectoryStream<Path> old = Files.newDirectoryStream(mods, "OptiFine*.jar")) {
			for (Path file : old) {
				Files.deleteIfExists(file);
			}
		}
		if (!enabled) {
			return;
		}
		Path jar = installedJar();
		if (jar == null) {
			throw new IOException("Para usar Forge + OptiFine, selecione antes o arquivo do OptiFine 1.8.9 no launcher.");
		}
		Files.copy(jar, mods.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
	}

	/** Legacy OptiFabric (MPL-2.0, github.com/tildejustin/optifabric): carrega o OptiFine dentro do Legacy Fabric. */
	private static final String OPTIFABRIC = "/com/vfxclient/launcher/loaders/optifabric-legacy.jar";
	/** Versão do Legacy OptiFabric embutido. */
	public static final String OPTIFABRIC_VERSION = "2.3.6+1.3.1-1.13.2";

	/**
	 * Perfil VfxClient: coloca (ou tira) o OptiFine e o Legacy OptiFabric na pasta de mods, para
	 * o VfxClient e o OptiFine rodarem juntos no mesmo jogo.
	 *
	 * @param gameDir pasta do jogo do perfil
	 * @param enabled se o OptiFine deve ir junto
	 * @return {@code true} se o OptiFine ficou instalado (precisa do jar do OptiFine importado)
	 * @throws IOException em erro de disco
	 */
	public static boolean syncFabricMods(Path gameDir, boolean enabled) throws IOException {
		Path mods = gameDir.resolve("mods");
		Files.createDirectories(mods);
		Path jar = enabled ? installedJar() : null;
		Path optifabric = mods.resolve("optifabric-" + OPTIFABRIC_VERSION + ".jar");
		try (DirectoryStream<Path> old = Files.newDirectoryStream(mods, "{OptiFine,optifabric}*.jar")) {
			for (Path file : old) {
				boolean keep = jar != null && (file.equals(optifabric) || file.getFileName().equals(jar.getFileName()));
				if (!keep) {
					Files.deleteIfExists(file);
				}
			}
		}
		if (jar == null) {
			return false;
		}
		Path target = mods.resolve(jar.getFileName());
		if (!Files.exists(target) || Files.size(target) != Files.size(jar)) {
			Files.copy(jar, target, StandardCopyOption.REPLACE_EXISTING);
		}
		if (!Files.exists(optifabric)) {
			try (java.io.InputStream in = OptiFine.class.getResourceAsStream(OPTIFABRIC)) {
				if (in == null) {
					throw new IOException("O launcher foi gerado sem o Legacy OptiFabric embutido.");
				}
				Files.copy(in, optifabric, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		return true;
	}

	private static JsonObject library(String name) {
		JsonObject library = new JsonObject();
		library.addProperty("name", name);
		return library;
	}
}
