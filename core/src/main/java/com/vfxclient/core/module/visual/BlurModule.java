package com.vfxclient.core.module.visual;

import com.vfxclient.core.gui.BlurredScreen;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.render.PostEffect;
import net.minecraft.client.MinecraftClient;

/** Desfoque do jogo atrás das telas do VfxClient (menu de pausa). */
public class BlurModule extends Module {

	private final NumberSetting strength = add(new NumberSetting("strength", "Intensidade", 12, 2, 20, 1, ""));
	private final PostEffect effect = new PostEffect("shaders/post/blur.json");

	public BlurModule() {
		super("menuBlur", "Desfoque nos menus", "Jogo desfocado atrás do menu de pausa", Category.VISUAL, true);
	}

	/**
	 * Chamado pelo mixin depois que o mundo é desenhado.
	 *
	 * @param client    instância do Minecraft
	 * @param tickDelta fração do tick
	 */
	public void render(MinecraftClient client, float tickDelta) {
		if (!isEnabled() || !(client.currentScreen instanceof BlurredScreen)) {
			return;
		}
		float progress = ((BlurredScreen) client.currentScreen).blurProgress();
		float radius = Math.max(1F, strength.get().floatValue() * progress);
		effect.render(client, tickDelta, "Radius", radius);
	}

	@Override
	protected void onDisable() {
		effect.close();
	}
}