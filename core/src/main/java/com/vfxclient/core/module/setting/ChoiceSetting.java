package com.vfxclient.core.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Opção de escolha entre valores fixos; na Click GUI, cada clique avança para o próximo. */
public class ChoiceSetting extends Setting<Integer> {

	private final String[] options;

	/**
	 * @param id           chave do JSON
	 * @param name         nome exibido
	 * @param defaultIndex posição padrão em {@code options}
	 * @param options      rótulos exibidos, na ordem
	 */
	public ChoiceSetting(String id, String name, int defaultIndex, String... options) {
		super(id, name, defaultIndex);
		this.options = options;
	}

	@Override
	public void set(Integer value) {
		int index = value == null ? 0 : value;
		this.value = Math.max(0, Math.min(options.length - 1, index));
	}

	/**
	 * Avança (ou volta) para a próxima opção, dando a volta no fim da lista.
	 *
	 * @param direction {@code 1} para frente, {@code -1} para trás
	 */
	public void cycle(int direction) {
		value = ((value + direction) % options.length + options.length) % options.length;
	}

	/** @return rótulo da opção atual */
	public String format() {
		return options[value];
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
