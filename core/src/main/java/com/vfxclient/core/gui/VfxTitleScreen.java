package com.vfxclient.core.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.mixin.TitleScreenAccessor;
import net.minecraft.client.gui.screen.ResourcePackScreen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

/**
 * Tela inicial do VfxClient: fundo panorâmico desfocado e escurecido, barra superior com a
 * logo e ícones (opções, menu do VfxClient, pacotes de textura, sair), um cartão central com
 * a logo e os botões Um jogador / Multijogador, e o idioma embaixo.
 *
 * <p>Estende a tela do Minecraft para o jogo continuar tratando-a como tela inicial
 * (limpar chat, Realms etc.). Desligável no módulo "Tela inicial".</p>
 */
public class VfxTitleScreen extends TitleScreen {

	private static final int BUTTON_W = 150;
	private static final int BUTTON_H = 20;
	private static final int ICON = 22;

	private static final int SINGLEPLAYER = 0;
	private static final int MULTIPLAYER = 1;
	private static final int OPTIONS = 2;
	private static final int VFX_MENU = 3;
	private static final int PACKS = 4;
	private static final int QUIT = 5;
	private static final int LANGUAGE = 6;

	private final List<Hit> hits = new ArrayList<Hit>();
	private final float[] starX = new float[70];
	private final float[] starY = new float[70];
	private final float[] starSpeed = new float[70];
	private final long openedAt = System.currentTimeMillis();

	public VfxTitleScreen() {
		Random random = new Random();
		for (int i = 0; i < starX.length; i++) {
			starX[i] = random.nextFloat();
			starY[i] = random.nextFloat();
			starSpeed[i] = 0.004F + random.nextFloat() * 0.012F;
		}
	}

	/** Área clicável desenhada nesta tela. */
	private static final class Hit {
		final int id;
		final int x;
		final int y;
		final int w;
		final int h;

		Hit(int id, int x, int y, int w, int h) {
			this.id = id;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}

		boolean contains(int mx, int my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	@Override
	public void init() {
		super.init(); // cria a textura do fundo panorâmico
		buttons.clear();
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		hits.clear();
		GlStateManager.disableAlphaTest();
		((TitleScreenAccessor) (Object) this).vfx$renderBackground(mouseX, mouseY, tickDelta);
		GlStateManager.enableAlphaTest();

		// Escurece o panorama e dá um brilho roxo bem leve.
		fillGradient(0, 0, width, height, 0xC0050309, 0xE0050309);
		Render2D.gradientH(0, 0, width, height, 0x00000000, 0x186A2BD8);
		drawStars();

		float open = Anim.easeOutCubic((System.currentTimeMillis() - openedAt) / 400F);

		// Barra superior: logo + nome à esquerda, ícones à direita.
		Logo.draw(10, 8, 20);
		GlStateManager.pushMatrix();
		GlStateManager.translate(36F, 11F, 0F);
		GlStateManager.scale(1.6F, 1.6F, 1F);
		textRenderer.drawWithShadow("VFXCLIENT", 0, 0, 0xFFFFFFFF);
		GlStateManager.popMatrix();

		int ix = width - 8 - ICON;
		iconButton(QUIT, ix, 8, mouseX, mouseY);
		ix -= ICON + 5;
		iconButton(PACKS, ix, 8, mouseX, mouseY);
		ix -= ICON + 5;
		iconButton(VFX_MENU, ix, 8, mouseX, mouseY);
		ix -= ICON + 5;
		iconButton(OPTIONS, ix, 8, mouseX, mouseY);

		// Cartão central.
		int cardW = BUTTON_W + 60;
		int cardH = 150;
		int cx = width / 2;
		int top = (int) (height / 2 - cardH / 2 + (1F - open) * 12F);
		Render2D.roundedRect(cx - cardW / 2F, top, cardW, cardH, 8, Theme.withAlpha(0x66000000, open));
		int logoSize = 64;
		Logo.draw(cx - logoSize / 2, top + 14, logoSize);
		GlStateManager.color(1F, 1F, 1F, 1F);
		button(SINGLEPLAYER, "UM JOGADOR", cx - BUTTON_W / 2, top + 92, mouseX, mouseY);
		button(MULTIPLAYER, "MULTIJOGADOR", cx - BUTTON_W / 2, top + 118, mouseX, mouseY);

		// Idioma (embaixo, no centro) e textos dos cantos.
		iconButton(LANGUAGE, cx - ICON / 2, height - ICON - 2, mouseX, mouseY);
		String version = "VFXCLIENT 1.8.9 (v" + VfxCore.get().getVersion() + ")";
		textRenderer.drawWithShadow(version, 4, height - 12, 0xFFB8B8C0);
		String copyright = "Copyright Mojang AB. Do not distribute!";
		textRenderer.drawWithShadow(copyright, width - textRenderer.getStringWidth(copyright) - 4, height - 12, 0xFFB8B8C0);
	}

	private void drawStars() {
		float t = (System.currentTimeMillis() % 1000000L) / 1000F;
		for (int i = 0; i < starX.length; i++) {
			float y = (starY[i] - t * starSpeed[i]) % 1F;
			if (y < 0) {
				y += 1F;
			}
			float x = starX[i] * width;
			float alpha = 0.25F + 0.5F * (float) Math.abs(Math.sin(t * 0.8 + i));
			Render2D.rawRect(x, y * height, 1F, 1F, Theme.withAlpha(0xFFE6DAFF, alpha));
		}
	}

	/** Botão plano escuro com contorno fino, texto em maiúsculas. */
	private void button(int id, String label, int x, int y, int mouseX, int mouseY) {
		boolean hover = mouseX >= x && mouseX < x + BUTTON_W && mouseY >= y && mouseY < y + BUTTON_H;
		Render2D.outlinedRect(x, y, BUTTON_W, BUTTON_H, 2, hover ? 0xFFFFFFFF : 0x99FFFFFF, hover ? 0xF0262230 : 0xE0141218);
		drawCenteredString(textRenderer, label, x + BUTTON_W / 2, y + 6, hover ? 0xFFFFFFFF : 0xFFE6E6EA);
		hits.add(new Hit(id, x, y, BUTTON_W, BUTTON_H));
	}

	/** Botão quadrado com ícone desenhado com formas simples. */
	private void iconButton(int id, int x, int y, int mouseX, int mouseY) {
		boolean hover = mouseX >= x && mouseX < x + ICON && mouseY >= y && mouseY < y + ICON;
		int border = hover ? (id == QUIT ? 0xFFE04848 : 0xFFFFFFFF) : 0x66FFFFFF;
		Render2D.outlinedRect(x, y, ICON, ICON, 2, border, hover ? 0xF0262230 : 0xD0141218);
		int color = hover ? 0xFFFFFFFF : 0xFFD8D8DE;
		float cx = x + ICON / 2F;
		float cy = y + ICON / 2F;
		switch (id) {
			case OPTIONS:
				Render2D.gear(cx, cy, 5F, color);
				break;
			case VFX_MENU:
				Logo.draw(x + 4, y + 4, ICON - 8);
				GlStateManager.color(1F, 1F, 1F, 1F);
				break;
			case PACKS:
				Render2D.rawRect(cx - 6, cy - 6, 5, 5, color);
				Render2D.rawRect(cx + 1, cy - 6, 5, 5, Theme.withAlpha(color, 0.6F));
				Render2D.rawRect(cx - 6, cy + 1, 5, 5, Theme.withAlpha(color, 0.6F));
				Render2D.rawRect(cx + 1, cy + 1, 5, 5, color);
				break;
			case QUIT:
				drawCross(cx, cy, 6F, color);
				break;
			case LANGUAGE:
				Render2D.ring(cx, cy, 6F, 1.2F, color);
				Render2D.rawRect(cx - 6, cy - 0.5F, 12, 1, color);
				Render2D.rawRect(cx - 0.5F, cy - 6, 1, 12, color);
				GlStateManager.pushMatrix();
				GlStateManager.translate(cx, cy, 0F);
				GlStateManager.scale(0.45F, 1F, 1F);
				Render2D.ring(0, 0, 6F, 1.6F, color);
				GlStateManager.popMatrix();
				break;
			default:
				break;
		}
		hits.add(new Hit(id, x, y, ICON, ICON));
	}

	private static void drawCross(float cx, float cy, float r, int color) {
		for (int angle : new int[] {45, -45}) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(cx, cy, 0F);
			GlStateManager.rotate(angle, 0F, 0F, 1F);
			Render2D.rawRect(-r, -0.75F, r * 2, 1.5F, color);
			GlStateManager.popMatrix();
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0) {
			return;
		}
		for (Hit hit : hits) {
			if (hit.contains(mouseX, mouseY)) {
				client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
						new net.minecraft.util.Identifier("gui.button.press"), 1.0F));
				open(hit.id);
				return;
			}
		}
	}

	private void open(int id) {
		switch (id) {
			case SINGLEPLAYER:
				client.setScreen(new SelectWorldScreen(this));
				break;
			case MULTIPLAYER:
				client.setScreen(new MultiplayerScreen(this));
				break;
			case OPTIONS:
				client.setScreen(new SettingsScreen(this, client.options));
				break;
			case VFX_MENU:
				client.setScreen(new ClickGuiScreen());
				break;
			case PACKS:
				client.setScreen(new ResourcePackScreen(this));
				break;
			case LANGUAGE:
				client.setScreen(new LanguageOptionsScreen(this, client.options, client.getLanguageManager()));
				break;
			case QUIT:
				client.scheduleStop();
				break;
			default:
				break;
		}
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		// Tela inicial: ESC não faz nada.
	}
}
