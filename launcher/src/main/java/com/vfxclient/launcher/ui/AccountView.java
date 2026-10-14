package com.vfxclient.launcher.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import javafx.util.StringConverter;

/**
 * Conta, do jeito mais simples: lista das contas (clique escolhe, "x" remove), campo de nick
 * para entrar offline e, à direita, a skin. A skin aceita arrastar o PNG para cima dela.
 */
public final class AccountView extends HBox {

	private final LauncherContext ctx;
	private final VBox accounts = new VBox(6);
	private final SkinPreview preview = new SkinPreview(9);
	private final Label dropHint = new Label("Arraste a skin (PNG) aqui\nou clique para escolher");
	private final VBox dropZone = new VBox(12, preview, dropHint);
	private final CheckBox slim = new CheckBox("Braço fino");
	private final ComboBox<MinecraftServices.Cape> capes = new ComboBox<>();
	private final VBox capeBox = new VBox(8, label("Capa"), capes);
	private final Label message = new Label();

	/**
	 * @param ctx estado do launcher
	 */
	public AccountView(LauncherContext ctx) {
		super(20);
		this.ctx = ctx;
		getStyleClass().add("view");
		setPadding(new Insets(24, 28, 20, 28));

		Label title = new Label("Conta");
		title.getStyleClass().add("view-title");

		message.getStyleClass().add("accent");
		message.setWrapText(true);
		message.setOpacity(0);

		VBox accountCard = accountCard();
		VBox left = new VBox(14, title, accountCard, message);
		left.setPrefWidth(380);
		left.setMinWidth(320);

		VBox skinCard = skinCard();
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

	/** Coluna esquerda: lista de contas, nick + Entrar e o link da Microsoft. */
	private VBox accountCard() {
		ScrollPane accountScroll = new ScrollPane(accounts);
		accountScroll.setFitToWidth(true);
		accountScroll.getStyleClass().add("news-scroll");
		accountScroll.setPrefHeight(200);

		TextField nick = new TextField();
		nick.setPromptText("Seu nick");
		nick.getStyleClass().add("big-field");
		nick.setOnAction(e -> addOfflineAccount(nick));

		Button enter = new Button("Entrar");
		enter.getStyleClass().add("primary-button");
		enter.setMinWidth(Region.USE_PREF_SIZE);
		enter.setOnAction(e -> addOfflineAccount(nick));

		HBox enterRow = new HBox(8, nick, enter);
		HBox.setHgrow(nick, Priority.ALWAYS);

		Button microsoft = new Button("Entrar com Microsoft (conta original)");
		microsoft.getStyleClass().add("link-button");
		microsoft.setOnAction(e -> loginMicrosoft());

		VBox card = new VBox(12, label("Suas contas"), accountScroll, enterRow, microsoft);
		card.getStyleClass().add("card");
		return card;
	}

	private void addOfflineAccount(TextField nick) {
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
	}

	/** Coluna direita: skin (arrastar e soltar), braço fino e capa. */
	private VBox skinCard() {
		dropHint.getStyleClass().add("muted");
		dropHint.setStyle("-fx-text-alignment: center;");
		setupDropZone();

		slim.setOnAction(e -> {
			Account account = ctx.accounts().selected();
			if (account != null) {
				account.skinModel = slim.isSelected() ? "slim" : "classic";
				ctx.accountChanged();
			}
			preview.setSkin(currentSkinImage(), slim.isSelected());
		});

		setupCapes();

		VBox card = new VBox(12, label("Skin"), dropZone, slim, capeBox);
		card.getStyleClass().add("card");
		VBox.setVgrow(card, Priority.ALWAYS);
		return card;
	}

	/** A prévia da skin aceita clique (escolher arquivo) e arrastar um PNG. */
	private void setupDropZone() {
		dropZone.setAlignment(Pos.CENTER);
		dropZone.getStyleClass().add("drop-zone");
		VBox.setVgrow(dropZone, Priority.ALWAYS);

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
	}

	private void setupCapes() {
		capes.setMaxWidth(Double.MAX_VALUE);
		capes.setConverter(new StringConverter<>() {
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
			InputStream logo = AccountView.class.getResourceAsStream("/com/vfxclient/launcher/logo-256.png");
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
		if (microsoft) {
			ctx.runAsync(() -> loadMicrosoftProfile(account));
		}
	}

	/** Conta original: renova o login se preciso, baixa a skin atual e a lista de capas. */
	private void loadMicrosoftProfile(Account account) {
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
			LauncherContext.ui(() -> showCapes(account, list));
		} catch (IOException | RuntimeException e) {
			LauncherContext.ui(() -> preview.setSkin(currentSkinImage(), slim.isSelected()));
		}
	}

	private void showCapes(Account account, List<MinecraftServices.Cape> list) {
		preview.setSkin(currentSkinImage(), "slim".equals(account.skinModel));

		// Sem o listener enquanto a lista é trocada, para não "aplicar" a capa atual de novo.
		capes.setOnAction(null);
		capes.getItems().setAll(list);
		capes.getItems().add(0, null);
		capes.setValue(list.stream().filter(MinecraftServices.Cape::active).findFirst().orElse(null));
		capes.setOnAction(e -> applyCape());
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
		if (!isSkinImage(file)) {
			return;
		}

		account.skinModel = slim.isSelected() ? "slim" : "classic";
		if (account.type == Account.Type.OFFLINE) {
			saveLocalSkin(account, file);
		} else {
			uploadSkin(account, file);
		}
	}

	/** Confere se o arquivo é uma skin (PNG 64x64 ou o formato antigo 64x32); se não for, avisa. */
	private boolean isSkinImage(File file) {
		try {
			BufferedImage image = ImageIO.read(file);
			if (image == null || image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
				Motion.toast(message, "Use um PNG de skin do Minecraft (64x64).");
				return false;
			}
			return true;
		} catch (IOException e) {
			Motion.toast(message, "Não deu para ler a imagem.");
			return false;
		}
	}

	private void saveLocalSkin(Account account, File file) {
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
	}

	private void uploadSkin(Account account, File file) {
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
