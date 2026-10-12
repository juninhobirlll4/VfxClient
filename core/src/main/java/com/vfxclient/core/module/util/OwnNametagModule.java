package com.vfxclient.core.module.util;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;

/** Mostra o seu próprio nome acima da cabeça na visão em terceira pessoa (F5). */
public class OwnNametagModule extends Module {

	public OwnNametagModule() {
		super("ownNametag", "Meu nome", "Ver o próprio nome no F5", Category.UTILITY, false);
	}
}
