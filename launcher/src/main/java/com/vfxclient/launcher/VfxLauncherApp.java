package com.vfxclient.launcher;

import java.io.InputStream;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Janela principal do launcher.
 *
 * <p>No M0 é só o esqueleto visual: logo, versões e o botão Jogar (desativado).
 * Login, perfis de mods, download e auto-update entram nas próximas versões
 * (ver roadmap no README).</p>
 */
public final class VfxLauncherApp extends Application {

	private static final String RESOURCE_BASE = "/com/vfxclient/launcher/";

	/**
	 * Sobe o runtime do JavaFX e abre a janela.
	 *
	 * @param args argumentos de linha de comando
	 */
	static void launchApp(String[] args) {
		launch(VfxLauncherApp.class, args);
	}

	@Override
	public void start(Stage stage) {
		BuildInfo info = BuildInfo.load();

		BorderPane root = new BorderPane();
		root.getStyleClass().add("root-pane");
		root.setTop(buildHeader());
		root.setCenter(buildCenter());
		root.setBottom(buildFooter(info));

		Scene scene = new Scene(root, 960, 600);
		scene.getStylesheets().add(
				VfxLauncherApp.class.getResource(RESOURCE_BASE + "launcher.css").toExternalForm());

		stage.setTitle("VfxClient");
		Image icon = loadImage("logo-256.png");
		if (icon != null) {
			stage.getIcons().add(icon);
		}
		stage.setMinWidth(800);
		stage.setMinHeight(500);
		stage.setScene(scene);
		stage.show();
	}

	/** Faixa superior com o degradê roxo → branco e o nome do client. */
	private HBox buildHeader() {
		Label title = new Label("VfxClient");
		title.getStyleClass().add("brand-title");

		HBox header = new HBox(12, title);
		header.getStyleClass().add("header");
		header.setAlignment(Pos.CENTER_LEFT);
		header.setPadding(new Insets(16, 24, 16, 24));
		return header;
	}

	/** Área central com a logo. */
	private VBox buildCenter() {
		VBox center = new VBox(16);
		center.setAlignment(Pos.CENTER);

		Image logo = loadImage("logo-256.png");
		if (logo != null) {
			ImageView view = new ImageView(logo);
			view.setFitWidth(160);
			view.setPreserveRatio(true);
			view.setSmooth(true);
			center.getChildren().add(view);
		}

		Label tagline = new Label("Feito por nós, para nós.");
		tagline.getStyleClass().add("tagline");
		center.getChildren().add(tagline);
		return center;
	}

	/** Rodapé com versões, status de download e o botão Jogar. */
	private HBox buildFooter(BuildInfo info) {
		Label versions = new Label("Client " + info.clientVersion() + "  ·  Minecraft " + info.minecraftVersion());
		versions.getStyleClass().add("muted");

		Label status = new Label("Pronto");
		status.getStyleClass().add("status");

		VBox left = new VBox(4, versions, status);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		Button play = new Button("JOGAR");
		play.getStyleClass().add("play-button");
		// Download e inicialização do jogo chegam na v0.4.
		play.setDisable(true);

		HBox footer = new HBox(16, left, spacer, play);
		footer.getStyleClass().add("footer");
		footer.setAlignment(Pos.CENTER_LEFT);
		footer.setPadding(new Insets(16, 24, 16, 24));
		return footer;
	}

	/**
	 * Carrega uma imagem dos recursos do launcher.
	 *
	 * @param name nome do arquivo dentro de {@code com/vfxclient/launcher/}
	 * @return a imagem, ou {@code null} se o arquivo não existir
	 */
	private static Image loadImage(String name) {
		InputStream in = VfxLauncherApp.class.getResourceAsStream(RESOURCE_BASE + name);
		return in == null ? null : new Image(in);
	}
}
