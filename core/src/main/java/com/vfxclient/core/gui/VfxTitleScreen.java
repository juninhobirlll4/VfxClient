package com.vfxclient.core.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.gui.font.Fonts;
import com.vfxclient.core.mixin.TitleScreenAccessor;
import net.minecraft.client.gui.screen.ResourcePackScreen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;

/**
 * Tela inicial do VfxClient: panorama do Minecraft desfocado e escurecido, barra de cima com a
 * logo e ícones, cartão central com a logo e os botões Um jogador / Multijogador, e o idioma
 * embaixo. Desenhada em medidas de referência 1920 x 1080 ({@link Canvas}).
 *
 * <p>Estende a tela do Minecraft para o jogo continuar tratando-a como tela inicial.</p>
 */
public class VfxTitleScreen extends TitleScreen {

	private static final int SINGLEPLAYER = 0;
	private static final int MULTIPLAYER = 1;
	private static final int OPTIONS = 2;
	private static final int VFX_MENU = 3;
	private static final int PACKS = 4;
	private static final int QUIT = 5;
	private static final int LANGUAGE = 6;

	private static final float CARD_W = 560;
	private static final float CARD_H = 430;
	private static final float BUTTON_W = 464;
	private static final float BUTTON_H = 58;
	private static final int STARS = 90;

	private final List<ClickArea> areas = new ArrayList<ClickArea>();
	private final float[] starX = new float[STARS];
	private final float[] starY = new float[STARS];
	private final float[] starSpeed = new float[STARS];
	private final long openedAt = System.currentTimeMillis();
	private Canvas canvas;

	public VfxTitleScreen() {
		Random random = new Random();
		for (int i = 0; i < STARS; i++) {
			starX[i] = random.nextFloat();
			starY[i] = random.nextFloat();
			starSpeed[i] = 0.004F + random.nextFloat() * 0.012F;
		}
	}

	@Override
	public void init() {
		// Cria a textura do panorama; os botões do jogo não são usados.
		super.init();
		buttons.clear();
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		areas.clear();
		drawBackground(mouseX, mouseY, tickDelta);

		canvas = Canvas.begin(width, height);
		float mx = canvas.toCanvas(mouseX);
		float my = canvas.toCanvas(mouseY);

		drawStars();
		drawTopBar(mx, my);
		drawCard(mx, my);
		drawBottom(mx, my);

		canvas.end();
		Ui.restore();
	}

	private void drawBackground(int mouseX, int mouseY, float tickDelta) {
		GlStateManager.disableAlphaTest();
		((TitleScreenAccessor) (Object) this).vfx$renderBackground(mouseX, mouseY, tickDelta);
		GlStateManager.enableAlphaTest();
		fillGradient(0, 0, width, height, 0xB8050309, 0xE0050309);
	}

	private void drawStars() {
		float time = (System.currentTimeMillis() % 1000000L) / 1000F;
		for (int i = 0; i < STARS; i++) {
			float y = (starY[i] - time * starSpeed[i]) % 1F;
			if (y < 0) {
				y += 1F;
			}

			float x = starX[i] * canvas.width();
			y *= canvas.height();
			if (behindCard(x, y)) {
				continue;
			}

			float alpha = 0.25F + 0.5F * (float) Math.abs(Math.sin(time * 0.8 + i));
			Render2D.rawRect(x, y, 3, 3, Theme.withAlpha(0xFFE6DAFF, alpha));
		}
	}

	/** O cartão é translúcido: estrelas atrás dele apareceriam por dentro. */
	private boolean behindCard(float x, float y) {
		float margin = 4;
		float left = canvas.centerX() - CARD_W / 2F - margin;
		float top = canvas.centerY() - CARD_H / 2F - margin;
		// A altura conta os 24 da animação de entrada, em que o cartão começa mais para baixo.
		return Ui.inside(x, y, left, top, CARD_W + margin * 2, CARD_H + 24 + margin * 2);
	}

	private void drawTopBar(float mx, float my) {
		Ui.brand(30, 24);

		float x = canvas.width() - 28 - 58;
		int[] actions = {QUIT, PACKS, VFX_MENU, OPTIONS};
		Ui.Icon[] icons = {Ui.Icon.CLOSE, Ui.Icon.PACKS, Ui.Icon.MENU, Ui.Icon.SETTINGS};
		for (int i = 0; i < actions.length; i++) {
			Ui.iconButton(icons[i], x, 24, Ui.inside(mx, my, x, 24, 58, 58));
			areas.add(new ClickArea(actions[i], x, 24, 58, 58));
			x -= 70;
		}
	}

	private void drawCard(float mx, float my) {
		float appear = Anim.easeOutCubic((System.currentTimeMillis() - openedAt) / 400F);
		float cardX = canvas.centerX() - CARD_W / 2F;
		float cardY = canvas.centerY() - CARD_H / 2F + (1F - appear) * 24F;

		Render2D.outlinedRect(cardX, cardY, CARD_W, CARD_H, 22, 0x2EB48CFF, 0x9E0A0810);
		Logo.draw((int) (canvas.centerX() - 64), (int) (cardY + 44), 128);
		Ui.restore();

		float x = canvas.centerX() - BUTTON_W / 2F;
		float y = cardY + 210;
		button("UM JOGADOR", SINGLEPLAYER, x, y, Ui.Style.PRIMARY, mx, my);
		button("MULTIJOGADOR", MULTIPLAYER, x, y + BUTTON_H + 16, Ui.Style.NORMAL, mx, my);
	}

	private void button(String label, int action, float x, float y, Ui.Style style, float mx, float my) {
		Ui.button(label, x, y, BUTTON_W, BUTTON_H, 20, style, Ui.inside(mx, my, x, y, BUTTON_W, BUTTON_H));
		areas.add(new ClickArea(action, x, y, BUTTON_W, BUTTON_H));
	}

	private void drawBottom(float mx, float my) {
		float globeX = canvas.centerX() - 29;
		float globeY = canvas.height() - 58 - 6;
		Ui.iconButton(Ui.Icon.LANGUAGE, globeX, globeY, Ui.inside(mx, my, globeX, globeY, 58, 58));
		areas.add(new ClickArea(LANGUAGE, globeX, globeY, 58, 58));

		float textY = canvas.height() - 40;
		String version = "VFXCLIENT 1.8.9 (v" + VfxCore.get().getVersion() + ")";
		Fonts.drawWithShadow(Fonts.TEXT_BOLD, version, 28, textY, 18, 0xFFB8B8C0);

		String copyright = "Copyright Mojang AB. Do not distribute!";
		float copyrightX = canvas.width() - 28 - Fonts.width(Fonts.TEXT_BOLD, copyright, 18);
		Fonts.drawWithShadow(Fonts.TEXT_BOLD, copyright, copyrightX, textY, 18, 0xFFB8B8C0);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0 || canvas == null) {
			return;
		}
		float mx = canvas.toCanvas(mouseX);
		float my = canvas.toCanvas(mouseY);
		for (ClickArea area : areas) {
			if (area.contains(mx, my)) {
				client.getSoundManager().play(PositionedSoundInstance.master(new Identifier("gui.button.press"), 1.0F));
				open(area.action());
				return;
			}
		}
	}

	private void open(int action) {
		switch (action) {
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