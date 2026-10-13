package com.vfxclient.core.module;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.gui.Theme;
import com.vfxclient.core.module.general.HudStyleModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

/**
 * Módulo que desenha algo na HUD, numa posição que o jogador pode arrastar.
 *
 * <p>Subclasses informam o tamanho do conteúdo e o desenham a partir de (0, 0);
 * fundo, espaçamento interno e escala ficam a cargo desta classe. Para módulos
 * que só mostram texto, use {@link TextHudModule}.</p>
 *
 * <p>As posições ficam em pixels escalados da GUI e são salvas no JSON.</p>
 */
public abstract class HudModule extends Module {

	/** Espaço interno padrão (usado antes do estilo global carregar). */
	public static final int PADDING = 3;

	protected final BooleanSetting background = add(new BooleanSetting("background", "Fundo", true));
	protected final BooleanSetting shadow = addAdvanced(new BooleanSetting("shadow", "Sombra no texto", false));
	protected final BooleanSetting brandColor = addAdvanced(new BooleanSetting("brandColor", "Texto na cor de destaque", false));
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
		this(id, name, description, Category.HUD, enabledByDefault, defaultX, defaultY);
	}

	/**
	 * Igual ao outro construtor, mas para módulos de outra categoria que também desenham na HUD.
	 *
	 * @param id               chave do JSON
	 * @param name             nome exibido
	 * @param description      descrição curta
	 * @param category         categoria da Click GUI
	 * @param enabledByDefault se começa ligado
	 * @param defaultX         posição X padrão
	 * @param defaultY         posição Y padrão
	 */
	protected HudModule(String id, String name, String description, Category category, boolean enabledByDefault, int defaultX, int defaultY) {
		super(id, name, description, category, enabledByDefault);
		this.defaultX = defaultX;
		this.defaultY = defaultY;
		this.x = defaultX;
		this.y = defaultY;
	}

	/**
	 * Se há algo para desenhar agora.
	 *
	 * @param client  instância do Minecraft
	 * @param preview {@code true} no editor de HUD (deve mostrar um exemplo mesmo sem dados)
	 * @return se o elemento aparece
	 */
	public abstract boolean isVisible(MinecraftClient client, boolean preview);

	/**
	 * @param client  instância do Minecraft
	 * @param preview modo editor
	 * @return largura do conteúdo, sem escala e sem espaçamento
	 */
	protected abstract int contentWidth(MinecraftClient client, boolean preview);

	/**
	 * @param client  instância do Minecraft
	 * @param preview modo editor
	 * @return altura do conteúdo, sem escala e sem espaçamento
	 */
	protected abstract int contentHeight(MinecraftClient client, boolean preview);

	/**
	 * Desenha o conteúdo com origem em (0, 0).
	 *
	 * @param client  instância do Minecraft
	 * @param preview modo editor
	 */
	protected abstract void renderContent(MinecraftClient client, boolean preview);

	/** @return {@code false} se o módulo desenha o próprio fundo (ex.: teclas) */
	protected boolean usesSharedBackground() {
		return true;
	}

	/**
	 * Largura final, com espaçamento e escala.
	 *
	 * @param client  instância do Minecraft
	 * @param preview modo editor
	 * @return largura em pixels escalados
	 */
	public int getWidth(MinecraftClient client, boolean preview) {
		return (int) Math.ceil((contentWidth(client, preview) + padding() * 2) * scale.get());
	}

	/**
	 * Altura final, com espaçamento e escala.
	 *
	 * @param client  instância do Minecraft
	 * @param preview modo editor
	 * @return altura em pixels escalados
	 */
	public int getHeight(MinecraftClient client, boolean preview) {
		return (int) Math.ceil((contentHeight(client, preview) + padding() * 2) * scale.get());
	}

	/**
	 * Desenha o elemento na posição indicada.
	 *
	 * @param client  instância do Minecraft
	 * @param drawX   posição X
	 * @param drawY   posição Y
	 * @param preview modo editor
	 */
	public void render(MinecraftClient client, int drawX, int drawY, boolean preview) {
		float s = scale.get().floatValue();
		int pad = padding();
		int w = contentWidth(client, preview) + pad * 2;
		int h = contentHeight(client, preview) + pad * 2;

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) drawX, (float) drawY, 0F);
		GlStateManager.scale(s, s, 1F);
		if (usesSharedBackground() && background.get()) {
			HudStyleModule style = style();
			Render2D.roundedRect(0, 0, w, h, style == null ? 3F : style.radius(), style == null ? Theme.HUD_BACKGROUND : style.backgroundColor());
		}
		GlStateManager.translate((float) pad, (float) pad, 0F);
		renderContent(client, preview);
		GlStateManager.popMatrix();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private int padding() {
		if (!usesSharedBackground()) {
			return 0;
		}
		HudStyleModule style = style();
		return style == null ? PADDING : style.padding();
	}

	/** @return estilo global da HUD, ou {@code null} antes do client carregar */
	protected static HudStyleModule style() {
		return VfxCore.module(HudStyleModule.class);
	}

	/** @return {@code true} quando o jogador escolheu o modo avançado da HUD */
	protected static boolean advanced() {
		HudStyleModule style = style();
		return style != null && style.advanced();
	}

	/**
	 * Desenha texto com a cor e a sombra escolhidas pelo jogador.
	 *
	 * @param client instância do Minecraft
	 * @param text   texto
	 * @param x      posição X dentro do conteúdo
	 * @param y      posição Y dentro do conteúdo
	 */
	protected void drawText(MinecraftClient client, String text, float x, float y) {
		HudStyleModule style = style();
		client.textRenderer.draw(text, x, y, textColor(), shadow.get() || (style != null && style.shadow()));
	}

	/** @return cor do texto conforme a opção "cor da marca" */
	protected int textColor() {
		HudStyleModule style = style();
		if (style == null) {
			return brandColor.get() ? Theme.PRIMARY_LIGHT : Theme.WHITE;
		}
		return brandColor.get() ? style.accentColor() : style.textColor();
	}

	/** @return cor de destaque do estilo global (para rótulos no modo avançado) */
	protected int accentColor() {
		HudStyleModule style = style();
		return style == null ? Theme.PRIMARY_LIGHT : style.accentColor();
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
