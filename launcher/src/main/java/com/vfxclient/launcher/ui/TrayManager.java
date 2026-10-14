package com.vfxclient.launcher.ui;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;

import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Ícone do VfxClient na bandeja do Windows (perto do relógio). Quando o jogo abre, o launcher
 * se esconde ali; clicando no ícone ele volta, e o menu tem "Abrir VfxClient" e "Sair".
 * Também é usado para as notificações do Windows (versão nova).
 */
public final class TrayManager {

	private static TrayIcon icon;
	private static Stage stage;

	private TrayManager() {
	}

	/**
	 * Prepara o ícone (uma vez). Precisa ser chamado antes de esconder a janela.
	 *
	 * @param mainStage janela do launcher
	 */
	public static synchronized void install(Stage mainStage) {
		stage = mainStage;
		if (icon != null || !SystemTray.isSupported()) {
			return;
		}
		// A janela pode ficar escondida na bandeja sem o programa fechar.
		Platform.setImplicitExit(false);
		java.awt.EventQueue.invokeLater(() -> {
			try {
				java.awt.Image image = javax.imageio.ImageIO.read(TrayManager.class.getResource("/com/vfxclient/launcher/logo-256.png"));
				PopupMenu menu = new PopupMenu();
				MenuItem open = new MenuItem("Abrir VfxClient");
				open.addActionListener(e -> restore());
				MenuItem quit = new MenuItem("Sair do launcher");
				quit.addActionListener(e -> quit());
				menu.add(open);
				menu.addSeparator();
				menu.add(quit);
				TrayIcon tray = new TrayIcon(image, "VfxClient", menu);
				tray.setImageAutoSize(true);
				tray.addActionListener(e -> restore());
				SystemTray.getSystemTray().add(tray);
				icon = tray;
			} catch (Exception ignored) {
				// Sem bandeja: o launcher só minimiza.
			}
		});
	}

	/** @return se a bandeja do Windows está disponível */
	public static boolean available() {
		return SystemTray.isSupported();
	}

	/** Esconde a janela; ela fica só na bandeja. */
	public static void hideToTray() {
		if (stage == null) {
			return;
		}
		if (!available()) {
			stage.setIconified(true);
			return;
		}
		stage.hide();
		message("VfxClient", "O launcher está aqui na bandeja. Clique para abrir.");
	}

	/** Mostra a janela de novo (clique no ícone ou "Abrir VfxClient"). */
	public static void restore() {
		Platform.runLater(() -> {
			if (stage == null) {
				return;
			}
			stage.show();
			stage.setIconified(false);
			stage.toFront();
		});
	}

	/** Fecha o launcher de vez (o jogo, se estiver aberto, continua). */
	public static void quit() {
		Platform.runLater(() -> {
			if (stage != null && stage.getOnCloseRequest() != null) {
				stage.getOnCloseRequest().handle(null);
			}
			Platform.exit();
			System.exit(0);
		});
	}

	/**
	 * Notificação do Windows.
	 *
	 * @param title título
	 * @param text  texto
	 */
	public static void message(String title, String text) {
		java.awt.EventQueue.invokeLater(() -> {
			if (icon != null) {
				icon.displayMessage(title, text, TrayIcon.MessageType.INFO);
			}
		});
	}
}
