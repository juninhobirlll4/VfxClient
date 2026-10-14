package com.vfxclient.core.module.util;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;

/** Ajustes do placar lateral (scoreboard) dos servidores: esconder, sem números e tamanho menor (compacto). */
public class ScoreboardModule extends Module {

	private final BooleanSetting hide = add(new BooleanSetting("hide", "Esconder o placar", false));
	private final BooleanSetting hideNumbers = add(new BooleanSetting("hideNumbers", "Esconder números vermelhos", true));
	private final NumberSetting size = add(new NumberSetting("size", "Tamanho (compacto)", 1.0, 0.5, 1.0, 0.05, "x"));

	public ScoreboardModule() {
		super("scoreboard", "Placar", "Compacto, sem números ou escondido", Category.UTILITY, false);
	}

	/** @return se o placar inteiro deve sumir */
	public boolean hideAll() {
		return isEnabled() && hide.get();
	}

	/** @return escala do placar (1 = tamanho normal) */
	public float scale() {
		return isEnabled() ? size.get().floatValue() : 1F;
	}

	/** @return se os números vermelhos devem sumir */
	public boolean hideNumbers() {
		return isEnabled() && hideNumbers.get();
	}
}
