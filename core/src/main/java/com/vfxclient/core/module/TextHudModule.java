package com.vfxclient.core.module;

import java.util.Collections;
import java.util.List;

import com.vfxclient.core.module.general.HudStyleModule;
import net.minecraft.client.MinecraftClient;

/**
 * Módulo de HUD que mostra uma ou mais linhas de texto.
 *
 * <p>Implemente {@link #getText} (uma linha) ou sobrescreva {@link #getLines}
 * (várias linhas, ex.: efeitos de poção).</p>
 */
public abstract class TextHudModule extends HudModule {

	private static final int LINE_HEIGHT = 10;

	private List<String> cached = Collections.<String>emptyList();
	private long cachedAt;

	/**
	 * @param id               chave do JSON
	 * @param name             nome exibido
	 * @param description      descrição curta
	 * @param enabledByDefault se começa ligado
	 * @param defaultX         posição X padrão
	 * @param defaultY         posição Y padrão
	 */
	protected TextHudModule(String id, String name, String description, boolean enabledByDefault, int defaultX, int defaultY) {
		super(id, name, description, enabledByDefault, defaultX, defaultY);
	}

	/**
	 * Para módulos de outra categoria que também mostram texto na HUD.
	 *
	 * @param id               chave do JSON
	 * @param name             nome exibido
	 * @param description      descrição curta
	 * @param category         categoria da Click GUI
	 * @param enabledByDefault se começa ligado
	 * @param defaultX         posição X padrão
	 * @param defaultY         posição Y padrão
	 */
	protected TextHudModule(String id, String name, String description, Category category, boolean enabledByDefault, int defaultX, int defaultY) {
		super(id, name, description, category, enabledByDefault, defaultX, defaultY);
	}

	/**
	 * Texto de uma linha.
	 *
	 * @param client instância do Minecraft
	 * @return o texto, ou {@code null} para não desenhar nada agora
	 */
	public String getText(MinecraftClient client) {
		return null;
	}

	/** @return texto de exemplo para o editor de HUD */
	public abstract String getSampleText();

	/**
	 * Linhas a desenhar. Por padrão, só {@link #getText}.
	 *
	 * @param client  instância do Minecraft
	 * @param preview modo editor (usa o exemplo se não houver dados)
	 * @return linhas (vazia = nada a desenhar)
	 */
	public List<String> getLines(MinecraftClient client, boolean preview) {
		String text = getText(client);
		if ((text == null || text.isEmpty()) && preview) {
			text = getSampleText();
		}
		return text == null || text.isEmpty() ? Collections.<String>emptyList() : Collections.singletonList(text);
	}

	/**
	 * Linhas com cache (HUD caching): fora do editor, o texto é recalculado no máximo na
	 * frequência escolhida em Estilo da HUD, em vez de várias vezes por quadro.
	 */
	private List<String> lines(MinecraftClient client, boolean preview) {
		if (preview) {
			return getLines(client, true);
		}
		HudStyleModule style = style();
		long interval = style == null ? 0 : style.cacheMillis();
		long now = System.currentTimeMillis();
		if (interval <= 0 || now - cachedAt >= interval || now < cachedAt) {
			cached = getLines(client, false);
			cachedAt = now;
		}
		return cached;
	}

	@Override
	public boolean isVisible(MinecraftClient client, boolean preview) {
		return !lines(client, preview).isEmpty();
	}

	@Override
	protected int contentWidth(MinecraftClient client, boolean preview) {
		int width = 0;
		for (String line : lines(client, preview)) {
			width = Math.max(width, client.textRenderer.getStringWidth(strip(line)));
		}
		return width;
	}

	@Override
	protected int contentHeight(MinecraftClient client, boolean preview) {
		int lines = lines(client, preview).size();
		return lines == 0 ? 0 : lines * LINE_HEIGHT - 2;
	}

	@Override
	protected void renderContent(MinecraftClient client, boolean preview) {
		int y = 0;
		for (String line : lines(client, preview)) {
			drawText(client, line, 0, y);
			y += LINE_HEIGHT;
		}
	}
}
