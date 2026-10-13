package com.vfxclient.core.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Opção de escolha entre valores; na Click GUI, clique esquerdo avança e direito volta.
 *
 * <p>Para listas que mudam (ex.: perfis de configuração), sobrescreva {@link #options()}.</p>
 */
public class ChoiceSetting extends Setting<Integer> {

	private final String[] fixedOptions;

	/**
	 * @param id           chave do JSON
	 * @param name         nome exibido
	 * @param defaultIndex posição padrão em {@code options}
	 * @param options      rótulos exibidos, na ordem
	 */
	public ChoiceSetting(String id, String name, int defaultIndex, String... options) {
		super(id, name, defaultIndex);
		this.fixedOptions = options;
	}

	/** @return rótulos disponíveis agora */
	public String[] options() {
		return fixedOptions;
	}

	@Override
	public void set(Integer value) {
		int count = Math.max(1, options().length);
		int index = value == null ? 0 : value;
		this.value = Math.max(0, Math.min(count - 1, index));
	}

	/**
	 * Avança (ou volta) para a próxima opção, dando a volta no fim da lista.
	 *
	 * @param direction {@code 1} para frente, {@code -1} para trás
	 */
	public void cycle(int direction) {
		int count = Math.max(1, options().length);
		value = ((value + direction) % count + count) % count;
	}

	/** @return rótulo da opção atual */
	public String format() {
		String[] options = options();
		return options.length == 0 ? "-" : options[Math.min(value, options.length - 1)];
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
