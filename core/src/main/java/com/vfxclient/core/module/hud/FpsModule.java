package com.vfxclient.core.module.hud;

import com.vfxclient.core.module.HudModule;
import net.minecraft.client.MinecraftClient;

/** Mostra os quadros por segundo. */
public class FpsModule extends HudModule {

	public FpsModule() {
		super("fps", "FPS", "Quadros por segundo", true, 4, 4);
	}

	@Override
	public String getText(MinecraftClient client) {
		return MinecraftClient.getCurrentFps() + " FPS";
	}

	@Override
	public String getSampleText() {
		return "144 FPS";
	}
}
