package com.vfxclient.core.module.general;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.VfxPaths;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.ChoiceSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Skin local: usa {@code .minecraft/vfxclient/skins/skin.png} como a sua skin.
 *
 * <p>Pensado para contas offline (sem skin oficial). O launcher grava o arquivo quando você
 * escolhe uma skin. Só você vê essa skin até existir o servidor de skins do VfxClient.</p>
 */
public class LocalSkinModule extends Module {

	private final ChoiceSetting model = add(new ChoiceSetting("model", "Modelo do braço", 0, "Clássico (4 px)", "Fino (3 px)"));

	private Identifier texture;
	private long loadedModified = -1L;
	private long lastCheck;

	public LocalSkinModule() {
		super("localSkin", "Skin local", "Usa a skin escolhida no launcher", Category.GENERAL, true);
	}

	/** @return caminho do arquivo da skin */
	public static Path skinFile() {
		return VfxPaths.root().resolve("skins").resolve("skin.png");
	}

	/**
	 * Textura da skin local para o jogador indicado.
	 *
	 * @param uuid id do jogador sendo desenhado
	 * @return a textura, ou {@code null} para usar a skin normal
	 */
	public Identifier textureFor(UUID uuid) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!isEnabled() || client.player == null || uuid == null || !uuid.equals(client.player.getUuid())) {
			return null;
		}
		reloadIfChanged();
		return texture;
	}

	/** @return {@code "slim"} ou {@code "default"} conforme a opção */
	public String modelName() {
		return model.get() == 1 ? "slim" : "default";
	}

	private void reloadIfChanged() {
		long now = System.currentTimeMillis();
		if (now - lastCheck < 2000L && texture != null) {
			return;
		}

		lastCheck = now;
		Path file = skinFile();
		try {
			if (!Files.exists(file)) {
				texture = null;
				return;
			}
			long modified = Files.getLastModifiedTime(file).toMillis();
			if (modified == loadedModified && texture != null) {
				return;
			}
			BufferedImage image = ImageIO.read(file.toFile());
			if (image == null || image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
				VfxCore.LOGGER.warn("Skin local inválida (precisa ser PNG 64x64 ou 64x32): {}", file);
				texture = null;
				loadedModified = modified;
				return;
			}
			texture = MinecraftClient.getInstance().getTextureManager()
					.registerDynamicTexture("vfx_local_skin", new NativeImageBackedTexture(image));
			loadedModified = modified;
		} catch (Exception e) {
			VfxCore.LOGGER.warn("Não foi possível carregar a skin local", e);
			texture = null;
		}
	}
}
