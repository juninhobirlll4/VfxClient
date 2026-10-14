package com.vfxclient.launcher.mods;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.vfxclient.launcher.BuildInfo;

/**
 * Instala o mod do VfxClient ({@code vfxcore}) na pasta de mods do perfil.
 *
 * <p>O jar vem dentro do próprio launcher, então a versão do mod sempre combina com a do
 * launcher e funciona offline. Versões antigas do mod na pasta são removidas.</p>
 */
public final class ModInstaller {

	private static final String RESOURCE = "/com/vfxclient/launcher/mods/vfxcore.jar";

	private ModInstaller() {
	}

	/**
	 * @param gameDir pasta do jogo do perfil
	 * @return caminho do mod instalado
	 * @throws IOException se o launcher não tiver o mod embutido ou der erro de disco
	 */
	public static Path install(Path gameDir) throws IOException {
		Path mods = gameDir.resolve("mods");
		Files.createDirectories(mods);
		String version = BuildInfo.load().clientVersion();
		Path target = mods.resolve("vfxcore-" + version + ".jar");

		try (DirectoryStream<Path> old = Files.newDirectoryStream(mods, "vfxcore-*.jar")) {
			for (Path file : old) {
				if (!file.equals(target)) {
					Files.deleteIfExists(file);
				}
			}
		}

		try (InputStream in = ModInstaller.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IOException("O launcher foi gerado sem o mod embutido (rode o build completo: gradlew build).");
			}
			Path temp = mods.resolve("vfxcore.jar.tmp");
			Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		}

		return target;
	}

	/**
	 * Copia a skin escolhida no launcher para o mod usar (conta offline), ou remove a antiga.
	 *
	 * @param gameDir pasta do jogo
	 * @param skin    PNG da skin, ou {@code null} para nenhuma
	 * @throws IOException em erro de disco
	 */
	public static void installLocalSkin(Path gameDir, Path skin) throws IOException {
		Path target = gameDir.resolve("vfxclient").resolve("skins").resolve("skin.png");
		if (skin == null || !Files.exists(skin)) {
			Files.deleteIfExists(target);
			return;
		}
		Files.createDirectories(target.getParent());
		Files.copy(skin, target, StandardCopyOption.REPLACE_EXISTING);
	}
}
