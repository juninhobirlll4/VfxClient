package com.vfxclient.core.module.hud;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

/** Mostra o horário do computador; no modo avançado, também a data. */
public class ClockModule extends TextHudModule {

	private final BooleanSetting format24h = add(new BooleanSetting("format24h", "Formato 24 horas", true));

	public ClockModule() {
		super("clock", "Relógio", "Horário do computador", false, 4, 58);
	}

	@Override
	public String getText(MinecraftClient client) {
		String time = format24h.get() ? "HH:mm" : "hh:mm a";
		String pattern = advanced() ? time + "  '§7'dd/MM" : time;
		return new SimpleDateFormat(pattern).format(new Date());
	}

	@Override
	public String getSampleText() {
		return advanced() ? "21:45  §712/10" : "21:45";
	}
}
