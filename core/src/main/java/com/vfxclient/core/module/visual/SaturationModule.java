package com.vfxclient.core.module.visual;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.render.PostEffect;
import net.minecraft.client.MinecraftClient;

/** Cores mais vivas (ou mais apagadas), sem shader pack: usa o efeito "color_convolve" do Minecraft. */
public class SaturationModule extends Module {

	private final NumberSetting amount = add(new NumberSetting("amount", "Saturação", 130, 0, 200, 5, "%"));
	private final PostEffect effect = new PostEffect("shaders/post/color_convolve.json");

	public SaturationModule() {
		super("saturation", "Saturação de cor", "Cores mais vivas", Category.VISUAL, false);
	}

	/**
	 * Chamado pelo mixin depois que o mundo é desenhado.
	 *
	 * @param client    instância do Minecraft
	 * @param tickDelta fração do tick
	 */
	public void render(MinecraftClient client, float tickDelta) {
		if (isEnabled()) {
			effect.render(client, tickDelta, "Saturation", amount.get().floatValue() / 100F);
		}
	}

	@Override
	protected void onDisable() {
		effect.close();
	}
}