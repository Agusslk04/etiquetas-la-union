package ar.com.launion.etiquetas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ar.com.launion.etiquetas.util.Precio;

class PrecioTest {

	@Test
	void leeLosFormatosQueEscribeLaGente() {
		assertEquals(new BigDecimal("1299.00"), Precio.parsear("1299"));
		assertEquals(new BigDecimal("1299.00"), Precio.parsear("1.299"));
		assertEquals(new BigDecimal("1299.50"), Precio.parsear("1.299,50"));
		assertEquals(new BigDecimal("1299.50"), Precio.parsear("1299,5"));
		assertEquals(new BigDecimal("1299.50"), Precio.parsear("$ 1.299,50"));
		assertEquals(new BigDecimal("1299.50"), Precio.parsear("1,299.50"));
		assertEquals(new BigDecimal("12.50"), Precio.parsear("12.5"));
		assertEquals(new BigDecimal("1299999.00"), Precio.parsear("1.299.999"));
	}

	@Test
	void rechazaTextoQueNoEsPrecio() {
		assertNull(Precio.parsear(""));
		assertNull(Precio.parsear("abc"));
		assertNull(Precio.parsear("1.2.3,4,5"));
		assertFalse(Precio.esValido(Precio.parsear("0")));
	}

	@Test
	void formateaAlEstiloArgentino() {
		BigDecimal precio = new BigDecimal("1299.50");
		assertEquals("1.299", Precio.entero(precio));
		assertEquals("50", Precio.centavos(precio));
		assertEquals("$ 1.299,50", Precio.texto(precio));
		assertEquals("", Precio.centavos(new BigDecimal("1299.00")));
		assertEquals("12.999", Precio.editable(new BigDecimal("12999")));
	}

}
