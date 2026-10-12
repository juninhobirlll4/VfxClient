package com.vfxclient.core.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Dá acesso ao método protegido {@code showCrosshair} da HUD. */
@Mixin(InGameHud.class)
public interface InGameHudAccessor {

	/** @return se o jogo desenharia a mira agora (sem F3, sem tela aberta etc.) */
	@Invoker("showCrosshair")
	boolean vfx$showCrosshair();
}
