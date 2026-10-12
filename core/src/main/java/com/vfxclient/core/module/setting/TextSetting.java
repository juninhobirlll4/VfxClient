package com.vfxclient.core.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Opção de texto livre, editada direto na Click GUI. */
public class TextSetting extends Setting<String> {

	private final int maxLength;

	/**
	 * @param id           chave do JSON
	 * @param name         nome exibido
	 * @param defaultValue texto padrão
	 * @param maxLength    número máximo de caracteres
	 */
	public TextSetting(String id, String name, String defaultValue, int maxLength) {
		super(id, name, defaultValue);
		this.maxLength = maxLength;
	}

	@Override
	public void set(String value) {
		String safe = value == null ? "" : value;
		this.value = safe.length() > maxLength ? safe.substring(0, maxLength) : safe;
	}

	/** @return número máximo de caracteres */
	public int getMaxLength() {
		return maxLength;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
			set(json.getAsString());
		}
	}
}
