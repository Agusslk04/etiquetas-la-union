package ar.com.launion.etiquetas.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Tamaños de página del sistema.
 */
public final class Paginas {

	/** Filas por página en todas las listas (productos, hoja A4, cargas). */
	public static final int TAMANIO = 15;

	private Paginas() {
	}

	/** Parte una lista en grupos (por ejemplo, de a 4 etiquetas por hoja). */
	public static <T> List<List<T>> dividir(List<T> lista, int tamanio) {
		List<List<T>> grupos = new ArrayList<>();
		for (int i = 0; i < lista.size(); i += tamanio) {
			grupos.add(lista.subList(i, Math.min(i + tamanio, lista.size())));
		}
		return grupos;
	}

	/** Número de página válido (nunca negativo). */
	public static int normalizar(int pagina) {
		return Math.max(0, pagina);
	}

}
