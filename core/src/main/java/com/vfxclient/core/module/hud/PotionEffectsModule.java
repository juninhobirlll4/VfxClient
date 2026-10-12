package com.vfxclient.core.module.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vfxclient.core.module.TextHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Lista os efeitos de poção ativos com nível e tempo restante. */
public class PotionEffectsModule extends TextHudModule {

	private static final String[] LEVELS = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

	public PotionEffectsModule() {
		super("potions", "Efeitos", "Poções ativas e tempo", false, 4, 112);
	}

	@Override
	public List<String> getLines(MinecraftClient client, boolean preview) {
		if (client.player == null || client.player.getStatusEffectInstances().isEmpty()) {
			return preview ? Collections.singletonList(getSampleText()) : Collections.<String>emptyList();
		}
		List<String> lines = new ArrayList<String>();
		for (StatusEffectInstance instance : client.player.getStatusEffectInstances()) {
			StatusEffect effect = StatusEffect.STATUS_EFFECTS[instance.getEffectId()];
			if (effect == null) {
				continue;
			}
			int amplifier = instance.getAmplifier();
			String level = amplifier >= 0 && amplifier < LEVELS.length ? LEVELS[amplifier] : " " + (amplifier + 1);
			lines.add(I18n.translate(effect.getTranslationKey()) + level + "  §7" + StatusEffect.getFormattedDuration(instance));
		}
		return lines;
	}

	@Override
	public String getSampleText() {
		return "Velocidade II  §71:30";
	}
}
