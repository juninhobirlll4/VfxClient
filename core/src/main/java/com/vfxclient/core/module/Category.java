package com.vfxclient.core.module;

/** Categorias exibidas na barra lateral da Click GUI. */
public enum Category {
	GENERAL("Geral", "Menu, tela inicial e comandos"),
	HUD("HUD", "Elementos na tela durante o jogo"),
	UTILITY("Utilidades", "Sprint, mira, chat e outros ajustes"),
	VISUAL("Visual", "Aparência do mundo e do jogo"),
	PERFORMANCE("Desempenho", "Ganhos de FPS"),
	SOCIAL("Social", "Amigos, skins e waypoints");

	private final String displayName;
	private final String description;

	Category(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}

	/** @return nome exibido */
	public String getDisplayName() {
		return displayName;
	}

	/** @return descrição curta exibida no topo da categoria */
	public String getDescription() {
		return description;
	}
}
