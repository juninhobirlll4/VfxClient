package com.vfxclient.launcher.game;

/** Recebe o andamento de uma etapa demorada (download, verificação, inicialização). */
public interface Progress {

	/** Não faz nada (útil em testes e no modo linha de comando sem saída). */
	Progress NONE = (stage, fraction, detail) -> { };

	/**
	 * @param stage    etapa atual, ex.: "Baixando bibliotecas"
	 * @param fraction de 0 a 1, ou {@code -1} quando não dá para medir
	 * @param detail   detalhe opcional, ex.: "12/340 arquivos · 18 MB"
	 */
	void update(String stage, double fraction, String detail);
}
