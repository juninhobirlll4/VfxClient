package com.vfxclient.core.module.performance;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.FireworkRocketEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;

/**
 * Não desenha entidades que só enfeitam: suportes de armadura, molduras, orbes de XP, fogos e,
 * se quiser, itens no chão. Pesa bastante em lobbies cheios de decoração.
 */
public class HideDecorationsModule extends Module {

	private final BooleanSetting armorStands = add(new BooleanSetting("armorStands", "Suportes de armadura", true));
	private final BooleanSetting keepHolograms = add(new BooleanSetting("keepHolograms", "Manter textos flutuantes (hologramas)", true));
	private final BooleanSetting itemFrames = add(new BooleanSetting("itemFrames", "Molduras de item", true));
	private final BooleanSetting xpOrbs = add(new BooleanSetting("xpOrbs", "Orbes de experiência", true));
	private final BooleanSetting fireworks = add(new BooleanSetting("fireworks", "Fogos de artifício", true));
	private final BooleanSetting droppedItems = add(new BooleanSetting("droppedItems", "Itens no chão", false));

	public HideDecorationsModule() {
		super("hideDecorations", "Esconder decorações", "Suportes de armadura, molduras, XP e fogos somem", Category.PERFORMANCE, false);
	}

	/**
	 * @param entity entidade que o jogo vai desenhar
	 * @return {@code true} se deve ser pulada
	 */
	public boolean shouldSkip(Entity entity) {
		if (!isEnabled()) {
			return false;
		}
		if (entity instanceof ArmorStandEntity) {
			return armorStands.get() && !(keepHolograms.get() && isHologram((ArmorStandEntity) entity));
		}
		if (entity instanceof ItemFrameEntity) {
			return itemFrames.get();
		}
		if (entity instanceof ExperienceOrbEntity) {
			return xpOrbs.get();
		}
		if (entity instanceof FireworkRocketEntity) {
			return fireworks.get();
		}
		return entity instanceof ItemEntity && droppedItems.get();
	}

	/** Servidores fazem textos flutuantes com suportes invisíveis que têm nome. */
	private static boolean isHologram(ArmorStandEntity stand) {
		return stand.isInvisible() && stand.hasCustomName();
	}
}
