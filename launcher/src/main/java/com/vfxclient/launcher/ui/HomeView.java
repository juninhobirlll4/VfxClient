package com.vfxclient.launcher.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.vfxclient.launcher.BuildInfo;
import com.vfxclient.launcher.auth.Account;
import com.vfxclient.launcher.game.GameLauncher;
import com.vfxclient.launcher.game.VersionManifest;
import com.vfxclient.launcher.optimization.Diagnostics;
import com.vfxclient.launcher.profiles.Profile;
import com.vfxclient.launcher.updater.Updater;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import com.vfxclient.launcher.game.OptiFine;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Tela inicial: escolha do perfil/versão, botão Iniciar, barra de progresso, status do jogo,
 * diagnóstico do PC e aviso de atualização.
 */
public final class HomeView extends BorderPane {

	private final LauncherContext ctx;
	private final ComboBox<Profile> profileBox = new ComboBox<>();
	private final Label profileInfo = new Label();
	private final Label diagnosticSummary = new Label("Analisando o computador...");
	private final Label diagnosticPreset = new Label();
	private final VBox warnings = new VBox(4);
	private final ProgressBar progressBar = new ProgressBar(0);
	private final Label status = new Label("Pronto para jogar");
	private final Label detail = new Label();
	private final Button play = new Button("INICIAR");
	private final Button openLog = new Button("Ver log do jogo");
	private final HBox updateBanner = new HBox(12);
	private final ToggleGroup loaderGroup = new ToggleGroup();
	private final HBox loaderButtons = new HBox(6);
	private final Label loaderHint = new Label();
	private final VBox optifineBox = new VBox(10);
	private final Label optifineStatus = new Label();
	private final CheckBox forgeWithOptifine = new CheckBox("Usar o OptiFine junto com o Forge (como mod)");
	private final CheckBox vfxWithOptifine = new CheckBox("OptiFine junto com o VfxClient (recomendado)");
	private final HBox forgeTools = new HBox(12);
	private HBox loaderRow;
	private Path lastLog;
	private Process running;

	/**
	 * @param ctx estado do launcher
	 */
	public HomeView(LauncherContext ctx) {
		this.ctx = ctx;
		getStyleClass().add("view");
		setPadding(new Insets(28, 32, 24, 32));

		Label title = new Label("Pronto para jogar");
		title.getStyleClass().add("view-title");
		Label subtitle = new Label("Escolha o perfil e clique em Iniciar.");
		subtitle.getStyleClass().add("muted");

		profileBox.getStyleClass().add("big-combo");
		profileBox.setMaxWidth(Double.MAX_VALUE);
		profileBox.setOnAction(e -> {
			Profile selected = profileBox.getValue();
			if (selected != null) {
				ctx.profiles().select(selected);
				ctx.saveAll();
				refreshProfileInfo();
				runDiagnostics();
			}
		});
		Button newProfile = new Button("+ Outra versão");
		newProfile.getStyleClass().add("ghost-button");
		newProfile.setOnAction(e -> createVanillaProfile());
		HBox profileRow = new HBox(10, profileBox, newProfile);
		HBox.setHgrow(profileBox, Priority.ALWAYS);
		profileRow.setAlignment(Pos.CENTER_LEFT);

		VBox profileCard = card("Perfil e versão", profileRow, buildLoaderRow(), profileInfo, optifineBox);
		VBox diagnosticsCard = card("Diagnóstico do computador", diagnosticSummary, diagnosticPreset, warnings);

		updateBanner.getStyleClass().add("update-banner");
		updateBanner.setVisible(false);
		updateBanner.setManaged(false);

		VBox center = new VBox(16, new VBox(4, title, subtitle), updateBanner, profileCard, diagnosticsCard);
		setCenter(center);

		progressBar.setMaxWidth(Double.MAX_VALUE);
		progressBar.getStyleClass().add("launch-progress");
		status.getStyleClass().add("status");
		detail.getStyleClass().add("muted");
		play.getStyleClass().add("play-button");
		play.setOnAction(e -> startGame());
		openLog.getStyleClass().add("ghost-button");
		openLog.setVisible(false);
		openLog.setOnAction(e -> {
			if (lastLog != null) {
				ctx.host().showDocument(lastLog.toUri().toString());
			}
		});
		VBox statusBox = new VBox(6, new HBox(10, status, openLog), detail, progressBar);
		HBox.setHgrow(statusBox, Priority.ALWAYS);
		HBox bottom = new HBox(20, statusBox, play);
		bottom.setAlignment(Pos.BOTTOM_LEFT);
		bottom.getStyleClass().add("launch-bar");
		setBottom(bottom);
		BorderPane.setMargin(bottom, new Insets(18, 0, 0, 0));

		ctx.onProfilesChanged(this::reloadProfiles);
		reloadProfiles();
		runDiagnostics();
		checkUpdates();
	}

	// ------------------------------------------------------------------
	// Loaders do 1.8.9 (VfxClient / OptiFine / Forge)
	// ------------------------------------------------------------------

	private HBox buildLoaderRow() {
		String[] names = {"VfxClient", "OptiFine", "Forge"};
		Profile.Kind[] kinds = {Profile.Kind.VFX, Profile.Kind.OPTIFINE, Profile.Kind.FORGE};
		for (int i = 0; i < names.length; i++) {
			Profile.Kind kind = kinds[i];
			ToggleButton button = new ToggleButton(names[i]);
			button.setToggleGroup(loaderGroup);
			button.setUserData(kind);
			button.getStyleClass().add("loader-button");
			button.setOnAction(e -> {
				Profile target = ctx.profiles().loaderProfile(kind);
				if (target != null) {
					ctx.profiles().select(target);
					ctx.profilesChanged();
					runDiagnostics();
				}
			});
			loaderButtons.getChildren().add(button);
		}
		Label label = new Label("Loader");
		label.getStyleClass().add("field-label");
		label.setMinWidth(60);
		loaderHint.getStyleClass().add("muted");
		HBox row = new HBox(10, label, loaderButtons, loaderHint);
		row.setAlignment(Pos.CENTER_LEFT);
		loaderRow = row;

		optifineStatus.setWrapText(true);
		Button download = new Button("Baixar no site oficial");
		download.getStyleClass().add("ghost-button");
		download.setOnAction(e -> ctx.host().showDocument(OptiFine.DOWNLOADS_PAGE));
		Button select = new Button("Selecionar OptiFine...");
		select.getStyleClass().add("primary-button");
		select.setOnAction(e -> selectOptiFine());
		forgeWithOptifine.setOnAction(e -> {
			Profile profile = ctx.profiles().selected();
			profile.optifineWithForge = forgeWithOptifine.isSelected();
			ctx.profilesChanged();
		});
		vfxWithOptifine.setOnAction(e -> {
			Profile profile = ctx.profiles().selected();
			profile.optifineWithVfx = vfxWithOptifine.isSelected();
			ctx.profilesChanged();
		});
		Button mods = new Button("Abrir pasta de mods");
		mods.getStyleClass().add("ghost-button");
		mods.setOnAction(e -> openMods());
		forgeTools.getChildren().setAll(forgeWithOptifine, mods);
		forgeTools.setAlignment(Pos.CENTER_LEFT);
		HBox optifineButtons = new HBox(10, download, select);
		optifineBox.getChildren().setAll(vfxWithOptifine, optifineStatus, optifineButtons, forgeTools);
		optifineBox.getStyleClass().add("loader-panel");
		return row;
	}

	/** Mostra os botões de loader e o painel do OptiFine/Forge conforme o perfil escolhido. */
	private void refreshLoaders() {
		Profile profile = ctx.profiles().selected();
		if (profile.kind != Profile.Kind.VANILLA) {
			ctx.runAsync(() -> {
				if (OptiFine.installedJar() == null && OptiFine.autoDetect() != null) {
					LauncherContext.ui(this::refreshLoaders);
				}
			});
		}
		boolean legacy = "1.8.9".equals(profile.minecraftVersion) && profile.kind != Profile.Kind.VANILLA;
		loaderRow.setVisible(legacy);
		loaderRow.setManaged(legacy);
		for (javafx.scene.Node node : loaderButtons.getChildren()) {
			ToggleButton button = (ToggleButton) node;
			button.setSelected(button.getUserData() == profile.kind);
		}
		loaderHint.setText(switch (profile.kind) {
			case VFX -> "Menu, HUD e otimizações do VfxClient";
			case OPTIFINE -> "Minecraft + OptiFine, sem os recursos do VfxClient";
			case FORGE -> "Forge para seus mods, sem os recursos do VfxClient";
			default -> "";
		});

		boolean forge = profile.kind == Profile.Kind.FORGE;
		boolean vfx = profile.kind == Profile.Kind.VFX;
		boolean showOptifine = profile.kind == Profile.Kind.OPTIFINE || forge || vfx;
		vfxWithOptifine.setVisible(vfx);
		vfxWithOptifine.setManaged(vfx);
		vfxWithOptifine.setSelected(profile.optifineWithVfx);
		optifineBox.setVisible(showOptifine);
		optifineBox.setManaged(showOptifine);
		forgeTools.setVisible(forge);
		forgeTools.setManaged(forge);
		forgeWithOptifine.setSelected(profile.optifineWithForge);
		Path jar = OptiFine.installedJar();
		optifineStatus.setText(jar != null
				? "OptiFine pronto: " + jar.getFileName()
				: "OptiFine não encontrado. Baixe o OptiFine 1.8.9 no site oficial e deixe na Área de Trabalho ou em Downloads, "
				+ "que o launcher encontra sozinho (ou use Selecionar OptiFine). Sem ele, o jogo abre só com o VfxClient.");
		optifineStatus.getStyleClass().setAll(jar != null ? "accent" : "warning");
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
			status.setText("OptiFine selecionado.");
			detail.setText(file.getName());
		} catch (IOException ex) {
			status.setText("Arquivo recusado");
			detail.setText(ex.getMessage());
		}
		refreshLoaders();
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

	private static VBox card(String heading, javafx.scene.Node... children) {
		Label label = new Label(heading);
		label.getStyleClass().add("card-title");
		VBox box = new VBox(10);
		box.getChildren().add(label);
		box.getChildren().addAll(children);
		box.getStyleClass().add("card");
		return box;
	}

	/** Recarrega a lista de perfis (após criar/editar/remover). */
	public void reloadProfiles() {
		profileBox.setItems(FXCollections.observableArrayList(ctx.profiles().all()));
		profileBox.setValue(ctx.profiles().selected());
		refreshProfileInfo();
		refreshLoaders();
	}

	private void refreshProfileInfo() {
		Profile profile = ctx.profiles().selected();
		String mod = profile.isVfx() ? "VfxClient " + BuildInfo.load().clientVersion() + " + Legacy Fabric" : "Minecraft oficial, sem mods";
		profileInfo.setText("Minecraft " + profile.minecraftVersion + "  ·  " + mod + "  ·  " + profile.ramMb + " MB de RAM");
		profileInfo.getStyleClass().setAll("muted");
	}

	private void runDiagnostics() {
		Profile profile = ctx.profiles().selected();
		ctx.runAsync(() -> {
			Diagnostics.Report report = Diagnostics.run(profile);
			LauncherContext.ui(() -> {
				diagnosticSummary.setText(report.summary());
				diagnosticPreset.setText("Preset recomendado para este PC: " + report.presetLabel()
						+ (profile.isVfx() && "auto".equals(profile.preset) ? " (aplicado automaticamente)" : ""));
				diagnosticPreset.getStyleClass().setAll("accent");
				warnings.getChildren().clear();
				for (String warning : report.warnings()) {
					Label label = new Label("•  " + warning);
					label.setWrapText(true);
					label.getStyleClass().add("warning");
					warnings.getChildren().add(label);
				}
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

	private void startGame() {
		if (running != null && running.isAlive()) {
			return;
		}
		Profile profile = ctx.profiles().selected();
		Account account = ctx.accounts().selected();
		play.setDisable(true);
		openLog.setVisible(false);
		progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
		status.setText("Preparando...");
		detail.setText("");
		ctx.runAsync(() -> {
			try {
				GameLauncher.Session session = GameLauncher.launch(profile, account, ctx.accounts(), ctx.settings(),
						(stage, fraction, text) -> LauncherContext.ui(() -> {
							status.setText(stage);
							detail.setText(text == null ? "" : text);
							progressBar.setProgress(fraction < 0 ? ProgressBar.INDETERMINATE_PROGRESS : fraction);
						}));
				running = session.process();
				lastLog = session.log();
				LauncherContext.ui(() -> {
					status.setText("Jogo aberto: " + profile.name);
					detail.setText("Feche o Minecraft para voltar ao launcher.");
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
		play.setText("INICIAR");
		progressBar.setProgress(0);
		if (code == 0) {
			status.setText("O jogo foi fechado.");
			detail.setText("");
		} else {
			status.setText("O jogo fechou com erro (código " + code + ").");
			detail.setText("Veja o log para entender o que aconteceu.");
			openLog.setVisible(lastLog != null && Files.exists(lastLog));
		}
	}

	private void fail(String message) {
		play.setDisable(false);
		play.setText("INICIAR");
		progressBar.setProgress(0);
		status.setText("Não foi possível iniciar");
		detail.setText(message == null ? "Erro desconhecido" : message);
	}
}
