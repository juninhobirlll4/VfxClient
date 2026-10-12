package com.vfxclient.core.module.util;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.ChoiceSetting;

/** Hora do dia só na sua tela (o servidor e os outros jogadores não são afetados). */
public class TimeChangerModule extends Module {

	private static final long[] TIMES = {1000L, 6000L, 12500L, 14000L, 18000L, 23200L};

	private final ChoiceSetting time = add(new ChoiceSetting("time", "Hora", 1,
			"Manhã", "Meio-dia", "Pôr do sol", "Noite", "Meia-noite", "Nascer do sol"));

	public TimeChangerModule() {
		super("timeChanger", "Hora local", "Escolha a hora só para você", Category.UTILITY, false);
	}

	/** @return hora do dia em ticks do Minecraft */
	public long timeOfDay() {
		return TIMES[time.get()];
	}
}
