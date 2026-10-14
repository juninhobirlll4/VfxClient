package com.vfxclient.core.mixin.util;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.util.ScrollTooltipsModule;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tooltips com rolagem: guarda o item, move o tooltip e lê a rodinha do mouse no inventário. */
@Mixin(Screen.class)
public abstract class ScreenTooltipMixin {

	@Inject(method = "renderTooltip(Lnet/minecraft/item/ItemStack;II)V", at = @At("HEAD"))
	private void vfx$tooltipItem(ItemStack stack, int x, int y, CallbackInfo ci) {
		ScrollTooltipsModule module = VfxCore.module(ScrollTooltipsModule.class);
		if (module != null) {
			module.onItemTooltip(stack);
		}
	}

	@ModifyVariable(method = "renderTooltip(Ljava/util/List;II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int vfx$tooltipX(int x) {
		ScrollTooltipsModule module = VfxCore.module(ScrollTooltipsModule.class);
		return module == null ? x : x + module.offsetX();
	}

	@ModifyVariable(method = "renderTooltip(Ljava/util/List;II)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private int vfx$tooltipY(int y) {
		ScrollTooltipsModule module = VfxCore.module(ScrollTooltipsModule.class);
		return module == null ? y : y + module.offsetY();
	}

	@Inject(method = "handleMouse()V", at = @At("TAIL"))
	private void vfx$tooltipScroll(CallbackInfo ci) {
		if (!((Object) this instanceof HandledScreen)) {
			return;
		}
		ScrollTooltipsModule module = VfxCore.module(ScrollTooltipsModule.class);
		int wheel = Mouse.getEventDWheel();
		if (module != null && wheel != 0) {
			module.scroll(wheel, Screen.hasShiftDown());
		}
	}
}