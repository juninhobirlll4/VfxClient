package com.vfxclient.core.module.hud;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.JsonObject;
import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.ChoiceSetting;
import net.minecraft.client.MinecraftClient;

/**
 * Tempo de jogo: da sessão atual, do dia ou total. Conta só enquanto há um mundo aberto;
 * o total e o do dia ficam salvos na config.
 */
public class PlaytimeModule extends TextHudModule {

	private static final int SESSION = 0;
	private static final int TODAY = 1;
	private static final int TOTAL = 2;

	private final ChoiceSetting period = add(new ChoiceSetting("period", "Mostrar", SESSION, "Sessão atual", "Hoje", "Total"));

	private long sessionTicks;
	private long todayTicks;
	private long totalTicks;
	private String today = "";

	public PlaytimeModule() {
		super("playtime", "Tempo de jogo", "Sessão, hoje ou total", false, 4, 18);
		setDefaultAnchor(TOP_RIGHT);
	}

	@Override
	protected boolean isSecondary() {
		return true;
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (client.world == null || client.isPaused()) {
			return;
		}
		String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		if (!date.equals(today)) {
			today = date;
			todayTicks = 0;
		}
		sessionTicks++;
		todayTicks++;
		totalTicks++;
		if (totalTicks % 1200 == 0) {
			markDirty(); // salva a cada minuto
		}
	}

	private static String duration(long ticks) {
		long seconds = ticks / 20;
		long h = seconds / 3600;
		long m = seconds / 60 % 60;
		long s = seconds % 60;
		return h > 0 ? String.format("%dh %02dmin", h, m) : String.format("%dmin %02ds", m, s);
	}

	private String format(long ticks) {
		String label = period.get() == SESSION ? "Sessão" : period.get() == TODAY ? "Hoje" : "Total";
		return label + " " + VALUE + duration(ticks);
	}

	@Override
	public String getText(MinecraftClient client) {
		if (client.world == null) {
			return null;
		}
		return format(period.get() == SESSION ? sessionTicks : period.get() == TODAY ? todayTicks : totalTicks);
	}

	@Override
	public String getSampleText() {
		return format(20L * (47 * 60 + 12));
	}

	@Override
	public JsonObject toJson() {
		JsonObject json = super.toJson();
		json.addProperty("totalTicks", totalTicks);
		json.addProperty("todayTicks", todayTicks);
		json.addProperty("today", today);
		return json;
	}

	@Override
	public void fromJson(JsonObject json) {
		super.fromJson(json);
		if (json.has("totalTicks")) {
			totalTicks = json.get("totalTicks").getAsLong();
		}
		if (json.has("todayTicks") && json.has("today")) {
			todayTicks = json.get("todayTicks").getAsLong();
			today = json.get("today").getAsString();
		}
	}
}
