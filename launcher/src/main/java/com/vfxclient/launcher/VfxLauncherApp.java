package com.vfxclient.launcher;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.vfxclient.launcher.auth.Account;
import com.vfxclient.launcher.game.GameLauncher;
import com.vfxclient.launcher.ui.AccountView;
import com.vfxclient.launcher.ui.HomeView;
import com.vfxclient.launcher.ui.LauncherContext;
import com.vfxclient.launcher.ui.ScreenshotsView;
import com.vfxclient.launcher.ui.SettingsView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Janela principal do launcher: barra lateral (Início, Conta, Configurações) e a tela atual.
 */
public final class VfxLauncherApp extends Application {

	private static final String RESOURCE_BASE = "/com/vfxclient/launcher/";

	private LauncherContext ctx;
	private final StackPane content = new StackPane();
	private final VBox nav = new VBox(6);
	private final Label accountName = new Label();
	private final ImageView accountHead = new ImageView();
	private HomeView home;
	private AccountView account;
	private SettingsView settings;
	private ScreenshotsView screenshots;

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
		ctx = new LauncherContext(stage, getHostServices());
		home = new HomeView(ctx);
		account = new AccountView(ctx);
		settings = new SettingsView(ctx);
		screenshots = new ScreenshotsView(ctx);

		BorderPane root = new BorderPane();
		root.getStyleClass().add("root-pane");
		root.setLeft(sidebar());
		root.setCenter(content);
		show(home, 0);

		ctx.onAccountChanged(this::refreshAccountChip);
		refreshAccountChip();
		preinstallLoaders();

		Scene scene = new Scene(root, 1040, 640);
		scene.getStylesheets().add(VfxLauncherApp.class.getResource(RESOURCE_BASE + "launcher.css").toExternalForm());
		stage.setTitle("VfxClient");
		Image icon = loadImage("logo-256.png");
		if (icon != null) {
			stage.getIcons().add(icon);
		}
		stage.setMinWidth(900);
		stage.setMinHeight(580);
		stage.setScene(scene);
		stage.setOnCloseRequest(e -> ctx.saveAll());

		String snapshotDir = System.getProperty("vfx.snapshot");
		if (snapshotDir != null) {
			// Modo de revisão visual: desenha as telas sem mostrar a janela (não rouba o foco).
			snapshots(scene, Path.of(snapshotDir));
			return;
		}
		stage.show();
	}

	/** Modo de desenvolvimento: salva um PNG de cada tela e fecha (usado para revisar o visual). */
	private void snapshots(Scene scene, Path dir) {
		javafx.animation.PauseTransition wait = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
		wait.setOnFinished(e -> {
			Node[] views = {home, account, screenshots, settings};
			String[] names = {"inicio", "conta", "prints", "configuracoes"};
			try {
				Files.createDirectories(dir);
				for (int i = 0; i < views.length; i++) {
					show(views[i], i);
					scene.getRoot().applyCss();
					scene.getRoot().layout();
					WritableImage image = scene.snapshot(null);
					java.awt.image.BufferedImage out = new java.awt.image.BufferedImage((int) image.getWidth(), (int) image.getHeight(),
							java.awt.image.BufferedImage.TYPE_INT_ARGB);
					for (int x = 0; x < out.getWidth(); x++) {
						for (int y = 0; y < out.getHeight(); y++) {
							out.setRGB(x, y, image.getPixelReader().getArgb(x, y));
						}
					}
					javax.imageio.ImageIO.write(out, "png", dir.resolve(names[i] + ".png").toFile());
				}
			} catch (java.io.IOException ex) {
				System.err.println("Snapshot falhou: " + ex.getMessage());
			}
			javafx.application.Platform.exit();
		});
		wait.play();
	}

	/**
	 * Deixa os loaders prontos assim que o launcher abre: o Forge vem embutido e o OptiFine
	 * é procurado no computador (o launcher não pode distribuí-lo). Roda em segundo plano.
	 */
	private void preinstallLoaders() {
		ctx.runAsync(() -> {
			com.vfxclient.launcher.game.OptiFine.autoDetect();
			try {
				com.vfxclient.launcher.game.Forge.install("1.8.9", com.vfxclient.launcher.game.Progress.NONE);
			} catch (java.io.IOException e) {
				System.err.println("Forge não pré-instalado: " + e.getMessage());
			}
			LauncherContext.ui(home::reloadProfiles);
		});
	}

	private VBox sidebar() {
		ImageView logo = new ImageView(loadImage("logo-256.png"));
		logo.setFitWidth(34);
		logo.setPreserveRatio(true);
		Label brand = new Label("VfxClient");
		brand.getStyleClass().add("brand");
		HBox header = new HBox(10, logo, brand);
		header.setAlignment(Pos.CENTER_LEFT);
		header.setPadding(new Insets(0, 0, 18, 4));

		String[] labels = {"Início", "Conta", "Prints", "Configurações"};
		Node[] views = {home, account, screenshots, settings};
		for (int i = 0; i < labels.length; i++) {
			int index = i;
			Button button = new Button(labels[i]);
			button.getStyleClass().add("nav-button");
			button.setMaxWidth(Double.MAX_VALUE);
			button.setOnAction(e -> show(views[index], index));
			nav.getChildren().add(button);
		}

		accountHead.setFitWidth(28);
		accountHead.setFitHeight(28);
		accountName.getStyleClass().add("account-chip-name");
		HBox chip = new HBox(10, accountHead, accountName);
		chip.setAlignment(Pos.CENTER_LEFT);
		chip.getStyleClass().add("account-chip");
		chip.setOnMouseClicked(e -> show(account, 1));

		Label version = new Label("Launcher " + BuildInfo.load().clientVersion() + " · MC " + BuildInfo.load().minecraftVersion());
		version.getStyleClass().add("muted");

		Region spacer = new Region();
		VBox.setVgrow(spacer, Priority.ALWAYS);
		VBox sidebar = new VBox(header, nav, spacer, chip, version);
		sidebar.setSpacing(8);
		sidebar.getStyleClass().add("sidebar");
		sidebar.setPadding(new Insets(22, 16, 18, 16));
		sidebar.setPrefWidth(210);
		return sidebar;
	}

	private void show(Node view, int navIndex) {
		boolean changed = content.getChildren().isEmpty() || content.getChildren().get(0) != view;
		content.getChildren().setAll(view);
		if (changed && com.vfxclient.launcher.ui.Motion.ENABLED) {
			// Troca de aba suave: aparece e desliza um pouco para cima.
			view.setOpacity(0);
			view.setTranslateY(10);
			javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(220), view);
			fade.setToValue(1);
			javafx.animation.TranslateTransition move = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(220), view);
			move.setToY(0);
			move.setInterpolator(javafx.animation.Interpolator.SPLINE(0.2, 0.8, 0.2, 1.0));
			new javafx.animation.ParallelTransition(fade, move).play();
		}
		for (int i = 0; i < nav.getChildren().size(); i++) {
			nav.getChildren().get(i).getStyleClass().remove("nav-selected");
			if (i == navIndex) {
				nav.getChildren().get(i).getStyleClass().add("nav-selected");
			}
		}
	}

	/** Mostra a conta escolhida (rosto da skin + nick) no rodapé da barra lateral. */
	private void refreshAccountChip() {
		Account selected = ctx.accounts().selected();
		accountName.setText(selected == null ? "Sem conta" : selected.username);
		Image face = null;
		if (selected != null) {
			Path skin = selected.type == Account.Type.OFFLINE
					? GameLauncher.localSkin(selected)
					: LauncherPaths.launcher().resolve("skins").resolve(selected.uuid + "-oficial.png");
			if (Files.exists(skin)) {
				face = com.vfxclient.launcher.ui.Faces.face(new Image(skin.toUri().toString()), 4);
			}
		}
		accountHead.setImage(face != null ? face : loadImage("logo-256.png"));
	}

	private static Image loadImage(String name) {
		InputStream in = VfxLauncherApp.class.getResourceAsStream(RESOURCE_BASE + name);
		return in == null ? null : new Image(in);
	}
}
