package com.vfxclient.launcher.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.vfxclient.launcher.BuildInfo;
import com.vfxclient.launcher.auth.Account;
import com.vfxclient.launcher.game.GameLauncher;
import com.vfxclient.launcher.game.OptiFine;
import com.vfxclient.launcher.game.VersionManifest;
import com.vfxclient.launcher.optimization.Diagnostics;
import com.vfxclient.launcher.profiles.Profile;
import com.vfxclient.launcher.updater.Updater;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Tela inicial, minimalista: cartão principal com o botão JOGAR, versão e progresso; ao lado,
 * as novidades da versão. O resto (RAM, preset, Java) fica em Configurações.
 *
 * <p>O OptiFine vai junto com o VfxClient sem nenhuma opção na tela; só aparece um aviso
 * quando o arquivo do OptiFine ainda não está no computador.</p>
 */
public final class HomeView extends BorderPane {

	/** Página oficial do OptiFine 1.8.9 (o jogador baixa lá; o launcher importa sozinho da pasta Downloads). */
	private static final String OPTIFINE_PAGE = "https://optifine.net/adloadx?f=OptiFine_1.8.9_HD_U_M5.jar";

	private final LauncherContext ctx;
	private final ComboBox<Profile> profileBox = new ComboBox<>();
	private final Label profileName = new Label();
	private final Label profileSub = new Label();
	private final Label footer = new Label("Analisando o computador...");
	private final ProgressBar progressBar = new ProgressBar(0);
	private final Label status = new Label();
	private final Label detail = new Label();
	private final Button play = new Button();
	private final Button openLog = new Button("Ver log do jogo");
	private final HBox notice = new HBox(10);
	private final Button modsFolder = new Button("Abrir pasta de mods");
	private final HBox updateBanner = new HBox(12);
	private Path lastLog;
	private Process running;
	private Timeline optifineWatch;

	/**
	 * @param ctx estado do launcher
	 */
	public HomeView(LauncherContext ctx) {
		this.ctx = ctx;
		getStyleClass().add("view");
		setPadding(new Insets(24, 28, 20, 28));

		updateBanner.getStyleClass().add("update-banner");
		updateBanner.setVisible(false);
		updateBanner.setManaged(false);

		HBox main = new HBox(18, heroCard(), newsCard());
		HBox.setHgrow(main.getChildren().get(0), Priority.ALWAYS);
		VBox.setVgrow(main, Priority.ALWAYS);

		footer.getStyleClass().add("muted");
		VBox center = new VBox(14, updateBanner, main, footer);
		setCenter(center);

		ctx.onProfilesChanged(this::reloadProfiles);
		reloadProfiles();
		sceneProperty().addListener((obs, old, scene) -> {
			if (scene != null) {
				Motion.enter(main.getChildren().get(0), 0);
				Motion.enter(main.getChildren().get(1), 90);
			}
		});
		runDiagnostics();
		checkUpdates();
	}

	// ------------------------------------------------------------------
	// Montagem
	// ------------------------------------------------------------------

	/** Cartão principal: logo, perfil, botão JOGAR com o seletor de versão e o andamento. */
	private VBox heroCard() {
		ImageView logo = new ImageView();
		java.io.InputStream in = HomeView.class.getResourceAsStream("/com/vfxclient/launcher/logo-256.png");
		if (in != null) {
			logo.setImage(new Image(in));
		}
		logo.setFitWidth(76);
		logo.setPreserveRatio(true);

		profileName.getStyleClass().add("hero-title");
		profileSub.getStyleClass().add("muted");
		VBox titles = new VBox(2, profileName, profileSub);
		titles.setAlignment(Pos.CENTER);

		play.getStyleClass().add("play-button");
		play.setOnAction(e -> startGame());
		Motion.hoverGrow(play, 1.03);
		play.setMaxWidth(Double.MAX_VALUE);

		profileBox.getStyleClass().add("version-picker");
		// No botão aparece só a versão (o nome completo fica na lista aberta).
		profileBox.setButtonCell(new javafx.scene.control.ListCell<>() {
			@Override
			protected void updateItem(Profile item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					return;
				}
				String kind = switch (item.kind) {
					case FORGE -> "  Forge";
					case OPTIFINE -> "  OptiFine";
					case VANILLA -> "  oficial";
					default -> "";
				};
				setText(item.minecraftVersion + kind);
			}
		});
		profileBox.setOnAction(e -> {
			Profile selected = profileBox.getValue();
			if (selected != null && selected != ctx.profiles().selected()) {
				ctx.profiles().select(selected);
				ctx.saveAll();
				refreshProfile();
				runDiagnostics();
			}
		});
		Button newProfile = new Button("+");
		newProfile.getStyleClass().add("icon-button");
		newProfile.setOnAction(e -> createVanillaProfile());
		javafx.scene.control.Tooltip.install(newProfile, new javafx.scene.control.Tooltip("Outra versão do Minecraft"));

		HBox launchRow = new HBox(8, play, profileBox, newProfile);
		launchRow.setAlignment(Pos.CENTER);
		HBox.setHgrow(play, Priority.ALWAYS);
		launchRow.setMaxWidth(460);

		progressBar.setMaxWidth(460);
		progressBar.getStyleClass().add("launch-progress");
		progressBar.setVisible(false);
		status.getStyleClass().add("status");
		detail.getStyleClass().add("muted");
		openLog.getStyleClass().add("link-button");
		openLog.setVisible(false);
		openLog.setManaged(false);
		openLog.setOnAction(e -> {
			if (lastLog != null) {
				ctx.host().showDocument(lastLog.toUri().toString());
			}
		});
		notice.getStyleClass().add("notice");
		notice.setAlignment(Pos.CENTER);
		notice.setMaxWidth(460);
		notice.setVisible(false);
		notice.setManaged(false);

		modsFolder.getStyleClass().add("link-button");
		modsFolder.setOnAction(e -> openMods());

		VBox box = new VBox(14, logo, titles, launchRow, progressBar, status, detail, openLog, notice, modsFolder);
		box.setAlignment(Pos.CENTER);
		box.getStyleClass().add("hero-card");
		box.setPadding(new Insets(30, 24, 26, 24));
		return box;
	}

	/** Painel de novidades (lista em resources/changelog.txt). */
	private VBox newsCard() {
		Label title = new Label("Novidades");
		title.getStyleClass().add("news-title");
		Region underline = new Region();
		underline.getStyleClass().add("news-underline");
		VBox items = new VBox(8);
		for (String line : changelog()) {
			Label label = new Label(line.startsWith("#") ? line.substring(1).trim() : "•  " + line);
			label.setWrapText(true);
			label.getStyleClass().add(line.startsWith("#") ? "news-version" : "news-item");
			items.getChildren().add(label);
			Motion.enter(label, 120 + items.getChildren().size() * 35);
		}
		javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(items);
		scroll.setFitToWidth(true);
		scroll.getStyleClass().add("news-scroll");
		VBox.setVgrow(scroll, Priority.ALWAYS);
		VBox box = new VBox(10, new VBox(4, title, underline), scroll);
		box.getStyleClass().add("news-card");
		box.setPrefWidth(280);
		box.setMinWidth(240);
		return box;
	}

	private static List<String> changelog() {
		List<String> lines = new ArrayList<>();
		try (java.io.InputStream in = HomeView.class.getResourceAsStream("/com/vfxclient/launcher/changelog.txt")) {
			if (in != null) {
				for (String line : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
					if (!line.isBlank()) {
						lines.add(line.trim());
					}
				}
			}
		} catch (IOException ignored) {
			// Sem novidades: o painel fica vazio.
		}
		return lines;
	}

	// ------------------------------------------------------------------
	// Estado do perfil
	// ------------------------------------------------------------------

	/** Recarrega a lista de perfis (após criar/editar/remover). */
	public void reloadProfiles() {
		profileBox.setItems(FXCollections.observableArrayList(ctx.profiles().all()));
		profileBox.setValue(ctx.profiles().selected());
		refreshProfile();
	}

	private void refreshProfile() {
		Profile profile = ctx.profiles().selected();
		profileName.setText(profile.isVfx() ? "VfxClient" : profile.name);
		String sub = switch (profile.kind) {
			case VFX -> "Versão " + BuildInfo.load().clientVersion() + (profile.optifineWithVfx ? "  ·  com OptiFine" : "");
			case FORGE -> "Forge" + (profile.optifineWithForge ? " + OptiFine" : "") + "  ·  seus mods";
			case OPTIFINE -> "Só OptiFine";
			default -> "Minecraft oficial";
		};
		profileSub.setText(sub);
		if (running == null || !running.isAlive()) {
			play.setText("JOGAR " + profile.minecraftVersion);
			status.setText("");
			detail.setText("");
		}
		boolean forge = profile.kind == Profile.Kind.FORGE;
		modsFolder.setVisible(forge);
		modsFolder.setManaged(forge);
		checkOptiFine(profile);
	}

	private void openMods() {
		try {
			Path mods = ctx.profiles().selected().gameDir().resolve("mods");
			Files.createDirectories(mods);
			ctx.host().showDocument(mods.toUri().toString());
		} catch (IOException ex) {
			detail.setText(ex.getMessage());
		}
	}

	/**
	 * Perfil com OptiFine e sem o arquivo no PC: mostra um aviso com um botão que abre a página
	 * oficial; o launcher fica de olho na pasta Downloads e importa sozinho quando o arquivo chegar.
	 */
	private void checkOptiFine(Profile profile) {
		boolean needs = (profile.kind == Profile.Kind.VFX && profile.optifineWithVfx)
				|| profile.kind == Profile.Kind.OPTIFINE
				|| (profile.kind == Profile.Kind.FORGE && profile.optifineWithForge);
		if (!needs) {
			showNotice(false);
			return;
		}
		ctx.runAsync(() -> {
			Path jar = OptiFine.installedJar() != null ? OptiFine.installedJar() : OptiFine.autoDetect();
			LauncherContext.ui(() -> showNotice(jar == null));
		});
	}

	private void showNotice(boolean show) {
		if (show && notice.getChildren().isEmpty()) {
			Label text = new Label("Falta o OptiFine neste PC.");
			text.getStyleClass().add("warning");
			Button download = new Button("Baixar OptiFine");
			download.getStyleClass().add("primary-button");
			download.setOnAction(e -> {
				ctx.host().showDocument(OPTIFINE_PAGE);
				watchForOptiFine(text);
			});
			Button select = new Button("Já tenho");
			select.getStyleClass().add("link-button");
			select.setOnAction(e -> selectOptiFine());
			notice.getChildren().setAll(text, download, select);
		}
		notice.setVisible(show);
		notice.setManaged(show);
	}

	/** Depois de abrir a página oficial, procura o arquivo baixado a cada 2 s (por até 10 minutos). */
	private void watchForOptiFine(Label text) {
		if (optifineWatch != null) {
			optifineWatch.stop();
		}
		text.setText("Clique em Download na página que abriu...");
		optifineWatch = new Timeline(new KeyFrame(Duration.seconds(2), e -> ctx.runAsync(() -> {
			Path found = OptiFine.autoDetect();
			if (found != null) {
				LauncherContext.ui(() -> {
					optifineWatch.stop();
					showNotice(false);
					status.setText("OptiFine pronto!");
					detail.setText(found.getFileName().toString());
				});
			}
		})));
		optifineWatch.setCycleCount(300);
		optifineWatch.play();
	}

	private void selectOptiFine() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Selecionar o OptiFine 1.8.9");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OptiFine (.jar)", "*.jar"));
		java.io.File file = chooser.showOpenDialog(ctx.stage());
		if (file == null) {
			return;
		}
		try {
			OptiFine.importJar(file.toPath());
			showNotice(false);
			status.setText("OptiFine pronto!");
			detail.setText(file.getName());
		} catch (IOException ex) {
			status.setText("Arquivo recusado");
			detail.setText(ex.getMessage());
		}
	}

	private void runDiagnostics() {
		Profile profile = ctx.profiles().selected();
		ctx.runAsync(() -> {
			Diagnostics.Report report = Diagnostics.run(profile);
			LauncherContext.ui(() -> {
				String text = "Preset para este PC: " + report.presetLabel() + "  ·  " + profile.ramMb + " MB de RAM";
				if (!report.warnings().isEmpty()) {
					text += "  ·  " + report.warnings().get(0);
				}
				footer.setText(text);
			});
		});
	}

	private void checkUpdates() {
		if (!ctx.settings().checkUpdates) {
			return;
		}
		ctx.runAsync(() -> {
			try {
				Updater.Update update = Updater.check();
				if (update == null) {
					return;
				}
				LauncherContext.ui(() -> {
					Label text = new Label("Nova versão disponível: " + update.version());
					Button install = new Button("Atualizar agora");
					install.getStyleClass().add("primary-button");
					install.setOnAction(e -> {
						install.setDisable(true);
						install.setText("Baixando...");
						ctx.runAsync(() -> {
							try {
								Updater.downloadAndRun(update);
								LauncherContext.ui(() -> ((Stage) getScene().getWindow()).close());
							} catch (IOException ex) {
								LauncherContext.ui(() -> {
									install.setText("Falhou: " + ex.getMessage());
									install.setDisable(false);
								});
							}
						});
					});
					Region spacer = new Region();
					HBox.setHgrow(spacer, Priority.ALWAYS);
					updateBanner.getChildren().setAll(text, spacer, install);
					updateBanner.setVisible(true);
					updateBanner.setManaged(true);
				});
			} catch (IOException ignored) {
				// Sem internet ou repositório privado: segue sem aviso.
			}
		});
	}

	private void createVanillaProfile() {
		ctx.runAsync(() -> {
			List<String> versions = new ArrayList<>(VersionManifest.releases());
			LauncherContext.ui(() -> {
				if (versions.isEmpty()) {
					status.setText("Sem internet: não foi possível listar as versões.");
					return;
				}
				ChoiceDialog<String> dialog = new ChoiceDialog<>(versions.get(0), versions);
				dialog.setTitle("Nova versão");
				dialog.setHeaderText("Minecraft oficial (sem mods)");
				dialog.setContentText("Versão:");
				dialog.initOwner(ctx.stage());
				dialog.showAndWait().ifPresent(version -> {
					ctx.profiles().createVanilla(version);
					ctx.profilesChanged();
				});
			});
		});
	}

	// ------------------------------------------------------------------
	// Jogo
	// ------------------------------------------------------------------

	private void startGame() {
		if (running != null && running.isAlive()) {
			return;
		}
		Profile profile = ctx.profiles().selected();
		Account account = ctx.accounts().selected();
		play.setDisable(true);
		setLogVisible(false);
		progressBar.setVisible(true);
		progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
		status.setText("Preparando...");
		detail.setText("");
		ctx.runAsync(() -> {
			try {
				GameLauncher.Session session = GameLauncher.launch(profile, account, ctx.accounts(), ctx.settings(),
						(stage, fraction, text) -> LauncherContext.ui(() -> {
							status.setText(stage);
							detail.setText(text == null ? "" : text);
							Motion.progress(progressBar, fraction);
						}));
				running = session.process();
				lastLog = session.log();
				LauncherContext.ui(() -> {
					status.setText("Jogo aberto");
					detail.setText("Feche o Minecraft para voltar ao launcher.");
					progressBar.setVisible(false);
					play.setText("JOGANDO");
					afterLaunch();
				});
				int code = session.process().waitFor();
				LauncherContext.ui(() -> gameClosed(code));
			} catch (IOException | RuntimeException e) {
				LauncherContext.ui(() -> fail(e.getMessage()));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
	}

	private void afterLaunch() {
		switch (ctx.settings().onLaunch) {
			case "close":
				ctx.stage().close();
				break;
			case "minimize":
				ctx.stage().setIconified(true);
				break;
			default:
				break;
		}
	}

	private void gameClosed(int code) {
		ctx.stage().setIconified(false);
		play.setDisable(false);
		progressBar.setVisible(false);
		refreshProfile();
		if (code != 0) {
			status.setText("O jogo fechou com erro (código " + code + ").");
			detail.setText("Veja o log para entender o que aconteceu.");
			setLogVisible(lastLog != null && Files.exists(lastLog));
		}
	}

	private void fail(String message) {
		play.setDisable(false);
		progressBar.setVisible(false);
		play.setText("JOGAR " + ctx.profiles().selected().minecraftVersion);
		status.setText("Não foi possível iniciar");
		detail.setText(message == null ? "Erro desconhecido" : message);
	}

	private void setLogVisible(boolean visible) {
		openLog.setVisible(visible);
		openLog.setManaged(visible);
	}
}
