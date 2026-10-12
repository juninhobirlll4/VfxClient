package com.vfxclient.core.module.performance;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/** Não desenha baús, placas, cabeças, banners e outros blocos especiais além de uma distância. */
public class BlockEntityDistanceModule extends Module {

	private final NumberSetting distance = add(new NumberSetting("distance", "Distância máxima", 32, 8, 64, 4, " blocos"));

	public BlockEntityDistanceModule() {
		super("blockEntityDistance", "Distância de baús", "Baús, placas e cabeças longe somem", Category.PERFORMANCE, false);
	}

	/**
	 * @param blockEntity bloco especial que o jogo vai desenhar
	 * @return {@code true} se deve ser pulado
	 */
	public boolean shouldSkip(BlockEntity blockEntity) {
		if (!isEnabled()) {
			return false;
		}
		Entity camera = MinecraftClient.getInstance().getCameraEntity();
		if (camera == null) {
			return false;
		}
		BlockPos pos = blockEntity.getPos();
		double dx = pos.getX() + 0.5 - camera.x;
		double dy = pos.getY() + 0.5 - camera.y;
		double dz = pos.getZ() + 0.5 - camera.z;
		double max = distance.get();
		return dx * dx + dy * dy + dz * dz > max * max;
	}
}
