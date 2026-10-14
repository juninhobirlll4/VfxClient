package com.vfxclient.core.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Acesso à tela anterior e à lista da tela de servidores (usado pela tela do VfxClient). */
@Mixin(MultiplayerScreen.class)
public interface MultiplayerScreenAccessor {

	@Accessor("parent")
	Screen vfx$getParent();

	@Accessor("serverListWidget")
	MultiplayerServerListWidget vfx$getServerListWidget();
}