package com.vfxclient.core.module.visual;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.KeySetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.input.Keyboard;

/** Aproxima a visão enquanto a tecla estiver segurada. */
public class ZoomModule extends Module {

	private final KeySetting key = add(new KeySetting("key", "Tecla do zoom", Keyboard.KEY_C));
	private final NumberSetting factor = add(new NumberSetting("factor", "Aproximação", 4, 2, 10, 0.5, "x"));
	private final BooleanSetting smooth = add(new BooleanSetting("smooth", "Câmera suave no zoom", true));

	private boolean zooming;
	private boolean previousSmooth;

	public ZoomModule() {
		super("zoom", "Zoom", "Segure a tecla para aproximar", Category.VISUAL, true);
	}

	@Override
	public void onTick(MinecraftClient client) {
		boolean active = client.currentScreen == null && key.isDown();
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
	}

	/**
	 * Multiplicador do campo de visão (usado pelo mixin do GameRenderer).
	 *
	 * @return {@code 1} sem zoom, menor que 1 com zoom
	 */
	public float fovMultiplier() {
		if (!isEnabled()) {
			return 1F;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen != null || !key.isDown()) {
			return 1F;
		}
		return (float) (1.0 / factor.get());
	}
}
