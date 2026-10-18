package com.vfxclient.core.module.visual;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.KeySetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

/**
 * Aproxima a visão enquanto a tecla estiver segurada. A aproximação entra e sai com animação, e
 * a rodinha do mouse (com a tecla segurada) aumenta ou diminui o zoom aos poucos.
 */
public class ZoomModule extends Module {

	/** Quanto cada "clique" da rodinha muda o zoom (15%). */
	private static final double SCROLL_STEP = 1.15;
	private static final double MIN_SCROLL_ZOOM = 1.2;
	/** Nível abaixo do qual a animação de saída termina de vez. */
	private static final double DONE = 1.002;

	/** {@code -Dvfx.zoomHeld=true}: o zoom age como se a tecla estivesse segurada (testes). */
	private static final boolean TEST_HELD = Boolean.getBoolean("vfx.zoomHeld");

	private final KeySetting key = add(new KeySetting("key", "Tecla do zoom", Keyboard.KEY_C));
	private final NumberSetting factor = add(new NumberSetting("factor", "Aproximação", 4, 2, 10, 0.5, "x"));
	private final BooleanSetting scroll = add(new BooleanSetting("scroll", "Rodinha do mouse muda o zoom", true));
	private final NumberSetting maxZoom = add(new NumberSetting("maxZoom", "Zoom máximo da rodinha", 20, 4, 40, 1, "x"));
	private final NumberSetting animation = add(new NumberSetting("animation", "Suavidade da animação", 180, 0, 600, 20, " ms"));
	private final BooleanSetting remember = add(new BooleanSetting("remember", "Lembrar o zoom da rodinha", false));
	private final BooleanSetting smooth = add(new BooleanSetting("smooth", "Câmera suave no zoom", true));

	private boolean zooming;
	private boolean previousSmooth;
	private boolean checkedOptiFine;

	/** Zoom escolhido (com a rodinha) enquanto a tecla está segurada. */
	private double wanted;
	/** Zoom que está sendo mostrado agora (anda até o desejado). */
	private double level = 1;
	private boolean wasHeld;
	private long lastFrame;

	public ZoomModule() {
		super("zoom", "Zoom", "Segure a tecla para aproximar; a rodinha ajusta", Category.VISUAL, true);
	}

	/**
	 * Com o OptiFine junto, o zoom dele usa a mesma tecla (C) e os dois somariam. Fica valendo o
	 * nosso (suave e configurável): a tecla de zoom do OptiFine é desligada se for igual à nossa.
	 */
	private void resolveOptiFineConflict(MinecraftClient client) {
		checkedOptiFine = true;
		if (!FabricLoader.getInstance().isModLoaded("optifabric") || client.options == null) {
			return;
		}
		for (KeyBinding binding : client.options.allKeys) {
			if ("of.key.zoom".equals(binding.getTranslationKey()) && binding.getCode() == key.get()) {
				binding.setCode(0);
				KeyBinding.updateKeysByCode();
				VfxCore.LOGGER.info("Zoom do OptiFine desligado (mesma tecla do zoom do VfxClient).");
			}
		}
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (!checkedOptiFine) {
			resolveOptiFineConflict(client);
		}

		boolean active = isHeld(client);
		if (active == zooming) {
			return;
		}

		zooming = active;
		if (smooth.get()) {
			if (active) {
				previousSmooth = client.options.smoothCameraEnabled;
				client.options.smoothCameraEnabled = true;
			} else {
				client.options.smoothCameraEnabled = previousSmooth;
			}
		}
	}

	@Override
	protected void onDisable() {
		if (zooming && smooth.get()) {
			MinecraftClient.getInstance().options.smoothCameraEnabled = previousSmooth;
		}
		zooming = false;
		level = 1;
		lastFrame = 0;
	}

	/**
	 * A rodinha do mouse, chamada pelo jogo no lugar de trocar o item da barra.
	 *
	 * @param delta quanto a rodinha girou (positivo = para cima, aproxima)
	 * @return {@code true} se o zoom usou o movimento (o item da barra não deve trocar)
	 */
	public boolean consumeScroll(int delta) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!isEnabled() || !scroll.get() || delta == 0 || !isHeld(client)) {
			return false;
		}
		double next = delta > 0 ? wanted * SCROLL_STEP : wanted / SCROLL_STEP;
		wanted = Math.max(MIN_SCROLL_ZOOM, Math.min(maxZoom.get(), next));
		return true;
	}

	/**
	 * Multiplicador do campo de visão (usado pelo mixin do GameRenderer; roda uma vez por quadro).
	 *
	 * @return {@code 1} sem zoom, menor que 1 com zoom
	 */
	public float fovMultiplier() {
		if (!isEnabled()) {
			return 1F;
		}

		boolean held = isHeld(MinecraftClient.getInstance());
		if (held && !wasHeld && !(remember.get() && wanted > 1)) {
			wanted = factor.get();
		}
		wasHeld = held;

		double target = held ? wanted : 1;
		if (!held && level <= DONE) {
			level = 1;
			lastFrame = 0;
			return 1F;
		}

		level = approach(level, target);
		return (float) (1.0 / level);
	}

	/** Anda do zoom atual para o desejado, com desaceleração (em escala de zoom, não de ângulo). */
	private double approach(double current, double target) {
		long now = System.nanoTime();
		double seconds = lastFrame == 0 ? 0 : Math.min(0.25, (now - lastFrame) / 1e9);
		lastFrame = now;

		double duration = animation.get() / 1000.0;
		if (duration <= 0) {
			return target;
		}
		// Chega a quase todo o caminho em "duration" segundos.
		double progress = 1 - Math.exp(-seconds / (duration / 3.0));
		return Math.exp(Math.log(current) + (Math.log(target) - Math.log(current)) * progress);
	}

	private boolean isHeld(MinecraftClient client) {
		return client.currentScreen == null && (key.isDown() || TEST_HELD);
	}
}
