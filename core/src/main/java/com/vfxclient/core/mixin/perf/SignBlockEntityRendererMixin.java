package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.BlockDetailsModule;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Texto das placas só de perto ({@link BlockDetailsModule}). A placa continua aparecendo; só
 * as letras (a parte cara de desenhar) somem.
 */
@Mixin(SignBlockEntityRenderer.class)
public abstract class SignBlockEntityRendererMixin {

	@Unique
	private boolean vfx$hideText;

	/** As coordenadas recebidas já são relativas à câmera. */
	@Inject(method = "render(Lnet/minecraft/block/entity/SignBlockEntity;DDDFI)V", at = @At("HEAD"))
	private void vfx$checkDistance(SignBlockEntity sign, double x, double y, double z, float tickDelta, int destroyProgress,
			CallbackInfo ci) {
		BlockDetailsModule module = VfxCore.module(BlockDetailsModule.class);
		vfx$hideText = module != null && module.hideSignText(x * x + y * y + z * z);
	}

	@Redirect(method = "render(Lnet/minecraft/block/entity/SignBlockEntity;DDDFI)V", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;III)I"))
	private int vfx$signText(TextRenderer textRenderer, String text, int x, int y, int color) {
		return vfx$hideText ? 0 : textRenderer.draw(text, x, y, color);
	}
}
