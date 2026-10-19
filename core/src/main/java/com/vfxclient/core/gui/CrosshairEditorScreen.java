package com.vfxclient.core.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.font.Fonts;
import com.vfxclient.core.module.util.CrosshairDrawing;
import com.vfxclient.core.module.util.CrosshairModule;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import org.lwjgl.input.Keyboard;

/**
 * Editor da mira: uma grade de 17x17 onde o jogador pinta a mira do jeito que quiser, com
 * cores, espelho, desfazer e predefinições. A prévia mostra o resultado em tamanho real e
 * ampliado, sobre o céu e sobre um fundo escuro.
 */
public class CrosshairEditorScreen extends Screen {

	private static final int CLEAR = 1;
	private static final int UNDO = 2;
	private static final int MIRROR_X = 3;
	private static final int MIRROR_Y = 4;
	private static final int DONE = 5;
	private static final int PRESET = 10;
	private static final int BRUSH = 30;

	private static final float CELL = 34;
	private static final float GRID_X = 190;
	private static final float GRID_Y = 215;
	private static final float PANEL_X = 900;
	private static final int UNDO_LIMIT = 40;

	private final CrosshairModule module;
	private final Screen parent;
	private final List<ClickArea> areas = new ArrayList<ClickArea>();
	private final Deque<String> undo = new ArrayDeque<String>();
	private char[] cells;
	private int brush = 1;
	private boolean mirrorX;
	private boolean mirrorY;
	private int stroke = -1;
	private Canvas canvas;

	public CrosshairEditorScreen(CrosshairModule module, Screen parent) {
		this.module = module;
		this.parent = parent;
		this.cells = module.drawing().toCharArray();
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		areas.clear();
		VanillaStyle.drawBackground(width, height);

		canvas = Canvas.begin(width, height);
		float mx = canvas.toCanvas(mouseX);
		float my = canvas.toCanvas(mouseY);

		Ui.brand(30, 24);
		Fonts.drawSpaced(Fonts.TITLE, "MINHA MIRA", GRID_X, 130, 40, 0.1F, 0xFFFFFFFF);
		Fonts.draw(Fonts.TEXT, "Botão esquerdo pinta, botão direito apaga.", GRID_X, 178, 17, Ui.MUTED);

		drawGrid(mx, my);
		drawBrushes(mx, my);
		drawTools(mx, my);
		drawPreview();

		button("CONCLUIR", DONE, 1480, 960, 400, 64, 21, Ui.Style.PRIMARY, mx, my);
		Fonts.draw(Fonts.TEXT, "ESC volta ao menu · Ctrl+Z desfaz", GRID_X, 960 + 20, 16, Ui.MUTED);

		canvas.end();
		Ui.restore();
	}

	private void drawGrid(float mx, float my) {
		float size = CELL * CrosshairDrawing.SIDE;
		Render2D.outlinedRect(GRID_X - 12, GRID_Y - 12, size + 24, size + 24, 14, 0x33B48CFF, 0xFF0C0912);

		int hover = cellAt(mx, my);
		for (int row = 0; row < CrosshairDrawing.SIDE; row++) {
			for (int column = 0; column < CrosshairDrawing.SIDE; column++) {
				float x = GRID_X + column * CELL;
				float y = GRID_Y + row * CELL;
				Render2D.rawRect(x, y, CELL, CELL, (row + column) % 2 == 0 ? 0xFF16111F : 0xFF1C1626);

				char code = cells[row * CrosshairDrawing.SIDE + column];
				int color = CrosshairDrawing.argb(code, module.mainColor(), module.outlineArgb());
				if (color != 0) {
					Render2D.rawRect(x, y, CELL, CELL, color);
				}
			}
		}

		// Marca o quadradinho do meio (é onde o jogo aponta).
		float centerX = GRID_X + CrosshairDrawing.CENTER * CELL;
		float centerY = GRID_Y + CrosshairDrawing.CENTER * CELL;
		Render2D.roundedOutline(centerX, centerY, CELL, CELL, 2, 2F, 0x99B48CFF);

		if (hover >= 0) {
			float x = GRID_X + hover % CrosshairDrawing.SIDE * CELL;
			float y = GRID_Y + hover / CrosshairDrawing.SIDE * CELL;
			Render2D.roundedOutline(x, y, CELL, CELL, 2, 2F, 0xFFFFFFFF);
		}
		Ui.restore();
	}

	private void drawBrushes(float mx, float my) {
		Fonts.drawSpaced(Fonts.TEXT_BOLD, "COR DO PINCEL", PANEL_X, 215, 16, 0.12F, Ui.MUTED);
		for (int i = 0; i < CrosshairDrawing.CODES.length(); i++) {
			float x = PANEL_X + (i % 6) * 84;
			float y = 248 + (i / 6) * 84;
			int color = brushColor(i);
			boolean selected = i == brush;

			Render2D.outlinedRect(x, y, 72, 72, 10, selected ? 0xFFFFFFFF : (Ui.inside(mx, my, x, y, 72, 72) ? 0xAAFFFFFF : 0x44FFFFFF), 0xFF0F0A16);
			if (color != 0) {
				Render2D.roundedRect(x + 8, y + 8, 56, 56, 6, color);
			} else {
				Ui.drawCross(x + 36, y + 36, 14F, 3F, 0xFFAAAAAA);
			}
			areas.add(new ClickArea(BRUSH + i, x, y, 72, 72));
		}
		Ui.restore();

		String name = brush == 0 ? "Borracha" : CrosshairDrawing.NAMES[brush];
		String extra = brush == 1 || brush == 2 ? " (muda no menu da mira)" : "";
		Fonts.draw(Fonts.TEXT, "Pincel: " + name + extra, PANEL_X, 424, 16, 0xFFD8D2E6);
	}

	/** Cor mostrada na amostra do pincel (as duas primeiras seguem as cores do menu). */
	private int brushColor(int index) {
		return CrosshairDrawing.argb(CrosshairDrawing.CODES.charAt(index), module.mainColor(), module.outlineArgb());
	}

	private void drawTools(float mx, float my) {
		button("ESPELHAR ESQ./DIR.", MIRROR_X, PANEL_X, 462, 252, 52, 15, mirrorX ? Ui.Style.PRIMARY : Ui.Style.NORMAL, mx, my);
		button("ESPELHAR CIMA/BAIXO", MIRROR_Y, PANEL_X + 264, 462, 252, 52, 15, mirrorY ? Ui.Style.PRIMARY : Ui.Style.NORMAL, mx, my);
		button("DESFAZER", UNDO, PANEL_X + 528, 462, 150, 52, 15, undo.isEmpty() ? Ui.Style.DISABLED : Ui.Style.NORMAL, mx, my);
		button("LIMPAR", CLEAR, PANEL_X + 690, 462, 140, 52, 15, Ui.Style.DANGER, mx, my);

		Fonts.drawSpaced(Fonts.TEXT_BOLD, "PREDEFINIÇÕES", PANEL_X, 548, 16, 0.12F, Ui.MUTED);
		for (int i = 0; i < CrosshairDrawing.PRESETS.length; i++) {
			String label = CrosshairDrawing.PRESETS[i].toUpperCase(Locale.ROOT);
			button(label, PRESET + i, PANEL_X + i * 142, 580, 132, 52, 14, Ui.Style.NORMAL, mx, my);
		}
	}

	/** Prévia: em tamanho real e ampliada, sobre o céu e sobre um fundo escuro. */
	private void drawPreview() {
		Fonts.drawSpaced(Fonts.TEXT_BOLD, "PRÉVIA", PANEL_X, 670, 16, 0.12F, Ui.MUTED);
		previewBox(PANEL_X, 702, 1, "Tamanho real");
		previewBox(PANEL_X + 424, 702, 4, "Ampliada (4x)");
	}

	private void previewBox(float x, float y, int zoom, String label) {
		float w = 400;
		float h = 230;
		Render2D.rawRect(x, y, w, h / 2F, 0xFF8DB4F2);
		Render2D.rawRect(x, y + h / 2F, w, h / 2F, 0xFF2B2B30);

		// A mira desenha em unidades da tela; contra a escala da área de desenho, cada quadradinho
		// fica do tamanho que terá no jogo (vezes o zoom).
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + w / 2F, y + h / 2F, 0F);
		float scale = zoom / canvas.unit();
		GlStateManager.scale(scale, scale, 1F);
		CrosshairDrawing.draw(new String(cells), 0, 0, 1, module::mainColor, module::outlineArgb);
		GlStateManager.popMatrix();
		Ui.restore();

		Fonts.draw(Fonts.TEXT, label, x, y + h + 8, 15, Ui.MUTED);
	}

	private void button(String label, int action, float x, float y, float w, float h, float size, Ui.Style style, float mx, float my) {
		Ui.button(label, x, y, w, h, size, style, style != Ui.Style.DISABLED && Ui.inside(mx, my, x, y, w, h));
		areas.add(new ClickArea(action, x, y, w, h));
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		float mx = canvas.toCanvas(mouseX);
		float my = canvas.toCanvas(mouseY);

		for (ClickArea area : areas) {
			if (button == 0 && area.contains(mx, my)) {
				playClick();
				run(area.action());
				return;
			}
		}

		int cell = cellAt(mx, my);
		if (cell >= 0 && (button == 0 || button == 1)) {
			pushUndo();
			stroke = button;
			paint(cell);
		}
	}

	@Override
	protected void mouseDragged(int mouseX, int mouseY, int button, long lastClicked) {
		if (stroke < 0) {
			return;
		}
		int cell = cellAt(canvas.toCanvas(mouseX), canvas.toCanvas(mouseY));
		if (cell >= 0) {
			paint(cell);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		if (stroke >= 0) {
			stroke = -1;
			save();
		}
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			finish();
		} else if (keyCode == Keyboard.KEY_Z && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
			undo();
		}
	}

	private void run(int action) {
		if (action >= BRUSH) {
			brush = action - BRUSH;
		} else if (action >= PRESET) {
			pushUndo();
			cells = CrosshairDrawing.preset(CrosshairDrawing.PRESETS[action - PRESET]).toCharArray();
			save();
		} else if (action == CLEAR) {
			pushUndo();
			cells = CrosshairDrawing.blank().toCharArray();
			save();
		} else if (action == UNDO) {
			undo();
		} else if (action == MIRROR_X) {
			mirrorX = !mirrorX;
		} else if (action == MIRROR_Y) {
			mirrorY = !mirrorY;
		} else if (action == DONE) {
			finish();
		}
	}

	/** Pinta (ou apaga) o quadradinho e os espelhados dele. */
	private void paint(int cell) {
		char code = stroke == 1 || brush == 0 ? '0' : CrosshairDrawing.CODES.charAt(brush);
		int last = CrosshairDrawing.SIDE - 1;
		int x = cell % CrosshairDrawing.SIDE;
		int y = cell / CrosshairDrawing.SIDE;

		set(x, y, code);
		if (mirrorX) {
			set(last - x, y, code);
		}
		if (mirrorY) {
			set(x, last - y, code);
		}
		if (mirrorX && mirrorY) {
			set(last - x, last - y, code);
		}
	}

	private void set(int x, int y, char code) {
		cells[y * CrosshairDrawing.SIDE + x] = code;
	}

	private void pushUndo() {
		undo.push(new String(cells));
		while (undo.size() > UNDO_LIMIT) {
			undo.removeLast();
		}
	}

	private void undo() {
		if (!undo.isEmpty()) {
			cells = undo.pop().toCharArray();
			save();
		}
	}

	private void save() {
		module.setDrawing(new String(cells));
	}

	/** Concluir: o desenho passa a ser o formato da mira e o menu volta. */
	private void finish() {
		save();
		module.useDrawing();
		client.setScreen(parent);
	}

	/** @return índice do quadradinho sob o ponto, ou -1 */
	private int cellAt(float mx, float my) {
		float column = (mx - GRID_X) / CELL;
		float row = (my - GRID_Y) / CELL;
		if (column < 0 || row < 0 || column >= CrosshairDrawing.SIDE || row >= CrosshairDrawing.SIDE) {
			return -1;
		}
		return (int) row * CrosshairDrawing.SIDE + (int) column;
	}

	private void playClick() {
		client.getSoundManager().play(PositionedSoundInstance.master(new Identifier("gui.button.press"), 1.0F));
	}
}
