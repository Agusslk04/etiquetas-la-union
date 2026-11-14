package ar.com.launion.etiquetas.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.com.launion.etiquetas.config.DireccionRed;
import ar.com.launion.etiquetas.service.ProductoService;

/**
 * Datos que usan todas las pantallas: la fecha del día (va impresa en la etiqueta),
 * cuántas etiquetas hay en la hoja A4 (contador del menú) y la dirección de red.
 */
@ControllerAdvice(assignableTypes = { EtiquetaController.class, ProductoController.class, HojaController.class,
		ImpresionController.class, CargaController.class })
public class DatosGlobales {

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final ProductoService productoService;
	private final DireccionRed direccionRed;

	public DatosGlobales(ProductoService productoService, DireccionRed direccionRed) {
		this.productoService = productoService;
		this.direccionRed = direccionRed;
	}

	@ModelAttribute("hoy")
	public String hoy() {
		return LocalDate.now().format(FECHA);
	}

	@ModelAttribute("cantidadHoja")
	public long cantidadHoja() {
		return productoService.contarEtiquetasEnHoja();
	}

	@ModelAttribute("direccionRed")
	public String direccionRed() {
		return direccionRed.getDireccion();
	}

}
