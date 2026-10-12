package com.vfxclient.core.module.hud;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.Anim;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.gui.Theme;
import com.vfxclient.core.hud.ClickTracker;
import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.KeySetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

/** Mostra W, A, S, D, cliques e espaço acendendo quando pressionados. */
public class KeystrokesModule extends HudModule {

	private static final int KEY = 22;
	private static final int GAP = 2;
	private static final int WIDTH = KEY * 3 + GAP * 2;
	private static final int SPACE_H = 10;
	private static final int PRESSED = 0xE6FFFFFF;

	private final BooleanSetting showMouse = add(new BooleanSetting("showMouse", "Mostrar cliques", true));
	private final BooleanSetting showCps = add(new BooleanSetting("showCps", "CPS nos cliques", true));
	private final BooleanSetting showSpace = add(new BooleanSetting("showSpace", "Mostrar espaço", true));

	private final Map<String, Anim> anims = new HashMap<String, Anim>();

	public KeystrokesModule() {
		super("keystrokes", "Teclas", "WASD e cliques na tela", false, 4, 160);
	}

	@Override
	protected boolean usesSharedBackground() {
		return false;
	}

	@Override
	public boolean isVisible(MinecraftClient client, boolean preview) {
		return true;
	}

	@Override
	protected int contentWidth(MinecraftClient client, boolean preview) {
		return WIDTH;
	}

	@Override
	protected int contentHeight(MinecraftClient client, boolean preview) {
		int h = KEY * 2 + GAP;
		if (showMouse.get()) {
			h += GAP + KEY;
		}
		if (showSpace.get()) {
			h += GAP + SPACE_H;
		}
		return h;
	}

	@Override
	protected void renderContent(MinecraftClient client, boolean preview) {
		int y = 0;
		drawKey(client, client.options.forwardKey, KEY + GAP, y, KEY, KEY, null);
		y += KEY + GAP;
		drawKey(client, client.options.leftKey, 0, y, KEY, KEY, null);
		drawKey(client, client.options.backKey, KEY + GAP, y, KEY, KEY, null);
		drawKey(client, client.options.rightKey, (KEY + GAP) * 2, y, KEY, KEY, null);
		y += KEY + GAP;
		if (showMouse.get()) {
			int half = (WIDTH - GAP) / 2;
			drawKey(client, client.options.attackKey, 0, y, half, KEY, showCps.get() ? ClickTracker.leftCps() + " CPS" : null);
			drawKey(client, client.options.useKey, half + GAP, y, WIDTH - half - GAP, KEY, showCps.get() ? ClickTracker.rightCps() + " CPS" : null);
			y += KEY + GAP;
		}
		if (showSpace.get()) {
			drawKey(client, client.options.jumpKey, 0, y, WIDTH, SPACE_H, "");
		}
	}

	private void drawKey(MinecraftClient client, KeyBinding binding, int x, int y, int w, int h, String subtitle) {
		Anim anim = anims.get(binding.getTranslationKey());
		if (anim == null) {
			anim = new Anim(0F);
			anims.put(binding.getTranslationKey(), anim);
		}
		float pressed = anim.update(binding.isPressed() ? 1F : 0F, 20F);
		int idle = background.get() ? Theme.HUD_BACKGROUND : 0x00000000;
		Render2D.roundedRect(x, y, w, h, 3, Theme.lerp(idle, PRESSED, pressed));

		int color = Theme.lerp(textColor(), Theme.PRIMARY_DEEP, pressed);
		String label = labelFor(binding);
		if (subtitle != null && subtitle.isEmpty()) {
			// Barra de espaço: só um traço no meio.
			Render2D.roundedRect(x + w / 2F - 12, y + h / 2F - 0.5F, 24, 1, 0.5F, color);
			return;
		}
		int textW = client.textRenderer.getStringWidth(label);
		if (subtitle == null) {
			client.textRenderer.draw(label, x + (w - textW) / 2F, y + (h - 8) / 2F, color, shadow.get() && pressed < 0.5F);
		} else {
			client.textRenderer.draw(label, x + (w - textW) / 2F, y + 3, color, shadow.get() && pressed < 0.5F);
			GlStateManager.pushMatrix();
			GlStateManager.translate(x + w / 2F, y + 13F, 0F);
			GlStateManager.scale(0.5F, 0.5F, 1F);
			int subW = client.textRenderer.getStringWidth(subtitle);
			client.textRenderer.draw(subtitle, -subW / 2F, 0F, color, false);
			GlStateManager.popMatrix();
		}
	}

	private static String labelFor(KeyBinding binding) {
		int code = binding.getCode();
		if (code == ClickTracker.LEFT) {
			return "ESQ";
		}
		if (code == ClickTracker.RIGHT) {
			return "DIR";
		}
		String name = KeySetting.describe(code);
		return name.length() > 3 ? name.substring(0, 3) : name;
	}
}
