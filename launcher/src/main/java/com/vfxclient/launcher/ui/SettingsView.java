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
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Configurações: perfil (nome, RAM, preset de desempenho, perfil de config do VfxClient),
 * comportamento do launcher, login Microsoft e manutenção (cache, pastas, atualizações).
 */
public final class SettingsView extends ScrollPane {

	private static final String[] PRESET_IDS = {"auto", "low", "balanced", "high", "custom"};
	private static final String[] PRESET_NAMES = {"Automático (pelo diagnóstico)", "Baixo (mais FPS)", "Equilibrado", "Alto (visual)", "Personalizado (não mexer)"};
	private static final String[] LAUNCH_IDS = {"minimize", "keep", "close"};
	private static final String[] LAUNCH_NAMES = {"Minimizar o launcher", "Manter aberto", "Fechar o launcher"};

	private final LauncherContext ctx;
	private final ComboBox<Profile> profileBox = new ComboBox<>();
	private final TextField name = new TextField();
	private final Slider ram = new Slider();
	private final Spinner<Integer> ramField = new Spinner<>(512, 65536, 2048, 256);
	private final Label ramInfo = new Label();
	private final ComboBox<String> preset = new ComboBox<>(FXCollections.observableArrayList(PRESET_NAMES));
	private final CheckBox autoOptimize = new CheckBox("Desativar recursos pesados automaticamente em PC fraco");
	private final TextField configProfile = new TextField();
	private final TextField jvmArgs = new TextField();
	private final Label saved = new Label();

	/**
	 * @param ctx estado do launcher
	 */
	public SettingsView(LauncherContext ctx) {
		this.ctx = ctx;
		getStyleClass().add("view-scroll");
		setFitToWidth(true);

		Label title = new Label("Configurações");
		title.getStyleClass().add("view-title");

		VBox content = new VBox(16, title, profileCard(), launcherCard(), maintenanceCard());
		content.setPadding(new Insets(28, 32, 32, 32));
		content.getStyleClass().add("view");
		setContent(content);
		loadProfile(ctx.profiles().selected());
		ctx.onProfilesChanged(() -> {
			Profile current = profileBox.getValue();
			profileBox.setItems(FXCollections.observableArrayList(ctx.profiles().all()));
			profileBox.setValue(current != null && ctx.profiles().all().contains(current) ? current : ctx.profiles().selected());
		});
	}

	// ------------------------------------------------------------------
	// Perfil
	// ------------------------------------------------------------------

	private VBox profileCard() {
		profileBox.setItems(FXCollections.observableArrayList(ctx.profiles().all()));
		profileBox.setMaxWidth(Double.MAX_VALUE);
		profileBox.setOnAction(e -> {
			if (profileBox.getValue() != null) {
				loadProfile(profileBox.getValue());
			}
		});

		int max = SystemInfo.maxSafeRamMb();
		ram.setMin(512);
		ram.setMax(Math.max(1024, max));
		ram.setMajorTickUnit(1024);
		ram.setBlockIncrement(256);
		ram.setShowTickMarks(true);
		ram.valueProperty().addListener((obs, old, value) -> {
			int mb = (int) (Math.round(value.doubleValue() / 256.0) * 256);
			if (ramField.getValue() != mb) {
				ramField.getValueFactory().setValue(mb);
			}
		});
		ramField.setEditable(true);
		ramField.setPrefWidth(110);
		ramField.valueProperty().addListener((obs, old, value) -> {
			if (value != null && Math.abs(ram.getValue() - value) >= 1) {
				ram.setValue(value);
			}
			updateRamInfo();
		});
		Button recommended = new Button("Usar recomendado");
		recommended.getStyleClass().add("ghost-button");
		recommended.setOnAction(e -> ramField.getValueFactory().setValue(SystemInfo.recommendedRamMb()));
		HBox ramRow = new HBox(10, ram, ramField, new Label("MB"), recommended);
		ramRow.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(ram, Priority.ALWAYS);
		ramInfo.getStyleClass().add("muted");
		ramInfo.setWrapText(true);

		preset.setMaxWidth(Double.MAX_VALUE);
		configProfile.setPromptText("Padrao");
		jvmArgs.setPromptText("Ex.: -XX:+UseStringDeduplication (deixe vazio se não souber)");

		Button save = new Button("Salvar perfil");
		save.getStyleClass().add("primary-button");
		save.setOnAction(e -> saveProfile());
		Button delete = new Button("Remover perfil");
		delete.getStyleClass().add("ghost-button");
		delete.setOnAction(e -> {
			Profile profile = profileBox.getValue();
			if (profile != null && ctx.profiles().remove(profile)) {
				ctx.profilesChanged();
				profileBox.setItems(FXCollections.observableArrayList(ctx.profiles().all()));
				loadProfile(ctx.profiles().selected());
				saved.setText("Perfil removido.");
			} else {
				saved.setText("O perfil principal do VfxClient não pode ser removido.");
			}
		});
		saved.getStyleClass().add("accent");

		GridPane grid = new GridPane();
		grid.setHgap(14);
		grid.setVgap(10);
		int row = 0;
		grid.addRow(row++, label("Perfil"), profileBox);
		grid.addRow(row++, label("Nome"), name);
		grid.addRow(row++, label("Memória RAM"), ramRow);
		grid.add(ramInfo, 1, row++);
		grid.addRow(row++, label("Preset de desempenho"), preset);
		grid.add(autoOptimize, 1, row++);
		grid.addRow(row++, label("Perfil de config do VfxClient"), configProfile);
		grid.addRow(row++, label("Argumentos da JVM (avançado)"), jvmArgs);
		GridPane.setHgrow(profileBox, Priority.ALWAYS);
		GridPane.setHgrow(name, Priority.ALWAYS);
		GridPane.setHgrow(ramRow, Priority.ALWAYS);

		HBox actions = new HBox(10, save, delete, saved);
		actions.setAlignment(Pos.CENTER_LEFT);
		return card("Perfil de jogo", grid, actions);
	}

	private void loadProfile(Profile profile) {
		profileBox.setValue(profile);
		name.setText(profile.name);
		ram.setValue(profile.ramMb);
		ramField.getValueFactory().setValue(profile.ramMb);
		int index = java.util.Arrays.asList(PRESET_IDS).indexOf(profile.preset);
		preset.getSelectionModel().select(Math.max(0, index));
		boolean vfx = profile.isVfx();
		preset.setDisable(!vfx);
		autoOptimize.setDisable(!vfx);
		configProfile.setDisable(!vfx);
		autoOptimize.setSelected(profile.autoOptimize);
		configProfile.setText(profile.configProfile);
		jvmArgs.setText(profile.extraJvmArgs);
		saved.setText(vfx ? "" : "Perfil sem mods: preset e config do VfxClient não se aplicam.");
		updateRamInfo();
	}

	private void updateRamInfo() {
		int total = SystemInfo.totalRamMb();
		int chosen = ramField.getValue() == null ? 0 : ramField.getValue();
		String warning = chosen > SystemInfo.maxSafeRamMb() ? "  ·  Cuidado: deixa pouca memória para o Windows." : "";
		ramInfo.setText(String.format("Seu PC tem %.1f GB. Recomendado para o VfxClient: %d MB (mais que isso não aumenta o FPS).%s",
				total / 1024.0, SystemInfo.recommendedRamMb(), warning));
	}

	private void saveProfile() {
		Profile profile = profileBox.getValue();
		if (profile == null) {
			return;
		}
		profile.name = name.getText().isBlank() ? profile.name : name.getText().trim();
		profile.ramMb = Math.max(512, ramField.getValue());
		profile.preset = PRESET_IDS[Math.max(0, preset.getSelectionModel().getSelectedIndex())];
		profile.autoOptimize = autoOptimize.isSelected();
		String config = configProfile.getText().replaceAll("[^A-Za-z0-9 _-]", "").trim();
		profile.configProfile = config.isEmpty() ? "Padrao" : config;
		profile.extraJvmArgs = jvmArgs.getText().trim();
		ctx.profilesChanged();
		saved.setText("Salvo.");
	}

	// ------------------------------------------------------------------
	// Launcher
	// ------------------------------------------------------------------

	private VBox launcherCard() {
		ComboBox<String> onLaunch = new ComboBox<>(FXCollections.observableArrayList(LAUNCH_NAMES));
		onLaunch.getSelectionModel().select(Math.max(0, java.util.Arrays.asList(LAUNCH_IDS).indexOf(ctx.settings().onLaunch)));
		onLaunch.setOnAction(e -> {
			ctx.settings().onLaunch = LAUNCH_IDS[onLaunch.getSelectionModel().getSelectedIndex()];
			ctx.saveAll();
		});
		CheckBox updates = new CheckBox("Procurar atualizações ao abrir");
		updates.setSelected(ctx.settings().checkUpdates);
		updates.setOnAction(e -> {
			ctx.settings().checkUpdates = updates.isSelected();
			ctx.saveAll();
		});
		TextField clientId = new TextField(ctx.settings().microsoftClientId);
		clientId.setPromptText("Client ID do app no Azure (veja INSTRUCOES.txt)");
		clientId.textProperty().addListener((obs, old, value) -> {
			ctx.settings().microsoftClientId = value.trim();
			ctx.saveAll();
		});
		Label clientHint = new Label("Necessário para o login Microsoft. Sem ele, use contas offline.");
		clientHint.getStyleClass().add("muted");

		GridPane grid = new GridPane();
		grid.setHgap(14);
		grid.setVgap(10);
		grid.addRow(0, label("Ao iniciar o jogo"), onLaunch);
		grid.add(updates, 1, 1);
		grid.addRow(2, label("Login Microsoft"), clientId);
		grid.add(clientHint, 1, 3);
		GridPane.setHgrow(clientId, Priority.ALWAYS);
		return card("Launcher", grid);
	}

	// ------------------------------------------------------------------
	// Manutenção
	// ------------------------------------------------------------------

	private VBox maintenanceCard() {
		Label result = new Label();
		result.getStyleClass().add("accent");
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
		Button openGame = new Button("Abrir pasta do jogo");
		openGame.getStyleClass().add("ghost-button");
		openGame.setOnAction(e -> {
			try {
				Files.createDirectories(ctx.profiles().selected().gameDir());
				ctx.host().showDocument(ctx.profiles().selected().gameDir().toUri().toString());
			} catch (IOException ex) {
				result.setText(ex.getMessage());
			}
		});
		Button openData = new Button("Abrir pasta do launcher");
		openData.getStyleClass().add("ghost-button");
		openData.setOnAction(e -> ctx.host().showDocument(LauncherPaths.root().toUri().toString()));
		Button checkUpdate = new Button("Verificar atualização");
		checkUpdate.getStyleClass().add("ghost-button");
		checkUpdate.setOnAction(e -> ctx.runAsync(() -> {
			try {
				Updater.Update update = Updater.check();
				LauncherContext.ui(() -> result.setText(update == null
						? "Você está na versão mais recente (" + BuildInfo.load().clientVersion() + ")."
						: "Versão " + update.version() + " disponível: use o aviso na tela inicial para atualizar."));
			} catch (IOException ex) {
				LauncherContext.ui(() -> result.setText("Não foi possível verificar: " + ex.getMessage()));
			}
		}));

		Label explain = new Label("A limpeza apaga downloads temporários, arquivos nativos (recriados ao jogar), logs com mais de 7 dias "
				+ "e relatórios de crash com mais de 30 dias. Mundos, prints e configurações nunca são apagados.");
		explain.setWrapText(true);
		explain.getStyleClass().add("muted");
		HBox buttons = new HBox(10, clean, openGame, openData, checkUpdate);
		return card("Manutenção", buttons, explain, result);
	}

	private static Label label(String text) {
		Label label = new Label(text);
		label.getStyleClass().add("field-label");
		label.setMinWidth(190);
		return label;
	}

	private static VBox card(String heading, javafx.scene.Node... children) {
		Label label = new Label(heading);
		label.getStyleClass().add("card-title");
		VBox box = new VBox(12);
		box.getChildren().add(label);
		box.getChildren().addAll(children);
		box.getStyleClass().add("card");
		return box;
	}
}
