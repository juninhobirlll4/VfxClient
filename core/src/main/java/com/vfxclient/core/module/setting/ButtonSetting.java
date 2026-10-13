package com.vfxclient.core.module.setting;

import com.google.gson.JsonElement;

/** Botão dentro das opções de um módulo: executa uma ação ao ser clicado (não é salvo no JSON). */
public class ButtonSetting extends Setting<Runnable> {

	private final String label;

	/**
	 * @param id     identificador (não vai para o JSON)
	 * @param name   texto à esquerda da linha
	 * @param label  texto do botão
	 * @param action o que fazer ao clicar
	 */
	public ButtonSetting(String id, String name, String label, Runnable action) {
		super(id, name, action);
		this.label = label;
	}

	/** @return texto do botão */
	public String getLabel() {
		return label;
	}

	/** Executa a ação. */
	public void press() {
		if (value != null) {
			value.run();
		}
	}

	@Override
	public JsonElement toJson() {
		return null;
	}

	@Override
	public void fromJson(JsonElement json) {
	}
}
