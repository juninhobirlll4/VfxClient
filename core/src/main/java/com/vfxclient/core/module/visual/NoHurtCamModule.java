package com.vfxclient.core.module.visual;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;

/** Remove o tremor da câmera ao levar dano. */
public class NoHurtCamModule extends Module {

	public NoHurtCamModule() {
		super("noHurtCam", "Sem tremor", "Câmera não balança ao levar dano", Category.VISUAL, false);
	}
}
