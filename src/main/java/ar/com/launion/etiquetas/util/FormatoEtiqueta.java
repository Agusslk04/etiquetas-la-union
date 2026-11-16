package ar.com.launion.etiquetas.util;

import java.util.Arrays;
import java.util.List;

/**
 * Los tamaños que se pueden imprimir.
 *
 * Etiquetas (diseño naranja, con código de barras y fecha):
 *  - GRANDE: 13 x 8 cm, 4 por hoja A4 horizontal (2 x 2).
 *  - MINI:    9 x 5 cm, 10 por hoja A4 vertical  (2 x 5).
 *  - CHICA:   8 x 4 cm, 14 por hoja A4 vertical  (2 x 7).
 *
 * Carteles (diseño azul con el precio en el recuadro amarillo):
 *  - OFERTA_A4:    A4 horizontal entero, con el título "Ofertas de fin de semana".
 *  - MEDIA_OFERTA: media A4 (21 x 14,85 cm) con el título; 2 por hoja A4 vertical.
 *  - MEDIA:        media A4 sin título, para precios normales; 2 por hoja A4 vertical.
 */
public enum FormatoEtiqueta {

	GRANDE("grande", "13 × 8 cm", "Etiqueta de 13 × 8 cm", 4, false, false),
	MINI("mini", "9 × 5 cm", "Etiqueta de 9 × 5 cm", 10, true, false),
	CHICA("chica", "8 × 4 cm", "Etiqueta de 8 × 4 cm", 14, true, false),

	OFERTA_A4("oferta-a4", "A4 oferta", "A4 · Ofertas de fin de semana", 1, false, true),
	MEDIA_OFERTA("media-oferta", "Media A4 oferta", "Media A4 · Ofertas de fin de semana", 2, true, true),
	MEDIA("media", "Media A4", "Media A4 · Sin título", 2, true, true);

	/** Nombre de la cookie donde cada compu recuerda el último tamaño usado. */
	public static final String COOKIE = "formato";

	private final String clave;
	private final String medida;
	private final String descripcion;
	private final int porHoja;
	private final boolean hojaVertical;
	private final boolean cartel;

	FormatoEtiqueta(String clave, String medida, String descripcion, int porHoja, boolean hojaVertical, boolean cartel) {
		this.clave = clave;
		this.medida = medida;
		this.descripcion = descripcion;
		this.porHoja = porHoja;
		this.hojaVertical = hojaVertical;
		this.cartel = cartel;
	}

	/** "chica" → CHICA, "oferta-a4" → OFERTA_A4...; cualquier otra cosa (o null) → GRANDE. */
	public static FormatoEtiqueta de(String clave) {
		String buscada = clave == null ? "" : clave.trim();
		for (FormatoEtiqueta f : values()) {
			if (f.clave.equalsIgnoreCase(buscada)) {
				return f;
			}
		}
		return GRANDE;
	}

	/** Las etiquetas (cartel = false) o los carteles (cartel = true), en el orden de los botones. */
	public static List<FormatoEtiqueta> grupo(boolean carteles) {
		return Arrays.stream(values()).filter(f -> f.cartel == carteles).toList();
	}

	/** Cuántas hojas A4 hacen falta para esa cantidad de etiquetas. */
	public long hojasPara(long etiquetas) {
		return (etiquetas + porHoja - 1) / porHoja;
	}

	/** Para la vista de impresión: "3 etiquetas de 13 × 8 cm" o "1 cartel · A4 · Ofertas de fin de semana". */
	public String cantidad(long n) {
		if (cartel) {
			return n + (n == 1 ? " cartel · " : " carteles · ") + descripcion;
		}
		return n + (n == 1 ? " etiqueta de " : " etiquetas de ") + medida;
	}

	/** Clases CSS de la etiqueta (ver etiqueta.css). */
	public String getClases() {
		return switch (this) {
			case OFERTA_A4 -> "cartel cartel-a4 cartel-con-titulo";
			case MEDIA_OFERTA -> "cartel cartel-media cartel-con-titulo";
			case MEDIA -> "cartel cartel-media cartel-sin-titulo";
			default -> "etiqueta-" + clave;
		};
	}

	/** Imagen de fondo de los carteles (sacada de los Word de La Unión); null en las etiquetas. */
	public String getFondo() {
		return switch (this) {
			case OFERTA_A4, MEDIA_OFERTA -> "/img/cartel-oferta.jpg";
			case MEDIA -> "/img/cartel-precio.jpg";
			default -> null;
		};
	}

	public String getClave() {
		return clave;
	}

	public String getMedida() {
		return medida;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public int getPorHoja() {
		return porHoja;
	}

	public boolean isHojaVertical() {
		return hojaVertical;
	}

	public boolean isCartel() {
		return cartel;
	}

}
