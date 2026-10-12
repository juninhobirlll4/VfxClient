package com.vfxclient.core.module;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.gui.Theme;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

/**
 * Módulo que desenha um texto na HUD, numa posição que o jogador pode arrastar.
 *
 * <p>As posições ficam em pixels escalados da GUI (o mesmo sistema de coordenadas
 * das telas do Minecraft) e são salvas no JSON junto com as opções.</p>
 */
public abstract class HudModule extends Module {

	/** Espaço interno entre o texto e a borda do fundo. */
	public static final int PADDING = 4;

	protected final BooleanSetting background = add(new BooleanSetting("background", "Fundo", true));
	protected final BooleanSetting shadow = add(new BooleanSetting("shadow", "Sombra no texto", true));
	protected final BooleanSetting brandColor = add(new BooleanSetting("brandColor", "Texto na cor da marca", false));
	protected final NumberSetting scale = add(new NumberSetting("scale", "Tamanho", 1.0, 0.5, 2.0, 0.1, "x"));

	private final int defaultX;
	private final int defaultY;
	private int x;
	private int y;

	/**
	 * @param id               chave do JSON
	 * @param name             nome exibido
	 * @param description      descrição curta
	 * @param enabledByDefault se começa ligado
	 * @param defaultX         posição X padrão (pixels escalados)
	 * @param defaultY         posição Y padrão (pixels escalados)
	 */
	protected HudModule(String id, String name, String description, boolean enabledByDefault, int defaultX, int defaultY) {
		super(id, name, description, Category.HUD, enabledByDefault);
		this.defaultX = defaultX;
		this.defaultY = defaultY;
		this.x = defaultX;
		this.y = defaultY;
	}

	/**
	 * Texto atual do elemento.
	 *
	 * @param client instância do Minecraft
	 * @return o texto, ou {@code null} para não desenhar nada agora
	 */
	public abstract String getText(MinecraftClient client);

	/**
	 * Texto de exemplo mostrado no editor de HUD quando {@link #getText} não tem valor.
	 *
	 * @return texto de exemplo
	 */
	public abstract String getSampleText();

	/**
	 * Largura final do elemento, já com fundo e escala.
	 *
	 * @param textRenderer fonte do jogo
	 * @param text         texto a medir
	 * @return largura em pixels escalados
	 */
	public int getWidth(TextRenderer textRenderer, String text) {
		return (int) Math.ceil((textRenderer.getStringWidth(text) + PADDING * 2) * scale.get());
	}

	/**
	 * Altura final do elemento, já com fundo e escala.
	 *
	 * @param textRenderer fonte do jogo
	 * @return altura em pixels escalados
	 */
	public int getHeight(TextRenderer textRenderer) {
		return (int) Math.ceil((textRenderer.fontHeight - 1 + PADDING * 2) * scale.get());
	}

	/**
	 * Desenha o elemento na posição indicada.
	 *
	 * @param textRenderer fonte do jogo
	 * @param text         texto
	 * @param drawX        posição X
	 * @param drawY        posição Y
	 */
	public void render(TextRenderer textRenderer, String text, int drawX, int drawY) {
		float s = scale.get().floatValue();
		int w = textRenderer.getStringWidth(text) + PADDING * 2;
		int h = textRenderer.fontHeight - 1 + PADDING * 2;

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) drawX, (float) drawY, 0f);
		GlStateManager.scale(s, s, 1f);
		if (background.get()) {
			Render2D.roundedRect(0, 0, w, h, 3, Theme.HUD_BACKGROUND);
		}
		int color = brandColor.get() ? Theme.PRIMARY_LIGHT : Theme.WHITE;
		textRenderer.draw(text, (float) PADDING, (float) PADDING, color, shadow.get());
		GlStateManager.popMatrix();
	}

	/** @return posição X salva */
	public int getX() {
		return x;
	}

	/** @return posição Y salva */
	public int getY() {
		return y;
	}

	/**
	 * Move o elemento e agenda o salvamento.
	 *
	 * @param x nova posição X
	 * @param y nova posição Y
	 */
	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
		markDirty();
	}

	/** Volta só a posição ao padrão. */
	public void resetPosition() {
		setPosition(defaultX, defaultY);
	}

	@Override
	public void resetToDefaults() {
		x = defaultX;
		y = defaultY;
		super.resetToDefaults();
	}

	@Override
	public JsonObject toJson() {
		JsonObject json = super.toJson();
		json.addProperty("x", x);
		json.addProperty("y", y);
		return json;
	}

	@Override
	public void fromJson(JsonObject json) {
		super.fromJson(json);
		x = readInt(json, "x", x);
		y = readInt(json, "y", y);
	}
}
