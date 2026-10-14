package com.vfxclient.core.module.performance;

import java.util.Arrays;

import com.vfxclient.core.command.VfxCommands;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.ButtonSetting;
import net.minecraft.client.MinecraftClient;

/**
 * Mede o FPS por 30 segundos e mostra no chat a média, os 1% de quadros mais lentos (as
 * "travadinhas") e o pior segundo. Serve para comparar antes e depois de mudar opções.
 */
public class FpsTestModule extends Module {

	private static final long DURATION = 30_000_000_000L;
	private static final long SECOND = 1_000_000_000L;
	/** Até 4000 FPS durante os 30 s. */
	private static final int MAX_FRAMES = 120_000;

	private final long[] frameTimes = new long[MAX_FRAMES];
	private int frames;
	private long startedAt = -1;
	private long lastFrame;
	private long secondStart;
	private int framesThisSecond;
	private int worstSecond;
	private String previousResult;

	public FpsTestModule() {
		super("fpsTest", "Teste de FPS", "Mede o FPS por 30 s para comparar opções", Category.PERFORMANCE, true);
		add(new ButtonSetting("start", "Medir o FPS por 30 segundos", "Iniciar", this::start));
	}

	@Override
	public boolean isAlwaysOn() {
		return true;
	}

	private void start() {
		if (MinecraftClient.getInstance().world == null) {
			VfxCommands.say("Entre num mundo ou servidor para medir o FPS.");
			return;
		}
		if (startedAt >= 0) {
			VfxCommands.say("O teste de FPS já está rodando.");
			return;
		}

		frames = 0;
		worstSecond = Integer.MAX_VALUE;
		framesThisSecond = 0;
		startedAt = System.nanoTime();
		lastFrame = startedAt;
		secondStart = startedAt;
		VfxCommands.say("Teste de FPS começou: jogue normalmente por 30 segundos.");
	}

	/** Chamado pelo mixin a cada quadro desenhado. */
	public void onFrame() {
		if (startedAt < 0) {
			return;
		}
		if (MinecraftClient.getInstance().world == null) {
			startedAt = -1;
			return;
		}

		long now = System.nanoTime();
		if (frames < MAX_FRAMES) {
			frameTimes[frames++] = now - lastFrame;
		}
		lastFrame = now;

		framesThisSecond++;
		if (now - secondStart >= SECOND) {
			worstSecond = Math.min(worstSecond, framesThisSecond);
			framesThisSecond = 0;
			secondStart = now;
		}

		if (now - startedAt >= DURATION) {
			finish(now - startedAt);
		}
	}

	private void finish(long elapsed) {
		startedAt = -1;
		int average = (int) Math.round(frames / (elapsed / (double) SECOND));
		String result = "média " + average + " FPS, 1% mais lentos " + slowestPercentFps() + " FPS, pior segundo " + worstSecond + " FPS";

		VfxCommands.say("Teste de FPS: §a" + result);
		if (previousResult != null) {
			VfxCommands.say("Teste anterior: §7" + previousResult);
		}
		previousResult = result;
	}

	/** FPS equivalente à média do 1% de quadros que mais demoraram. */
	private int slowestPercentFps() {
		long[] sorted = Arrays.copyOf(frameTimes, frames);
		Arrays.sort(sorted);
		int count = Math.max(1, frames / 100);

		long total = 0;
		for (int i = frames - count; i < frames; i++) {
			total += sorted[i];
		}
		double averageNanos = total / (double) count;
		return averageNanos <= 0 ? 0 : (int) Math.round(SECOND / averageNanos);
	}
}
