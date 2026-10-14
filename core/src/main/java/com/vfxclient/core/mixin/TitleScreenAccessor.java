package com.vfxclient.core.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Acesso ao fundo panorâmico desfocado da tela inicial (usado pela tela inicial do VfxClient). */
@Mixin(TitleScreen.class)
public interface TitleScreenAccessor {

	@Invoker("renderBackground")
	void vfx$renderBackground(int mouseX, int mouseY, float tickDelta);
}