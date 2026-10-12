package com.vfxclient.core.module.visual;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import net.minecraft.client.MinecraftClient;

/**
 * Deixa o mundo totalmente iluminado aumentando o gamma além do máximo do menu.
 *
 * <p>O gamma original é guardado e devolvido quando o módulo é desligado, inclusive
 * entre sessões (fica salvo no JSON).</p>
 */
public class FullbrightModule extends Module {

	private static final float TARGET_GAMMA = 15.0F;

	private float previousGamma = -1F;

	public FullbrightModule() {
		super("fullbright", "Brilho total", "Enxergue no escuro", Category.VISUAL, false);
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (client.options == null || client.options.gamma >= TARGET_GAMMA) {
			return;
		}
		if (previousGamma < 0F) {
			previousGamma = client.options.gamma;
			markDirty();
		}
		client.options.gamma = TARGET_GAMMA;
	}

	@Override
	protected void onDisable() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.options != null) {
			client.options.gamma = previousGamma >= 0F && previousGamma <= 1F ? previousGamma : 1F;
		}
		previousGamma = -1F;
	}

	@Override
	public JsonObject toJson() {
		JsonObject json = super.toJson();
		json.addProperty("previousGamma", previousGamma);
		return json;
	}

	@Override
	public void fromJson(JsonObject json) {
		JsonElement saved = json.get("previousGamma");
		if (saved != null && saved.isJsonPrimitive() && saved.getAsJsonPrimitive().isNumber()) {
			previousGamma = saved.getAsFloat();
		}
		super.fromJson(json);
	}
}
