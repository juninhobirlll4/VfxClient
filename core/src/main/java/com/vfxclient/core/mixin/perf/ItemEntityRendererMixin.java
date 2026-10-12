package com.vfxclient.core.mixin.perf;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.RenderTweaksModule;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Itens no chão desenhados como um só modelo.
 *
 * <p>O jogo desenha até 5 cópias do modelo para pilhas grandes; com muitos drops
 * (farms, PvP) isso pesa bastante.</p>
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

	/** {@code method_10222} = quantas cópias do modelo desenhar para a pilha (sem nome no Yarn 1.8.9). */
	@Inject(method = "method_10222(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
	private void vfx$singleModel(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		RenderTweaksModule tweaks = VfxCore.module(RenderTweaksModule.class);
		if (tweaks != null && tweaks.stackedItems()) {
			cir.setReturnValue(1);
		}
	}
}
