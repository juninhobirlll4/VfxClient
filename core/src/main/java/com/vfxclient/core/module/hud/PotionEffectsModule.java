package com.vfxclient.core.module.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;

/** Lista os efeitos de poção ativos com ícone, nível, tempo restante e cor por tipo de efeito. */
public class PotionEffectsModule extends HudModule {

	private static final String[] LEVELS = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
	private static final Identifier INVENTORY = new Identifier("textures/gui/container/inventory.png");
	private static final int ROW = 12;
	private static final int ICON = 10;

	private final BooleanSetting icons = add(new BooleanSetting("icons", "Ícones", true));
	private final BooleanSetting effectColor = add(new BooleanSetting("effectColor", "Cor por tipo de efeito", true));
	private final BooleanSetting blink = addAdvanced(new BooleanSetting("blink", "Piscar nos últimos 10 s", true));

	public PotionEffectsModule() {
		super("potions", "Efeitos", "Poções ativas e tempo", false, 4, 112);
	}

	/** Linha pronta para desenhar. */
	private static final class Line {
		final String text;
		final int color;
		final int iconIndex;

		Line(String text, int color, int iconIndex) {
			this.text = text;
			this.color = color;
			this.iconIndex = iconIndex;
		}
	}

	private List<Line> lines(MinecraftClient client, boolean preview) {
		if (client.player == null || client.player.getStatusEffectInstances().isEmpty()) {
			return preview ? Collections.singletonList(new Line("Velocidade II  §71:30", 0xFF7CAFC6, 0)) : Collections.<Line>emptyList();
		}
		List<Line> lines = new ArrayList<Line>();
		for (StatusEffectInstance instance : client.player.getStatusEffectInstances()) {
			StatusEffect effect = StatusEffect.STATUS_EFFECTS[instance.getEffectId()];
			if (effect == null) {
				continue;
			}
			int amplifier = instance.getAmplifier();
			String level = amplifier >= 0 && amplifier < LEVELS.length ? LEVELS[amplifier] : " " + (amplifier + 1);
			String time = StatusEffect.getFormattedDuration(instance);
			if (blink.get() && instance.getDuration() < 200 && (System.currentTimeMillis() / 400) % 2 == 0) {
				time = "§c" + time;
			} else {
				time = "§7" + time;
			}
			int icon = effect.hasIcon() ? effect.getIconLevel() : -1;
			lines.add(new Line(I18n.translate(effect.getTranslationKey()) + level + "  " + time, readable(effect.getColor()), icon));
		}
		return lines;
	}

	/** Clareia a cor do efeito (algumas são quase pretas) para ficar legível no fundo escuro. */
	private static int readable(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		r = r + (255 - r) * 2 / 5;
		g = g + (255 - g) * 2 / 5;
		b = b + (255 - b) * 2 / 5;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	@Override
	public boolean isVisible(MinecraftClient client, boolean preview) {
		return !lines(client, preview).isEmpty();
	}

	private int textX() {
		return icons.get() ? ICON + 3 : 0;
	}

	@Override
	protected int contentWidth(MinecraftClient client, boolean preview) {
		int width = 0;
		for (Line line : lines(client, preview)) {
			width = Math.max(width, client.textRenderer.getStringWidth(line.text));
		}
		return width + textX();
	}

	@Override
	protected int contentHeight(MinecraftClient client, boolean preview) {
		int count = lines(client, preview).size();
		return count == 0 ? 0 : count * ROW - 2;
	}

	@Override
	protected void renderContent(MinecraftClient client, boolean preview) {
		int y = 0;
		for (Line line : lines(client, preview)) {
			if (icons.get() && line.iconIndex >= 0) {
				GlStateManager.color(1F, 1F, 1F, 1F);
				GlStateManager.enableBlend();
				client.getTextureManager().bindTexture(INVENTORY);
				GlStateManager.pushMatrix();
				GlStateManager.translate(0F, y - 1F, 0F);
				GlStateManager.scale(ICON / 18F, ICON / 18F, 1F);
				client.inGameHud.drawTexture(0, 0, line.iconIndex % 8 * 18, 198 + line.iconIndex / 8 * 18, 18, 18);
				GlStateManager.popMatrix();
			}
			if (effectColor.get()) {
				boolean shadowed = shadow.get() || (style() != null && style().shadow());
				client.textRenderer.draw(line.text, textX(), y, line.color, shadowed);
			} else {
				drawText(client, line.text, textX(), y);
			}
			y += ROW;
		}
	}
}
