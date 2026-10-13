package com.vfxclient.launcher.settings;

import java.io.IOException;
import java.nio.file.Path;

import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.util.Json;

/** Configurações do launcher, salvas em {@code launcher/settings.json}. */
public final class LauncherSettings {

	private static final Path FILE = LauncherPaths.launcher().resolve("settings.json");

	/** Client ID do app registrado no Azure (login Microsoft). Vazio = só contas offline. */
	public String microsoftClientId = "";
	/** O que fazer ao abrir o jogo: {@code keep}, {@code minimize} ou {@code close}. */
	public String onLaunch = "minimize";
	/** Procurar atualizações ao abrir o launcher. */
	public boolean checkUpdates = true;
	/** Rodar o diagnóstico rápido antes de cada início. */
	public boolean diagnoseBeforeLaunch = true;

	/** @return configurações do disco (ou padrão) */
	public static LauncherSettings load() {
		try {
			LauncherSettings settings = Json.read(FILE, LauncherSettings.class);
			return settings != null ? settings : new LauncherSettings();
		} catch (IOException e) {
			return new LauncherSettings();
		}
	}

	/**
	 * Grava no disco.
	 *
	 * @throws IOException em erro de disco
	 */
	public void save() throws IOException {
		Json.write(FILE, this);
	}

	/** @return se o login Microsoft está configurado */
	public boolean microsoftEnabled() {
		return microsoftClientId != null && !microsoftClientId.isBlank();
	}
}
