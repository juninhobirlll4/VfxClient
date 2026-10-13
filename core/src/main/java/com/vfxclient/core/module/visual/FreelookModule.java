package com.vfxclient.core.module.visual;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.KeySetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.lwjgl.input.Keyboard;

/**
 * Freelook: segura a tecla e a câmera gira em volta do jogador (terceira pessoa) sem
 * mudar para onde o personagem olha nem anda.
 *
 * <p>Desligado por padrão: alguns servidores proíbem freelook. Confira as regras do servidor
 * antes de usar.</p>
 */
public class FreelookModule extends Module {

	private static final int HOLD = 0;

	private final KeySetting key = add(new KeySetting("key", "Tecla", Keyboard.KEY_LMENU));
	private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Modo", HOLD, "Segurar", "Alternar"));

	private boolean active;
	private boolean wasDown;
	private int previousPerspective;
	private float yaw;
	private float pitch;

	/** Rotação real do jogador guardada enquanto a câmera usa a do freelook. */
	private float savedYaw;
	private float savedPitch;
	private float savedPrevYaw;
	private float savedPrevPitch;

	public FreelookModule() {
		super("freelook", "Freelook", "Olhar em volta sem virar (veja as regras do servidor)", Category.VISUAL, false);
	}

	@Override
	public void onTick(MinecraftClient client) {
		boolean down = client.currentScreen == null && key.isDown();
		boolean want;
		if (mode.get() == HOLD) {
			want = down;
		} else {
			want = active;
			if (down && !wasDown) {
				want = !active;
			}
		}
		wasDown = down;
		if (client.player == null) {
			want = false;
		}
		if (want && !active) {
			start(client);
		} else if (!want && active) {
			stop(client);
		}
	}

	@Override
	protected void onDisable() {
		if (active) {
			stop(MinecraftClient.getInstance());
		}
	}

	private void start(MinecraftClient client) {
		active = true;
		previousPerspective = client.options.perspective;
		client.options.perspective = 1;
		yaw = client.player.yaw;
		pitch = client.player.pitch;
	}

	private void stop(MinecraftClient client) {
		active = false;
		client.options.perspective = previousPerspective;
	}

	/**
	 * Recebe o movimento do mouse no lugar do jogador.
	 *
	 * @param dx movimento horizontal (mesma unidade do jogo)
	 * @param dy movimento vertical
	 * @return {@code true} se o freelook usou o movimento (o jogador não deve girar)
	 */
	public boolean consumeMouse(float dx, float dy) {
		if (!active) {
			return false;
		}
		yaw += dx * 0.15F;
		pitch = Math.max(-90F, Math.min(90F, pitch - dy * 0.15F));
		return true;
	}

	/**
	 * Antes de posicionar a câmera: troca a rotação da entidade pela do freelook.
	 *
	 * @param camera entidade da câmera
	 */
	public void beginCamera(Entity camera) {
		if (!active || camera == null) {
			return;
		}
		savedYaw = camera.yaw;
		savedPitch = camera.pitch;
		savedPrevYaw = camera.prevYaw;
		savedPrevPitch = camera.prevPitch;
		camera.yaw = yaw;
		camera.prevYaw = yaw;
		camera.pitch = pitch;
		camera.prevPitch = pitch;
	}

	/**
	 * Depois de posicionar a câmera: devolve a rotação real (mira, movimento e modelo).
	 *
	 * @param camera entidade da câmera
	 */
	public void endCamera(Entity camera) {
		if (!active || camera == null) {
			return;
		}
		camera.yaw = savedYaw;
		camera.pitch = savedPitch;
		camera.prevYaw = savedPrevYaw;
		camera.prevPitch = savedPrevPitch;
	}
}
