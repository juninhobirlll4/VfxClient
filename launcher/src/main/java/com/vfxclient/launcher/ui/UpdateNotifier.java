package com.vfxclient.launcher.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.vfxclient.launcher.BuildInfo;
import com.vfxclient.launcher.updater.Updater;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Aviso de versão nova: confere o GitHub ao abrir e a cada 30 minutos. Achou versão nova,
 * abre uma janelinha com as novidades e os botões "Atualizar agora" e "Mais tarde"; se o
 * launcher estiver minimizado (por exemplo, durante o jogo), mostra também uma notificação
 * do Windows. "Atualizar agora" baixa, instala em silêncio e reabre o launcher.
 */
public final class UpdateNotifier {

	private static final Duration INTERVAL = Duration.minutes(30);

	private final LauncherContext ctx;
	private final Consumer<Updater.Update> onLater;
	private String dismissed;
	private boolean showing;

	/**
	 * @param ctx     estado do launcher
	 * @param onLater chamado quando o jogador escolhe "Mais tarde" (para deixar um lembrete na tela)
	 */
	public UpdateNotifier(LauncherContext ctx, Consumer<Updater.Update> onLater) {
		this.ctx = ctx;
		this.onLater = onLater;
	}

	/** Confere agora e depois a cada 30 minutos. */
	public void start() {
		check();
		Timeline timer = new Timeline(new KeyFrame(INTERVAL, e -> check()));
		timer.setCycleCount(Timeline.INDEFINITE);
		timer.play();
	}

	private void check() {
		if (!ctx.settings().checkUpdates) {
			return;
		}
		ctx.runAsync(() -> {
			try {
				// Modo de teste: -Dvfx.fakeUpdate=9.9.9 simula uma versão nova (sem baixar nada de verdade).
				String fake = System.getProperty("vfx.fakeUpdate");
				Updater.Update update = fake != null
						? new Updater.Update(fake, "https://example.invalid/teste.exe", 0, "* Tela inicial nova\n* Launcher mais simples\n**Full Changelog**: x")
						: Updater.check();
				if (update != null && !update.version().equals(dismissed)) {
					LauncherContext.ui(() -> notify(update));
				}
			} catch (IOException ignored) {
				// Sem internet ou GitHub fora do ar: tenta de novo na próxima vez.
			}
		});
	}

	private void notify(Updater.Update update) {
		if (showing) {
			return;
		}
		if (System.getProperty("vfx.snapshot") == null && (!ctx.stage().isShowing() || ctx.stage().isIconified() || !ctx.stage().isFocused())) {
			TrayManager.message("VfxClient " + update.version() + " disponível", "Abra o launcher para atualizar agora ou mais tarde.");
		}
		show(update);
	}

	/** Janela de aviso, no visual do launcher. */
	public void show(Updater.Update update) {
		showing = true;
		Stage dialog = new Stage(StageStyle.TRANSPARENT);
		dialog.initOwner(ctx.stage());
		dialog.initModality(Modality.WINDOW_MODAL);

		ImageView logo = new ImageView();
		InputStream in = UpdateNotifier.class.getResourceAsStream("/com/vfxclient/launcher/logo-256.png");
		if (in != null) {
			logo.setImage(new Image(in));
		}
		logo.setFitWidth(52);
		logo.setPreserveRatio(true);
		Label title = new Label("Versão " + update.version() + " disponível");
		title.getStyleClass().add("hero-title");
		title.setStyle("-fx-font-size: 20px;");
		Label current = new Label("Você está na " + BuildInfo.load().clientVersion());
		current.getStyleClass().add("muted");

		Label notes = new Label(cleanNotes(update.notes()));
		notes.setWrapText(true);
		notes.getStyleClass().add("news-item");
		ScrollPane notesScroll = new ScrollPane(notes);
		notesScroll.setFitToWidth(true);
		notesScroll.setPrefHeight(120);
		notesScroll.getStyleClass().add("news-scroll");

		ProgressBar bar = new ProgressBar(0);
		bar.setMaxWidth(Double.MAX_VALUE);
		bar.getStyleClass().add("launch-progress");
		bar.setVisible(false);
		Label status = new Label();
		status.getStyleClass().add("muted");

		Button later = new Button("Mais tarde");
		later.getStyleClass().add("ghost-button");
		Button now = new Button("Atualizar agora");
		now.getStyleClass().add("primary-button");
		Motion.hoverGrow(now, 1.04);
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox buttons = new HBox(10, spacer, later, now);
		buttons.setAlignment(Pos.CENTER_RIGHT);

		later.setOnAction(e -> {
			dismissed = update.version();
			showing = false;
			dialog.close();
			onLater.accept(update);
		});
		now.setOnAction(e -> {
			now.setDisable(true);
			later.setDisable(true);
			bar.setVisible(true);
			status.setText("Baixando...");
			ctx.runAsync(() -> {
				try {
					Path installer = Updater.download(update, (stage, fraction, detail) -> LauncherContext.ui(() -> {
						Motion.progress(bar, fraction);
						status.setText(fraction < 0 ? "Baixando..." : "Baixando... " + Math.round(fraction * 100) + "%");
					}));
					LauncherContext.ui(() -> status.setText("Instalando... o launcher abre de novo sozinho."));
					ctx.saveAll();
					Updater.run(installer);
					// Fecha de vez (inclusive a bandeja do Windows) para o instalador trocar os arquivos.
					System.exit(0);
				} catch (IOException ex) {
					LauncherContext.ui(() -> {
						status.setText("Não deu para baixar agora. Tente de novo mais tarde.");
						now.setDisable(false);
						later.setDisable(false);
					});
				}
			});
		});

		VBox head = new VBox(2, title, current);
		HBox top = new HBox(14, logo, head);
		top.setAlignment(Pos.CENTER_LEFT);
		VBox box = new VBox(14, top, notesScroll, bar, status, buttons);
		box.setPadding(new Insets(22));
		box.setPrefWidth(420);
		box.getStyleClass().add("update-dialog");

		Scene scene = new Scene(box);
		scene.setFill(null);
		scene.getStylesheets().addAll(ctx.stage().getScene().getStylesheets());
		dialog.setScene(scene);
		String snapshot = System.getProperty("vfx.snapshot");
		if (snapshot != null) {
			// Modo de prints: salva a janela sem mostrá-la.
			box.applyCss();
			box.layout();
			saveSnapshot(scene, java.nio.file.Path.of(snapshot, "atualizacao.png"));
			return;
		}
		dialog.setOnShown(e -> {
			dialog.setX(ctx.stage().getX() + (ctx.stage().getWidth() - dialog.getWidth()) / 2);
			dialog.setY(ctx.stage().getY() + (ctx.stage().getHeight() - dialog.getHeight()) / 2);
			Motion.enter(box, 0);
		});
		dialog.show();
	}

	private static void saveSnapshot(Scene scene, Path file) {
		javafx.scene.image.WritableImage image = scene.getRoot().snapshot(null, null);
		java.awt.image.BufferedImage out = new java.awt.image.BufferedImage((int) image.getWidth(), (int) image.getHeight(),
				java.awt.image.BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < out.getWidth(); x++) {
			for (int y = 0; y < out.getHeight(); y++) {
				out.setRGB(x, y, image.getPixelReader().getArgb(x, y));
			}
		}
		try {
			java.nio.file.Files.createDirectories(file.getParent());
			javax.imageio.ImageIO.write(out, "png", file.toFile());
		} catch (IOException ignored) {
			// Só para revisão visual.
		}
	}

	/** Tira a parte técnica das notas geradas pelo GitHub (links de commits, "Full Changelog"). */
	private static String cleanNotes(String notes) {
		if (notes == null || notes.isBlank()) {
			return "Melhorias e correções.";
		}
		StringBuilder out = new StringBuilder();
		for (String line : notes.split("\\R")) {
			String l = line.trim();
			if (l.isEmpty() || l.startsWith("**Full Changelog") || l.startsWith("## What")) {
				continue;
			}
			l = l.replaceAll("\\s+by @\\S+ in https?://\\S+", "").replaceAll("^[*-]\\s*", "•  ");
			out.append(l).append('\n');
		}
		return out.length() == 0 ? "Melhorias e correções." : out.toString().trim();
	}
}
