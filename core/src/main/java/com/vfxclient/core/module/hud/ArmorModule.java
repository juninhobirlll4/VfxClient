package com.vfxclient.core.module.hud;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;

/**
 * Mostra as peças de armadura (e o item na mão) com a durabilidade restante: número ou
 * porcentagem, cor por estado (verde, amarelo, vermelho) e barra de durabilidade opcional.
 */
public class ArmorModule extends HudModule {

	private static final int ROW = 17;
	private static final int ICON = 16;

	private final BooleanSetting showHeld = add(new BooleanSetting("showHeld", "Mostrar item na mão", true));
	private final BooleanSetting percent = add(new BooleanSetting("percent", "Durabilidade em %", false));
	private final BooleanSetting bar = add(new BooleanSetting("bar", "Barra de durabilidade", true));
	private final BooleanSetting stateColor = add(new BooleanSetting("stateColor", "Cor por estado", true));

	private static final int BAR_WIDTH = 22;

	public ArmorModule() {
		super("armor", "Armadura", "Peças e durabilidade", false, 4, 130);
	}

	private List<ItemStack> items(MinecraftClient client) {
		List<ItemStack> result = new ArrayList<ItemStack>();
		if (client.player == null) {
			return result;
		}
		ItemStack[] armor = client.player.inventory.armor;
		for (int i = armor.length - 1; i >= 0; i--) {
			if (armor[i] != null) {
				result.add(armor[i]);
			}
		}
		if (showHeld.get() && client.player.getMainHandStack() != null) {
			result.add(client.player.getMainHandStack());
		}
		return result;
	}

	private String durability(ItemStack stack) {
		if (!stack.isDamageable() || stack.getMaxDamage() <= 0) {
			return stack.count > 1 ? String.valueOf(stack.count) : "";
		}
		int left = stack.getMaxDamage() - stack.getDamage();
		if (percent.get()) {
			return Math.round(left * 100F / stack.getMaxDamage()) + "%";
		}
		return String.valueOf(left);
	}

	/** @return fração de durabilidade restante (0 a 1), ou -1 se o item não se desgasta */
	private static float fraction(ItemStack stack) {
		if (!stack.isDamageable() || stack.getMaxDamage() <= 0) {
			return -1F;
		}
		return (stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage();
	}

	private static String stateCode(float left) {
		return left > 0.6F ? "§a" : left > 0.25F ? "§e" : "§c";
	}

	private static int barColor(float left) {
		return left > 0.6F ? 0xFF55FF55 : left > 0.25F ? 0xFFFFFF55 : 0xFFFF5555;
	}

	@Override
	public boolean isVisible(MinecraftClient client, boolean preview) {
		return preview || !items(client).isEmpty();
	}

	@Override
	protected int contentWidth(MinecraftClient client, boolean preview) {
		List<ItemStack> items = items(client);
		if (items.isEmpty()) {
			return client.textRenderer.getStringWidth("Armadura");
		}
		int width = 0;
		for (ItemStack stack : items) {
			width = Math.max(width, client.textRenderer.getStringWidth(durability(stack)));
		}
		if (bar.get()) {
			width = Math.max(width, BAR_WIDTH);
		}
		return ICON + (width > 0 ? 4 + width : 0);
	}

	@Override
	protected int contentHeight(MinecraftClient client, boolean preview) {
		int count = items(client).size();
		return count == 0 ? 8 : count * ROW - 1;
	}

	@Override
	protected void renderContent(MinecraftClient client, boolean preview) {
		List<ItemStack> items = items(client);
		if (items.isEmpty()) {
			drawText(client, "Armadura", 0, 0);
			return;
		}
		int y = 0;
		for (ItemStack stack : items) {
			GlStateManager.enableRescaleNormal();
			DiffuseLighting.enable();
			client.getItemRenderer().renderInGuiWithOverrides(stack, 0, y);
			DiffuseLighting.disable();
			GlStateManager.disableRescaleNormal();
			GlStateManager.enableBlend();
			String text = durability(stack);
			float left = fraction(stack);
			boolean damageable = left >= 0;
			if (!text.isEmpty()) {
				String colored = damageable && stateColor.get() ? stateCode(left) + text : text;
				drawText(client, colored, ICON + 4, y + (bar.get() && damageable ? 1 : 4));
			}
			if (bar.get() && damageable) {
				Render2D.rawRect(ICON + 4, y + 11, BAR_WIDTH, 2, 0xAA000000);
				Render2D.rawRect(ICON + 4, y + 11, Math.max(1, BAR_WIDTH * left), 2, barColor(left));
			}
			y += ROW;
		}
	}
}
