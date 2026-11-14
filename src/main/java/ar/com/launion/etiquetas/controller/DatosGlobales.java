package ar.com.launion.etiquetas.controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.com.launion.etiquetas.config.DireccionRed;
import ar.com.launion.etiquetas.service.ProductoService;
import ar.com.launion.etiquetas.util.FormatoEtiqueta;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Datos que usan todas las pantallas: la fecha del día (va impresa en la etiqueta),
 * cuántas etiquetas hay en la hoja A4, la dirección de red y el tamaño de cartel
 * que eligió esta compu la última vez.
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

	/** Último tamaño usado en esta compu (13 x 8 si nunca eligió). */
	@ModelAttribute("formato")
	public FormatoEtiqueta formato(@CookieValue(name = FormatoEtiqueta.COOKIE, required = false) String cookie) {
		return FormatoEtiqueta.de(cookie);
	}

	@ModelAttribute("formatos")
	public FormatoEtiqueta[] formatos() {
		return FormatoEtiqueta.values();
	}

	/** Recuerda el tamaño elegido por un año en esta compu. */
	public static void recordarFormato(HttpServletResponse response, FormatoEtiqueta formato) {
		ResponseCookie cookie = ResponseCookie.from(FormatoEtiqueta.COOKIE, formato.getClave())
				.path("/").maxAge(Duration.ofDays(365)).sameSite("Lax").build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

}
