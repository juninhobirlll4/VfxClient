package com.vfxclient.core.module.hud;

import com.vfxclient.core.module.TextHudModule;
import net.minecraft.client.MinecraftClient;

/** Mostra os quadros por segundo; no modo avançado, também o menor valor dos últimos segundos. */
public class FpsModule extends TextHudModule {

	private static final long WINDOW_MS = 5000L;

	private int minFps = Integer.MAX_VALUE;
	private long windowStart;
	private int lastMin;

	public FpsModule() {
		super("fps", "FPS", "Quadros por segundo", true, 4, 4);
	}

	@Override
	public String getText(MinecraftClient client) {
		int fps = MinecraftClient.getCurrentFps();
		if (!advanced()) {
			return fps + " FPS";
		}
		long now = System.currentTimeMillis();
		minFps = Math.min(minFps, fps);
		if (now - windowStart > WINDOW_MS) {
			lastMin = minFps == Integer.MAX_VALUE ? fps : minFps;
			minFps = Integer.MAX_VALUE;
			windowStart = now;
		}
		return "FPS " + fps + "  §7min " + lastMin;
	}

	@Override
	public String getSampleText() {
		return advanced() ? "FPS 144  §7min 121" : "144 FPS";
	}
}
