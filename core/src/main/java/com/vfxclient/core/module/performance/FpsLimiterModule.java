package com.vfxclient.core.module.performance;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.NumberSetting;
import org.lwjgl.opengl.Display;

/**
 * Limita o FPS quando a janela do jogo não está em foco (alt-tab, minimizado).
 *
 * <p>Libera CPU/GPU para o resto do computador e esfria a máquina.</p>
 */
public class FpsLimiterModule extends Module {

	private final NumberSetting unfocused = add(new NumberSetting("unfocused", "FPS sem foco", 15, 5, 60, 5, " FPS"));

	public FpsLimiterModule() {
		super("fpsLimiter", "FPS sem foco", "Economiza PC fora do jogo", Category.PERFORMANCE, true);
	}

	/**
	 * Ajusta o limite de FPS calculado pelo Minecraft.
	 *
	 * @param vanilla limite original
	 * @return limite a usar
	 */
	public int adjust(int vanilla) {
		if (!isEnabled() || Display.isActive()) {
			return vanilla;
		}
		return Math.min(vanilla, unfocused.getInt());
	}
}
