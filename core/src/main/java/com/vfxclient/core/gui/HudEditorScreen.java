package com.vfxclient.core.gui;

import java.util.ArrayList;
import java.util.List;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.hud.HudRenderer;
import com.vfxclient.core.module.HudModule;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Editor da HUD: arraste os elementos para reposicionar.
 *
 * <p>Botão esquerdo arrasta, botão direito volta o elemento para a posição padrão.
 * Os elementos "grudam" nas bordas e no centro da tela. ESC volta para a Click GUI.</p>
 *
 * <p>Redimensionar: arraste a alça no canto inferior direito do elemento, ou use a rodinha
 * do mouse em cima dele. O canto superior esquerdo fica parado.</p>
 */
public class HudEditorScreen extends Screen {

	private static final int SNAP = 4;
	/** Tamanho da alça de redimensionar, em pixels da GUI. */
	private static final int HANDLE = 5;

	private final Screen parent;
	private HudModule dragging;
	private int offsetX;
	private int offsetY;
	private HudModule resizing;
	private int resizeLeft;
	private int resizeTop;
	private int resizeStartW;
	private int resizeStartH;
	private double resizeStartScale;

	/** @param parent tela para onde voltar com ESC */
	public HudEditorScreen(Screen parent) {
		this.parent = parent;
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}

	@Override
	public void removed() {
		VfxCore.get().getConfig().save();
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		DrawableHelper.fill(0, 0, width, height, 0x55000000);
		DrawableHelper.fill(width / 2, 0, width / 2 + 1, height, Theme.DIVIDER);
		DrawableHelper.fill(0, height / 2, width, height / 2 + 1, Theme.DIVIDER);

		HudModule hovered = resizing != null ? resizing : dragging != null ? dragging : findAt(mouseX, mouseY);
		if (hovered == null) {
			hovered = findHandleAt(mouseX, mouseY);
		}
		for (HudModule module : enabledModules()) {
			int w = module.getWidth(client, true);
			int h = module.getHeight(client, true);
			int x = module.screenX(width, w);
			int y = module.screenY(height, h);
			module.render(client, x, y, true);
			Render2D.outline(x - 1, y - 1, w + 2, h + 2, module == hovered ? Theme.PRIMARY_LIGHT : 0x66FFFFFF);
			if (module == hovered) {
				// Alça de redimensionar no canto inferior direito.
				DrawableHelper.fill(x + w - HANDLE + 1, y + h - HANDLE + 1, x + w + 2, y + h + 2, Theme.PRIMARY_LIGHT);
				if (module == resizing) {
					String label = String.format("%.1fx", module.getScale());
					textRenderer.drawWithShadow(label, x + w + 4, y + h - 4, 0xFFFFFFFF);
				}
			}
		}

		String hint = "Arraste: mover  -  canto/rodinha: tamanho  -  botão direito: padrão  -  ESC: voltar";
		int hintW = textRenderer.getStringWidth(hint) + 16;
		Render2D.roundedRect(width / 2F - hintW / 2F, height - 26, hintW, 16, 5, Theme.PANEL);
		textRenderer.draw(hint, width / 2F - textRenderer.getStringWidth(hint) / 2F, height - 22, Theme.TEXT, false);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		HudModule handle = button == 0 ? findHandleAt(mouseX, mouseY) : null;
		if (handle != null) {
			resizing = handle;
			resizeStartW = handle.getWidth(client, true);
			resizeStartH = handle.getHeight(client, true);
			resizeLeft = handle.screenX(width, resizeStartW);
			resizeTop = handle.screenY(height, resizeStartH);
			resizeStartScale = handle.getScale();
			return;
		}
		HudModule module = findAt(mouseX, mouseY);
		if (module == null) {
			return;
		}
		if (button == 0) {
			dragging = module;
			offsetX = mouseX - module.screenX(width, module.getWidth(client, true));
			offsetY = mouseY - module.screenY(height, module.getHeight(client, true));
		} else if (button == 1) {
			module.resetPosition();
		}
	}

	@Override
	protected void mouseDragged(int mouseX, int mouseY, int button, long timeSinceClick) {
		if (resizing != null) {
			double fx = (mouseX - resizeLeft) / (double) Math.max(1, resizeStartW);
			double fy = (mouseY - resizeTop) / (double) Math.max(1, resizeStartH);
			resize(resizing, resizeStartScale * Math.max(fx, fy), resizeLeft, resizeTop);
			return;
		}
		if (dragging == null) {
			return;
		}
		int w = dragging.getWidth(client, true);
		int h = dragging.getHeight(client, true);
		int x = snap(mouseX - offsetX, w, width);
		int y = snap(mouseY - offsetY, h, height);
		dragging.moveTo(HudRenderer.clamp(x, width - w), HudRenderer.clamp(y, height - h), width, height, w, h);
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		dragging = null;
		resizing = null;
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int wheel = Mouse.getEventDWheel();
		if (wheel == 0 || dragging != null || resizing != null) {
			return;
		}
		int mouseX = Mouse.getEventX() * width / client.width;
		int mouseY = height - Mouse.getEventY() * height / client.height - 1;
		HudModule module = findAt(mouseX, mouseY);
		if (module != null) {
			int w = module.getWidth(client, true);
			int h = module.getHeight(client, true);
			resize(module, module.getScale() + (wheel > 0 ? 0.1 : -0.1), module.screenX(width, w), module.screenY(height, h));
		}
	}

	/** Muda o tamanho mantendo o canto superior esquerdo no lugar. */
	private void resize(HudModule module, double scale, int left, int top) {
		module.setScale(scale);
		int w = module.getWidth(client, true);
		int h = module.getHeight(client, true);
		module.moveTo(HudRenderer.clamp(left, width - w), HudRenderer.clamp(top, height - h), width, height, w, h);
	}

	/** @return o elemento cuja alça de redimensionar está sob o mouse */
	private HudModule findHandleAt(int mouseX, int mouseY) {
		List<HudModule> modules = enabledModules();
		for (int i = modules.size() - 1; i >= 0; i--) {
			HudModule module = modules.get(i);
			int w = module.getWidth(client, true);
			int h = module.getHeight(client, true);
			int x = module.screenX(width, w);
			int y = module.screenY(height, h);
			if (mouseX >= x + w - HANDLE && mouseX <= x + w + 2 && mouseY >= y + h - HANDLE && mouseY <= y + h + 2) {
				return module;
			}
		}
		return null;
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			client.setScreen(parent);
			return;
		}
	}

	/** Gruda o elemento nas bordas e no centro quando está perto deles. */
	private static int snap(int pos, int size, int screen) {
		if (Math.abs(pos) <= SNAP) {
			return 0;
		}
		if (Math.abs(pos + size - screen) <= SNAP) {
			return screen - size;
		}
		int centered = (screen - size) / 2;
		if (Math.abs(pos - centered) <= SNAP) {
			return centered;
		}
		return pos;
	}

	private HudModule findAt(int mouseX, int mouseY) {
		List<HudModule> modules = enabledModules();
		for (int i = modules.size() - 1; i >= 0; i--) {
			HudModule module = modules.get(i);
			int w = module.getWidth(client, true);
			int h = module.getHeight(client, true);
			int x = module.screenX(width, w);
			int y = module.screenY(height, h);
			if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
				return module;
			}
		}
		return null;
	}

	private static List<HudModule> enabledModules() {
		List<HudModule> result = new ArrayList<HudModule>();
		for (HudModule module : VfxCore.get().getModules().hudModules()) {
			if (module.isEnabled()) {
				result.add(module);
			}
		}
		return result;
	}
}
