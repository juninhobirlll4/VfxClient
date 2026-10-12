package com.vfxclient.core.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Opção de tecla (código LWJGL). {@link #NONE} significa "sem tecla".
 *
 * <p>Botões do mouse seguem a convenção do Minecraft: código negativo
 * ({@code -100} = esquerdo, {@code -99} = direito, {@code -98} = meio...).</p>
 */
public class KeySetting extends Setting<Integer> {

	/** Valor para "nenhuma tecla". */
	public static final int NONE = 0;

	/**
	 * @param id           chave do JSON
	 * @param name         nome exibido
	 * @param defaultValue código LWJGL padrão
	 */
	public KeySetting(String id, String name, int defaultValue) {
		super(id, name, defaultValue);
	}

	/** @return se a tecla está pressionada agora */
	public boolean isDown() {
		int code = value;
		if (code == NONE) {
			return false;
		}
		if (code < 0) {
			return Mouse.isButtonDown(code + 100);
		}
		return Keyboard.isKeyDown(code);
	}

	/** @return nome legível da tecla, ex.: {@code RSHIFT} ou {@code Nenhuma} */
	public String format() {
		return describe(value);
	}

	/**
	 * Nome legível de um código de tecla.
	 *
	 * @param code código LWJGL
	 * @return nome da tecla
	 */
	public static String describe(int code) {
		if (code == NONE) {
			return "Nenhuma";
		}
		if (code < 0) {
			return "Mouse " + (code + 101);
		}
		String name = Keyboard.getKeyName(code);
		return name == null ? "Tecla " + code : name;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			set(json.getAsInt());
		}
	}
}
