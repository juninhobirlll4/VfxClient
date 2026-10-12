package com.vfxclient.core.module.util;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

/**
 * Sprint automático (correr sem segurar a tecla) e agachar alternado.
 *
 * <p>Funciona fazendo o jogo "achar" que a tecla de correr (ou agachar) está pressionada,
 * do mesmo jeito que um jogador segurando a tecla. Opcionalmente mostra o estado na tela.</p>
 */
public class ToggleSprintModule extends TextHudModule {

	private final BooleanSetting alwaysSprint = add(new BooleanSetting("alwaysSprint", "Sempre correr", true));
	private final BooleanSetting toggleSneak = add(new BooleanSetting("toggleSneak", "Agachar alternado (aperte 1x)", false));
	private final BooleanSetting showStatus = add(new BooleanSetting("showStatus", "Mostrar estado na tela", true));

	private boolean sprintToggled = true;
	private boolean sneakToggled;

	public ToggleSprintModule() {
		super("toggleSprint", "Sprint automático", "Corre sem segurar a tecla", Category.UTILITY, true, 4, 196);
	}

	/**
	 * Chamado a cada tecla apertada no jogo: alterna os estados.
	 *
	 * @param keyCode código da tecla
	 */
	public void onKeyPressed(int keyCode) {
		if (!isEnabled()) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (keyCode == client.options.sprintKey.getCode()) {
			sprintToggled = !sprintToggled;
		} else if (keyCode == client.options.sneakKey.getCode() && toggleSneak.get()) {
			sneakToggled = !sneakToggled;
		}
	}

	/**
	 * Usado pelo mixin de {@code KeyBinding.isPressed}: diz se a tecla deve contar como apertada.
	 *
	 * @param binding tecla consultada pelo jogo
	 * @return {@code true} para forçar "apertada"
	 */
	public boolean forcePressed(KeyBinding binding) {
		if (!isEnabled()) {
			return false;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen != null || client.player == null) {
			return false;
		}
		if (binding == client.options.sprintKey) {
			return alwaysSprint.get() && sprintToggled && client.options.forwardKey.isPressed();
		}
		if (binding == client.options.sneakKey) {
			return toggleSneak.get() && sneakToggled;
		}
		return false;
	}

	@Override
	protected void onDisable() {
		sneakToggled = false;
	}

	@Override
	public String getText(MinecraftClient client) {
		if (!showStatus.get() || client.player == null) {
			return null;
		}
		if (toggleSneak.get() && sneakToggled) {
			return "[Agachado (alternado)]";
		}
		if (client.player.isSprinting()) {
			return alwaysSprint.get() && sprintToggled ? "[Correndo (automático)]" : "[Correndo]";
		}
		return null;
	}

	@Override
	public String getSampleText() {
		return "[Correndo (automático)]";
	}
}
