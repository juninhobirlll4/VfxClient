package com.vfxclient.core.mixin;

import com.vfxclient.core.gui.Logo;
import com.vfxclient.core.net.VfxUsers;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Logo do VfxClient ao lado do nick, em cima de quem usa o client. */
@Mixin(EntityRenderer.class)
public abstract class NametagLogoMixin {

	/** Tamanho da logo, na unidade do texto (o texto tem 8 de altura). */
	@Unique
	private static final int VFX$LOGO = 9;
	@Unique
	private static final int VFX$GAP = 2;

	@Unique
	private boolean vfx$badge;

	@Inject(method = "renderLabelIfPresent", at = @At("HEAD"))
	private void vfx$checkBadge(Entity entity, String text, double x, double y, double z, int maxDistance, CallbackInfo ci) {
		// Só no nome do jogador: o jogo chama este método de novo para a linha da vida ("20 ♥") embaixo dele.
		vfx$badge = entity instanceof PlayerEntity
				&& VfxUsers.showsBadge(((PlayerEntity) entity).getGameProfile().getName())
				&& text.equals(entity.getName().asFormattedString());
	}

	/** A segunda escrita do nome é a visível (a primeira é o contorno por trás de paredes). */
	@Redirect(method = "renderLabelIfPresent", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;III)I", ordinal = 1))
	private int vfx$drawName(TextRenderer textRenderer, String text, int x, int y, int color) {
		int result = textRenderer.draw(text, x, y, color);
		if (vfx$badge) {
			Logo.drawSmall(x - VFX$LOGO - VFX$GAP, y - 1, VFX$LOGO);
		}
		return result;
	}
}
