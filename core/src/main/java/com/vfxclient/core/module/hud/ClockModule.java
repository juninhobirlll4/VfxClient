package com.vfxclient.core.module.hud;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

/** Mostra o horário do computador. */
public class ClockModule extends HudModule {

	private final BooleanSetting format24h = add(new BooleanSetting("format24h", "Formato 24 horas", true));

	public ClockModule() {
		super("clock", "Relógio", "Horário do computador", false, 4, 58);
	}

	@Override
	public String getText(MinecraftClient client) {
		String pattern = format24h.get() ? "HH:mm" : "hh:mm a";
		return new SimpleDateFormat(pattern).format(new Date());
	}

	@Override
	public String getSampleText() {
		return "21:45";
	}
}
