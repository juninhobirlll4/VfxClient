package com.vfxclient.core.module.setting;

import com.google.gson.JsonElement;

/**
 * Opção configurável de um módulo, exibida na Click GUI e salva no JSON.
 *
 * @param <T> tipo do valor
 */
public abstract class Setting<T> {

	private final String id;
	private final String name;
	private final T defaultValue;
	protected T value;

	/**
	 * @param id           chave usada no JSON (sem espaços, ex.: {@code background})
	 * @param name         nome exibido na interface
	 * @param defaultValue valor padrão
	 */
	protected Setting(String id, String name, T defaultValue) {
		this.id = id;
		this.name = name;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	/** @return chave do JSON */
	public String getId() {
		return id;
	}

	/** @return nome exibido */
	public String getName() {
		return name;
	}

	/** @return valor atual */
	public T get() {
		return value;
	}

	/**
	 * Altera o valor.
	 *
	 * @param value novo valor
	 */
	public void set(T value) {
		this.value = value;
	}

	/** Volta ao valor padrão. */
	public void reset() {
		set(defaultValue);
	}

	/** @return o valor atual em JSON */
	public abstract JsonElement toJson();

	/**
	 * Lê o valor do JSON. Valores inválidos são ignorados e o atual é mantido.
	 *
	 * @param json elemento salvo
	 */
	public abstract void fromJson(JsonElement json);
}
