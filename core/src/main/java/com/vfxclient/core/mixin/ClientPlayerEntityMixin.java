package com.vfxclient.core.mixin;

import com.vfxclient.core.command.VfxCommands;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Intercepta comandos {@code /vfx} antes de irem para o servidor. */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

	@Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
	private void vfx$commands(String message, CallbackInfo ci) {
		if (VfxCommands.handle(message)) {
			ci.cancel();
		}
	}
}
