package com.vfxclient.core.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.general.MenuModule;
import com.vfxclient.core.module.performance.PerformanceProfileModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ButtonSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.ColorSetting;
import com.vfxclient.core.module.setting.KeySetting;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.module.setting.Setting;
import com.vfxclient.core.module.setting.TextSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Menu principal do VfxClient, aberto com Right Shift (configurável em Geral > Menu).
 *
 * <p>Barra lateral com as categorias; à direita, cartões de módulos (clique liga/desliga,
 * botão direito ou "Opções" abre as configurações). Nas configurações há sliders (clique no
 * valor para digitar), escolhas (esquerdo avança, direito volta), teclas, textos, botões e
 * seletor de cor (RGB, alfa, HEX e paleta). Módulos de HUD mostram uma prévia ao vivo.</p>
 */
public class ClickGuiScreen extends Screen {

	private static final int SIDEBAR_W = 92;
	private static final int CARD_MIN_W = 112;
	private static final int CARD_H = 42;
	private static final int GAP = 6;
	private static final int ROW_H = 20;
	private static final int SLIDER_W = 80;
	private static final int TEXT_BOX_W = 110;
	private static final long OPEN_ANIM_MS = 200L;

	private static final int PICKER_W = 216;
	private static final int PICKER_H = 152;
	private static final int PICKER_SLIDER_W = 104;
	private static final String[] CHANNEL_NAMES = {"A", "R", "G", "B"};

	/** Lembra a última categoria entre aberturas do menu. */
	private static Category lastCategory = Category.HUD;

	private final Map<Object, Anim> hoverAnims = new HashMap<Object, Anim>();
	private final Map<Object, Anim> toggleAnims = new HashMap<Object, Anim>();
	private final Anim scrollAnim = new Anim(0F);
	private final Anim settingsScrollAnim = new Anim(0F);
	private final long openedAt = System.currentTimeMillis();

	private Category category = lastCategory;
	private Module settingsModule;
	private float scrollTarget;
	private float settingsScrollTarget;
	private int lastMouseX;

	// Edição em andamento (só uma por vez).
	private NumberSetting draggingSlider;
	private int sliderX;
	private KeySetting capturingKey;
	private TextSetting editingText;
	private NumberSetting editingNumber;
	private String numberBuffer = "";

	// Seletor de cor.
	private ColorSetting picker;
	private int pickerOriginal;
	private int draggingChannel = -1;
	private boolean editingHex;
	private String hexBuffer = "";

	// Layout calculado a cada frame.
	private int px;
	private int py;
	private int panelW;
	private int panelH;
	private int contentX;
	private int contentY;
	private int contentW;
	private int contentH;
	private int listW;

	@Override
	public void init() {
		Keyboard.enableRepeatEvents(true);
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
		lastCategory = category;
		closePicker();
		stopEditing();
		VfxCore.get().getConfig().save();
	}

	// ------------------------------------------------------------------
	// Desenho
	// ------------------------------------------------------------------

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		layout();
		lastMouseX = mouseX;
		float open = Anim.easeOutCubic((System.currentTimeMillis() - openedAt) / (float) OPEN_ANIM_MS);

		DrawableHelper.fill(0, 0, width, height, Theme.withAlpha(Theme.BACKDROP, open));

		GlStateManager.pushMatrix();
		float scale = 0.94F + 0.06F * open;
		GlStateManager.translate(width / 2F, height / 2F, 0F);
		GlStateManager.scale(scale, scale, 1F);
		GlStateManager.translate(-width / 2F, -height / 2F, 0F);

		Render2D.roundedRect(px, py, panelW, panelH, 6, Theme.PANEL);
		drawSidebar(mouseX, mouseY);
		int listMouseX = picker != null ? -1 : mouseX;
		if (settingsModule != null) {
			drawSettings(listMouseX, mouseY);
		} else {
			drawModules(listMouseX, mouseY);
		}
		if (picker != null) {
			drawPicker(mouseX, mouseY);
		}

		GlStateManager.popMatrix();
	}

	private void layout() {
		panelW = Math.min(width - 20, 420);
		panelH = Math.min(height - 20, 250);
		px = (width - panelW) / 2;
		py = (height - panelH) / 2;
		contentX = px + SIDEBAR_W + 12;
		contentY = py + 40;
		contentW = panelW - SIDEBAR_W - 24;
		contentH = panelH - 40 - 10;
		listW = settingsModule instanceof HudModule && contentW > 220 ? contentW - 96 : contentW;
	}

	private void drawSidebar(int mouseX, int mouseY) {
		Render2D.roundedRect(px, py, SIDEBAR_W, panelH, 6, Theme.SIDEBAR);
		DrawableHelper.fill(px + SIDEBAR_W - 6, py, px + SIDEBAR_W, py + panelH, Theme.SIDEBAR);

		Logo.draw(px + 10, py + 10, 16);
		text(VfxCore.NAME, px + 31, py + 14, Theme.TEXT);

		int y = py + 36;
		for (Category c : Category.values()) {
			boolean hovered = picker == null && inside(mouseX, mouseY, px + 6, y, SIDEBAR_W - 12, 16);
			float hover = hover(c, hovered);
			boolean selected = c == category;
			if (selected) {
				Render2D.roundedRect(px + 6, y, SIDEBAR_W - 12, 16, 4, Theme.withAlpha(Theme.PRIMARY, 0.35F));
				DrawableHelper.fill(px + 6, y + 3, px + 8, y + 13, Theme.PRIMARY_LIGHT);
			} else if (hover > 0.01F) {
				Render2D.roundedRect(px + 6, y, SIDEBAR_W - 12, 16, 4, Theme.withAlpha(Theme.CARD_HOVER, hover));
			}
			text(c.getDisplayName(), px + 13, y + 4, selected ? Theme.WHITE : Theme.lerp(Theme.TEXT_MUTED, Theme.TEXT, hover));
			y += 18;
		}

		int by = py + panelH - 24;
		boolean hovered = picker == null && inside(mouseX, mouseY, px + 6, by, SIDEBAR_W - 12, 16);
		Render2D.roundedRect(px + 6, by, SIDEBAR_W - 12, 16, 4, Theme.lerp(Theme.TRACK_OFF, Theme.PRIMARY, hover("hud-editor", hovered)));
		textCentered("Editar HUD", px + SIDEBAR_W / 2, by + 4, Theme.WHITE);
		text("v" + VfxCore.get().getVersion(), px + 10, by - 12, Theme.TEXT_MUTED);
	}

	private void drawHeader(String title, String subtitle) {
		text(title, contentX, py + 10, Theme.TEXT);
		text(subtitle, contentX, py + 22, Theme.TEXT_MUTED);
		DrawableHelper.fill(contentX, py + 34, contentX + contentW, py + 35, Theme.DIVIDER);
	}

	private void drawModules(int mouseX, int mouseY) {
		String subtitle = category.getDescription();
		if (category == Category.PERFORMANCE) {
			subtitle += "  -  agora: " + MinecraftClient.getCurrentFps() + " FPS";
		}
		drawHeader(category.getDisplayName(), subtitle);
		List<Module> modules = VfxCore.get().getModules().byCategory(category);

		if (modules.isEmpty()) {
			textCentered("Nada aqui ainda - chega numa próxima versão.", contentX + contentW / 2, contentY + contentH / 2 - 4, Theme.TEXT_MUTED);
			return;
		}

		int cols = Math.max(1, (contentW + GAP) / (CARD_MIN_W + GAP));
		int cardW = (contentW - GAP * (cols - 1)) / cols;
		int rows = (modules.size() + cols - 1) / cols;
		float maxScroll = Math.max(0, rows * (CARD_H + GAP) - GAP - contentH);
		scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));
		int scroll = Math.round(scrollAnim.update(scrollTarget, 16F));

		boolean mouseInContent = inside(mouseX, mouseY, contentX, contentY, contentW, contentH);
		Render2D.scissor(contentX, contentY, contentW, contentH);
		for (int i = 0; i < modules.size(); i++) {
			Module module = modules.get(i);
			int x = contentX + (i % cols) * (cardW + GAP);
			int y = contentY + (i / cols) * (CARD_H + GAP) - scroll;
			boolean hovered = mouseInContent && inside(mouseX, mouseY, x, y, cardW, CARD_H);
			drawCard(module, x, y, cardW, hovered, mouseX, mouseY);
		}
		Render2D.endScissor();
	}

	private void drawCard(Module module, int x, int y, int w, boolean hovered, int mouseX, int mouseY) {
		float hover = hover(module, hovered);
		float on = toggle(module, module.isEnabled());
		int base = Theme.lerp(Theme.CARD, Theme.CARD_ENABLED, module.isAlwaysOn() ? 0F : on);
		Render2D.roundedRect(x, y, w, CARD_H, 5, Theme.lerp(base, Theme.CARD_HOVER, hover * 0.6F));

		text(module.getName(), x + 8, y + 7, Theme.TEXT);
		text(textRenderer.trimToWidth(module.getDescription(), w - 16), x + 8, y + 18, Theme.TEXT_MUTED);

		if (!module.getSettings().isEmpty()) {
			boolean linkHovered = hovered && inside(mouseX, mouseY, x + 6, y + CARD_H - 13, 40, 11);
			text("Opções", x + 8, y + CARD_H - 11, linkHovered ? Theme.PRIMARY_LIGHT : Theme.withAlpha(Theme.TEXT_MUTED, 0.8F));
		}
		if (!module.isAlwaysOn()) {
			drawSwitch(x + w - 26, y + CARD_H - 14, on);
		}
	}

	private void drawSettings(int mouseX, int mouseY) {
		Module module = settingsModule;
		boolean backHovered = inside(mouseX, mouseY, contentX - 2, py + 8, 50, 12);
		String back = "< Voltar";
		text(back, contentX, py + 10, backHovered ? Theme.PRIMARY_LIGHT : Theme.TEXT_MUTED);
		text(module.getName(), contentX + textRenderer.getStringWidth(back) + 8, py + 10, Theme.TEXT);
		text(textRenderer.trimToWidth(module.getDescription(), contentW), contentX, py + 22, Theme.TEXT_MUTED);
		DrawableHelper.fill(contentX, py + 34, contentX + contentW, py + 35, Theme.DIVIDER);

		List<Setting<?>> rows = settingRows(module);
		int listH = settingsListHeight();
		float maxScroll = Math.max(0, rows.size() * ROW_H - 2 - listH);
		settingsScrollTarget = Math.max(0, Math.min(settingsScrollTarget, maxScroll));
		int scroll = Math.round(settingsScrollAnim.update(settingsScrollTarget, 16F));

		boolean mouseInList = inside(mouseX, mouseY, contentX, contentY, listW, listH);
		Render2D.scissor(contentX, contentY, listW, listH);
		int y = contentY - scroll;
		for (Setting<?> setting : rows) {
			drawRow(y, mouseInList ? mouseX : -1, mouseY, module, setting);
			y += ROW_H;
		}
		Render2D.endScissor();

		if (module instanceof HudModule && listW < contentW) {
			drawPreview((HudModule) module, contentX + listW + 6, contentY, contentW - listW - 6, listH);
		}

		int bw = 96;
		int bx = contentX + contentW - bw;
		int by = contentY + contentH - 16;
		boolean resetHovered = inside(mouseX, mouseY, bx, by, bw, 16);
		Render2D.roundedRect(bx, by, bw, 16, 4, Theme.lerp(Theme.TRACK_OFF, Theme.PRIMARY, hover("reset", resetHovered)));
		textCentered("Restaurar padrão", bx + bw / 2, by + 4, Theme.WHITE);

		int hidden = hiddenAdvancedCount(module);
		String hint = hidden > 0 ? "+" + hidden + " opções no modo Completo (Geral > Menu)" : maxScroll > 0 ? "Role para ver mais" : "";
		text(textRenderer.trimToWidth(hint, contentW - bw - 8), contentX, by + 4, Theme.TEXT_MUTED);
	}

	/** Prévia ao vivo do elemento de HUD, redimensionada para caber na caixa. */
	private void drawPreview(HudModule module, int x, int y, int w, int h) {
		Render2D.roundedRect(x, y, w, h, 5, 0xFF0B0812);
		textCentered("Prévia", x + w / 2, y + 4, Theme.TEXT_MUTED);
		if (!module.isEnabled()) {
			textCentered("(desligado)", x + w / 2, y + h / 2 - 4, Theme.TEXT_MUTED);
			return;
		}
		int mw = module.getWidth(client, true);
		int mh = module.getHeight(client, true);
		if (mw <= 0 || mh <= 0) {
			return;
		}
		float fit = Math.min(1F, Math.min((w - 10F) / mw, (h - 22F) / mh));
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + w / 2F - mw * fit / 2F, y + 14 + (h - 14) / 2F - mh * fit / 2F, 0F);
		GlStateManager.scale(fit, fit, 1F);
		module.render(client, 0, 0, true);
		GlStateManager.popMatrix();
	}

	/** Linhas da tela de opções; {@code null} representa a chave "Ativado". */
	private List<Setting<?>> settingRows(Module module) {
		boolean advanced = VfxCore.get().getModules().get(MenuModule.class).showAdvanced();
		List<Setting<?>> rows = new ArrayList<Setting<?>>();
		if (!module.isAlwaysOn()) {
			rows.add(null);
		}
		for (Setting<?> setting : module.getSettings()) {
			if (module.isSettingVisible(setting) && (advanced || !setting.isAdvanced())) {
				rows.add(setting);
			}
		}
		return rows;
	}

	private int hiddenAdvancedCount(Module module) {
		if (VfxCore.get().getModules().get(MenuModule.class).showAdvanced()) {
			return 0;
		}
		int count = 0;
		for (Setting<?> setting : module.getSettings()) {
			if (module.isSettingVisible(setting) && setting.isAdvanced()) {
				count++;
			}
		}
		return count;
	}

	private int settingsListHeight() {
		return contentH - 22;
	}

	private void drawRow(int y, int mouseX, int mouseY, Module module, Setting<?> setting) {
		boolean hovered = inside(mouseX, mouseY, contentX, y, listW, ROW_H - 2);
		Object key = setting == null ? module : setting;
		String label = setting == null ? "Ativado" : setting.getName();
		Render2D.roundedRect(contentX, y, listW, ROW_H - 2, 4, Theme.lerp(Theme.CARD, Theme.CARD_HOVER, hover("row:" + System.identityHashCode(key), hovered)));

		int right = contentX + listW - 6;
		int controlW = controlWidth(setting);
		text(textRenderer.trimToWidth(label, listW - controlW - 20), contentX + 7, y + 5, Theme.TEXT);

		if (setting == null) {
			drawSwitch(right - 20, y + 4, toggle(module, module.isEnabled()));
		} else if (setting instanceof BooleanSetting) {
			drawSwitch(right - 20, y + 4, toggle(setting, ((BooleanSetting) setting).get()));
		} else if (setting instanceof NumberSetting) {
			NumberSetting number = (NumberSetting) setting;
			int sx = right - SLIDER_W;
			if (number == draggingSlider) {
				number.setFraction(Math.max(0, Math.min(1, (lastMouseX - sliderX) / (double) SLIDER_W)));
			}
			drawSlider(sx, y + 7, number);
			if (number == editingNumber) {
				int bw = 44;
				Render2D.roundedRect(sx - bw - 6, y + 2, bw, 14, 3, Theme.PRIMARY);
				Render2D.roundedRect(sx - bw - 5, y + 3, bw - 2, 12, 2, Theme.PANEL);
				text(numberBuffer + caret(), sx - bw - 2, y + 5, Theme.TEXT);
			} else {
				String value = number.format();
				boolean overValue = inside(mouseX, mouseY, sx - textRenderer.getStringWidth(value) - 8, y, textRenderer.getStringWidth(value) + 4, ROW_H);
				text(value, sx - textRenderer.getStringWidth(value) - 6, y + 5, overValue ? Theme.PRIMARY_LIGHT : Theme.TEXT_MUTED);
			}
		} else if (setting instanceof ChoiceSetting) {
			ChoiceSetting choice = (ChoiceSetting) setting;
			String value = "< " + choice.format() + " >";
			int bw = Math.max(64, textRenderer.getStringWidth(value) + 12);
			boolean over = inside(mouseX, mouseY, right - bw, y + 2, bw, 14);
			Render2D.roundedRect(right - bw, y + 2, bw, 14, 4, Theme.lerp(Theme.TRACK_OFF, Theme.PRIMARY, hover("choice:" + System.identityHashCode(choice), over)));
			textCentered(value, right - bw / 2, y + 5, Theme.WHITE);
		} else if (setting instanceof ColorSetting) {
			ColorSetting color = (ColorSetting) setting;
			int sw = 22;
			checker(right - sw, y + 3, sw, 12);
			DrawableHelper.fill(right - sw, y + 3, right, y + 15, color.argb());
			Render2D.outline(right - sw - 1, y + 2, sw + 2, 14, 0x55FFFFFF);
			String hex = color.toHex();
			text(hex, right - sw - 6 - textRenderer.getStringWidth(hex), y + 5, Theme.TEXT_MUTED);
		} else if (setting instanceof ButtonSetting) {
			ButtonSetting button = (ButtonSetting) setting;
			int bw = Math.max(50, textRenderer.getStringWidth(button.getLabel()) + 14);
			boolean over = inside(mouseX, mouseY, right - bw, y + 2, bw, 14);
			Render2D.roundedRect(right - bw, y + 2, bw, 14, 4, Theme.lerp(Theme.PRIMARY, Theme.PRIMARY_LIGHT, hover("button:" + System.identityHashCode(button), over)));
			textCentered(button.getLabel(), right - bw / 2, y + 5, Theme.WHITE);
		} else if (setting instanceof KeySetting) {
			KeySetting keySetting = (KeySetting) setting;
			boolean capturing = keySetting == capturingKey;
			String value = capturing ? "Aperte uma tecla..." : keySetting.format();
			int bw = Math.max(50, textRenderer.getStringWidth(value) + 12);
			Render2D.roundedRect(right - bw, y + 2, bw, 14, 4, capturing ? Theme.PRIMARY : Theme.TRACK_OFF);
			textCentered(value, right - bw / 2, y + 5, Theme.WHITE);
		} else if (setting instanceof TextSetting) {
			TextSetting textSetting = (TextSetting) setting;
			boolean editing = textSetting == editingText;
			int bx = right - TEXT_BOX_W;
			Render2D.roundedRect(bx, y + 2, TEXT_BOX_W, 14, 4, editing ? Theme.PRIMARY : Theme.TRACK_OFF);
			Render2D.roundedRect(bx + 1, y + 3, TEXT_BOX_W - 2, 12, 3, Theme.PANEL);
			String shown = textRenderer.trimToWidth(textSetting.get() + (editing ? caret() : ""), TEXT_BOX_W - 8, true);
			text(shown, bx + 4, y + 5, Theme.TEXT);
		}
	}

	private int controlWidth(Setting<?> setting) {
		if (setting == null || setting instanceof BooleanSetting) {
			return 24;
		}
		if (setting instanceof NumberSetting) {
			return SLIDER_W + 40;
		}
		if (setting instanceof TextSetting) {
			return TEXT_BOX_W;
		}
		if (setting instanceof ColorSetting) {
			return 80;
		}
		return 70;
	}

	private void drawSwitch(int x, int y, float on) {
		Render2D.roundedRect(x, y, 20, 10, 5, Theme.lerp(Theme.TRACK_OFF, Theme.PRIMARY, on));
		Render2D.circle(x + 5 + 10 * on, y + 5, 3.5F, Theme.WHITE);
	}

	private void drawSlider(int x, int y, NumberSetting setting) {
		float fraction = (float) setting.getFraction();
		Render2D.roundedRect(x, y, SLIDER_W, 4, 2, Theme.TRACK_OFF);
		if (fraction > 0) {
			Render2D.roundedRect(x, y, SLIDER_W * fraction, 4, 2, Theme.PRIMARY);
		}
		Render2D.circle(x + SLIDER_W * fraction, y + 2, 4F, Theme.WHITE);
	}

	// ------------------------------------------------------------------
	// Seletor de cor
	// ------------------------------------------------------------------

	private int pickerX() {
		return contentX + (contentW - PICKER_W) / 2;
	}

	private int pickerY() {
		return contentY + Math.max(0, (contentH - PICKER_H) / 2) - 6;
	}

	private void drawPicker(int mouseX, int mouseY) {
		int ox = pickerX();
		int oy = pickerY();
		DrawableHelper.fill(px, py, px + panelW, py + panelH, 0x66000000);
		Render2D.roundedRect(ox - 1, oy - 1, PICKER_W + 2, PICKER_H + 2, 7, Theme.withAlpha(Theme.PRIMARY_LIGHT, 0.5F));
		Render2D.roundedRect(ox, oy, PICKER_W, PICKER_H, 6, Theme.SIDEBAR);
		text(textRenderer.trimToWidth(picker.getName(), PICKER_W - 30), ox + 8, oy + 7, Theme.TEXT);
		boolean overClose = inside(mouseX, mouseY, ox + PICKER_W - 16, oy + 4, 12, 12);
		text("x", ox + PICKER_W - 12, oy + 6, overClose ? Theme.PRIMARY_LIGHT : Theme.TEXT_MUTED);

		// Prévia: cor nova em cima, cor original embaixo.
		int sx = ox + 8;
		int sy = oy + 22;
		checker(sx, sy, 40, 40);
		DrawableHelper.fill(sx, sy, sx + 40, sy + 28, picker.argb());
		DrawableHelper.fill(sx, sy + 28, sx + 40, sy + 40, pickerOriginal);
		Render2D.outline(sx - 1, sy - 1, 42, 42, 0x55FFFFFF);

		// Sliders R, G, B e alfa.
		int[] order = {1, 2, 3, 0};
		for (int i = 0; i < order.length; i++) {
			int channel = order[i];
			int rowY = oy + 22 + i * 11;
			int trackX = ox + 66;
			if (channel == draggingChannel) {
				int amount = Math.round((lastMouseX - trackX) * 255F / PICKER_SLIDER_W);
				picker.setChannel(channel, amount);
			}
			text(CHANNEL_NAMES[channel], ox + 56, rowY, Theme.TEXT_MUTED);
			int zero = withChannel(picker.argb() | 0xFF000000, channel, 0);
			int full = withChannel(picker.argb() | 0xFF000000, channel, 255);
			if (channel == 0) {
				checker(trackX, rowY + 1, PICKER_SLIDER_W, 6);
				zero = picker.rgb();
				full = 0xFF000000 | picker.rgb();
			}
			Render2D.gradientH(trackX, rowY + 1, PICKER_SLIDER_W, 6, zero, full);
			float knob = trackX + picker.channel(channel) * PICKER_SLIDER_W / 255F;
			DrawableHelper.fill((int) knob - 1, rowY - 1, (int) knob + 2, rowY + 9, Theme.WHITE);
			text(String.valueOf(picker.channel(channel)), ox + 66 + PICKER_SLIDER_W + 6, rowY, Theme.TEXT);
		}

		// Campo HEX.
		int hx = ox + 8;
		int hy = oy + 70;
		text("HEX", hx, hy + 3, Theme.TEXT_MUTED);
		Render2D.roundedRect(hx + 24, hy, 80, 14, 3, editingHex ? Theme.PRIMARY : Theme.TRACK_OFF);
		Render2D.roundedRect(hx + 25, hy + 1, 78, 12, 2, Theme.PANEL);
		text(editingHex ? hexBuffer + caret() : picker.toHex(), hx + 29, hy + 3, Theme.TEXT);

		// Paleta.
		int palY = oy + 92;
		text("Paleta", ox + 8, palY, Theme.TEXT_MUTED);
		for (int i = 0; i < ColorSetting.PALETTE.length; i++) {
			int cx = ox + 8 + (i % 8) * 15;
			int cy = palY + 11 + (i / 8) * 15;
			boolean over = inside(mouseX, mouseY, cx, cy, 12, 12);
			DrawableHelper.fill(cx, cy, cx + 12, cy + 12, ColorSetting.PALETTE[i]);
			Render2D.outline(cx - 1, cy - 1, 14, 14, over ? Theme.WHITE : 0x33FFFFFF);
		}

		// Botões.
		int bx = ox + PICKER_W - 58;
		int by = oy + PICKER_H - 22;
		boolean overOk = inside(mouseX, mouseY, bx, by, 50, 14);
		Render2D.roundedRect(bx, by, 50, 14, 4, Theme.lerp(Theme.PRIMARY, Theme.PRIMARY_LIGHT, hover("picker-ok", overOk)));
		textCentered("OK", bx + 25, by + 3, Theme.WHITE);
		int cx = bx - 64;
		boolean overCancel = inside(mouseX, mouseY, cx, by, 58, 14);
		Render2D.roundedRect(cx, by, 58, 14, 4, Theme.lerp(Theme.TRACK_OFF, Theme.CARD_HOVER, hover("picker-cancel", overCancel)));
		textCentered("Cancelar", cx + 29, by + 3, Theme.WHITE);
	}

	private boolean clickPicker(int mouseX, int mouseY, int button) {
		int ox = pickerX();
		int oy = pickerY();
		if (!inside(mouseX, mouseY, ox, oy, PICKER_W, PICKER_H)) {
			closePicker();
			return true;
		}
		applyHexIfEditing();
		if (inside(mouseX, mouseY, ox + PICKER_W - 16, oy + 4, 12, 12)
				|| inside(mouseX, mouseY, ox + PICKER_W - 58, oy + PICKER_H - 22, 50, 14)) {
			closePicker();
			return true;
		}
		if (inside(mouseX, mouseY, ox + PICKER_W - 122, oy + PICKER_H - 22, 58, 14)) {
			picker.set(pickerOriginal);
			closePicker();
			return true;
		}
		int[] order = {1, 2, 3, 0};
		for (int i = 0; i < order.length; i++) {
			int rowY = oy + 22 + i * 11;
			if (inside(mouseX, mouseY, ox + 60, rowY - 2, PICKER_SLIDER_W + 12, 11)) {
				draggingChannel = order[i];
				return true;
			}
		}
		if (inside(mouseX, mouseY, ox + 32, oy + 70, 80, 14)) {
			editingHex = true;
			hexBuffer = picker.toHex();
			return true;
		}
		for (int i = 0; i < ColorSetting.PALETTE.length; i++) {
			int cx = ox + 8 + (i % 8) * 15;
			int cy = oy + 103 + (i / 8) * 15;
			if (inside(mouseX, mouseY, cx, cy, 12, 12)) {
				// A paleta troca a cor, mas mantém a transparência escolhida.
				picker.set(picker.alpha() << 24 | (ColorSetting.PALETTE[i] & 0xFFFFFF));
				return true;
			}
		}
		return true;
	}

	private void openPicker(ColorSetting setting) {
		picker = setting;
		pickerOriginal = setting.argb();
		editingHex = false;
		draggingChannel = -1;
	}

	private void closePicker() {
		if (picker == null) {
			return;
		}
		applyHexIfEditing();
		picker = null;
		draggingChannel = -1;
		editingHex = false;
		if (settingsModule != null) {
			changed(settingsModule, null);
		}
	}

	private void applyHexIfEditing() {
		if (editingHex && picker != null) {
			picker.setHex(hexBuffer);
			editingHex = false;
		}
	}

	private static int withChannel(int argb, int channel, int amount) {
		int shift = (3 - channel) * 8;
		return (argb & ~(0xFF << shift)) | ((amount & 0xFF) << shift);
	}

	/** Xadrez cinza para mostrar a transparência das cores. */
	private static void checker(int x, int y, int w, int h) {
		int size = 4;
		for (int i = 0; i < w; i += size) {
			for (int j = 0; j < h; j += size) {
				int color = ((i / size + j / size) % 2 == 0) ? 0xFF9A9A9A : 0xFFD0D0D0;
				DrawableHelper.fill(x + i, y + j, x + Math.min(w, i + size), y + Math.min(h, j + size), color);
			}
		}
	}

	// ------------------------------------------------------------------
	// Entrada
	// ------------------------------------------------------------------

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		layout();
		if (capturingKey != null) {
			// Botões do mouse também podem virar tecla (ex.: botão lateral para o zoom).
			capturingKey.set(button - 100);
			stopEditing();
			return;
		}
		if (picker != null) {
			clickPicker(mouseX, mouseY, button);
			return;
		}
		if (editingText != null || editingNumber != null) {
			stopEditing();
		}
		if (!inside(mouseX, mouseY, px, py, panelW, panelH)) {
			return;
		}

		int y = py + 36;
		for (Category c : Category.values()) {
			if (button == 0 && inside(mouseX, mouseY, px + 6, y, SIDEBAR_W - 12, 16)) {
				category = c;
				closeSettings();
				scrollTarget = 0;
				return;
			}
			y += 18;
		}
		if (button == 0 && inside(mouseX, mouseY, px + 6, py + panelH - 24, SIDEBAR_W - 12, 16)) {
			client.setScreen(new HudEditorScreen(this));
			return;
		}

		if (settingsModule != null) {
			clickSettings(mouseX, mouseY, button);
		} else {
			clickModules(mouseX, mouseY, button);
		}
	}

	private void clickModules(int mouseX, int mouseY, int button) {
		if (!inside(mouseX, mouseY, contentX, contentY, contentW, contentH)) {
			return;
		}
		List<Module> modules = VfxCore.get().getModules().byCategory(category);
		int cols = Math.max(1, (contentW + GAP) / (CARD_MIN_W + GAP));
		int cardW = (contentW - GAP * (cols - 1)) / cols;
		int scroll = Math.round(scrollAnim.get());
		for (int i = 0; i < modules.size(); i++) {
			Module module = modules.get(i);
			int x = contentX + (i % cols) * (cardW + GAP);
			int y = contentY + (i / cols) * (CARD_H + GAP) - scroll;
			if (!inside(mouseX, mouseY, x, y, cardW, CARD_H)) {
				continue;
			}
			boolean hasSettings = !module.getSettings().isEmpty();
			boolean onOptions = inside(mouseX, mouseY, x + 6, y + CARD_H - 13, 40, 11);
			if (hasSettings && (button == 1 || (button == 0 && (onOptions || module.isAlwaysOn())))) {
				openSettings(module);
			} else if (button == 0) {
				module.toggle();
				changed(module, null);
			}
			return;
		}
	}

	private void clickSettings(int mouseX, int mouseY, int button) {
		if (button == 0 && inside(mouseX, mouseY, contentX - 2, py + 8, 50, 12)) {
			closeSettings();
			return;
		}
		int bw = 96;
		if (button == 0 && inside(mouseX, mouseY, contentX + contentW - bw, contentY + contentH - 16, bw, 16)) {
			settingsModule.resetToDefaults();
			changed(settingsModule, null);
			return;
		}
		if (!inside(mouseX, mouseY, contentX, contentY, listW, settingsListHeight())) {
			return;
		}
		int y = contentY - Math.round(settingsScrollAnim.get());
		for (Setting<?> setting : settingRows(settingsModule)) {
			if (inside(mouseX, mouseY, contentX, y, listW, ROW_H - 2)) {
				clickRow(setting, mouseX, button);
				return;
			}
			y += ROW_H;
		}
	}

	private void clickRow(Setting<?> setting, int mouseX, int button) {
		int right = contentX + listW - 6;
		if (setting instanceof ChoiceSetting) {
			((ChoiceSetting) setting).cycle(button == 1 ? -1 : 1);
			changed(settingsModule, setting);
			return;
		}
		if (setting instanceof NumberSetting) {
			NumberSetting number = (NumberSetting) setting;
			int sx = right - SLIDER_W;
			if (button == 1 || mouseX < sx - 4) {
				// Botão direito ou clique no valor: digitar o número.
				editingNumber = number;
				numberBuffer = number.format().replaceAll("[^0-9.,-]", "");
			} else if (button == 0) {
				draggingSlider = number;
				sliderX = sx;
			}
			return;
		}
		if (button != 0) {
			return;
		}
		if (setting == null) {
			settingsModule.toggle();
			changed(settingsModule, null);
		} else if (setting instanceof BooleanSetting) {
			((BooleanSetting) setting).toggle();
			changed(settingsModule, setting);
		} else if (setting instanceof ColorSetting) {
			openPicker((ColorSetting) setting);
		} else if (setting instanceof ButtonSetting) {
			((ButtonSetting) setting).press();
			changed(settingsModule, setting);
		} else if (setting instanceof KeySetting) {
			capturingKey = (KeySetting) setting;
		} else if (setting instanceof TextSetting) {
			editingText = (TextSetting) setting;
		}
	}

	/**
	 * Avisa o módulo e a config de que o jogador mudou algo. Mudanças manuais em
	 * Desempenho deixam o preset de FPS como "Personalizado".
	 */
	private void changed(Module module, Setting<?> setting) {
		module.onSettingChanged(setting);
		if (module.getCategory() == Category.PERFORMANCE && !(module instanceof PerformanceProfileModule)) {
			VfxCore.get().getModules().get(PerformanceProfileModule.class).markCustom();
		}
		VfxCore.get().getConfig().markDirty();
	}

	private void openSettings(Module module) {
		module.onSettingsOpened();
		settingsModule = module;
		settingsScrollTarget = 0;
		stopEditing();
	}

	private void closeSettings() {
		closePicker();
		settingsModule = null;
		stopEditing();
	}

	private void stopEditing() {
		if (editingNumber != null) {
			try {
				editingNumber.set(Double.parseDouble(numberBuffer.replace(',', '.')));
			} catch (NumberFormatException ignored) {
				// Valor inválido: mantém o anterior.
			}
		}
		Setting<?> edited = editingText != null ? editingText : capturingKey != null ? capturingKey : editingNumber;
		if (edited != null && settingsModule != null) {
			changed(settingsModule, edited);
		}
		editingText = null;
		capturingKey = null;
		editingNumber = null;
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		if (draggingChannel >= 0) {
			draggingChannel = -1;
		}
		if (draggingSlider != null) {
			NumberSetting released = draggingSlider;
			draggingSlider = null;
			if (settingsModule != null) {
				changed(settingsModule, released);
			}
		}
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int wheel = Mouse.getEventDWheel();
		if (wheel == 0 || picker != null) {
			return;
		}
		if (settingsModule == null) {
			scrollTarget -= Math.signum(wheel) * (CARD_H + GAP) * 0.75F;
		} else {
			settingsScrollTarget -= Math.signum(wheel) * ROW_H * 1.5F;
		}
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (capturingKey != null) {
			if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
				capturingKey.set(KeySetting.NONE);
			} else if (keyCode != Keyboard.KEY_ESCAPE && keyCode != Keyboard.KEY_NONE) {
				capturingKey.set(keyCode);
			}
			stopEditing();
			return;
		}
		if (picker != null) {
			keyPicker(character, keyCode);
			return;
		}
		if (editingNumber != null) {
			if (keyCode == Keyboard.KEY_ESCAPE) {
				editingNumber = null;
			} else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
				stopEditing();
			} else if (keyCode == Keyboard.KEY_BACK) {
				numberBuffer = numberBuffer.isEmpty() ? "" : numberBuffer.substring(0, numberBuffer.length() - 1);
			} else if ((Character.isDigit(character) || character == '.' || character == ',' || character == '-') && numberBuffer.length() < 10) {
				numberBuffer += character;
			}
			return;
		}
		if (editingText != null) {
			if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
				stopEditing();
			} else if (keyCode == Keyboard.KEY_BACK) {
				String value = editingText.get();
				if (!value.isEmpty()) {
					editingText.set(value.substring(0, value.length() - 1));
				}
			} else if (character >= 32 && character != 127 && character != '§') {
				editingText.set(editingText.get() + character);
			}
			return;
		}
		if (keyCode == Keyboard.KEY_ESCAPE) {
			if (settingsModule != null) {
				closeSettings();
			} else {
				client.setScreen(null);
			}
		} else if (keyCode == VfxCore.get().getModules().get(MenuModule.class).getKey()) {
			client.setScreen(null);
		}
	}

	private void keyPicker(char character, int keyCode) {
		if (!editingHex) {
			if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
				closePicker();
			}
			return;
		}
		if (keyCode == Keyboard.KEY_ESCAPE) {
			editingHex = false;
		} else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
			applyHexIfEditing();
		} else if (keyCode == Keyboard.KEY_BACK) {
			hexBuffer = hexBuffer.isEmpty() ? "" : hexBuffer.substring(0, hexBuffer.length() - 1);
		} else if (hexBuffer.length() < 9 && (character == '#' || "0123456789abcdefABCDEF".indexOf(character) >= 0)) {
			hexBuffer += Character.toUpperCase(character);
			// Aplica na hora quando o código já está completo, para a prévia acompanhar.
			Integer parsed = ColorSetting.parseHex(hexBuffer, picker.alpha());
			String digits = hexBuffer.replace("#", "");
			if (parsed != null && (digits.length() == 6 || digits.length() == 8)) {
				picker.set(parsed);
			}
		}
	}

	// ------------------------------------------------------------------
	// Utilidades
	// ------------------------------------------------------------------

	private static String caret() {
		return (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "";
	}

	private float hover(Object key, boolean hovered) {
		Anim anim = hoverAnims.get(key);
		if (anim == null) {
			anim = new Anim(0F);
			hoverAnims.put(key, anim);
		}
		return anim.update(hovered ? 1F : 0F, 18F);
	}

	private float toggle(Object key, boolean on) {
		Anim anim = toggleAnims.get(key);
		if (anim == null) {
			anim = new Anim(on ? 1F : 0F);
			toggleAnims.put(key, anim);
		}
		return anim.update(on ? 1F : 0F, 16F);
	}

	private void text(String value, float x, float y, int color) {
		textRenderer.draw(value, x, y, color, false);
	}

	private void textCentered(String value, int centerX, int y, int color) {
		text(value, centerX - textRenderer.getStringWidth(value) / 2F, y, color);
	}

	private static boolean inside(int mx, int my, float x, float y, float w, float h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
