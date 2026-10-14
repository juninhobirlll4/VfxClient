package com.vfxclient.core.module.performance;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/**
 * Detalhe dos blocos especiais por distância: texto das placas só de perto, banners e cabeças
 * com distância própria e baús sem a animação da tampa.
 */
public class BlockDetailsModule extends Module {

	private final NumberSetting signText = add(new NumberSetting("signText", "Texto das placas até", 16, 4, 64, 4, " blocos"));
	private final NumberSetting banners = add(new NumberSetting("banners", "Banners até", 32, 8, 64, 4, " blocos"));
	private final NumberSetting skulls = add(new NumberSetting("skulls", "Cabeças até", 24, 8, 64, 4, " blocos"));
	private final BooleanSetting chestAnimation = add(new BooleanSetting("noChestAnimation", "Baús sem animação da tampa", false));

	public BlockDetailsModule() {
		super("blockDetails", "Placas e blocos", "Texto das placas, banners e cabeças só de perto", Category.PERFORMANCE, false);
	}

	/**
	 * @param squaredDistance distância da placa até a câmera, ao quadrado
	 * @return {@code true} se o texto da placa deve ser pulado
	 */
	public boolean hideSignText(double squaredDistance) {
		double max = signText.get();
		return isEnabled() && squaredDistance > max * max;
	}

	/**
	 * @param blockEntity bloco especial que o jogo vai desenhar
	 * @return {@code true} se é um banner ou cabeça além da distância escolhida
	 */
	public boolean shouldSkip(BlockEntity blockEntity) {
		if (!isEnabled()) {
			return false;
		}
		double max;
		if (blockEntity instanceof BannerBlockEntity) {
			max = banners.get();
		} else if (blockEntity instanceof SkullBlockEntity) {
			max = skulls.get();
		} else {
			return false;
		}
		return squaredDistance(blockEntity.getPos()) > max * max;
	}

	/** @return se a tampa dos baús deve ficar parada (fechada) */
	public boolean freezeChests() {
		return isEnabled() && chestAnimation.get();
	}

	private static double squaredDistance(BlockPos pos) {
		Entity camera = MinecraftClient.getInstance().getCameraEntity();
		if (camera == null) {
			return 0;
		}
		double dx = pos.getX() + 0.5 - camera.x;
		double dy = pos.getY() + 0.5 - camera.y;
		double dz = pos.getZ() + 0.5 - camera.z;
		return dx * dx + dy * dy + dz * dz;
	}
}
