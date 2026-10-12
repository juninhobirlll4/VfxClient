package com.vfxclient.core.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Opção numérica com mínimo, máximo e passo (exibida como slider). */
public class NumberSetting extends Setting<Double> {

	private final double min;
	private final double max;
	private final double step;
	private final String suffix;

	/**
	 * @param id           chave do JSON
	 * @param name         nome exibido
	 * @param defaultValue valor padrão
	 * @param min          valor mínimo
	 * @param max          valor máximo
	 * @param step         incremento (ex.: {@code 1} para inteiros, {@code 0.1} para decimais)
	 * @param suffix       texto após o valor, ex.: {@code "%"} (pode ser vazio)
	 */
	public NumberSetting(String id, String name, double defaultValue, double min, double max, double step, String suffix) {
		super(id, name, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
		this.suffix = suffix;
	}

	@Override
	public void set(Double value) {
		double clamped = Math.max(min, Math.min(max, value));
		if (step > 0) {
			clamped = min + Math.round((clamped - min) / step) * step;
		}
		this.value = Math.max(min, Math.min(max, clamped));
	}

	/**
	 * Define o valor a partir de uma fração do intervalo (usado pelo slider).
	 *
	 * @param fraction de 0 (mínimo) a 1 (máximo)
	 */
	public void setFraction(double fraction) {
		set(min + (max - min) * fraction);
	}

	/** @return posição do valor atual no intervalo, de 0 a 1 */
	public double getFraction() {
		return max == min ? 0 : (value - min) / (max - min);
	}

	/** @return valor formatado para exibição, ex.: {@code 75%} */
	public String format() {
		String number = step >= 1 ? String.valueOf(Math.round(value)) : String.format("%.1f", value);
		return number + suffix;
	}

	/** @return valor atual arredondado para inteiro */
	public int getInt() {
		return (int) Math.round(value);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			set(json.getAsDouble());
		}
	}
}
