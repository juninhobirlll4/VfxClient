package com.vfxclient.core.module.performance;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.Display;

/**
 * FPS dinâmico: limita o FPS quando a janela do jogo não está em foco (alt-tab, minimizado)
 * e quando o jogador fica parado (AFK). Volta ao normal no primeiro movimento ou tecla.
 *
 * <p>Libera CPU/GPU para o resto do computador e esfria a máquina.</p>
 */
public class FpsLimiterModule extends Module {

	private static volatile long lastInput = System.currentTimeMillis();

	private final NumberSetting unfocused = add(new NumberSetting("unfocused", "FPS sem foco", 15, 5, 60, 5, " FPS"));
	private final BooleanSetting afk = add(new BooleanSetting("afk", "Reduzir FPS parado (AFK)", true));
	private final NumberSetting afkMinutes = add(new NumberSetting("afkMinutes", "Parado por", 3, 1, 15, 1, " min"));
	private final NumberSetting afkFps = add(new NumberSetting("afkFps", "FPS parado", 30, 10, 60, 5, " FPS"));

	private double lastX;
	private double lastY;
	private double lastZ;
	private float lastYaw;
	private float lastPitch;

	public FpsLimiterModule() {
		super("fpsLimiter", "FPS dinâmico", "Menos FPS sem foco ou parado", Category.PERFORMANCE, true);
	}

	/** Chamado em qualquer tecla ou clique. */
	public static void markInput() {
		lastInput = System.currentTimeMillis();
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (client.player == null) {
			markInput();
			return;
		}
		if (client.player.x != lastX || client.player.y != lastY || client.player.z != lastZ
				|| client.player.yaw != lastYaw || client.player.pitch != lastPitch || client.currentScreen != null) {
			markInput();
		}
		lastX = client.player.x;
		lastY = client.player.y;
		lastZ = client.player.z;
		lastYaw = client.player.yaw;
		lastPitch = client.player.pitch;
	}

	/**
	 * Ajusta o limite de FPS calculado pelo Minecraft.
	 *
	 * @param vanilla limite original
	 * @return limite a usar
	 */
	public int adjust(int vanilla) {
		if (!isEnabled()) {
			return vanilla;
		}
		if (!Display.isActive()) {
			return Math.min(vanilla, unfocused.getInt());
		}
		if (afk.get() && System.currentTimeMillis() - lastInput > afkMinutes.getInt() * 60000L) {
			return Math.min(vanilla, afkFps.getInt());
		}
		return vanilla;
	}
}