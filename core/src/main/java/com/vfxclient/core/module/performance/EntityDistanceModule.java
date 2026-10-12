package com.vfxclient.core.module.performance;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** Não desenha entidades longe da câmera (mobs, itens, molduras...). */
public class EntityDistanceModule extends Module {

	private final NumberSetting distance = add(new NumberSetting("distance", "Distância máxima", 48, 16, 128, 8, " blocos"));
	private final BooleanSetting keepPlayers = add(new BooleanSetting("keepPlayers", "Sempre mostrar jogadores", true));

	public EntityDistanceModule() {
		super("entityDistance", "Distância de entidades", "Esconde entidades distantes", Category.PERFORMANCE, false);
	}

	/**
	 * @param entity entidade que o jogo vai desenhar
	 * @return {@code true} se deve ser pulada
	 */
	public boolean shouldSkip(Entity entity) {
		if (!isEnabled()) {
			return false;
		}
		Entity camera = MinecraftClient.getInstance().getCameraEntity();
		if (camera == null || entity == camera) {
			return false;
		}
		if (keepPlayers.get() && entity instanceof PlayerEntity) {
			return false;
		}
		double max = distance.get();
		return entity.squaredDistanceTo(camera) > max * max;
	}
}
