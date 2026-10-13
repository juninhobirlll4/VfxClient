package com.vfxclient.core.module.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.ColorSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.gui.DrawableHelper;

/** Mira personalizada desenhada no lugar da mira do jogo. */
public class CrosshairModule extends Module {

	private static final int CROSS = 0;
	private static final int DOT = 1;
	private static final int CROSS_DOT = 2;
	private static final int CIRCLE = 3;
	private static final int T_SHAPE = 4;

	private final ChoiceSetting style = add(new ChoiceSetting("style", "Formato", CROSS, "Cruz", "Ponto", "Cruz com ponto", "Círculo", "T"));
	private final ColorSetting color = add(new ColorSetting("color", "Cor", ColorSetting.WHITE));
	private final NumberSetting size = add(new NumberSetting("size", "Tamanho", 5, 1, 15, 1, ""));
	private final NumberSetting gap = addAdvanced(new NumberSetting("gap", "Espaço no meio", 2, 0, 10, 1, ""));
	private final NumberSetting thickness = addAdvanced(new NumberSetting("thickness", "Espessura", 1, 1, 4, 1, ""));
	private final BooleanSetting outline = add(new BooleanSetting("outline", "Contorno", true));
	private final ColorSetting outlineColor = addAdvanced(new ColorSetting("outlineColor", "Cor do contorno", 0xB0000000));
	private final BooleanSetting invert = addAdvanced(new BooleanSetting("invert", "Inverter cor como o jogo", false));

	private boolean raw;

	public CrosshairModule() {
		super("crosshair", "Mira", "Formato, cor e tamanho da mira", Category.UTILITY, false);
	}

	/**
	 * Desenha a mira no centro da tela.
	 *
	 * @param screenW largura escalada da tela
	 * @param screenH altura escalada da tela
	 */
	public void render(int screenW, int screenH) {
		int cx = screenW / 2;
		int cy = screenH / 2;
		int main = color.argb();
		int s = size.getInt();
		int g = gap.getInt();
		int t = thickness.getInt();

		GlStateManager.enableBlend();
		if (invert.get()) {
			raw = true;
			GlStateManager.blendFuncSeparate(775, 769, 1, 0);
			drawShape(cx, cy, s, g, t, 0xFFFFFFFF);
			GlStateManager.blendFuncSeparate(770, 771, 1, 0);
			raw = false;
			return;
		}
		if (outline.get()) {
			drawShape(cx, cy, s, g, t, outlineColor.argb(), 1);
		}
		drawShape(cx, cy, s, g, t, main);
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private void drawShape(int cx, int cy, int s, int g, int t, int argb) {
		drawShape(cx, cy, s, g, t, argb, 0);
	}

	private void drawShape(int cx, int cy, int s, int g, int t, int argb, int grow) {
		int half = t / 2;
		int x0 = cx - half;
		int y0 = cy - half;
		int shape = style.get();
		if (shape == CIRCLE) {
			float r = s + grow;
			Render2D.ring(cx + (t % 2) / 2F, cy + (t % 2) / 2F, r, t + grow * 2, argb);
			return;
		}
		if (shape == CROSS || shape == CROSS_DOT || shape == T_SHAPE) {
			if (shape != T_SHAPE) {
				rect(x0 - grow, y0 - g - s - grow, t + grow * 2, s + grow * 2, argb);
			}
			rect(x0 - grow, y0 + t + g - grow, t + grow * 2, s + grow * 2, argb);
			rect(x0 - g - s - grow, y0 - grow, s + grow * 2, t + grow * 2, argb);
			rect(x0 + t + g - grow, y0 - grow, s + grow * 2, t + grow * 2, argb);
		}
		if (shape == DOT || shape == CROSS_DOT) {
			rect(x0 - grow, y0 - grow, t + grow * 2, t + grow * 2, argb);
		}
	}

	private void rect(int x, int y, int w, int h, int argb) {
		if (raw) {
			// Sem trocar o modo de mistura: usado no modo "inverter cor".
			Render2D.rawRect(x, y, w, h, argb);
		} else {
			DrawableHelper.fill(x, y, x + w, y + h, argb);
		}
	}
}
