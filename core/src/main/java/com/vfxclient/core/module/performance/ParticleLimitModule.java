package com.vfxclient.core.module.performance;

import java.util.Random;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.NumberSetting;

/** Reduz a quantidade de partículas criadas (explosões, poções, chuva de crits...). */
public class ParticleLimitModule extends Module {

	private final NumberSetting amount = add(new NumberSetting("amount", "Quantidade", 50, 0, 100, 5, "%"));
	private final Random random = new Random();

	public ParticleLimitModule() {
		super("particleLimit", "Partículas", "Menos partículas, mais FPS", Category.PERFORMANCE, false);
	}

	/** @return {@code true} se a próxima partícula deve ser descartada */
	public boolean shouldDrop() {
		return isEnabled() && random.nextDouble() * 100 >= amount.get();
	}
}
