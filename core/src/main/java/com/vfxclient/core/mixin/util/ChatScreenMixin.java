package com.vfxclient.core.mixin.util;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.hud.ChatLines;
import com.vfxclient.core.module.util.ChatModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ctrl + clique numa mensagem do chat copia o texto dela. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

	@Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
	private void vfx$copyMessage(int mouseX, int mouseY, int button, CallbackInfo ci) {
		ChatModule module = VfxCore.module(ChatModule.class);
		if (button != 0 || module == null || !module.copyOnClick() || !Screen.hasControlDown()) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		String text = ((ChatLines) client.inGameHud.getChatHud()).vfx$messageAt(Mouse.getX(), Mouse.getY());
		if (text != null && !text.isEmpty()) {
			Screen.setClipboard(text);
			client.inGameHud.setOverlayMessage("Mensagem copiada", false);
			ci.cancel();
		}
	}
}