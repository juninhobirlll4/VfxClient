package com.vfxclient.launcher.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import com.vfxclient.launcher.LauncherPaths;
import com.vfxclient.launcher.auth.Account;
import com.vfxclient.launcher.auth.MicrosoftAuth;
import com.vfxclient.launcher.auth.MinecraftServices;
import com.vfxclient.launcher.auth.OfflineAuth;
import com.vfxclient.launcher.game.GameLauncher;
import com.vfxclient.launcher.util.Http;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Tela de conta: contas salvas, login Microsoft (código de dispositivo), conta offline,
 * prévia e troca de skin e escolha de capa.
 */
public final class AccountView extends HBox {

	private final LauncherContext ctx;
	private final ListView<Account> list = new ListView<>();
	private final SkinPreview preview = new SkinPreview(9);
	private final Label skinInfo = new Label();
	private final CheckBox slim = new CheckBox("Braço fino (modelo Alex)");
	private final ComboBox<MinecraftServices.Cape> capes = new ComboBox<>();
	private final Button applyCape = new Button("Usar capa");
	private final Label message = new Label();

	/**
	 * @param ctx estado do launcher
	 */
	public AccountView(LauncherContext ctx) {
		super(24);
		this.ctx = ctx;
		getStyleClass().add("view");
		setPadding(new Insets(28, 32, 24, 32));

		// Coluna esquerda: contas.
		Label title = new Label("Conta");
		title.getStyleClass().add("view-title");
		list.getStyleClass().add("account-list");
		list.setPrefHeight(170);
		list.getSelectionModel().selectedItemProperty().addListener((obs, old, account) -> {
			if (account != null) {
				ctx.accounts().select(account);
				ctx.accountChanged();
				refreshSkin();
			}
		});
		Button remove = new Button("Remover");
		remove.getStyleClass().add("ghost-button");
		remove.setOnAction(e -> {
			Account account = list.getSelectionModel().getSelectedItem();
			if (account != null) {
				ctx.accounts().remove(account);
				ctx.accountChanged();
				reload();
			}
		});

		Button microsoft = new Button("Entrar com Microsoft");
		microsoft.getStyleClass().add("primary-button");
		microsoft.setMaxWidth(Double.MAX_VALUE);
		microsoft.setOnAction(e -> loginMicrosoft());

		TextField nick = new TextField();
		nick.setPromptText("Nick offline");
		Button addOffline = new Button("Adicionar");
		addOffline.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
		addOffline.getStyleClass().add("ghost-button");
		addOffline.setOnAction(e -> {
			String name = nick.getText().trim();
			if (!OfflineAuth.validName(name)) {
				show("Nick inválido: use de 3 a 16 letras, números ou _.");
				return;
			}
			ctx.accounts().put(OfflineAuth.create(name));
			ctx.accountChanged();
			nick.clear();
			reload();
			show("Conta offline criada. Ela entra em servidores não-premium.");
		});
		HBox offlineRow = new HBox(8, nick, addOffline);
		HBox.setHgrow(nick, Priority.ALWAYS);

		Label offlineHint = new Label("Conta offline: só o nick, para servidores não-premium. Servidores premium exigem conta Microsoft.");
		offlineHint.setWrapText(true);
		offlineHint.getStyleClass().add("muted");
		message.setWrapText(true);
		message.getStyleClass().add("accent");

		VBox left = new VBox(12, title, card("Contas salvas", list, remove), card("Adicionar conta", microsoft, offlineRow, offlineHint), message);
		left.setPrefWidth(380);

		// Coluna direita: skin e capa.
		Button chooseSkin = new Button("Escolher skin (PNG)");
		chooseSkin.getStyleClass().add("primary-button");
		chooseSkin.setOnAction(e -> chooseSkin());
		slim.setOnAction(e -> preview.setSkin(currentSkinImage(), slim.isSelected()));
		skinInfo.setWrapText(true);
		skinInfo.getStyleClass().add("muted");
		VBox skinBox = new VBox(12, preview, slim, chooseSkin, skinInfo);
		skinBox.setAlignment(Pos.TOP_CENTER);

		capes.setMaxWidth(Double.MAX_VALUE);
		capes.setConverter(new javafx.util.StringConverter<>() {
			@Override
			public String toString(MinecraftServices.Cape cape) {
				return cape == null ? "Sem capa" : cape.alias() + (cape.active() ? " (em uso)" : "");
			}

			@Override
			public MinecraftServices.Cape fromString(String s) {
				return null;
			}
		});
		applyCape.getStyleClass().add("ghost-button");
		applyCape.setOnAction(e -> applyCape());
		VBox capeBox = card("Capa", capes, applyCape);

		VBox right = new VBox(14, card("Skin", skinBox), capeBox);
		HBox.setHgrow(right, Priority.ALWAYS);
		getChildren().addAll(left, right);
		reload();
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

	private void reload() {
		list.setItems(FXCollections.observableArrayList(ctx.accounts().all()));
		Account selected = ctx.accounts().selected();
		if (selected != null) {
			list.getSelectionModel().select(selected);
		}
		refreshSkin();
	}

	private void show(String text) {
		message.setText(text);
	}

	// ------------------------------------------------------------------
	// Login Microsoft
	// ------------------------------------------------------------------

	private void loginMicrosoft() {
		if (!ctx.settings().microsoftEnabled()) {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.initOwner(ctx.stage());
			alert.setTitle("Login Microsoft");
			alert.setHeaderText("Falta configurar o Client ID da Microsoft");
			alert.setContentText("O login Microsoft precisa de um aplicativo registrado no Azure (é grátis). "
					+ "Cole o Client ID em Configurações > Launcher. O passo a passo está no arquivo INSTRUCOES.txt.\n\n"
					+ "Enquanto isso, use uma conta offline.");
			alert.showAndWait();
			return;
		}
		AtomicBoolean cancelled = new AtomicBoolean();
		show("Pedindo código à Microsoft...");
		ctx.runAsync(() -> {
			try {
				MicrosoftAuth auth = new MicrosoftAuth(ctx.settings().microsoftClientId);
				MicrosoftAuth.DeviceCode code = auth.requestCode();
				LauncherContext.ui(() -> showCodeDialog(code, cancelled));
				Account account = auth.waitForLogin(code, cancelled::get);
				LauncherContext.ui(() -> {
					ctx.accounts().put(account);
					ctx.accountChanged();
					reload();
					show("Bem-vindo, " + account.username + "!");
				});
			} catch (IOException | RuntimeException e) {
				LauncherContext.ui(() -> show("Login Microsoft: " + e.getMessage()));
			}
		});
	}

	private void showCodeDialog(MicrosoftAuth.DeviceCode code, AtomicBoolean cancelled) {
		Alert dialog = new Alert(Alert.AlertType.INFORMATION);
		dialog.initOwner(ctx.stage());
		dialog.setTitle("Entrar com Microsoft");
		dialog.setHeaderText("Código: " + code.userCode());
		dialog.setContentText("1. Abra " + code.verificationUri() + "\n2. Digite o código acima\n3. Entre com a conta que tem o Minecraft\n\n"
				+ "O código já foi copiado. Esta janela pode ser fechada; o launcher continua esperando.");
		ClipboardContent content = new ClipboardContent();
		content.putString(code.userCode());
		Clipboard.getSystemClipboard().setContent(content);
		ctx.host().showDocument(code.verificationUri());
		ButtonType cancel = new ButtonType("Cancelar login");
		dialog.getButtonTypes().setAll(ButtonType.OK, cancel);
		dialog.showAndWait().ifPresent(button -> {
			if (button == cancel) {
				cancelled.set(true);
				show("Login cancelado.");
			} else {
				show("Esperando você confirmar o código no navegador...");
			}
		});
	}

	// ------------------------------------------------------------------
	// Skin e capa
	// ------------------------------------------------------------------

	private Image currentSkinImage() {
		Account account = ctx.accounts().selected();
		if (account == null) {
			return null;
		}
		Path file = account.type == Account.Type.OFFLINE ? GameLauncher.localSkin(account) : remoteSkinCache(account);
		return Files.exists(file) ? new Image(file.toUri().toString()) : null;
	}

	private static Path remoteSkinCache(Account account) {
		return LauncherPaths.launcher().resolve("skins").resolve(account.uuid + "-oficial.png");
	}

	private void refreshSkin() {
		Account account = ctx.accounts().selected();
		capes.getItems().clear();
		if (account == null) {
			preview.setSkin(null, false);
			skinInfo.setText("Crie ou escolha uma conta.");
			capes.setDisable(true);
			applyCape.setDisable(true);
			return;
		}
		slim.setSelected("slim".equals(account.skinModel));
		if (account.type == Account.Type.OFFLINE) {
			preview.setSkin(currentSkinImage(), slim.isSelected());
			skinInfo.setText("Conta offline: a skin vale dentro do VfxClient (só você vê, até existir o servidor de skins).");
			capes.setDisable(true);
			applyCape.setDisable(true);
			capes.setPromptText("Capas só em contas Microsoft");
			return;
		}
		skinInfo.setText("Skin oficial da conta Microsoft (todo mundo vê).");
		capes.setDisable(false);
		applyCape.setDisable(false);
		ctx.runAsync(() -> {
			try {
				if (!account.tokenValid() && ctx.settings().microsoftEnabled()) {
					Account refreshed = new MicrosoftAuth(ctx.settings().microsoftClientId).refresh(account);
					account.accessToken = refreshed.accessToken;
					account.refreshToken = refreshed.refreshToken;
					account.expiresAt = refreshed.expiresAt;
					ctx.saveAll();
				}
				MinecraftServices.fillProfile(account);
				if (account.skinUrl != null) {
					Path cache = remoteSkinCache(account);
					Files.createDirectories(cache.getParent());
					Http.download(account.skinUrl.replace("http://", "https://"), cache);
				}
				List<MinecraftServices.Cape> list = MinecraftServices.capes(account);
				LauncherContext.ui(() -> {
					preview.setSkin(currentSkinImage(), "slim".equals(account.skinModel));
					capes.getItems().setAll(list);
					capes.getItems().add(0, null);
					capes.setValue(list.stream().filter(MinecraftServices.Cape::active).findFirst().orElse(null));
				});
			} catch (IOException | RuntimeException e) {
				LauncherContext.ui(() -> {
					preview.setSkin(currentSkinImage(), slim.isSelected());
					skinInfo.setText("Não foi possível atualizar a skin oficial: " + e.getMessage());
				});
			}
		});
	}

	private void chooseSkin() {
		Account account = ctx.accounts().selected();
		if (account == null) {
			show("Crie ou escolha uma conta primeiro.");
			return;
		}
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Escolher skin");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Skin do Minecraft (PNG)", "*.png"));
		File file = chooser.showOpenDialog(ctx.stage());
		if (file == null) {
			return;
		}
		try {
			BufferedImage image = ImageIO.read(file);
			if (image == null || image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
				show("A skin precisa ser um PNG de 64x64 (ou 64x32, formato antigo).");
				return;
			}
		} catch (IOException e) {
			show("Não foi possível ler a imagem: " + e.getMessage());
			return;
		}
		account.skinModel = slim.isSelected() ? "slim" : "classic";
		if (account.type == Account.Type.OFFLINE) {
			try {
				Path target = GameLauncher.localSkin(account);
				Files.createDirectories(target.getParent());
				Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
				ctx.accountChanged();
				refreshSkin();
				show("Skin salva. Ela aparece no jogo na próxima vez que você iniciar.");
			} catch (IOException e) {
				show("Não foi possível salvar a skin: " + e.getMessage());
			}
			return;
		}
		show("Enviando a skin para a conta Microsoft...");
		ctx.runAsync(() -> {
			try {
				MinecraftServices.uploadSkin(account, file.toPath(), slim.isSelected());
				LauncherContext.ui(() -> {
					ctx.accountChanged();
					refreshSkin();
					show("Skin trocada na sua conta oficial.");
				});
			} catch (IOException e) {
				LauncherContext.ui(() -> show("A Mojang recusou a skin: " + e.getMessage()));
			}
		});
	}

	private void applyCape() {
		Account account = ctx.accounts().selected();
		if (account == null || account.type != Account.Type.MICROSOFT) {
			return;
		}
		MinecraftServices.Cape cape = capes.getValue();
		ctx.runAsync(() -> {
			try {
				MinecraftServices.setCape(account, cape == null ? null : cape.id());
				LauncherContext.ui(() -> {
					show(cape == null ? "Capa escondida." : "Capa \"" + cape.alias() + "\" em uso.");
					refreshSkin();
				});
			} catch (IOException e) {
				LauncherContext.ui(() -> show("Não foi possível trocar a capa: " + e.getMessage()));
			}
		});
	}
}
