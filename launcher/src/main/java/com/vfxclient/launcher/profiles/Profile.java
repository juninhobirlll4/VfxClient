package com.vfxclient.launcher.profiles;

import java.nio.file.Path;

import com.vfxclient.launcher.LauncherPaths;

/**
 * Perfil de jogo: qual versão abrir, com quanta RAM, qual preset de desempenho e qual
 * perfil de configuração do VfxClient. Cada perfil tem a própria pasta de jogo.
 *
 * <p>Campos públicos porque a classe é gravada direto em {@code profiles.json}.</p>
 */
public final class Profile {

	/** Tipo de perfil. */
	public enum Kind {
		/** Minecraft 1.8.9 + Legacy Fabric + VfxClient. */
		VFX,
		/** Minecraft oficial, sem mods. */
		VANILLA,
		/** Minecraft 1.8.9 + OptiFine (sem os recursos do VfxClient). */
		OPTIFINE,
		/** Minecraft 1.8.9 + Forge, para mods próprios (sem os recursos do VfxClient). */
		FORGE
	}

	/** Identificador (também nome da pasta da instância). */
	public String id;
	/** Nome exibido. */
	public String name;
	/** Tipo. */
	public Kind kind = Kind.VFX;
	/** Versão do Minecraft. */
	public String minecraftVersion = "1.8.9";
	/** RAM máxima do jogo, em MB. */
	public int ramMb = 2048;
	/** Preset de desempenho enviado ao VfxClient: {@code auto}, {@code low}, {@code balanced}, {@code high} ou {@code custom}. */
	public String preset = "auto";
	/** Perfil de configuração do VfxClient (pasta {@code vfxclient/config/profiles}). */
	public String configProfile = "Padrao";
	/** Argumentos extras da JVM (avançado). */
	public String extraJvmArgs = "";
	/** Diminuir sozinho recursos pesados em PC fraco. */
	public boolean autoOptimize = true;
	/** Perfil Forge: colocar o OptiFine na pasta de mods (OptiFine 1.8.9 funciona como mod do Forge). */
	public boolean optifineWithForge;

	/** @return pasta do jogo deste perfil */
	public Path gameDir() {
		return LauncherPaths.instances().resolve(id);
	}

	/** @return se é o perfil com o VfxClient */
	public boolean isVfx() {
		return kind == Kind.VFX;
	}

	@Override
	public String toString() {
		String loader = switch (kind) {
			case VFX -> " (VfxClient)";
			case OPTIFINE -> " (OptiFine)";
			case FORGE -> optifineWithForge ? " (Forge + OptiFine)" : " (Forge)";
			default -> "";
		};
		return name + "  ·  " + minecraftVersion + loader;
	}
}
