package com.vfxclient.core.module.visual;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.render.PostEffect;
import net.minecraft.client.MinecraftClient;

/**
 * Motion blur: cada quadro deixa um rastro suave do anterior. Usa o efeito "phosphor" que
 * vem no Minecraft. Custa um pouco de FPS; desligado por padrão.
 */
public class MotionBlurModule extends Module {

	private final NumberSetting intensity = add(new NumberSetting("intensity", "Intensidade", 40, 10, 85, 5, "%"));
	private final PostEffect effect = new PostEffect("shaders/post/phosphor.json");

	public MotionBlurModule() {
		super("motionBlur", "Motion blur", "Rastro suave no movimento", Category.VISUAL, false);
	}

	/**
	 * Chamado pelo mixin depois que o mundo é desenhado.
	 *
	 * @param client    instância do Minecraft
	 * @param tickDelta fração do tick
	 */
	public void render(MinecraftClient client, float tickDelta) {
		if (isEnabled()) {
			float v = intensity.get().floatValue() / 100F;
			effect.render(client, tickDelta, "Phosphor", v, v, v);
		}
	}

	@Override
	protected void onDisable() {
		effect.close();
	}
}