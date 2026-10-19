package com.vfxclient.core.module.hud;

import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.gui.Theme;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.resource.ResourcePackLoader;

/**
 * Aviso com o pacote de textura em uso: a logo (ícone do pacote), o nome inteiro do arquivo e a
 * descrição. Aparece ao entrar no mundo e quando você troca de pacote, e some sozinho.
 */
public class TexturePackModule extends HudModule {

	private static final long SHOW_MS = 6000L;
	private static final long FADE_MS = 500L;
	private static final int ICON = 36;
	private static final int PAD = 5;
	private static final int MAX_TEXT = 320;
	private static final int NAME_COLOR = 0xFF7FE9FF;
	private static final int DESCRIPTION_COLOR = 0xFFD8D8E0;

	private String signature;
	private boolean inWorld;
	private long shownAt;

	public TexturePackModule() {
		super("texturePack", "Aviso do pacote de textura", "Mostra o nome e a logo do pacote de textura em uso",
				Category.HUD, true, 4, 4);
		setDefaultAnchor(TOP_RIGHT);
	}

	@Override
	public void onTick(MinecraftClient client) {
		boolean worldNow = client.world != null;
		String current = signatureOf(client);

		// Mostra ao entrar no mundo e quando a lista de pacotes muda com o jogo aberto.
		if ((worldNow && !inWorld) || (worldNow && signature != null && !signature.equals(current))) {
			shownAt = System.currentTimeMillis();
		}
		inWorld = worldNow;
		signature = current;
	}

	@Override
	protected boolean usesSharedBackground() {
		return false;
	}

	@Override
	public boolean isVisible(MinecraftClient client, boolean preview) {
		return preview || System.currentTimeMillis() - shownAt < SHOW_MS && topPack(client) != null;
	}

	@Override
	protected int contentWidth(MinecraftClient client, boolean preview) {
		return PAD * 3 + ICON + textWidth(client, preview);
	}

	@Override
	protected int contentHeight(MinecraftClient client, boolean preview) {
		return PAD * 2 + ICON;
	}

	@Override
	protected void renderContent(MinecraftClient client, boolean preview) {
		long age = System.currentTimeMillis() - shownAt;
		float alpha = preview ? 1F : Math.max(0F, Math.min(1F, (SHOW_MS - age) / (float) FADE_MS));
		if (alpha <= 0.02F) {
			return;
		}

		ResourcePackLoader.Entry pack = topPack(client);
		String name = pack == null ? "Meu pacote de textura.zip" : safeName(pack);
		String description = pack == null ? "por você" : firstLine(safeDescription(pack));

		int w = contentWidth(client, preview);
		int h = contentHeight(client, preview);
		Render2D.outlinedRect(0, 0, w, h, 4, Theme.withAlpha(0xFFB48CFF, alpha), Theme.withAlpha(0xD0101014, alpha));

		drawIcon(client, pack, alpha);
		TextRenderer text = client.textRenderer;
		int x = PAD * 2 + ICON;
		text.draw(text.trimToWidth(name, MAX_TEXT), x, PAD + 6, Theme.withAlpha(NAME_COLOR, alpha), true);
		text.draw(text.trimToWidth(description, MAX_TEXT), x, PAD + 6 + 12, Theme.withAlpha(DESCRIPTION_COLOR, alpha), true);
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private void drawIcon(MinecraftClient client, ResourcePackLoader.Entry pack, float alpha) {
		if (pack == null) {
			return;
		}
		GlStateManager.enableBlend();
		GlStateManager.color(1F, 1F, 1F, alpha);
		try {
			pack.bindIcon(client.getTextureManager());
			// Região 1x1 de uma textura 1x1: a imagem inteira, em qualquer tamanho de ícone.
			DrawableHelper.drawTexture(PAD, PAD, 0F, 0F, 1, 1, ICON, ICON, 1F, 1F);
		} catch (RuntimeException e) {
			// Pacote sem ícone legível: fica só o texto.
		}
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private int textWidth(MinecraftClient client, boolean preview) {
		ResourcePackLoader.Entry pack = topPack(client);
		TextRenderer text = client.textRenderer;
		String name = pack == null ? "Meu pacote de textura.zip" : safeName(pack);
		String description = pack == null ? "por você" : firstLine(safeDescription(pack));
		return Math.min(MAX_TEXT, Math.max(text.getStringWidth(name), text.getStringWidth(description))) + PAD;
	}

	/** O pacote de maior prioridade (o último da lista), ou {@code null} se só o padrão estiver ativo. */
	private static ResourcePackLoader.Entry topPack(MinecraftClient client) {
		ResourcePackLoader loader = client.getResourcePackLoader();
		if (loader == null) {
			return null;
		}
		List<ResourcePackLoader.Entry> selected = loader.getSelectedResourcePacks();
		return selected.isEmpty() ? null : selected.get(selected.size() - 1);
	}

	private static String signatureOf(MinecraftClient client) {
		ResourcePackLoader.Entry pack = topPack(client);
		return pack == null ? "" : safeName(pack);
	}

	private static String safeName(ResourcePackLoader.Entry pack) {
		try {
			return pack.getName();
		} catch (RuntimeException e) {
			return "?";
		}
	}

	private static String safeDescription(ResourcePackLoader.Entry pack) {
		try {
			return pack.getDescription();
		} catch (RuntimeException e) {
			return "";
		}
	}

	private static String firstLine(String text) {
		int newline = text.indexOf('\n');
		return newline >= 0 ? text.substring(0, newline) : text;
	}
}
