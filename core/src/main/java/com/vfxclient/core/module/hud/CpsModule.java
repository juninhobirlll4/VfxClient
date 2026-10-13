package com.vfxclient.core.module.hud;

import com.vfxclient.core.hud.ClickTracker;
import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

/** Mostra os cliques por segundo. */
public class CpsModule extends TextHudModule {

	private final BooleanSetting showRight = add(new BooleanSetting("showRight", "Mostrar botão direito", true));

	public CpsModule() {
		super("cps", "CPS", "Cliques por segundo", false, 4, 76);
	}

	@Override
	public String getText(MinecraftClient client) {
		String value = showRight.get()
				? ClickTracker.leftCps() + " §7|§r " + ClickTracker.rightCps()
				: String.valueOf(ClickTracker.leftCps());
		return advanced() ? "CPS " + value : value + " CPS";
	}

	@Override
	public String getSampleText() {
		String value = showRight.get() ? "12 §7|§r 4" : "12";
		return advanced() ? "CPS " + value : value + " CPS";
	}
}
