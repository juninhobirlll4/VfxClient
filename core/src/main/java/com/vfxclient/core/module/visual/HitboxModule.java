package com.vfxclient.core.module.visual;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ColorSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Hitbox: a caixa de colisão das entidades (a mesma do F3+B do jogo), com cor e espessura
 * próprias e opção de mostrar só a dos jogadores.
 */
public class HitboxModule extends Module {

	private final ColorSetting color = add(new ColorSetting("color", "Cor", 0xFFFFFFFF));
	private final NumberSetting width = add(new NumberSetting("width", "Espessura", 2, 1, 5, 1, " px"));
	private final BooleanSetting onlyPlayers = add(new BooleanSetting("onlyPlayers", "Só jogadores", true));
	private final BooleanSetting eyeLine = add(new BooleanSetting("eyeLine", "Linha dos olhos", false));

	public HitboxModule() {
		super("hitbox", "Hitbox", "Caixa de colisão com cor e espessura", Category.VISUAL, false);
	}

	@Override
	public void onTick(MinecraftClient client) {
		client.getEntityRenderManager().setRenderHitboxes(true);
	}

	@Override
	protected void onDisable() {
		MinecraftClient.getInstance().getEntityRenderManager().setRenderHitboxes(false);
	}

	/**
	 * @param entity entidade
	 * @return se a caixa dessa entidade deve aparecer
	 */
	public boolean shows(Entity entity) {
		return !isEnabled() || !onlyPlayers.get() || entity instanceof PlayerEntity;
	}

	/** @return cor da caixa (ARGB) */
	public int color() {
		return color.argb();
	}

	/** @return espessura da linha */
	public float lineWidth() {
		return width.get().floatValue();
	}

	/** @return se a linha dos olhos aparece */
	public boolean eyeLine() {
		return eyeLine.get();
	}
}