package com.vfxclient.core.gui;

import java.awt.image.BufferedImage;
import java.util.Locale;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.gui.font.Fonts;
import com.vfxclient.core.mixin.OptionSliderWidgetAccessor;
import com.vfxclient.core.module.general.TitleScreenModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;

/**
 * Visual do VfxClient nas telas que continuam sendo do jogo (Opções, Controles, Vídeo...):
 * botões planos com texto em Montserrat, sliders no mesmo estilo e fundo escuro no lugar da terra.
 *
 * <p>Quem chama são os mixins de {@code ButtonWidget}, {@code Screen} e {@code ListWidget}.
 * Desligando o módulo "Telas do VfxClient", tudo volta ao original.</p>
 */
public final class VanillaStyle {

	/**
	 * Unidades de desenho por unidade da interface do jogo. Com 3, um botão de 20 de altura
	 * vira 60 unidades, a mesma medida dos botões das telas novas.
	 */
	private static final float DETAIL = 3F;

	private static final float TEXT_SIZE = 21F;
	private static final float TEXT_SIZE_SMALL = 17F;
	private static final float TRACKING = 0.06F;

	private static final int BACKGROUND_TOP = 0xFF150E24;
	private static final int BACKGROUND_BOTTOM = 0xFF06040B;
	/** Cor do véu das listas (o jogo ainda escurece com a cor dos vértices). */
	private static final int LIST_VEIL = 0x66FFFFFF;

	private static NativeImageBackedTexture listVeil;

	private VanillaStyle() {
	}

	/** @return se as telas do jogo devem usar o visual do VfxClient */
	public static boolean enabled() {
		TitleScreenModule module = VfxCore.module(TitleScreenModule.class);
		return module != null && module.isEnabled();
	}

	/**
	 * Fundo e contorno de um botão do jogo (o texto vem depois, em {@link #drawButtonLabel}).
	 *
	 * @param button  botão
	 * @param height  altura do botão (não tem getter no 1.8.9)
	 * @param hovered mouse em cima
	 */
	public static void drawButtonFrame(ButtonWidget button, int height, boolean hovered) {
		begin();
		float x = button.x * DETAIL;
		float y = button.y * DETAIL;
		float w = button.getWidth() * DETAIL;
		float h = height * DETAIL;

		Ui.buttonFrame(x, y, w, h, button.active ? Ui.Style.NORMAL : Ui.Style.DISABLED, hovered && button.active);

		float progress = sliderProgress(button);
		if (progress >= 0F) {
			drawSlider(x, y, w, h, progress);
		}
		end();
	}

	/**
	 * Trilha e pegador do slider na borda de baixo do botão, longe do texto (o pegador no
	 * meio do botão ficava por cima das letras).
	 */
	private static void drawSlider(float x, float y, float w, float h, float progress) {
		float inset = 14F;
		float trackY = y + h - 11F;
		float trackW = w - inset * 2;
		float knobX = x + inset + progress * trackW;

		// Leve tom roxo no fundo até onde o slider está.
		Render2D.roundedRect(x + 2, y + 2, Math.max(0F, knobX - x - 2), h - 4, 6, 0x247B2FF7);

		Render2D.roundedRect(x + inset, trackY, trackW, 4, 2, 0x33FFFFFF);
		Render2D.roundedRect(x + inset, trackY, knobX - x - inset, 4, 2, Ui.PURPLE);
		Render2D.circle(knobX, trackY + 2, 6F, Ui.PURPLE_LIGHT);
	}

	/**
	 * Texto de um botão do jogo, em maiúsculas e centralizado. Textos longos usam uma letra
	 * menor e, se ainda não couberem, são encolhidos.
	 *
	 * @param button botão
	 * @param height altura do botão
	 */
	public static void drawButtonLabel(ButtonWidget button, int height) {
		String label = Fonts.stripFormatting(button.message).toUpperCase(Locale.ROOT);
		if (label.isEmpty()) {
			return;
		}

		begin();
		float x = button.x * DETAIL;
		float y = button.y * DETAIL;
		float w = button.getWidth() * DETAIL;
		float h = height * DETAIL;
		float room = w - 16F;

		float size = TEXT_SIZE;
		float textW = Fonts.width(Fonts.BUTTON, label, size, TRACKING);
		if (textW > room) {
			size = TEXT_SIZE_SMALL;
			textW = Fonts.width(Fonts.BUTTON, label, size, TRACKING);
		}
		float squeeze = textW > room ? room / textW : 1F;

		GlStateManager.pushMatrix();
		GlStateManager.translate(x + w / 2F, y, 0F);
		GlStateManager.scale(squeeze, 1F, 1F);
		Ui.buttonLabel(label, -w / 2F, 0F, w, h, size, button.active ? Ui.Style.NORMAL : Ui.Style.DISABLED);
		GlStateManager.popMatrix();
		end();
	}

	/**
	 * Caixa de texto (nome do mundo, endereço do servidor): escura, com contorno roxo quando
	 * está sendo digitada.
	 *
	 * @param x       esquerda, em unidades da interface
	 * @param y       topo
	 * @param w       largura
	 * @param h       altura
	 * @param focused recebendo o teclado
	 */
	public static void drawTextField(int x, int y, int w, int h, boolean focused) {
		begin();
		int border = focused ? Ui.PURPLE_LIGHT : 0x59FFFFFF;
		Render2D.outlinedRect((x - 1) * DETAIL, (y - 1) * DETAIL, (w + 2) * DETAIL, (h + 2) * DETAIL, 8, border, 0xE60C0A12);
		end();
	}

	/**
	 * Título de uma tela do jogo ("Opções", "Controles"...) em Montserrat, como nas telas novas.
	 *
	 * @param text    título
	 * @param centerX centro, em unidades da interface
	 * @param y       topo, em unidades da interface
	 */
	public static void drawTitle(String text, int centerX, int y) {
		begin();
		String title = Fonts.stripFormatting(text).toUpperCase(Locale.ROOT);
		Fonts.drawSpacedCentered(Fonts.TITLE, title, centerX * DETAIL, y * DETAIL - 6F, 30F, 0.12F, 0xFFFFFFFF);
		end();
	}
	/**
	 * Fundo escuro no lugar da terra (telas sem mundo aberto).
	 *
	 * @param width  largura da tela
	 * @param height altura da tela
	 */
	public static void drawBackground(int width, int height) {
		Render2D.gradientV(0, 0, width, height, BACKGROUND_TOP, BACKGROUND_BOTTOM);
		Ui.restore();
	}

	/**
	 * Escurece o mundo atrás das telas abertas durante o jogo.
	 *
	 * @param width  largura da tela
	 * @param height altura da tela
	 */
	public static void drawWorldOverlay(int width, int height) {
		Render2D.gradientV(0, 0, width, height, 0xB0100A18, 0xD8050309);
		Ui.restore();
	}

	/**
	 * Troca a textura de terra que o jogo usa atrás das listas (Controles, mundos, pacotes) por um
	 * véu escuro translúcido: o fundo da tela continua aparecendo.
	 *
	 * @param id textura que o jogo pediu
	 * @return {@code true} se o véu foi ligado no lugar dela
	 */
	public static boolean bindInsteadOf(Identifier id) {
		if (!DrawableHelper.OPTIONS_BACKGROUND_TEXTURE.equals(id) || !enabled()) {
			return false;
		}
		if (listVeil == null) {
			BufferedImage pixel = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			pixel.setRGB(0, 0, LIST_VEIL);
			listVeil = new NativeImageBackedTexture(pixel);
		}
		GlStateManager.bindTexture(listVeil.getGlId());
		// O jogo desenha a terra sem mistura de cores; o véu precisa dela para ser translúcido.
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		return true;
	}
	/** @return posição do slider (0 a 1), ou -1 se o botão não for um slider */
	private static float sliderProgress(ButtonWidget button) {
		if (button instanceof SliderWidget) {
			return ((SliderWidget) button).getProgress();
		}
		if (button instanceof OptionSliderWidget) {
			return ((OptionSliderWidgetAccessor) button).vfx$getValue();
		}
		return -1F;
	}

	/** Passa a desenhar em unidades {@link #DETAIL} vezes menores que as da interface. */
	private static void begin() {
		GlStateManager.pushMatrix();
		GlStateManager.scale(1F / DETAIL, 1F / DETAIL, 1F);
		Fonts.setPixelsPerUnit(new Window(MinecraftClient.getInstance()).getScaleFactor() / DETAIL);
	}

	private static void end() {
		GlStateManager.popMatrix();
		Ui.restore();
	}
}
