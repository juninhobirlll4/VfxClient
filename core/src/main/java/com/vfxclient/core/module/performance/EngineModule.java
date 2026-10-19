package com.vfxclient.core.module.performance;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;

/**
 * Motor de desempenho: quatro técnicas que trabalham no próprio render do jogo, todas opcionais.
 *
 * <ul>
 *   <li><b>FPS Boost</b> (qualidade dinâmica): quando o FPS cai abaixo do limite, corta enfeites
 *       (animações de textura, chuva, brilho de encantamento) e volta quando o FPS se recupera;</li>
 *   <li><b>World Optimization</b>: tira as sombras de entidades;</li>
 *   <li><b>Lazy Chunk Loading</b>: limita o tempo por quadro gasto enviando chunks à placa de vídeo;</li>
 *   <li><b>GPU Fencing</b>: espera a placa terminar o quadro anterior (ARB_sync) para não deixar
 *       quadros acumulados na fila, o que reduz o atraso entre o mouse e a tela.</li>
 * </ul>
 *
 * <p>Convive com o OptiFine (Fast Render, Fast Math etc. ficam por conta dele).</p>
 */
public class EngineModule extends Module {

	private static final int ENTER_TICKS = 30;
	private static final int LEAVE_TICKS = 80;
	private static final int RECOVERY_MARGIN = 15;
	private static final long FENCE_TIMEOUT_NANOS = 50_000_000L;

	private final BooleanSetting dynamic = add(new BooleanSetting("dynamic", "FPS Boost: qualidade dinâmica", true));
	private final NumberSetting threshold = add(new NumberSetting("threshold", "Ligar abaixo de", 45, 20, 120, 5, " FPS"));
	private final BooleanSetting dynAnimations = addAdvanced(new BooleanSetting("dynAnimations", "Boost avançado: pausar texturas animadas", true));
	private final BooleanSetting dynWeather = addAdvanced(new BooleanSetting("dynWeather", "Boost avançado: sem chuva e neve", true));
	private final BooleanSetting dynGlint = addAdvanced(new BooleanSetting("dynGlint", "Boost avançado: sem brilho de encantamento", true));
	private final BooleanSetting dynChunks = addAdvanced(new BooleanSetting("dynChunks", "Boost avançado: carregar chunks mais devagar", true));
	private final BooleanSetting shadows = add(new BooleanSetting("shadows", "World Optimization: sem sombra de entidades", false));
	private final BooleanSetting lazyChunks = add(new BooleanSetting("lazyChunks", "Lazy Chunk Loading: chunks aos poucos", false));
	private final NumberSetting lazyMs = addAdvanced(new NumberSetting("lazyMs", "Tempo por quadro para chunks", 2, 1, 10, 1, " ms"));
	private final BooleanSetting fence = add(new BooleanSetting("fence", "GPU Fencing: menos atraso de entrada", false));

	private boolean low;
	private int slowTicks;
	private int fastTicks;
	private GLSync pending;
	private boolean fenceBroken;

	public EngineModule() {
		super("engine", "Motor de FPS", "FPS Boost, otimização do mundo, chunks aos poucos e sincronização com a GPU",
				Category.PERFORMANCE, false);
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (!dynamic.get() || client.world == null) {
			low = false;
			slowTicks = 0;
			fastTicks = 0;
			return;
		}

		int fps = MinecraftClient.getCurrentFps();
		if (fps <= 0) {
			return;
		}

		// Histerese: entra depois de ~1,5 s lento e só sai depois de ~4 s bem acima do limite.
		if (fps < threshold.getInt()) {
			slowTicks++;
			fastTicks = 0;
		} else if (fps > threshold.getInt() + RECOVERY_MARGIN) {
			fastTicks++;
			slowTicks = 0;
		}

		if (!low && slowTicks >= ENTER_TICKS) {
			low = true;
		} else if (low && fastTicks >= LEAVE_TICKS) {
			low = false;
		}
	}

	@Override
	public void onDisable() {
		low = false;
		releaseFence();
	}

	/** @return se o FPS Boost está cortando enfeites agora */
	private boolean boosting() {
		return isEnabled() && low;
	}

	/** @return {@code true} se as texturas animadas devem parar agora */
	public boolean pauseAnimations() {
		return boosting() && dynAnimations.get();
	}

	/** @return {@code true} se chuva e neve devem sumir agora */
	public boolean hideWeather() {
		return boosting() && dynWeather.get();
	}

	/** @return {@code true} se o brilho de encantamento deve sumir agora */
	public boolean hideGlint() {
		return boosting() && dynGlint.get();
	}

	/** @return {@code true} se as sombras de entidades devem sumir */
	public boolean noShadows() {
		return isEnabled() && shadows.get();
	}

	/**
	 * Limite de tempo por quadro para enviar chunks à placa de vídeo.
	 *
	 * @return nanossegundos, ou {@code 0} para o padrão do jogo
	 */
	public long chunkBudgetNanos() {
		if (!isEnabled()) {
			return 0L;
		}

		long budget = 0L;
		if (lazyChunks.get()) {
			budget = lazyMs.getInt() * 1_000_000L;
		}
		if (boosting() && dynChunks.get()) {
			budget = budget > 0 ? Math.min(budget, 1_000_000L) : 1_000_000L;
		}
		return budget;
	}

	/** Depois de trocar o quadro: espera o quadro anterior terminar na placa e marca o atual. */
	public void afterFrame() {
		if (!isEnabled() || !fence.get() || fenceBroken) {
			releaseFence();
			return;
		}

		try {
			if (!GLContext.getCapabilities().GL_ARB_sync) {
				fenceBroken = true;
				VfxCore.LOGGER.info("GPU Fencing: a placa de vídeo não tem ARB_sync, técnica ignorada.");
				return;
			}
			if (pending != null) {
				ARBSync.glClientWaitSync(pending, ARBSync.GL_SYNC_FLUSH_COMMANDS_BIT, FENCE_TIMEOUT_NANOS);
				ARBSync.glDeleteSync(pending);
			}
			pending = ARBSync.glFenceSync(ARBSync.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		} catch (RuntimeException e) {
			// Driver sem suporte de verdade: desliga só esta técnica.
			fenceBroken = true;
			pending = null;
			VfxCore.LOGGER.warn("GPU Fencing indisponível: {}", e.toString());
		}
	}

	private void releaseFence() {
		if (pending == null) {
			return;
		}
		try {
			ARBSync.glDeleteSync(pending);
		} catch (RuntimeException ignored) {
			// Contexto já fechado.
		}
		pending = null;
	}
}
