package com.vfxclient.core.module.util;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.ColorSetting;
import com.vfxclient.core.module.setting.NumberSetting;

/** Muda a cor que jogadores e mobs ficam ao levar dano (o "vermelho" do hit). */
public class HitColorModule extends Module {

	private final ColorSetting color = add(new ColorSetting("color", "Cor do hit", ColorSetting.BRAND));
	private final NumberSetting strength = add(new NumberSetting("strength", "Intensidade", 30, 5, 90, 5, "%"));

	public HitColorModule() {
		super("hitColor", "Cor do hit", "Cor ao levar dano", Category.UTILITY, false);
	}

	/**
	 * Componente da cor no formato que o jogo usa (0 a 1).
	 *
	 * @param index 0 = vermelho, 1 = verde, 2 = azul, 3 = intensidade
	 * @param vanilla valor original do jogo
	 * @return valor a usar
	 */
	public float component(int index, float vanilla) {
		if (!isEnabled()) {
			return vanilla;
		}
		int rgb = color.rgb();
		switch (index) {
			case 0:
				return ((rgb >> 16) & 0xFF) / 255F;
			case 1:
				return ((rgb >> 8) & 0xFF) / 255F;
			case 2:
				return (rgb & 0xFF) / 255F;
			default:
				return strength.get().floatValue() / 100F;
		}
	}
}
