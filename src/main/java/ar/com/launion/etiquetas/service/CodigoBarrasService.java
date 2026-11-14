package ar.com.launion.etiquetas.service;

public interface CodigoBarrasService {

	/** Números, letras, punto y guion; hasta 40 caracteres. */
	boolean esValido(String codigo);

	/**
	 * Dibuja el código como SVG (vectorial, se imprime nítido).
	 * Usa EAN-13 / EAN-8 / UPC-A si el número es válido; si no, Code 128.
	 */
	String generarSvg(String codigo);

}
