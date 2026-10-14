package com.vfxclient.launcher.ui;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vfxclient.launcher.LauncherPaths;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Galeria dos prints (F2) de todos os perfis, do mais novo para o mais antigo. Clique abre a
 * imagem no visualizador do Windows.
 */
public final class ScreenshotsView extends BorderPane {

	private static final int THUMB_W = 220;
	private static final int MAX = 120;

	private final LauncherContext ctx;
	private final FlowPane grid = new FlowPane(14, 14);
	private final Label count = new Label("Carregando...");

	/**
	 * @param ctx estado do launcher
	 */
	public ScreenshotsView(LauncherContext ctx) {
		this.ctx = ctx;
		getStyleClass().add("view");
		setPadding(new Insets(24, 28, 20, 28));

		Label title = new Label("Prints");
		title.getStyleClass().add("view-title");
		count.getStyleClass().add("muted");
		Button refresh = new Button("Atualizar");
		refresh.getStyleClass().add("ghost-button");
		refresh.setOnAction(e -> reload());
		Button folder = new Button("Abrir pasta");
		folder.getStyleClass().add("ghost-button");
		folder.setOnAction(e -> openFolder());
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox header = new HBox(10, new VBox(2, title, count), spacer, refresh, folder);
		header.setAlignment(Pos.CENTER_LEFT);

		ScrollPane scroll = new ScrollPane(grid);
		scroll.setFitToWidth(true);
		scroll.getStyleClass().add("news-scroll");
		grid.setPadding(new Insets(4));
		setTop(header);
		setCenter(scroll);
		BorderPane.setMargin(scroll, new Insets(16, 0, 0, 0));

		sceneProperty().addListener((obs, old, scene) -> {
			if (scene != null) {
				reload();
			}
		});
	}

	/** Lê de novo as pastas de prints (chamado ao abrir a aba). */
	public void reload() {
		ctx.runAsync(() -> {
			List<Path> shots = find();
			LauncherContext.ui(() -> show(shots));
		});
	}

	private static List<Path> find() {
		List<Path> result = new ArrayList<>();
		Path instances = LauncherPaths.instances();
		if (!Files.isDirectory(instances)) {
			return result;
		}
		try (DirectoryStream<Path> profiles = Files.newDirectoryStream(instances)) {
			for (Path profile : profiles) {
				Path dir = profile.resolve("screenshots");
				if (!Files.isDirectory(dir)) {
					continue;
				}
				try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.png")) {
					for (Path file : files) {
						if (!file.getFileName().toString().startsWith("vfx-teste")) {
							result.add(file);
						}
					}
				}
			}
		} catch (IOException ignored) {
			// Pasta inacessível: mostra o que deu para ler.
		}
		result.sort((a, b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()));
		return result.size() > MAX ? result.subList(0, MAX) : result;
	}

	private void show(List<Path> shots) {
		grid.getChildren().clear();
		count.setText(shots.isEmpty() ? "Nenhum print ainda. No jogo, aperte F2." : shots.size() + " print(s)");
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		for (Path shot : shots) {
			ImageView image = new ImageView(new Image(shot.toUri().toString(), THUMB_W, 0, true, true, true));
			image.setFitWidth(THUMB_W);
			image.setPreserveRatio(true);
			Label date = new Label(format.format(new Date(shot.toFile().lastModified())));
			date.getStyleClass().add("muted");
			VBox card = new VBox(6, image, date);
			card.getStyleClass().add("shot-card");
			card.setOnMouseClicked(e -> ctx.host().showDocument(shot.toUri().toString()));
			grid.getChildren().add(card);
		}
	}

	private void openFolder() {
		try {
			Path dir = ctx.profiles().selected().gameDir().resolve("screenshots");
			Files.createDirectories(dir);
			ctx.host().showDocument(dir.toUri().toString());
		} catch (IOException ignored) {
			// Sem permissão: nada a fazer.
		}
	}
}
