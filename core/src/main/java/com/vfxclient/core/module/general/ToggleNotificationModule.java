package com.vfxclient.core.module.general;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.gui.Theme;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.Module;
import net.minecraft.client.MinecraftClient;

/**
 * Aviso na tela quando um módulo é ligado ou desligado pela tecla de atalho:
 * o nome do módulo e "LIGADO" (verde) ou "DESLIGADO" (vermelho), sumindo sozinho.
 */
public class ToggleNotificationModule extends HudModule {

	private static final long SHOW_MS = 1600L;
	private static final long FADE_MS = 300L;

	private String name = "ZOOM";
	private boolean on = true;
	private long shownAt;

	public ToggleNotificationModule() {
		super("toggleNotice", "Aviso de atalho", "Mostra quando um módulo liga ou desliga", Category.GENERAL, true, 4, 56);
		setDefaultAnchor(BOTTOM_LEFT);
	}

	/**
	 * @param module módulo que acabou de ser alternado
	 */
	public void notify(Module module) {
		name = module.getName().toUpperCase();
		on = module.isEnabled();
		shownAt = System.currentTimeMillis();
	}

	@Override
	protected boolean usesSharedBackground() {
		return false;
	}

	@Override
	public boolean isVisible(MinecraftClient client, boolean preview) {
		return preview || System.currentTimeMillis() - shownAt < SHOW_MS;
	}

	@Override
	protected int contentWidth(MinecraftClient client, boolean preview) {
		return Math.max(client.textRenderer.getStringWidth(name), client.textRenderer.getStringWidth("DESLIGADO") * 2) + 12;
	}

	@Override
	protected int contentHeight(MinecraftClient client, boolean preview) {
		return 34;
	}

	@Override
	protected void renderContent(MinecraftClient client, boolean preview) {
		long age = System.currentTimeMillis() - shownAt;
		float alpha = preview ? 1F : Math.max(0F, Math.min(1F, (SHOW_MS - age) / (float) FADE_MS));
		if (alpha <= 0.02F) {
			return;
		}
		int w = contentWidth(client, preview);
		int color = on ? Theme.MODULE_ON : Theme.MODULE_OFF;
		Render2D.outlinedRect(0, 0, w, 34, 4, Theme.withAlpha(color, alpha), Theme.withAlpha(0xD0101014, alpha));
		int text = Theme.withAlpha(0xFFFFFFFF, alpha);
		client.textRenderer.draw(name, 6, 5, text, true);
		GlStateManager.pushMatrix();
		GlStateManager.translate(6F, 16F, 0F);
		GlStateManager.scale(1.5F, 1.5F, 1F);
		client.textRenderer.draw(on ? "LIGADO" : "DESLIGADO", 0, 0, Theme.withAlpha(color, alpha), true);
		GlStateManager.popMatrix();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}
}