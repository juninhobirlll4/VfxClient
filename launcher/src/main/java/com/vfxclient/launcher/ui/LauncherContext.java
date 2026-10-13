package com.vfxclient.launcher.ui;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vfxclient.launcher.auth.AccountStore;
import com.vfxclient.launcher.profiles.ProfileStore;
import com.vfxclient.launcher.settings.LauncherSettings;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.stage.Stage;

/** Estado compartilhado entre as telas do launcher (contas, perfis, configurações). */
public final class LauncherContext {

	private final Stage stage;
	private final HostServices hostServices;
	private final AccountStore accounts = new AccountStore();
	private final ProfileStore profiles = new ProfileStore();
	private final ExecutorService background = Executors.newCachedThreadPool(r -> {
		Thread thread = new Thread(r, "vfx-launcher-bg");
		thread.setDaemon(true);
		return thread;
	});
	private LauncherSettings settings;
	private final java.util.List<Runnable> accountListeners = new java.util.ArrayList<>();
	private final java.util.List<Runnable> profileListeners = new java.util.ArrayList<>();

	/**
	 * @param stage        janela principal
	 * @param hostServices para abrir links e pastas
	 */
	public LauncherContext(Stage stage, HostServices hostServices) {
		this.stage = stage;
		this.hostServices = hostServices;
		accounts.load();
		profiles.load();
		settings = LauncherSettings.load();
	}

	/** @return janela principal */
	public Stage stage() {
		return stage;
	}

	/** @return serviços do sistema (abrir links e arquivos) */
	public HostServices host() {
		return hostServices;
	}

	/** @return contas */
	public AccountStore accounts() {
		return accounts;
	}

	/** @return perfis */
	public ProfileStore profiles() {
		return profiles;
	}

	/** @return configurações do launcher */
	public LauncherSettings settings() {
		return settings;
	}

	/**
	 * Roda um trabalho fora da thread da interface.
	 *
	 * @param task trabalho
	 */
	public void runAsync(Runnable task) {
		background.submit(task);
	}

	/**
	 * Roda algo na thread da interface.
	 *
	 * @param task trabalho
	 */
	public static void ui(Runnable task) {
		Platform.runLater(task);
	}

	/** Salva contas, perfis e configurações (erros só são registrados). */
	public void saveAll() {
		try {
			accounts.save();
			profiles.save();
			settings.save();
		} catch (IOException e) {
			System.err.println("Não foi possível salvar: " + e.getMessage());
		}
	}

	/**
	 * @param listener chamado quando a conta selecionada muda
	 */
	public void onAccountChanged(Runnable listener) {
		accountListeners.add(listener);
	}

	/** Avisa as telas que a conta mudou. */
	public void accountChanged() {
		saveAll();
		accountListeners.forEach(Runnable::run);
	}

	/**
	 * @param listener chamado quando perfis são criados, editados ou removidos
	 */
	public void onProfilesChanged(Runnable listener) {
		profileListeners.add(listener);
	}

	/** Avisa as telas que os perfis mudaram. */
	public void profilesChanged() {
		saveAll();
		profileListeners.forEach(Runnable::run);
	}
}
