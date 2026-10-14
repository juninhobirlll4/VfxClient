package com.vfxclient.core.gui;

import java.util.List;
import java.util.Locale;

import com.vfxclient.core.emote.Emote;
import com.vfxclient.core.emote.Emotes;
import com.vfxclient.core.gui.font.Fonts;
import com.vfxclient.core.module.social.EmotesModule;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/**
 * Roda de emotes: segure a tecla (B), aponte o mouse para um emote e solte. Clicar também
 * escolhe; ESC fecha sem fazer nada.
 */
public class EmoteWheelScreen extends Screen {

	private static final float RING_RADIUS = 250;
	private static final float SLOT_W = 190;
	private static final float SLOT_H = 74;
	private static final float CENTER_RADIUS = 104;
	/** Perto do centro não escolhe nada (dá para desistir soltando a tecla ali). */
	private static final float DEAD_ZONE = 70;

	private final EmotesModule module;
	private final int key;
	private final long openedAt = System.currentTimeMillis();
	private boolean keyWasHeld;
	private Canvas canvas;
	private int hovered = -1;

	public EmoteWheelScreen(EmotesModule module, int key) {
		this.module = module;
		this.key = key;
		this.keyWasHeld = key > 0 && Keyboard.isKeyDown(key);
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		fillGradient(0, 0, width, height, 0x66050309, 0x99050309);

		canvas = Canvas.begin(width, height);
		float appear = Anim.easeOutCubic((System.currentTimeMillis() - openedAt) / 180F);
		hovered = slotAt(canvas.toCanvas(mouseX), canvas.toCanvas(mouseY));

		List<Emote> emotes = Emotes.ALL;
		for (int i = 0; i < emotes.size(); i++) {
			drawSlot(i, emotes.get(i), appear);
		}
		drawCenter();

		canvas.end();
		Ui.restore();
		releaseCheck();
	}

	private void drawSlot(int index, Emote emote, float appear) {
		double angle = slotAngle(index);
		float radius = RING_RADIUS * (0.85F + 0.15F * appear);
		float cx = canvas.centerX() + (float) Math.cos(angle) * radius;
		float cy = canvas.centerY() + (float) Math.sin(angle) * radius;
		float x = cx - SLOT_W / 2F;
		float y = cy - SLOT_H / 2F;

		boolean selected = index == hovered;
		int border = selected ? Ui.PURPLE_LIGHT : 0x40FFFFFF;
		int fill = selected ? 0xF2341B63 : 0xD9100C18;
		Render2D.outlinedRect(x, y, SLOT_W, SLOT_H, 14, border, fill);
		Ui.restore();

		String label = emote.name().toUpperCase(Locale.ROOT);
		float textY = cy - Fonts.lineHeight(Fonts.BUTTON, 19) / 2F;
		Fonts.drawSpacedCentered(Fonts.BUTTON, label, cx, textY, 19, 0.05F, selected ? 0xFFFFFFFF : 0xFFD8D2E6);
	}

	private void drawCenter() {
		float cx = canvas.centerX();
		float cy = canvas.centerY();
		Render2D.circle(cx, cy, CENTER_RADIUS, 0xE60A0810);
		Render2D.ring(cx, cy, CENTER_RADIUS, 2F, 0x40B48CFF);
		Ui.restore();

		String title = hovered >= 0 ? Emotes.ALL.get(hovered).name() : "Emotes";
		Fonts.drawCentered(Fonts.TITLE, title, cx, cy - 26, 26, 0xFFFFFFFF);
		String hint = keyWasHeld ? "solte para escolher" : "clique para escolher";
		Fonts.drawCentered(Fonts.TEXT, hint, cx, cy + 12, 15, Ui.MUTED);
	}

	/** Soltou a tecla que abriu a roda: faz o emote apontado (ou só fecha, se for o centro). */
	private void releaseCheck() {
		if (keyWasHeld && !Keyboard.isKeyDown(key)) {
			choose();
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button == 0) {
			choose();
		}
	}

	private void choose() {
		if (hovered >= 0) {
			module.play(Emotes.ALL.get(hovered));
		}
		client.setScreen(null);
	}

	/** @return índice do emote na direção do mouse, ou -1 perto do centro */
	private int slotAt(float mx, float my) {
		float dx = mx - canvas.centerX();
		float dy = my - canvas.centerY();
		if (dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) {
			return -1;
		}
		int count = Emotes.ALL.size();
		double step = Math.PI * 2 / count;
		double angle = Math.atan2(dy, dx) - slotAngle(0) + step / 2;
		angle = (angle % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
		return (int) (angle / step) % count;
	}

	/** O primeiro emote fica em cima; os outros seguem no sentido do relógio. */
	private static double slotAngle(int index) {
		return -Math.PI / 2 + index * Math.PI * 2 / Emotes.ALL.size();
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}
}
