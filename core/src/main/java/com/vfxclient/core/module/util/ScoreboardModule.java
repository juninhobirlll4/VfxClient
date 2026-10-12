package com.vfxclient.core.module.util;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;

/** Ajustes do placar lateral (scoreboard) dos servidores. */
public class ScoreboardModule extends Module {

	private final BooleanSetting hide = add(new BooleanSetting("hide", "Esconder o placar", false));
	private final BooleanSetting hideNumbers = add(new BooleanSetting("hideNumbers", "Esconder números vermelhos", true));

	public ScoreboardModule() {
		super("scoreboard", "Placar", "Esconde os números ou o placar", Category.UTILITY, false);
	}

	/** @return se o placar inteiro deve sumir */
	public boolean hideAll() {
		return isEnabled() && hide.get();
	}

	/** @return se os números vermelhos devem sumir */
	public boolean hideNumbers() {
		return isEnabled() && hideNumbers.get();
	}
}
