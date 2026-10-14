package com.vfxclient.launcher.game;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Hashing;
import com.vfxclient.launcher.util.Json;

/**
 * Instala o Forge 1.8.9 a partir do instalador oficial (maven.minecraftforge.net), do mesmo
 * jeito que o instalador do Forge faz: lê o perfil de versão de dentro dele e extrai o
 * jar "universal" para a pasta de bibliotecas.
 */
public final class Forge {

	/** Build recomendado do Forge para o 1.8.9 (o último lançado). */
	public static final String VERSION_1_8_9 = "1.8.9-11.15.1.2318-1.8.9";

	private static final String MAVEN = "https://maven.minecraftforge.net/net/minecraftforge/forge/";
	/** SHA-1 oficial do instalador (maven.minecraftforge.net), usado para conferir a cópia embutida e o download. */
	private static final String INSTALLER_SHA1 = "ec0293ff0776b8831f2ed90511bab76e635dda0c";
	/** O instalador do Forge vem dentro do launcher (licença LGPL permite), então funciona sem internet. */
	private static final String BUNDLED = "/com/vfxclient/launcher/loaders/forge-" + VERSION_1_8_9 + "-installer.jar";

	private Forge() {
	}

	/**
	 * @param minecraftVersion versão do jogo (hoje só {@code 1.8.9})
	 * @param progress         andamento
	 * @return id da versão do Forge, pronta para o {@link VersionResolver}
	 * @throws IOException se não der para baixar ou instalar
	 */
	public static String install(String minecraftVersion, Progress progress) throws IOException {
		if (!"1.8.9".equals(minecraftVersion)) {
			throw new IOException("O launcher instala o Forge só para o 1.8.9.");
		}

		String forge = VERSION_1_8_9;
		String id = "1.8.9-forge" + forge;
		Path universal = LauncherPaths.libraries().resolve(LibraryInstaller.mavenPath("net.minecraftforge:forge:" + forge, null));
		if (Files.exists(VersionResolver.jsonPath(id)) && Files.exists(universal)) {
			return id;
		}

		Path installer = LauncherPaths.cache().resolve("forge-" + forge + "-installer.jar");
		Files.createDirectories(installer.getParent());
		try (InputStream bundled = Forge.class.getResourceAsStream(BUNDLED)) {
			if (bundled != null) {
				progress.update("Instalando o Forge embutido", -1, forge);
				Files.copy(bundled, installer, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		if (!Hashing.matches(installer, INSTALLER_SHA1, 0)) {
			progress.update("Baixando o Forge " + forge, -1, null);
			String url = MAVEN + forge + "/forge-" + forge + "-installer.jar";
			Downloader.run("Instalador do Forge", List.of(new Downloader.Task(url, installer, INSTALLER_SHA1, 0)), progress);
		}

		progress.update("Instalando o Forge", -1, null);
		try (ZipFile zip = new ZipFile(installer.toFile())) {
			ZipEntry profileEntry = zip.getEntry("install_profile.json");
			if (profileEntry == null) {
				throw new IOException("Instalador do Forge sem install_profile.json");
			}
			JsonObject profile;
			try (InputStream in = zip.getInputStream(profileEntry)) {
				profile = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			}
			JsonObject install = profile.getAsJsonObject("install");
			JsonObject versionInfo = profile.getAsJsonObject("versionInfo");
			ZipEntry universalEntry = zip.getEntry(Json.string(install, "filePath"));
			if (universalEntry == null) {
				throw new IOException("Instalador do Forge sem o jar universal");
			}
			Files.createDirectories(universal.getParent());
			try (InputStream in = zip.getInputStream(universalEntry)) {
				Files.copy(in, universal, StandardCopyOption.REPLACE_EXISTING);
			}
			Json.write(VersionResolver.jsonPath(Json.string(versionInfo, "id")), versionInfo);
			return Json.string(versionInfo, "id");
		} finally {
			Files.deleteIfExists(installer);
		}
	}
}
