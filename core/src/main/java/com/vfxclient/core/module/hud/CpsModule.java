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
		if (showRight.get()) {
			return ClickTracker.leftCps() + " | " + ClickTracker.rightCps() + " CPS";
		}
		return ClickTracker.leftCps() + " CPS";
	}

	@Override
	public String getSampleText() {
		return "12 | 4 CPS";
	}
}
