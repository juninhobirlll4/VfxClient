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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Conta, do jeito mais simples: lista das contas (clique escolhe, "x" remove), campo de nick
 * para entrar offline e, à direita, a skin. A skin aceita arrastar o PNG para cima dela.
 */
public final class AccountView extends HBox {

	private final LauncherContext ctx;
	private final VBox accounts = new VBox(6);
	private final SkinPreview preview = new SkinPreview(9);
	private final VBox dropZone;
	private final Label dropHint = new Label("Arraste a skin (PNG) aqui\nou clique para escolher");
	private final CheckBox slim = new CheckBox("Braço fino");
	private final ComboBox<MinecraftServices.Cape> capes = new ComboBox<>();
	private final VBox capeBox;
	private final Label message = new Label();

	/**
	 * @param ctx estado do launcher
	 */
	public AccountView(LauncherContext ctx) {
		super(20);
		this.ctx = ctx;
		getStyleClass().add("view");
		setPadding(new Insets(24, 28, 20, 28));

		// Coluna esquerda: contas e entrar.
		Label title = new Label("Conta");
		title.getStyleClass().add("view-title");

		ScrollPane accountScroll = new ScrollPane(accounts);
		accountScroll.setFitToWidth(true);
		accountScroll.getStyleClass().add("news-scroll");
		accountScroll.setPrefHeight(200);

		TextField nick = new TextField();
		nick.setPromptText("Seu nick");
		nick.getStyleClass().add("big-field");
		Button enter = new Button("Entrar");
		enter.getStyleClass().add("primary-button");
		enter.setMinWidth(Region.USE_PREF_SIZE);
		Runnable addOffline = () -> {
			String name = nick.getText().trim();
			if (!OfflineAuth.validName(name)) {
				Motion.toast(message, "Use de 3 a 16 letras, números ou _.");
				return;
			}
			ctx.accounts().put(OfflineAuth.create(name));
			ctx.accountChanged();
			nick.clear();
			reload();
			Motion.toast(message, "Pronto, " + name + "!");
		};
		enter.setOnAction(e -> addOffline.run());
		nick.setOnAction(e -> addOffline.run());
		HBox enterRow = new HBox(8, nick, enter);
		HBox.setHgrow(nick, Priority.ALWAYS);

		Button microsoft = new Button("Entrar com Microsoft (conta original)");
		microsoft.getStyleClass().add("link-button");
		microsoft.setOnAction(e -> loginMicrosoft());

		message.getStyleClass().add("accent");
		message.setWrapText(true);
		message.setOpacity(0);

		VBox accountCard = new VBox(12, label("Suas contas"), accountScroll, enterRow, microsoft);
		accountCard.getStyleClass().add("card");
		VBox left = new VBox(14, title, accountCard, message);
		left.setPrefWidth(380);
		left.setMinWidth(320);

		// Coluna direita: skin (arrastar e soltar) e capa.
		dropHint.getStyleClass().add("muted");
		dropHint.setStyle("-fx-text-alignment: center;");
		slim.setOnAction(e -> {
			Account account = ctx.accounts().selected();
			if (account != null) {
				account.skinModel = slim.isSelected() ? "slim" : "classic";
				ctx.accountChanged();
			}
			preview.setSkin(currentSkinImage(), slim.isSelected());
		});
		dropZone = new VBox(12, preview, dropHint);
		dropZone.setAlignment(Pos.CENTER);
		dropZone.getStyleClass().add("drop-zone");
		dropZone.setOnMouseClicked(e -> chooseSkin());
		dropZone.setOnDragOver(e -> {
			if (e.getDragboard().hasFiles()) {
				e.acceptTransferModes(TransferMode.COPY);
				if (!dropZone.getStyleClass().contains("drop-active")) {
					dropZone.getStyleClass().add("drop-active");
				}
			}
			e.consume();
		});
		dropZone.setOnDragExited(e -> dropZone.getStyleClass().remove("drop-active"));
		dropZone.setOnDragDropped(e -> {
			List<File> files = e.getDragboard().getFiles();
			boolean ok = !files.isEmpty();
			if (ok) {
				applySkin(files.get(0));
			}
			e.setDropCompleted(ok);
			e.consume();
		});
		VBox.setVgrow(dropZone, Priority.ALWAYS);

		capes.setMaxWidth(Double.MAX_VALUE);
		capes.setConverter(new javafx.util.StringConverter<>() {
			@Override
			public String toString(MinecraftServices.Cape cape) {
				return cape == null ? "Sem capa" : cape.alias();
			}

			@Override
			public MinecraftServices.Cape fromString(String s) {
				return null;
			}
		});
		capes.setOnAction(e -> applyCape());
		capeBox = new VBox(8, label("Capa"), capes);

		VBox skinCard = new VBox(12, label("Skin"), dropZone, slim, capeBox);
		skinCard.getStyleClass().add("card");
		VBox.setVgrow(skinCard, Priority.ALWAYS);
		VBox right = new VBox(skinCard);
		HBox.setHgrow(right, Priority.ALWAYS);
		getChildren().addAll(left, right);
		reload();

		sceneProperty().addListener((obs, old, scene) -> {
			if (scene != null) {
				Motion.enter(accountCard, 0);
				Motion.enter(skinCard, 80);
			}
		});
	}

	private static Label label(String text) {
		Label label = new Label(text);
		label.getStyleClass().add("card-title");
		return label;
	}

	/** Monta a lista de contas (uma linha por conta). */
	private void reload() {
		accounts.getChildren().clear();
		Account selected = ctx.accounts().selected();
		List<Account> all = ctx.accounts().all();
		if (all.isEmpty()) {
			Label empty = new Label("Nenhuma conta ainda. Digite seu nick abaixo.");
			empty.getStyleClass().add("muted");
			accounts.getChildren().add(empty);
		}
		for (Account account : all) {
			accounts.getChildren().add(row(account, account == selected));
		}
		refreshSkin();
	}

	private HBox row(Account account, boolean selected) {
		ImageView head = new ImageView();
		Image face = null;
		Path skin = account.type == Account.Type.OFFLINE ? GameLauncher.localSkin(account) : remoteSkinCache(account);
		if (Files.exists(skin)) {
			face = Faces.face(new Image(skin.toUri().toString()), 3);
		}
		if (face == null) {
			java.io.InputStream logo = AccountView.class.getResourceAsStream("/com/vfxclient/launcher/logo-256.png");
			face = logo == null ? null : new Image(logo);
		}
		head.setImage(face);
		head.setFitWidth(24);
		head.setFitHeight(24);
		Label name = new Label(account.username);
		name.getStyleClass().add("account-chip-name");
		Label type = new Label(account.type == Account.Type.OFFLINE ? "offline" : "original");
		type.getStyleClass().add("tag");
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		Button remove = new Button("✕");
		remove.getStyleClass().add("remove-button");
		remove.setOnAction(e -> {
			ctx.accounts().remove(account);
			ctx.accountChanged();
			reload();
		});
		HBox row = new HBox(10, head, name, type, spacer, remove);
		row.setAlignment(Pos.CENTER_LEFT);
		row.getStyleClass().add(selected ? "account-row-selected" : "account-row");
		row.setOnMouseClicked(e -> {
			ctx.accounts().select(account);
			ctx.accountChanged();
			reload();
		});
		return row;
	}

	// ------------------------------------------------------------------
	// Login Microsoft
	// ------------------------------------------------------------------

	private void loginMicrosoft() {
		if (!ctx.settings().microsoftEnabled()) {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.initOwner(ctx.stage());
			alert.setTitle("Entrar com Microsoft");
			alert.setHeaderText("Ainda não disponível");
			alert.setContentText("O login com conta original ainda não foi liberado neste launcher. Por enquanto, entre só com o seu nick.");
			alert.showAndWait();
			return;
		}
		AtomicBoolean cancelled = new AtomicBoolean();
		Motion.toast(message, "Pedindo código à Microsoft...");
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
					Motion.toast(message, "Bem-vindo, " + account.username + "!");
				});
			} catch (IOException | RuntimeException e) {
				LauncherContext.ui(() -> Motion.toast(message, "Login Microsoft: " + e.getMessage()));
			}
		});
	}

	private void showCodeDialog(MicrosoftAuth.DeviceCode code, AtomicBoolean cancelled) {
		Alert dialog = new Alert(Alert.AlertType.INFORMATION);
		dialog.initOwner(ctx.stage());
		dialog.setTitle("Entrar com Microsoft");
		dialog.setHeaderText("Código: " + code.userCode());
		dialog.setContentText("Abrimos a página da Microsoft. Cole o código (já está copiado) e entre com a conta que tem o Minecraft.");
		ClipboardContent content = new ClipboardContent();
		content.putString(code.userCode());
		Clipboard.getSystemClipboard().setContent(content);
		ctx.host().showDocument(code.verificationUri());
		ButtonType cancel = new ButtonType("Cancelar");
		dialog.getButtonTypes().setAll(ButtonType.OK, cancel);
		dialog.showAndWait().ifPresent(button -> {
			if (button == cancel) {
				cancelled.set(true);
				Motion.toast(message, "Login cancelado.");
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
		boolean microsoft = account != null && account.type == Account.Type.MICROSOFT;
		capeBox.setVisible(microsoft);
		capeBox.setManaged(microsoft);
		dropZone.setDisable(account == null);
		if (account == null) {
			preview.setSkin(null, false);
			return;
		}
		slim.setSelected("slim".equals(account.skinModel));
		preview.setSkin(currentSkinImage(), slim.isSelected());
		if (!microsoft) {
			return;
		}
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
					capes.setOnAction(null);
					capes.getItems().setAll(list);
					capes.getItems().add(0, null);
					capes.setValue(list.stream().filter(MinecraftServices.Cape::active).findFirst().orElse(null));
					capes.setOnAction(e -> applyCape());
				});
			} catch (IOException | RuntimeException e) {
				LauncherContext.ui(() -> preview.setSkin(currentSkinImage(), slim.isSelected()));
			}
		});
	}

	private void chooseSkin() {
		if (ctx.accounts().selected() == null) {
			Motion.toast(message, "Entre com o seu nick primeiro.");
			return;
		}
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Escolher skin");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Skin do Minecraft (PNG)", "*.png"));
		File file = chooser.showOpenDialog(ctx.stage());
		if (file != null) {
			applySkin(file);
		}
	}

	/** Aplica a skin escolhida ou arrastada (offline: salva local; original: envia para a conta). */
	private void applySkin(File file) {
		Account account = ctx.accounts().selected();
		if (account == null) {
			Motion.toast(message, "Entre com o seu nick primeiro.");
			return;
		}
		try {
			BufferedImage image = ImageIO.read(file);
			if (image == null || image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
				Motion.toast(message, "Use um PNG de skin do Minecraft (64x64).");
				return;
			}
		} catch (IOException e) {
			Motion.toast(message, "Não deu para ler a imagem.");
			return;
		}
		account.skinModel = slim.isSelected() ? "slim" : "classic";
		if (account.type == Account.Type.OFFLINE) {
			try {
				Path target = GameLauncher.localSkin(account);
				Files.createDirectories(target.getParent());
				Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
				ctx.accountChanged();
				reload();
				Motion.toast(message, "Skin salva! Ela aparece no jogo.");
			} catch (IOException e) {
				Motion.toast(message, "Não deu para salvar a skin.");
			}
			return;
		}
		Motion.toast(message, "Enviando a skin...");
		ctx.runAsync(() -> {
			try {
				MinecraftServices.uploadSkin(account, file.toPath(), slim.isSelected());
				LauncherContext.ui(() -> {
					ctx.accountChanged();
					reload();
					Motion.toast(message, "Skin trocada na sua conta.");
				});
			} catch (IOException e) {
				LauncherContext.ui(() -> Motion.toast(message, "A Mojang recusou a skin."));
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
				LauncherContext.ui(() -> Motion.toast(message, cape == null ? "Capa escondida." : "Capa em uso."));
			} catch (IOException e) {
				LauncherContext.ui(() -> Motion.toast(message, "Não deu para trocar a capa."));
			}
		});
	}
}
