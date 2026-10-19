package com.vfxclient.core.module.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.CrosshairEditorScreen;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ButtonSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.ColorSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.module.setting.TextSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.input.Input;
import net.minecraft.entity.player.ClientPlayerEntity;

/**
 * Mira personalizada desenhada no lugar da mira do jogo: formatos prontos com as opções de uma
 * mira de CS (espaço, espessura, contorno, ponto no centro, abrir ao andar e atacar) ou um
 * desenho feito pelo jogador no editor.
 */
public class CrosshairModule extends Module {

	private static final int CROSS = 0;
	private static final int DOT = 1;
	private static final int CROSS_DOT = 2;
	private static final int CIRCLE = 3;
	private static final int T_SHAPE = 4;
	private static final int DRAWING = 5;

	private final ChoiceSetting style = add(new ChoiceSetting("style", "Formato", CROSS,
			"Cruz", "Ponto", "Cruz com ponto", "Círculo", "T", "Meu desenho"));
	private final ColorSetting color = add(new ColorSetting("color", "Cor", ColorSetting.WHITE));
	private final NumberSetting size = add(new NumberSetting("size", "Tamanho", 5, 1, 15, 1, ""));
	private final NumberSetting gap = addAdvanced(new NumberSetting("gap", "Espaço no meio", 2, 0, 10, 1, ""));
	private final NumberSetting thickness = addAdvanced(new NumberSetting("thickness", "Espessura", 1, 1, 4, 1, ""));
	private final BooleanSetting centerDot = add(new BooleanSetting("centerDot", "Ponto no centro", false));
	private final NumberSetting dotSize = addAdvanced(new NumberSetting("dotSize", "Tamanho do ponto", 1, 1, 5, 1, ""));
	private final BooleanSetting outline = add(new BooleanSetting("outline", "Contorno", true));
	private final NumberSetting outlineSize = addAdvanced(new NumberSetting("outlineSize", "Espessura do contorno", 1, 1, 3, 1, ""));
	private final ColorSetting outlineColor = addAdvanced(new ColorSetting("outlineColor", "Cor do contorno", 0xB0000000));
	private final BooleanSetting dynamic = add(new BooleanSetting("dynamic", "Abrir ao andar e atacar (como no CS)", false));
	private final NumberSetting dynamicAmount = addAdvanced(new NumberSetting("dynamicAmount", "Quanto abre", 3, 1, 8, 1, ""));
	private final BooleanSetting invert = addAdvanced(new BooleanSetting("invert", "Inverter cor como o jogo", false));
	private final NumberSetting drawingScale = addAdvanced(new NumberSetting("drawingScale", "Tamanho do desenho", 1, 1, 4, 1, "x"));
	private final TextSetting drawing = addAdvanced(new TextSetting("drawing", "Desenho da mira (feito no editor)", "", CrosshairDrawing.CELLS));
	@SuppressWarnings("unused")
	private final ButtonSetting edit = add(new ButtonSetting("edit", "Desenhar a minha mira", "Abrir editor", this::openEditor));

	private boolean raw;
	/** Quanto a mira está aberta agora (cresce ao andar/atacar e volta aos poucos). */
	private float spread;
	private long lastFrame;

	public CrosshairModule() {
		super("crosshair", "Mira", "Formato, cor e tamanho da mira; desenhe a sua", Category.UTILITY, false);
	}

	/** @return o desenho salvo, uma letra por quadradinho (veja {@link CrosshairDrawing}) */
	public String drawing() {
		return CrosshairDrawing.normalize(drawing.get());
	}

	public void setDrawing(String cells) {
		drawing.set(cells);
	}

	/** Usa o desenho como formato da mira (e liga a mira, se estiver desligada). */
	public void useDrawing() {
		style.set(DRAWING);
		setEnabled(true);
	}

	/** @return a cor da mira (usada pelo editor para o "pincel da cor da mira") */
	public int mainColor() {
		return color.argb();
	}

	/** @return a cor do contorno (idem, para o pincel do contorno) */
	public int outlineArgb() {
		return outlineColor.argb();
	}

	private void openEditor() {
		MinecraftClient client = MinecraftClient.getInstance();
		client.setScreen(new CrosshairEditorScreen(this, client.currentScreen));
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

		GlStateManager.enableBlend();
		if (style.get() == DRAWING) {
			CrosshairDrawing.draw(drawing(), cx, cy, drawingScale.getInt(), this::mainColor, this::outlineArgb);
			GlStateManager.color(1F, 1F, 1F, 1F);
			return;
		}

		int s = size.getInt();
		int g = gap.getInt() + Math.round(updateSpread());
		int t = thickness.getInt();
		int main = color.argb();

		if (invert.get()) {
			raw = true;
			GlStateManager.blendFuncSeparate(775, 769, 1, 0);
			drawShape(cx, cy, s, g, t, 0xFFFFFFFF, 0);
			GlStateManager.blendFuncSeparate(770, 771, 1, 0);
			raw = false;
			return;
		}
		if (outline.get()) {
			drawShape(cx, cy, s, g, t, outlineColor.argb(), outlineSize.getInt());
		}
		drawShape(cx, cy, s, g, t, main, 0);
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	/** Abre ao andar, correr ou atacar e fecha aos poucos (só se a opção estiver ligada). */
	private float updateSpread() {
		long now = System.nanoTime();
		float seconds = lastFrame == 0 ? 0F : Math.min(0.25F, (now - lastFrame) / 1e9F);
		lastFrame = now;

		float target = dynamic.get() && isBusy() ? dynamicAmount.getInt() : 0F;
		// Abre rápido e fecha devagar, como numa mira de tiro.
		float speed = target > spread ? 18F : 7F;
		spread += (target - spread) * Math.min(1F, seconds * speed);
		return spread;
	}

	private static boolean isBusy() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) {
			return false;
		}
		Input input = player.input;
		boolean moving = input != null && (input.movementForward != 0 || input.movementSideways != 0 || input.jumping);
		return moving || player.handSwinging;
	}

	private void drawShape(int cx, int cy, int s, int g, int t, int argb, int grow) {
		int half = t / 2;
		int x0 = cx - half;
		int y0 = cy - half;
		int shape = style.get();

		if (shape == CIRCLE) {
			float r = s + grow;
			Render2D.ring(cx + (t % 2) / 2F, cy + (t % 2) / 2F, r, t + grow * 2, argb);
		}

		if (shape == CROSS || shape == CROSS_DOT || shape == T_SHAPE) {
			if (shape != T_SHAPE) {
				rect(x0 - grow, y0 - g - s - grow, t + grow * 2, s + grow * 2, argb);
			}
			rect(x0 - grow, y0 + t + g - grow, t + grow * 2, s + grow * 2, argb);
			rect(x0 - g - s - grow, y0 - grow, s + grow * 2, t + grow * 2, argb);
			rect(x0 + t + g - grow, y0 - grow, s + grow * 2, t + grow * 2, argb);
		}

		if (shape == DOT || shape == CROSS_DOT || centerDot.get()) {
			int d = shape == DOT || shape == CROSS_DOT ? Math.max(t, dotSize.getInt()) : dotSize.getInt();
			int dx = cx - d / 2;
			int dy = cy - d / 2;
			rect(dx - grow, dy - grow, d + grow * 2, d + grow * 2, argb);
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
