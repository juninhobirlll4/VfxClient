package com.vfxclient.launcher.ui;

import java.io.IOException;
import java.nio.file.Files;

import com.vfxclient.launcher.BuildInfo;
import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.optimization.CacheCleaner;
import com.vfxclient.launcher.profiles.Profile;
import com.vfxclient.launcher.settings.SystemInfo;
import com.vfxclient.launcher.updater.Updater;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Configurações simples: memória, desempenho e o que fazer ao abrir o jogo. Tudo salva sozinho.
 * O resto (perfis, argumentos da JVM, Client ID, limpeza) fica em "Avançado", fechado por padrão.
 */
public final class SettingsView extends ScrollPane {

	private static final String[] PRESET_IDS = {"auto", "low", "balanced", "high"};
	private static final String[] PRESET_NAMES = {"Automático (recomendado)", "Mais FPS", "Equilibrado", "Mais bonito"};
	private static final String[] LAUNCH_IDS = {"minimize", "keep", "close"};
	private static final String[] LAUNCH_NAMES = {"Esconder na bandeja (perto do relógio)", "Deixar aberto", "Fechar o launcher"};

	private final LauncherContext ctx;
	private final Slider ram = new Slider();
	private final Label ramValue = new Label();
	private final ComboBox<String> preset = new ComboBox<>(FXCollections.observableArrayList(PRESET_NAMES));
	private final Label status = new Label();
	private boolean loading;

	/**
	 * @param ctx estado do launcher
	 */
	public SettingsView(LauncherContext ctx) {
		this.ctx = ctx;
		getStyleClass().add("view-scroll");
		setFitToWidth(true);

		Label title = new Label("Configurações");
		title.getStyleClass().add("view-title");
		status.getStyleClass().add("accent");
		status.setOpacity(0);

		VBox game = gameCard();
		VBox launcher = launcherCard();
		TitledPane advanced = advancedPane();
		VBox content = new VBox(16, title, game, launcher, advanced, status);
		content.setPadding(new Insets(24, 28, 28, 28));
		content.getStyleClass().add("view");
		setContent(content);
		load();
		ctx.onProfilesChanged(this::load);

		sceneProperty().addListener((obs, old, scene) -> {
			if (scene != null) {
				Motion.enter(game, 0);
				Motion.enter(launcher, 70);
				Motion.enter(advanced, 140);
			}
		});
	}

	private Profile profile() {
		return ctx.profiles().selected();
	}

	private void saved() {
		if (!loading) {
			ctx.profilesChanged();
			Motion.toast(status, "Salvo");
		}
	}

	// ------------------------------------------------------------------
	// Jogo: memória e desempenho
	// ------------------------------------------------------------------

	private VBox gameCard() {
		int max = SystemInfo.maxSafeRamMb();
		ram.setMin(1024);
		ram.setMax(Math.max(2048, max));
		ram.setMajorTickUnit(1024);
		ram.setMinorTickCount(3);
		ram.setSnapToTicks(true);
		ram.setBlockIncrement(512);
		ram.valueProperty().addListener((obs, old, value) -> {
			int mb = (int) (Math.round(value.doubleValue() / 512.0) * 512);
			ramValue.setText(String.format("%.1f GB", mb / 1024.0));
			if (!loading && profile().ramMb != mb) {
				profile().ramMb = mb;
			}
		});
		ram.setOnMouseReleased(e -> saved());
		ram.setOnKeyReleased(e -> saved());
		ramValue.getStyleClass().add("value-pill");
		ramValue.setMinWidth(70);
		HBox ramRow = new HBox(14, ram, ramValue);
		ramRow.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(ram, Priority.ALWAYS);
		Label ramHint = new Label(String.format("Recomendado para o seu PC: %.0f GB", SystemInfo.recommendedRamMb() / 1024.0));
		ramHint.getStyleClass().add("muted");

		preset.setMaxWidth(Double.MAX_VALUE);
		preset.setOnAction(e -> {
			int index = preset.getSelectionModel().getSelectedIndex();
			if (index >= 0 && !loading) {
				profile().preset = PRESET_IDS[index];
				saved();
			}
		});

		return card("Jogo",
				field("Memória", ramRow),
				ramHint,
				field("Desempenho", preset));
	}

	private void load() {
		loading = true;
		Profile profile = profile();
		ram.setValue(profile.ramMb);
		ramValue.setText(String.format("%.1f GB", profile.ramMb / 1024.0));
		int index = java.util.Arrays.asList(PRESET_IDS).indexOf(profile.preset);
		preset.getSelectionModel().select(Math.max(0, index));
		preset.setDisable(!profile.isVfx());
		loading = false;
	}

	// ------------------------------------------------------------------
	// Launcher
	// ------------------------------------------------------------------

	private VBox launcherCard() {
		ComboBox<String> onLaunch = new ComboBox<>(FXCollections.observableArrayList(LAUNCH_NAMES));
		onLaunch.setMaxWidth(Double.MAX_VALUE);
		onLaunch.getSelectionModel().select(Math.max(0, java.util.Arrays.asList(LAUNCH_IDS).indexOf(ctx.settings().onLaunch)));
		onLaunch.setOnAction(e -> {
			ctx.settings().onLaunch = LAUNCH_IDS[onLaunch.getSelectionModel().getSelectedIndex()];
			ctx.saveAll();
			Motion.toast(status, "Salvo");
		});
		CheckBox updates = new CheckBox("Avisar quando sair versão nova");
		updates.setSelected(ctx.settings().checkUpdates);
		updates.setOnAction(e -> {
			ctx.settings().checkUpdates = updates.isSelected();
			ctx.saveAll();
			Motion.toast(status, "Salvo");
		});
		return card("Launcher", field("Ao abrir o jogo", onLaunch), updates);
	}

	// ------------------------------------------------------------------
	// Avançado (fechado por padrão)
	// ------------------------------------------------------------------

	private TitledPane advancedPane() {
		ComboBox<Profile> profileBox = new ComboBox<>(FXCollections.observableArrayList(ctx.profiles().all()));
		profileBox.setMaxWidth(Double.MAX_VALUE);
		profileBox.setValue(profile());
		profileBox.setOnAction(e -> {
			if (profileBox.getValue() != null && profileBox.getValue() != profile()) {
				ctx.profiles().select(profileBox.getValue());
				ctx.profilesChanged();
			}
		});
		ctx.onProfilesChanged(() -> {
			profileBox.setItems(FXCollections.observableArrayList(ctx.profiles().all()));
			profileBox.setValue(profile());
		});

		CheckBox autoOptimize = new CheckBox("Desligar efeitos pesados sozinho em PC fraco");
		autoOptimize.setSelected(profile().autoOptimize);
		autoOptimize.setOnAction(e -> {
			profile().autoOptimize = autoOptimize.isSelected();
			saved();
		});
		TextField jvmArgs = new TextField(profile().extraJvmArgs);
		jvmArgs.setPromptText("Deixe vazio se não souber");
		jvmArgs.focusedProperty().addListener((obs, was, focused) -> {
			if (!focused) {
				profile().extraJvmArgs = jvmArgs.getText().trim();
				saved();
			}
		});
		TextField clientId = new TextField(ctx.settings().microsoftClientId);
		clientId.setPromptText("Client ID do Azure (login Microsoft)");
		clientId.textProperty().addListener((obs, old, value) -> {
			ctx.settings().microsoftClientId = value.trim();
			ctx.saveAll();
		});

		Label result = new Label();
		result.getStyleClass().add("muted");
		result.setWrapText(true);
		Button clean = new Button("Limpar cache");
		clean.getStyleClass().add("ghost-button");
		clean.setOnAction(e -> {
			clean.setDisable(true);
			ctx.runAsync(() -> {
				CacheCleaner.Result cleaned = CacheCleaner.clean();
				LauncherContext.ui(() -> {
					result.setText("Limpeza concluída: " + cleaned.describe() + ".");
					clean.setDisable(false);
				});
			});
		});
		Button openGame = new Button("Pasta do jogo");
		openGame.getStyleClass().add("ghost-button");
		openGame.setOnAction(e -> {
			try {
				Files.createDirectories(profile().gameDir());
				ctx.host().showDocument(profile().gameDir().toUri().toString());
			} catch (IOException ex) {
				result.setText(ex.getMessage());
			}
		});
		Button openData = new Button("Pasta do launcher");
		openData.getStyleClass().add("ghost-button");
		openData.setOnAction(e -> ctx.host().showDocument(LauncherPaths.root().toUri().toString()));
		Button checkUpdate = new Button("Verificar atualização");
		checkUpdate.getStyleClass().add("ghost-button");
		checkUpdate.setOnAction(e -> ctx.runAsync(() -> {
			try {
				Updater.Update update = Updater.check();
				LauncherContext.ui(() -> result.setText(update == null
						? "Você está na versão mais recente (" + BuildInfo.load().clientVersion() + ")."
						: "Versão " + update.version() + " disponível: o aviso aparece na tela Início."));
			} catch (IOException ex) {
				LauncherContext.ui(() -> result.setText("Não foi possível verificar agora."));
			}
		}));
		Button remove = new Button("Remover este perfil");
		remove.getStyleClass().add("ghost-button");
		remove.setOnAction(e -> {
			if (ctx.profiles().remove(profile())) {
				ctx.profilesChanged();
				result.setText("Perfil removido.");
			} else {
				result.setText("O perfil principal do VfxClient não pode ser removido.");
			}
		});

		HBox tools = new HBox(8, clean, openGame, openData, checkUpdate, remove);
		tools.setAlignment(Pos.CENTER_LEFT);
		VBox box = new VBox(12,
				field("Perfil", profileBox),
				autoOptimize,
				field("Argumentos da JVM", jvmArgs),
				field("Login Microsoft", clientId),
				tools,
				result);
		box.setPadding(new Insets(12, 4, 4, 4));
		TitledPane pane = new TitledPane("Avançado", box);
		pane.setExpanded(false);
		pane.setAnimated(true);
		pane.getStyleClass().add("advanced-pane");
		return pane;
	}

	private static HBox field(String text, Node control) {
		Label label = new Label(text);
		label.getStyleClass().add("field-label");
		label.setMinWidth(140);
		HBox row = new HBox(14, label, control);
		row.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(control, Priority.ALWAYS);
		return row;
	}

	private static VBox card(String heading, Node... children) {
		Label label = new Label(heading);
		label.getStyleClass().add("card-title");
		VBox box = new VBox(12);
		box.getChildren().add(label);
		box.getChildren().addAll(children);
		box.getStyleClass().add("card");
		return box;
	}
}
