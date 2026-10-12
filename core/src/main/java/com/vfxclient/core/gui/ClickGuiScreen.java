package com.vfxclient.core.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.module.setting.Setting;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Menu principal do VfxClient, aberto com Right Shift.
 *
 * <p>Layout: barra lateral com a marca e as categorias; à direita, cartões de módulos
 * (clique liga/desliga, botão direito ou "Opções" abre as configurações do módulo).</p>
 */
public class ClickGuiScreen extends Screen {

	private static final int SIDEBAR_W = 96;
	private static final int CARD_MIN_W = 110;
	private static final int CARD_H = 46;
	private static final int GAP = 6;
	private static final int ROW_H = 22;
	private static final int SLIDER_W = 90;
	private static final long OPEN_ANIM_MS = 220L;

	/** Lembra a última categoria entre aberturas do menu. */
	private static Category lastCategory = Category.HUD;

	private final Map<Object, Anim> hoverAnims = new HashMap<Object, Anim>();
	private final Map<Object, Anim> toggleAnims = new HashMap<Object, Anim>();
	private final Anim scrollAnim = new Anim(0F);
	private final long openedAt = System.currentTimeMillis();

	private Category category = lastCategory;
	private Module settingsModule;
	private NumberSetting draggingSlider;
	private int sliderX;
	private float scrollTarget;

	// Layout calculado a cada frame.
	private int px;
	private int py;
	private int panelW;
	private int panelH;
	private int contentX;
	private int contentY;
	private int contentW;
	private int contentH;

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
		VfxCore.get().getConfig().save();
	}

	// ------------------------------------------------------------------
	// Desenho
	// ------------------------------------------------------------------

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		layout();
		float open = Anim.easeOutCubic((System.currentTimeMillis() - openedAt) / (float) OPEN_ANIM_MS);

		DrawableHelper.fill(0, 0, width, height, Theme.withAlpha(Theme.BACKDROP, open));

		GlStateManager.pushMatrix();
		float scale = 0.92F + 0.08F * open;
		GlStateManager.translate(width / 2F, height / 2F, 0F);
		GlStateManager.scale(scale, scale, 1F);
		GlStateManager.translate(-width / 2F, -height / 2F, 0F);

		Render2D.roundedRect(px, py, panelW, panelH, 8, Theme.PANEL);
		drawSidebar(mouseX, mouseY);
		if (settingsModule != null) {
			drawSettings(mouseX, mouseY);
		} else {
			drawModules(mouseX, mouseY);
		}

		GlStateManager.popMatrix();
	}

	private void layout() {
		panelW = Math.min(width - 24, 400);
		panelH = Math.min(height - 24, 240);
		px = (width - panelW) / 2;
		py = (height - panelH) / 2;
		contentX = px + SIDEBAR_W + 12;
		contentY = py + 46;
		contentW = panelW - SIDEBAR_W - 24;
		contentH = panelH - 46 - 10;
	}

	private void drawSidebar(int mouseX, int mouseY) {
		Render2D.roundedRect(px, py, SIDEBAR_W, panelH, 8, Theme.SIDEBAR);
		DrawableHelper.fill(px + SIDEBAR_W - 8, py, px + SIDEBAR_W, py + panelH, Theme.SIDEBAR);

		Logo.draw(px + 10, py + 10, 18);
		text("§l" + VfxCore.NAME, px + 33, py + 15, Theme.TEXT);

		int y = py + 40;
		for (Category c : Category.values()) {
			boolean hovered = inside(mouseX, mouseY, px + 6, y, SIDEBAR_W - 12, 18);
			float hover = hover(c, hovered);
			int bg = c == category ? Theme.PRIMARY : Theme.withAlpha(Theme.CARD_HOVER, hover);
			Render2D.roundedRect(px + 6, y, SIDEBAR_W - 12, 18, 5, bg);
			text(c.getDisplayName(), px + 14, y + 5, c == category ? Theme.WHITE : Theme.lerp(Theme.TEXT_MUTED, Theme.TEXT, hover));
			y += 21;
		}

		int by = py + panelH - 26;
		boolean hovered = inside(mouseX, mouseY, px + 6, by, SIDEBAR_W - 12, 18);
		float hover = hover("hud-editor", hovered);
		Render2D.roundedRect(px + 6, by, SIDEBAR_W - 12, 18, 5, Theme.lerp(Theme.TRACK_OFF, Theme.PRIMARY, hover));
		textCentered("Editar HUD", px + SIDEBAR_W / 2, by + 5, Theme.WHITE);

		text("v" + VfxCore.get().getVersion(), px + 10, by - 13, Theme.TEXT_MUTED);
	}

	private void drawHeader(String title, String subtitle) {
		text("§l" + title, contentX, py + 12, Theme.TEXT);
		text(subtitle, contentX, py + 24, Theme.TEXT_MUTED);
		Render2D.gradientH(contentX, py + 38, contentW, 1, Theme.PRIMARY, Theme.withAlpha(Theme.WHITE, 0F));
	}

	private void drawModules(int mouseX, int mouseY) {
		drawHeader(category.getDisplayName(), category.getDescription());
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
		int base = Theme.lerp(Theme.CARD, Theme.CARD_ENABLED, on);
		Render2D.roundedRect(x, y, w, CARD_H, 6, Theme.lerp(base, Theme.CARD_HOVER, hover * 0.6F));
		if (on > 0.01F) {
			Render2D.gradientH(x + 6, y, (w - 12) * on, 1, Theme.PRIMARY_LIGHT, Theme.withAlpha(Theme.WHITE, 0F));
		}

		text(module.getName(), x + 8, y + 8, Theme.TEXT);
		text(textRenderer.trimToWidth(module.getDescription(), w - 16), x + 8, y + 19, Theme.TEXT_MUTED);

		if (!module.getSettings().isEmpty()) {
			boolean linkHovered = hovered && inside(mouseX, mouseY, x + 6, y + CARD_H - 15, 40, 12);
			text("Opções", x + 8, y + CARD_H - 13, linkHovered ? Theme.PRIMARY_LIGHT : Theme.TEXT_MUTED);
		}
		drawSwitch(x + w - 28, y + CARD_H - 16, on);
	}

	private void drawSettings(int mouseX, int mouseY) {
		Module module = settingsModule;
		boolean backHovered = inside(mouseX, mouseY, contentX - 2, py + 10, 60, 12);
		String back = "< Voltar";
		text(back, contentX, py + 12, backHovered ? Theme.PRIMARY_LIGHT : Theme.TEXT_MUTED);
		text("§l" + module.getName(), contentX + textRenderer.getStringWidth(back) + 8, py + 12, Theme.TEXT);
		text(module.getDescription(), contentX, py + 24, Theme.TEXT_MUTED);
		Render2D.gradientH(contentX, py + 38, contentW, 1, Theme.PRIMARY, Theme.withAlpha(Theme.WHITE, 0F));

		int y = contentY;
		y = drawRow("Ativado", y, mouseX, mouseY, module, null);
		for (Setting<?> setting : module.getSettings()) {
			y = drawRow(setting.getName(), y, mouseX, mouseY, module, setting);
		}

		int bw = 104;
		int bx = contentX + contentW - bw;
		int by = contentY + contentH - 18;
		boolean resetHovered = inside(mouseX, mouseY, bx, by, bw, 18);
		Render2D.roundedRect(bx, by, bw, 18, 5, Theme.lerp(Theme.TRACK_OFF, Theme.PRIMARY, hover("reset", resetHovered)));
		textCentered("Restaurar padrão", bx + bw / 2, by + 5, Theme.WHITE);
	}

	private int drawRow(String label, int y, int mouseX, int mouseY, Module module, Setting<?> setting) {
		boolean hovered = inside(mouseX, mouseY, contentX, y, contentW, ROW_H - 2);
		Object key = setting == null ? module : setting;
		Render2D.roundedRect(contentX, y, contentW, ROW_H - 2, 5, Theme.lerp(Theme.CARD, Theme.CARD_HOVER, hover("row:" + key.hashCode(), hovered)));
		text(label, contentX + 8, y + 6, Theme.TEXT);

		if (setting == null) {
			drawSwitch(contentX + contentW - 28, y + 5, toggle(module, module.isEnabled()));
		} else if (setting instanceof BooleanSetting) {
			drawSwitch(contentX + contentW - 28, y + 5, toggle(setting, ((BooleanSetting) setting).get()));
		} else if (setting instanceof NumberSetting) {
			NumberSetting number = (NumberSetting) setting;
			int sx = contentX + contentW - SLIDER_W - 8;
			if (number == draggingSlider) {
				number.setFraction(Math.max(0, Math.min(1, (mouseX - sliderX) / (double) SLIDER_W)));
			}
			drawSlider(sx, y + 8, number);
			String value = number.format();
			text(value, sx - textRenderer.getStringWidth(value) - 6, y + 6, Theme.TEXT_MUTED);
		}
		return y + ROW_H;
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
	// Entrada
	// ------------------------------------------------------------------

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		layout();
		if (!inside(mouseX, mouseY, px, py, panelW, panelH)) {
			return;
		}

		// Barra lateral: categorias e editor de HUD.
		int y = py + 40;
		for (Category c : Category.values()) {
			if (button == 0 && inside(mouseX, mouseY, px + 6, y, SIDEBAR_W - 12, 18)) {
				category = c;
				settingsModule = null;
				scrollTarget = 0;
				return;
			}
			y += 21;
		}
		if (button == 0 && inside(mouseX, mouseY, px + 6, py + panelH - 26, SIDEBAR_W - 12, 18)) {
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
			boolean onOptions = inside(mouseX, mouseY, x + 6, y + CARD_H - 15, 40, 12);
			if (hasSettings && (button == 1 || (button == 0 && onOptions))) {
				settingsModule = module;
			} else if (button == 0) {
				module.toggle();
			}
			return;
		}
	}

	private void clickSettings(int mouseX, int mouseY, int button) {
		if (button != 0) {
			return;
		}
		if (inside(mouseX, mouseY, contentX - 2, py + 10, 60, 12)) {
			settingsModule = null;
			return;
		}
		int bw = 104;
		if (inside(mouseX, mouseY, contentX + contentW - bw, contentY + contentH - 18, bw, 18)) {
			settingsModule.resetToDefaults();
			return;
		}

		int y = contentY;
		if (inside(mouseX, mouseY, contentX, y, contentW, ROW_H - 2)) {
			settingsModule.toggle();
			return;
		}
		y += ROW_H;
		for (Setting<?> setting : settingsModule.getSettings()) {
			if (inside(mouseX, mouseY, contentX, y, contentW, ROW_H - 2)) {
				if (setting instanceof BooleanSetting) {
					((BooleanSetting) setting).toggle();
					VfxCore.get().getConfig().markDirty();
				} else if (setting instanceof NumberSetting) {
					int sx = contentX + contentW - SLIDER_W - 8;
					if (mouseX >= sx - 4) {
						draggingSlider = (NumberSetting) setting;
						sliderX = sx;
					}
				}
				return;
			}
			y += ROW_H;
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		if (draggingSlider != null) {
			draggingSlider = null;
			VfxCore.get().getConfig().markDirty();
		}
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0 && settingsModule == null) {
			scrollTarget -= Math.signum(wheel) * (CARD_H + GAP) * 0.75F;
		}
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			if (settingsModule != null) {
				settingsModule = null;
			} else {
				client.setScreen(null);
			}
		} else if (keyCode == VfxCore.get().getConfig().getClickGuiKey()) {
			client.setScreen(null);
		}
	}

	// ------------------------------------------------------------------
	// Utilidades
	// ------------------------------------------------------------------

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
