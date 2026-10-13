package com.vfxclient.core.module.general;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.KeySetting;
import org.lwjgl.input.Keyboard;

/** Configurações do próprio menu do VfxClient. Sempre ligado. */
public class MenuModule extends Module {

	private final KeySetting key = add(new KeySetting("key", "Tecla do menu", Keyboard.KEY_RSHIFT));
	private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Opções no menu", 0, "Simples", "Completo"));

	/** @return {@code true} quando o menu deve mostrar também as opções avançadas */
	public boolean showAdvanced() {
		return mode.get() == 1;
	}

	public MenuModule() {
		super("menu", "Menu", "Tecla que abre este menu", Category.GENERAL, true);
	}

	@Override
	public boolean isAlwaysOn() {
		return true;
	}

	/** @return código da tecla que abre a Click GUI */
	public int getKey() {
		int code = key.get();
		return code == KeySetting.NONE ? Keyboard.KEY_RSHIFT : code;
	}

	/**
	 * Define a tecla (usado para migrar configs antigas).
	 *
	 * @param code código LWJGL
	 */
	public void setKey(int code) {
		key.set(code);
	}
}
