package com.vfxclient.core.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Opção liga/desliga. */
public class BooleanSetting extends Setting<Boolean> {

	/**
	 * @param id           chave do JSON
	 * @param name         nome exibido
	 * @param defaultValue valor padrão
	 */
	public BooleanSetting(String id, String name, boolean defaultValue) {
		super(id, name, defaultValue);
	}

	/** Inverte o valor atual. */
	public void toggle() {
		set(!value);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) {
			set(json.getAsBoolean());
		}
	}
}
