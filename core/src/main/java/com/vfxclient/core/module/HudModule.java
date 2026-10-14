package com.vfxclient.core.module;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.gui.Theme;
import com.vfxclient.core.module.general.HudStyleModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ColorSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.module.setting.Setting;
import net.minecraft.client.MinecraftClient;

/**
 * Módulo que desenha algo na HUD, numa posição que o jogador pode arrastar.
 *
 * <p>Subclasses informam o tamanho do conteúdo e o desenham a partir de (0, 0);
 * fundo, espaçamento interno e escala ficam a cargo desta classe. Para módulos
 * que só mostram texto, use {@link TextHudModule}.</p>
 *
 * <p>Cada elemento fica preso a uma das 9 âncoras da tela (cantos, meios das bordas e centro):
 * a posição salva é a distância até essa âncora, em pixels escalados da GUI. Assim o layout
 * continua no lugar ao mudar o tamanho da janela ou a escala da interface.</p>
 *
 * <p>Texto pode usar os marcadores {@link #VALUE} e {@link #TEXT} para pintar valores com a cor
 * de destaque (ex.: {@code "FPS " + VALUE + "144"}).</p>
 */
public abstract class HudModule extends Module {

	/** Espaço interno padrão (usado antes do estilo global carregar). */
	public static final int PADDING = 3;

	protected final BooleanSetting background = add(new BooleanSetting("background", "Fundo", true));
	protected final BooleanSetting shadow = addAdvanced(new BooleanSetting("shadow", "Sombra no texto", false));
	protected final BooleanSetting brandColor = addAdvanced(new BooleanSetting("brandColor", "Texto na cor de destaque", false));
	protected final NumberSetting scale = add(new NumberSetting("scale", "Tamanho", 1.0, 0.5, 2.0, 0.1, "x"));
	protected final BooleanSetting ownColors = addAdvanced(new BooleanSetting("ownColors", "Cores próprias", false));
	protected final ColorSetting ownText = addAdvanced(new ColorSetting("ownText", "Cor do texto (própria)", 0xFFF2F2F2));
	protected final ColorSetting ownBackground = addAdvanced(new ColorSetting("ownBackground", "Cor do fundo (própria)", 0x73000000));

	/** Marcador no texto: daqui em diante, cor de destaque (valores). */
	public static final char VALUE = '\u0001';
	/** Marcador no texto: volta para a cor normal do texto. */
	public static final char TEXT = '\u0002';

	/** Âncoras: linha * 3 + coluna (linha 0 = topo, coluna 0 = esquerda). */
	public static final int TOP_LEFT = 0;
	public static final int TOP_CENTER = 1;
	public static final int TOP_RIGHT = 2;
	public static final int BOTTOM_LEFT = 6;
	public static final int BOTTOM_CENTER = 7;
	public static final int BOTTOM_RIGHT = 8;

	private final int defaultX;
	private final int defaultY;
	private int defaultAnchor = TOP_LEFT;
	private int anchor = TOP_LEFT;
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

	@Override
	public boolean isSettingVisible(Setting<?> setting) {
		if (setting == ownText || setting == ownBackground) {
			return usesSharedBackground() && ownColors.get();
		}
		if (setting == ownColors) {
			return usesSharedBackground();
		}
		return super.isSettingVisible(setting);
	}

	/**
	 * Módulos secundários (relógio, servidor, tempo de jogo) usam um tom mais escuro da cor de
	 * destaque, para não competir com os importantes.
	 *
	 * @return {@code true} para usar a cor secundária
	 */
	protected boolean isSecondary() {
		return false;
	}

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
			int bg = ownColors.get() ? ownBackground.argb() : style == null ? Theme.HUD_BACKGROUND : style.backgroundColor();
			Render2D.roundedRect(0, 0, w, h, style == null ? 3F : style.radius(), bg);
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
		boolean shadowed = shadow.get() || (style != null && style.shadow());
		if (text.indexOf(VALUE) < 0) {
			client.textRenderer.draw(strip(text), x, y, textColor(), shadowed);
			return;
		}

		boolean highlight = style == null || style.highlightValues();
		int color = textColor();
		StringBuilder part = new StringBuilder();
		for (int i = 0; i <= text.length(); i++) {
			char ch = i < text.length() ? text.charAt(i) : TEXT;
			if (ch != VALUE && ch != TEXT) {
				part.append(ch);
				continue;
			}
			if (part.length() > 0) {
				String s = part.toString();
				client.textRenderer.draw(s, x, y, color, shadowed);
				x += client.textRenderer.getStringWidth(s);
				part.setLength(0);
			}
			color = ch == VALUE && highlight ? accentColor() : textColor();
		}
	}

	/**
	 * @param text texto com marcadores
	 * @return o texto sem os marcadores de cor (para medir a largura)
	 */
	public static String strip(String text) {
		if (text.indexOf(VALUE) < 0 && text.indexOf(TEXT) < 0) {
			return text;
		}
		return text.replace(String.valueOf(VALUE), "").replace(String.valueOf(TEXT), "");
	}

	/** @return cor do texto conforme a opção "cor da marca" */
	protected int textColor() {
		HudStyleModule style = style();
		if (ownColors.get() && usesSharedBackground()) {
			return ownText.argb();
		}
		if (style == null) {
			return brandColor.get() ? Theme.PRIMARY_LIGHT : Theme.WHITE;
		}
		return brandColor.get() ? accentColor() : style.textColor();
	}

	/** @return cor de destaque do estilo global (secundária nos módulos secundários) */
	protected int accentColor() {
		HudStyleModule style = style();
		if (style == null) {
			return Theme.PRIMARY_LIGHT;
		}
		return isSecondary() ? style.secondaryColor() : style.accentColor();
	}

	/**
	 * Posição X na tela, a partir da âncora.
	 *
	 * @param screenWidth largura da tela (pixels escalados)
	 * @param width       largura do elemento
	 * @return X do canto superior esquerdo, sempre dentro da tela
	 */
	public int screenX(int screenWidth, int width) {
		int column = anchor % 3;
		int value = column == 0 ? x : column == 1 ? (screenWidth - width) / 2 + x : screenWidth - width - x;
		return clamp(value, screenWidth - width);
	}

	/**
	 * Posição Y na tela, a partir da âncora.
	 *
	 * @param screenHeight altura da tela (pixels escalados)
	 * @param height       altura do elemento
	 * @return Y do canto superior esquerdo, sempre dentro da tela
	 */
	public int screenY(int screenHeight, int height) {
		int row = anchor / 3;
		int value = row == 0 ? y : row == 1 ? (screenHeight - height) / 2 + y : screenHeight - height - y;
		return clamp(value, screenHeight - height);
	}

	/**
	 * Move o elemento para um ponto da tela (editor de HUD): escolhe a âncora pelo terço da
	 * tela onde está o centro do elemento e guarda a distância até ela.
	 *
	 * @param left         X na tela
	 * @param top          Y na tela
	 * @param screenWidth  largura da tela
	 * @param screenHeight altura da tela
	 * @param width        largura do elemento
	 * @param height       altura do elemento
	 */
	public void moveTo(int left, int top, int screenWidth, int screenHeight, int width, int height) {
		int cx = left + width / 2;
		int cy = top + height / 2;
		int column = cx < screenWidth / 3 ? 0 : cx > screenWidth * 2 / 3 ? 2 : 1;
		int row = cy < screenHeight / 3 ? 0 : cy > screenHeight * 2 / 3 ? 2 : 1;
		int ox = column == 0 ? left : column == 1 ? left - (screenWidth - width) / 2 : screenWidth - width - left;
		int oy = row == 0 ? top : row == 1 ? top - (screenHeight - height) / 2 : screenHeight - height - top;
		place(row * 3 + column, ox, oy);
	}

	/**
	 * Coloca o elemento numa âncora (usado pelos layouts prontos).
	 *
	 * @param anchor  âncora ({@link #TOP_LEFT}, {@link #BOTTOM_RIGHT}...)
	 * @param offsetX distância horizontal até a âncora
	 * @param offsetY distância vertical até a âncora
	 */
	public void place(int anchor, int offsetX, int offsetY) {
		this.anchor = Math.max(0, Math.min(8, anchor));
		this.x = offsetX;
		this.y = offsetY;
		markDirty();
	}

	/**
	 * Define a âncora padrão (chamado no construtor das subclasses).
	 *
	 * @param anchor âncora usada por padrão
	 */
	protected void setDefaultAnchor(int anchor) {
		this.defaultAnchor = anchor;
		this.anchor = anchor;
	}

	/** @return tamanho (escala) do elemento */
	public double getScale() {
		return scale.get();
	}

	/**
	 * Muda o tamanho (escala) do elemento, respeitando os limites da opção "Tamanho".
	 *
	 * @param value nova escala
	 */
	public void setScale(double value) {
		scale.set(value);
		markDirty();
	}

	/** Volta só a posição ao padrão. */
	public void resetPosition() {
		place(defaultAnchor, defaultX, defaultY);
	}

	@Override
	public void resetToDefaults() {
		anchor = defaultAnchor;
		x = defaultX;
		y = defaultY;
		super.resetToDefaults();
	}

	@Override
	public JsonObject toJson() {
		JsonObject json = super.toJson();
		json.addProperty("anchor", anchor);
		json.addProperty("x", x);
		json.addProperty("y", y);
		return json;
	}

	@Override
	public void fromJson(JsonObject json) {
		super.fromJson(json);
		// Configs antigas não têm âncora: as posições eram a partir do canto superior esquerdo.
		anchor = Math.max(0, Math.min(8, readInt(json, "anchor", json.has("x") ? TOP_LEFT : anchor)));
		x = readInt(json, "x", x);
		y = readInt(json, "y", y);
	}

	private static int clamp(int value, int max) {
		return Math.max(0, Math.min(value, Math.max(0, max)));
	}
}
