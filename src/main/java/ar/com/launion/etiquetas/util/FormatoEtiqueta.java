package ar.com.launion.etiquetas.util;

/**
 * Los dos tamaños de cartel que se pueden imprimir.
 *
 *  - GRANDE: 13 x 8 cm, 4 por hoja A4 horizontal (2 x 2).
 *  - CHICA:   8 x 4 cm, 14 por hoja A4 vertical  (2 x 7).
 */
public enum FormatoEtiqueta {

	GRANDE("grande", "13 × 8 cm", 4, false),
	CHICA("chica", "8 × 4 cm", 14, true);

	/** Nombre de la cookie donde cada compu recuerda el último tamaño usado. */
	public static final String COOKIE = "formato";

	private final String clave;
	private final String medida;
	private final int porHoja;
	private final boolean hojaVertical;

	FormatoEtiqueta(String clave, String medida, int porHoja, boolean hojaVertical) {
		this.clave = clave;
		this.medida = medida;
		this.porHoja = porHoja;
		this.hojaVertical = hojaVertical;
	}

	/** "chica" → CHICA; cualquier otra cosa (o null) → GRANDE. */
	public static FormatoEtiqueta de(String clave) {
		return CHICA.clave.equalsIgnoreCase(clave == null ? "" : clave.trim()) ? CHICA : GRANDE;
	}

	/** Cuántas hojas A4 hacen falta para esa cantidad de etiquetas. */
	public long hojasPara(long etiquetas) {
		return (etiquetas + porHoja - 1) / porHoja;
	}

	public String getClave() {
		return clave;
	}

	public String getMedida() {
		return medida;
	}

	public int getPorHoja() {
		return porHoja;
	}

	public boolean isHojaVertical() {
		return hojaVertical;
	}

}
