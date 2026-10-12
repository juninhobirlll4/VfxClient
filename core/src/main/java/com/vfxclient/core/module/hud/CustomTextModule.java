package com.vfxclient.core.module.hud;

import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.TextSetting;
import net.minecraft.client.MinecraftClient;

/** Mostra um texto escolhido pelo jogador (aceita códigos de cor com &amp;). */
public class CustomTextModule extends TextHudModule {

	private final TextSetting text = add(new TextSetting("text", "Texto", "VfxClient", 48));

	public CustomTextModule() {
		super("customText", "Texto", "Um texto seu na tela", false, 4, 94);
	}

	@Override
	public String getText(MinecraftClient client) {
		String value = text.get().replace('&', '§');
		return value.trim().isEmpty() ? null : value;
	}

	@Override
	public String getSampleText() {
		return "Seu texto aqui";
	}
}
