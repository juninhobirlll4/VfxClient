package com.vfxclient.core.mixin;

import net.minecraft.client.gui.widget.OptionSliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Posição dos sliders de opção (0 a 1), para desenhar o pegador. */
@Mixin(OptionSliderWidget.class)
public interface OptionSliderWidgetAccessor {

	@Accessor("value")
	float vfx$getValue();
}
