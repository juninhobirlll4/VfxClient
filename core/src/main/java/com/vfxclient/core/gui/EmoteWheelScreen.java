package com.vfxclient.core.gui;

import java.util.List;
import java.util.Locale;

import com.vfxclient.core.emote.Emote;
import com.vfxclient.core.emote.Emotes;
import com.vfxclient.core.gui.font.Fonts;
import com.vfxclient.core.module.setting.KeySetting;
import com.vfxclient.core.module.social.EmotesModule;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Roda de emotes: um anel com 8 emotes por página, cada um com o bonequinho da pose.
 *
 * <ul>
 *   <li>Segure a tecla (B), aponte o mouse e solte; clicar também escolhe;</li>
 *   <li>a rodinha do mouse (ou as setas) troca de página;</li>
 *   <li>apertar uma tecla com o mouse sobre um emote cria o atalho dele (Delete tira);</li>
 *   <li>ESC fecha sem fazer nada.</li>
 * </ul>
 */
public class EmoteWheelScreen extends Screen {

	private static final float RING_OUTER = 318;
	private static final float RING_INNER = 122;
	private static final float GAP = 0.025F;
	private static final float ICON_SIZE = 132;
	private static final long MESSAGE_MS = 2200;

	private final EmotesModule module;
	private final int key;
	private final long openedAt = System.currentTimeMillis();
	private final boolean keyWasHeld;
	private boolean ignoreRelease;
	private Canvas canvas;
	private int page;
	private int hovered = -1;
	private String message;
	private long messageAt;

	public EmoteWheelScreen(EmotesModule module, int key) {
		this.module = module;
		this.key = key;
		this.keyWasHeld = key > 0 && Keyboard.isKeyDown(key);
		// -Dvfx.wheelPage / -Dvfx.wheelHover: só para os prints de teste.
		this.page = Integer.getInteger("vfx.wheelPage", module.wheelPage());
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		fillGradient(0, 0, width, height, 0x66050309, 0x99050309);

		canvas = Canvas.begin(width, height);
		float appear = Anim.easeOutCubic((System.currentTimeMillis() - openedAt) / 180F);
		hovered = Integer.getInteger("vfx.wheelHover", slotAt(canvas.toCanvas(mouseX), canvas.toCanvas(mouseY)));

		List<Emote> emotes = Emotes.page(page);
		for (int i = 0; i < emotes.size(); i++) {
			drawSlot(i, emotes.get(i), appear);
		}
		drawCenter(emotes);
		drawFooter();

		canvas.end();
		Ui.restore();

		if (keyWasHeld && !ignoreRelease && !Keyboard.isKeyDown(key)) {
			choose();
		}
	}

	private void drawSlot(int index, Emote emote, float appear) {
		boolean selected = index == hovered;
		double angle = slotAngle(index);
		float grow = 0.9F + 0.1F * appear;
		float outer = RING_OUTER * grow + (selected ? 10 : 0);

		int fill = selected ? 0xF0382066 : 0xE60F0B17;
		Render2D.ringSegment(canvas.centerX(), canvas.centerY(), RING_INNER * grow, outer,
				angle - Math.PI / 8 + GAP, angle + Math.PI / 8 - GAP, fill);
		if (selected) {
			arcOutline(angle, outer);
		}

		float mid = (RING_INNER + RING_OUTER) / 2F * grow;
		float x = canvas.centerX() + (float) Math.cos(angle) * mid;
		float y = canvas.centerY() + (float) Math.sin(angle) * mid;
		PoseIcon.draw(emote, x, y, ICON_SIZE * (selected ? 1.08F : 1F), selected ? 0.35F : 0F);

		// Número da posição, perto da borda de fora.
		float labelRadius = RING_OUTER * grow - 22;
		String number = String.valueOf(index + 1);
		Fonts.drawCentered(Fonts.TEXT_BOLD, number, canvas.centerX() + (float) Math.cos(angle) * labelRadius,
				canvas.centerY() + (float) Math.sin(angle) * labelRadius - 8, 16, Ui.MUTED);

		drawKeyChip(emote, angle, grow);
	}

	/** Contorno de luz na borda de fora da fatia apontada. */
	private void arcOutline(double angle, float outer) {
		Render2D.ringSegment(canvas.centerX(), canvas.centerY(), outer - 3, outer,
				angle - Math.PI / 8 + GAP, angle + Math.PI / 8 - GAP, Ui.PURPLE_LIGHT);
	}

	/** Etiqueta com a tecla de atalho do emote (se tiver). */
	private void drawKeyChip(Emote emote, double angle, float grow) {
		int bound = module.boundKey(emote);
		if (bound == KeySetting.NONE) {
			return;
		}
		String label = Keyboard.getKeyName(bound).toUpperCase(Locale.ROOT);
		float radius = RING_INNER * grow + 28;
		float chipW = Fonts.width(Fonts.TEXT_BOLD, label, 14) + 16;
		float x = canvas.centerX() + (float) Math.cos(angle) * radius - chipW / 2F;
		float y = canvas.centerY() + (float) Math.sin(angle) * radius - 12;

		Render2D.outlinedRect(x, y, chipW, 24, 8, Ui.PURPLE_LIGHT, 0xF0201238);
		Ui.restore();
		Fonts.drawCentered(Fonts.TEXT_BOLD, label, x + chipW / 2F, y + 4, 14, 0xFFFFFFFF);
	}

	private void drawCenter(List<Emote> emotes) {
		float cx = canvas.centerX();
		float cy = canvas.centerY();
		Render2D.circle(cx, cy, RING_INNER - 12, 0xF0140E22);
		Render2D.ring(cx, cy, RING_INNER - 12, 2F, 0x55B48CFF);
		Ui.restore();

		Logo.draw((int) (cx - 28), (int) (cy - 96), 56);
		Ui.restore();

		boolean has = hovered >= 0 && hovered < emotes.size();
		String title = has ? emotes.get(hovered).name() : "Emotes";
		Fonts.drawCentered(Fonts.TITLE, title, cx, cy - 22, 27, 0xFFFFFFFF);
		String sub = has ? shortcutHint(emotes.get(hovered)) : Emotes.PAGE_NAMES.get(page);
		Fonts.drawCentered(Fonts.TEXT, sub, cx, cy + 18, 15, Ui.MUTED);
	}

	private String shortcutHint(Emote emote) {
		return module.boundKey(emote) == KeySetting.NONE ? "aperte uma tecla: atalho" : "Delete: tirar atalho";
	}

	/** Página, ícone da rodinha do mouse e dicas, embaixo da roda. */
	private void drawFooter() {
		float cx = canvas.centerX();
		float y = canvas.centerY() + RING_OUTER + 22;

		String pageText = (page + 1) + "/" + Emotes.pages();
		Fonts.drawCentered(Fonts.TITLE, pageText, cx, y, 26, 0xFFFFFFFF);
		drawMouseIcon(cx + 70, y + 2);

		String name = Emotes.PAGE_NAMES.get(page);
		Fonts.drawCentered(Fonts.TEXT, name + " · rodinha do mouse troca de página", cx, y + 36, 15, Ui.MUTED);

		if (message != null && System.currentTimeMillis() - messageAt < MESSAGE_MS) {
			Fonts.drawCentered(Fonts.TEXT_BOLD, message, cx, canvas.centerY() - RING_OUTER - 56, 20, Ui.GREEN);
		}
	}

	/** Mouse com setas para cima e para baixo (a rodinha troca de página). */
	private void drawMouseIcon(float x, float y) {
		Render2D.roundedOutline(x, y, 24, 36, 12, 2F, 0xFFFFFFFF);
		Render2D.rawRect(x + 11, y + 7, 2, 8, 0xFFFFFFFF);
		Render2D.triangle(x + 34, y + 12, x + 46, y + 12, x + 40, y + 3, 0xFFFFFFFF);
		Render2D.triangle(x + 34, y + 24, x + 46, y + 24, x + 40, y + 33, 0xFFFFFFFF);
		Ui.restore();
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) {
			changePage(wheel > 0 ? -1 : 1);
		}
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			client.setScreen(null);
		} else if (keyCode == Keyboard.KEY_LEFT) {
			changePage(-1);
		} else if (keyCode == Keyboard.KEY_RIGHT) {
			changePage(1);
		} else if (keyCode != key && keyCode != Keyboard.KEY_NONE) {
			bindKey(keyCode);
		}
	}

	/** Com o mouse sobre um emote: a tecla vira o atalho dele (Delete/Backspace tira). */
	private void bindKey(int keyCode) {
		List<Emote> emotes = Emotes.page(page);
		if (hovered < 0 || hovered >= emotes.size()) {
			return;
		}
		Emote emote = emotes.get(hovered);
		ignoreRelease = true;

		if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
			module.unbind(emote);
			say(emote.name() + ": atalho removido");
		} else {
			module.bind(emote, keyCode);
			say(emote.name() + ": atalho " + Keyboard.getKeyName(keyCode));
		}
	}

	private void say(String text) {
		message = text;
		messageAt = System.currentTimeMillis();
	}

	private void changePage(int direction) {
		int pages = Emotes.pages();
		page = (page + direction + pages) % pages;
		module.setWheelPage(page);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button == 0) {
			choose();
		}
	}

	private void choose() {
		List<Emote> emotes = Emotes.page(page);
		if (hovered >= 0 && hovered < emotes.size()) {
			module.play(emotes.get(hovered));
		}
		client.setScreen(null);
	}

	/** @return índice do emote na direção do mouse, ou -1 no centro da roda */
	private int slotAt(float mx, float my) {
		float dx = mx - canvas.centerX();
		float dy = my - canvas.centerY();
		if (dx * dx + dy * dy < (RING_INNER - 12) * (RING_INNER - 12)) {
			return -1;
		}
		int count = Emotes.PER_PAGE;
		double step = Math.PI * 2 / count;
		double angle = Math.atan2(dy, dx) - slotAngle(0) + step / 2;
		angle = (angle % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
		int slot = (int) (angle / step) % count;
		return slot < Emotes.page(page).size() ? slot : -1;
	}

	/** O primeiro emote fica em cima; os outros seguem no sentido do relógio. */
	private static double slotAngle(int index) {
		return -Math.PI / 2 + index * Math.PI * 2 / Emotes.PER_PAGE;
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}
}
