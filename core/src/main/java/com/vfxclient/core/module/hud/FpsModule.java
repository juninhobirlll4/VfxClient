package com.vfxclient.core.module.hud;

import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

/**
 * Mostra os quadros por segundo; no modo avançado, também o menor valor dos últimos segundos.
 * Com "cor por faixa": verde a partir de 120, amarelo de 60 a 119 e vermelho abaixo de 60.
 */
public class FpsModule extends TextHudModule {

	private static final long WINDOW_MS = 5000L;

	private final BooleanSetting rangeColor = add(new BooleanSetting("rangeColor", "Cor por faixa (verde/amarelo/vermelho)", false));

	private int minFps = Integer.MAX_VALUE;
	private long windowStart;
	private int lastMin;

	public FpsModule() {
		super("fps", "FPS", "Quadros por segundo", true, 4, 4);
	}

	/**
	 * @param fps quadros por segundo
	 * @return código de cor do Minecraft para a faixa
	 */
	static String rangeCode(int fps) {
		return fps >= 120 ? "§a" : fps >= 60 ? "§e" : "§c";
	}

	private String value(int fps) {
		return rangeColor.get() ? rangeCode(fps) + fps + "§r" : VALUE + String.valueOf(fps) + TEXT;
	}

	private String format(int fps, int min) {
		if (!advanced()) {
			return value(fps) + " FPS";
		}
		return "FPS " + value(fps) + "  §7min " + min;
	}

	@Override
	public String getText(MinecraftClient client) {
		int fps = MinecraftClient.getCurrentFps();
		long now = System.currentTimeMillis();
		minFps = Math.min(minFps, fps);
		if (now - windowStart > WINDOW_MS) {
			lastMin = minFps == Integer.MAX_VALUE ? fps : minFps;
			minFps = Integer.MAX_VALUE;
			windowStart = now;
		}
		return format(fps, lastMin);
	}

	@Override
	public String getSampleText() {
		return format(144, 121);
	}
}
