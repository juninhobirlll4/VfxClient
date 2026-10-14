package com.vfxclient.core.mixin;

import java.util.List;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.LanServerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Servidores encontrados na rede local (aba "Na rede local"). */
@Mixin(MultiplayerServerListWidget.class)
public interface ServerListWidgetAccessor {

	@Accessor("lanServers")
	List<LanServerEntry> vfx$getLanServers();
}