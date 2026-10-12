package com.vfxclient.core.module.visual;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.NumberSetting;

/** Abaixa o fogo que cobre a tela quando o jogador está pegando fogo. */
public class LowFireModule extends Module {

	private final NumberSetting height = add(new NumberSetting("height", "Quanto abaixar", 30, 0, 50, 5, "%"));

	public LowFireModule() {
		super("lowFire", "Fogo baixo", "Fogo não cobre a visão", Category.VISUAL, false);
	}

	/** @return deslocamento vertical do fogo, em unidades da cena (0 a 0,5) */
	public float offset() {
		return height.get().floatValue() / 100F;
	}
}
