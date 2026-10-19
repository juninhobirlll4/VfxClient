package com.vfxclient.core.module.hud;

import java.util.List;

import com.vfxclient.core.gui.IconCard;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.resource.ResourcePackLoader;

/**
 * Cartão com o pacote de textura em uso: o ícone do pacote, o nome inteiro do arquivo e a
 * descrição. Aparece ao entrar no mundo e quando você troca de pacote (e some sozinho), ou fica
 * sempre na tela se a opção estiver ligada.
 */
public class TexturePackModule extends HudModule {

	private static final long SHOW_MS = 6000L;
	private static final long FADE_MS = 500L;
	private static final int MAX_TEXT = 320;
	private static final int PURPLE = 0xFF5B3FA0;
	private static final int BORDER = 0xAAB48CFF;

	private final BooleanSetting always = add(new BooleanSetting("always", "Deixar sempre na tela", false));

	private String signature;
	private boolean inWorld;
	private long shownAt;

	public TexturePackModule() {
		super("texturePack", "Pacote de textura", "Mostra o ícone, o nome e a descrição do pacote de textura em uso",
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
		if (preview) {
			return true;
		}
		return topPack(client) != null && (always.get() || System.currentTimeMillis() - shownAt < SHOW_MS);
	}

	@Override
	protected int contentWidth(MinecraftClient client, boolean preview) {
		ResourcePackLoader.Entry pack = topPack(client);
		TextRenderer text = client.textRenderer;
		int title = Math.min(MAX_TEXT, text.getStringWidth(titleOf(pack)));
		int subtitle = Math.min(MAX_TEXT, text.getStringWidth(subtitleOf(pack)));
		return IconCard.width(Math.max(title, subtitle));
	}

	@Override
	protected int contentHeight(MinecraftClient client, boolean preview) {
		return IconCard.HEIGHT;
	}

	@Override
	protected void renderContent(MinecraftClient client, boolean preview) {
		float alpha = 1F;
		if (!preview && !always.get()) {
			long age = System.currentTimeMillis() - shownAt;
			alpha = Math.max(0F, Math.min(1F, (SHOW_MS - age) / (float) FADE_MS));
			if (alpha <= 0.02F) {
				return;
			}
		}

		ResourcePackLoader.Entry pack = topPack(client);
		TextRenderer text = client.textRenderer;
		IconCard.Icon icon = c -> {
			if (pack == null) {
				return false;
			}
			try {
				pack.bindIcon(c.getTextureManager());
				return true;
			} catch (RuntimeException e) {
				return false;
			}
		};
		IconCard.draw(client, icon, "TX", PURPLE, text.trimToWidth(titleOf(pack), MAX_TEXT),
				text.trimToWidth(subtitleOf(pack), MAX_TEXT), contentWidth(client, preview), BORDER, alpha);
	}

	private static String titleOf(ResourcePackLoader.Entry pack) {
		return pack == null ? "Meu pacote de textura.zip" : safeName(pack);
	}

	private static String subtitleOf(ResourcePackLoader.Entry pack) {
		return pack == null ? "por você" : firstLine(safeDescription(pack));
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
