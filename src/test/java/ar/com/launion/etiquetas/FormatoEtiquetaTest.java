package ar.com.launion.etiquetas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.com.launion.etiquetas.util.FormatoEtiqueta;

class FormatoEtiquetaTest {

	@Test
	void reconoceElTamanioYPorDefectoEsElGrande() {
		assertEquals(FormatoEtiqueta.CHICA, FormatoEtiqueta.de("chica"));
		assertEquals(FormatoEtiqueta.CHICA, FormatoEtiqueta.de(" CHICA "));
		assertEquals(FormatoEtiqueta.MINI, FormatoEtiqueta.de("mini"));
		assertEquals(FormatoEtiqueta.OFERTA_A4, FormatoEtiqueta.de("oferta-a4"));
		assertEquals(FormatoEtiqueta.MEDIA_OFERTA, FormatoEtiqueta.de("media-oferta"));
		assertEquals(FormatoEtiqueta.MEDIA, FormatoEtiqueta.de("media"));
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

		// 9 x 5: 10 por hoja vertical (2 columnas x 5 filas)
		assertEquals(10, FormatoEtiqueta.MINI.getPorHoja());
		assertTrue(FormatoEtiqueta.MINI.isHojaVertical());
		assertEquals(199, FormatoEtiqueta.MINI.hojasPara(1989));

		// 8 x 4: 14 por hoja vertical (2 columnas x 7 filas)
		assertEquals(14, FormatoEtiqueta.CHICA.getPorHoja());
		assertTrue(FormatoEtiqueta.CHICA.isHojaVertical());
		assertEquals(1, FormatoEtiqueta.CHICA.hojasPara(14));
		assertEquals(2, FormatoEtiqueta.CHICA.hojasPara(15));
		assertEquals(143, FormatoEtiqueta.CHICA.hojasPara(1989));

		// Carteles: A4 entero (horizontal) y media A4 (2 por hoja vertical; uno solo usa media hoja)
		assertEquals(3, FormatoEtiqueta.OFERTA_A4.hojasPara(3));
		assertFalse(FormatoEtiqueta.OFERTA_A4.isHojaVertical());
		assertEquals(1, FormatoEtiqueta.MEDIA.hojasPara(1));
		assertEquals(2, FormatoEtiqueta.MEDIA_OFERTA.hojasPara(3));
		assertTrue(FormatoEtiqueta.MEDIA.isHojaVertical());
	}

	@Test
	void separaEtiquetasDeCarteles() {
		assertEquals(List.of(FormatoEtiqueta.GRANDE, FormatoEtiqueta.MINI, FormatoEtiqueta.CHICA), FormatoEtiqueta.grupo(false));
		assertEquals(List.of(FormatoEtiqueta.OFERTA_A4, FormatoEtiqueta.MEDIA_OFERTA, FormatoEtiqueta.MEDIA), FormatoEtiqueta.grupo(true));

		// Solo los carteles usan la imagen de los Word; el de oferta y el sin título tienen fondos distintos
		assertNull(FormatoEtiqueta.GRANDE.getFondo());
		assertEquals(FormatoEtiqueta.OFERTA_A4.getFondo(), FormatoEtiqueta.MEDIA_OFERTA.getFondo());
		assertFalse(FormatoEtiqueta.MEDIA.getFondo().equals(FormatoEtiqueta.OFERTA_A4.getFondo()));
	}

	@Test
	void describeLaCantidad() {
		assertEquals("1 etiqueta de 13 × 8 cm", FormatoEtiqueta.GRANDE.cantidad(1));
		assertEquals("12 etiquetas de 9 × 5 cm", FormatoEtiqueta.MINI.cantidad(12));
		assertEquals("1 cartel · A4 · Ofertas de fin de semana", FormatoEtiqueta.OFERTA_A4.cantidad(1));
		assertEquals("3 carteles · Media A4 · Sin título", FormatoEtiqueta.MEDIA.cantidad(3));
	}

}
