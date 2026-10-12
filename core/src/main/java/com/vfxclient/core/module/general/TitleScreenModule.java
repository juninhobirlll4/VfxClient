package com.vfxclient.core.module.general;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;

/** Troca a logo do Minecraft pela do VfxClient na tela inicial. */
public class TitleScreenModule extends Module {

	public TitleScreenModule() {
		super("titleScreen", "Tela inicial", "Nossa logo no menu principal", Category.GENERAL, true);
	}
}
