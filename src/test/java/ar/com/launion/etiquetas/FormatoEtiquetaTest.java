package ar.com.launion.etiquetas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ar.com.launion.etiquetas.util.FormatoEtiqueta;

class FormatoEtiquetaTest {

	@Test
	void reconoceElTamanioYPorDefectoEsElGrande() {
		assertEquals(FormatoEtiqueta.CHICA, FormatoEtiqueta.de("chica"));
		assertEquals(FormatoEtiqueta.CHICA, FormatoEtiqueta.de(" CHICA "));
		assertEquals(FormatoEtiqueta.GRANDE, FormatoEtiqueta.de("grande"));
		assertEquals(FormatoEtiqueta.GRANDE, FormatoEtiqueta.de(null));
		assertEquals(FormatoEtiqueta.GRANDE, FormatoEtiqueta.de("cualquier cosa"));
	}

	@Test
	void cuentaLasHojasA4() {
		// 13 x 8: 4 por hoja horizontal
		assertEquals(4, FormatoEtiqueta.GRANDE.getPorHoja());
		assertFalse(FormatoEtiqueta.GRANDE.isHojaVertical());
		assertEquals(498, FormatoEtiqueta.GRANDE.hojasPara(1989));

		// 8 x 4: 14 por hoja vertical (2 columnas x 7 filas)
		assertEquals(14, FormatoEtiqueta.CHICA.getPorHoja());
		assertTrue(FormatoEtiqueta.CHICA.isHojaVertical());
		assertEquals(1, FormatoEtiqueta.CHICA.hojasPara(14));
		assertEquals(2, FormatoEtiqueta.CHICA.hojasPara(15));
		assertEquals(143, FormatoEtiqueta.CHICA.hojasPara(1989));
	}

}
