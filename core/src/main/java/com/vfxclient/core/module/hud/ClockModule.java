package com.vfxclient.core.module.hud;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

/** Mostra o horário do computador (12 h ou 24 h, segundos opcionais); no modo avançado, também a data. */
public class ClockModule extends TextHudModule {

	private final BooleanSetting format24h = add(new BooleanSetting("format24h", "Formato 24 horas", true));
	private final BooleanSetting seconds = add(new BooleanSetting("seconds", "Mostrar segundos", false));

	public ClockModule() {
		super("clock", "Relógio", "Horário do computador", false, 4, 58);
	}

	@Override
	protected boolean isSecondary() {
		return true;
	}

	private String format(Date date) {
		String time = format24h.get() ? (seconds.get() ? "HH:mm:ss" : "HH:mm") : (seconds.get() ? "hh:mm:ss a" : "hh:mm a");
		String text = new SimpleDateFormat(time).format(date);
		return advanced() ? VALUE + text + TEXT + "  §7" + new SimpleDateFormat("dd/MM").format(date) : VALUE + text;
	}

	@Override
	public String getText(MinecraftClient client) {
		return format(new Date());
	}

	@Override
	public String getSampleText() {
		return format(new Date());
	}
}
