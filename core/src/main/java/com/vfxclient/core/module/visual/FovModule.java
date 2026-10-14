package com.vfxclient.core.module.visual;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

/**
 * FOV: campo de visão fixo (não muda ao correr, com velocidade ou lentidão) e valor próprio,
 * inclusive acima do limite das opções do jogo.
 */
public class FovModule extends Module {

	private final BooleanSetting staticFov = add(new BooleanSetting("static", "Não mudar ao correr ou com efeitos", true));
	private final BooleanSetting custom = add(new BooleanSetting("custom", "Usar FOV próprio", false));
	private final NumberSetting fov = add(new NumberSetting("fov", "FOV próprio", 90, 30, 130, 1, ""));

	public FovModule() {
		super("fov", "FOV", "Campo de visão fixo ou personalizado", Category.VISUAL, false);
	}

	/** @return se o FOV não deve mudar com movimento e efeitos */
	public boolean staticFov() {
		return isEnabled() && staticFov.get();
	}

	/**
	 * Ajusta o FOV calculado pelo jogo para o valor próprio.
	 *
	 * @param value FOV calculado
	 * @return FOV a usar
	 */
	public float adjust(float value) {
		if (!isEnabled() || !custom.get()) {
			return value;
		}
		float base = MinecraftClient.getInstance().options.fov;
		return base <= 0 ? value : value * fov.get().floatValue() / base;
	}
}