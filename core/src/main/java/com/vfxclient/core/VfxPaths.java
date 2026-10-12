package com.vfxclient.core;

import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Caminhos de disco usados pelo VfxClient.
 *
 * <p>Tudo que é nosso fica em {@code .minecraft/vfxclient/}, separado das pastas do
 * vanilla e de outros mods.</p>
 */
public final class VfxPaths {

	/** Nome da pasta do client dentro do diretório do jogo. */
	public static final String ROOT_FOLDER = "vfxclient";

	private VfxPaths() {
	}

	/**
	 * Pasta raiz do client.
	 *
	 * @return {@code <diretório do jogo>/vfxclient}
	 */
	public static Path root() {
		return FabricLoader.getInstance().getGameDir().resolve(ROOT_FOLDER);
	}

	/**
	 * Pasta dos arquivos de configuração JSON.
	 *
	 * @return {@code <diretório do jogo>/vfxclient/config}
	 */
	public static Path config() {
		return root().resolve("config");
	}
}
