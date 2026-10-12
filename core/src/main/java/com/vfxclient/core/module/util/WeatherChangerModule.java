package com.vfxclient.core.module.util;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.ChoiceSetting;

/** Clima só na sua tela: sempre limpo ou sempre chovendo. */
public class WeatherChangerModule extends Module {

	private static final int CLEAR = 0;
	private static final int RAIN = 1;

	private final ChoiceSetting weather = add(new ChoiceSetting("weather", "Clima", CLEAR, "Sempre limpo", "Sempre chuva", "Tempestade"));

	public WeatherChangerModule() {
		super("weatherChanger", "Clima local", "Sem chuva (ou só chuva) para você", Category.UTILITY, false);
	}

	/** @return intensidade da chuva de 0 a 1 */
	public float rain() {
		return weather.get() == CLEAR ? 0F : 1F;
	}

	/** @return intensidade de trovoada de 0 a 1 */
	public float thunder() {
		return weather.get() > RAIN ? 1F : 0F;
	}
}
