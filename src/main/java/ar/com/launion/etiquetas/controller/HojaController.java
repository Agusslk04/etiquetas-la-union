package ar.com.launion.etiquetas.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.service.ProductoService;
import ar.com.launion.etiquetas.util.FormatoEtiqueta;

@Controller
@RequestMapping("/hoja")
public class HojaController {

	private static final Set<Integer> REDONDEOS = Set.of(1, 10, 50, 100);

	private final ProductoService productoService;

	public HojaController(ProductoService productoService) {
		this.productoService = productoService;
	}

	// Hoja A4 con precios editables, de a 15: GET /hoja?pagina=0
	@GetMapping
	public String ver(@RequestParam(name = "pagina", defaultValue = "0") int pagina, Model model) {
		Page<Producto> productos = productoService.listarHoja(pagina);
		if (pagina > 0 && productos.getTotalPages() > 0 && pagina >= productos.getTotalPages()) {
			productos = productoService.listarHoja(productos.getTotalPages() - 1); // la página quedó vacía
		}
		long etiquetas = productoService.contarEtiquetasEnHoja();
		model.addAttribute("productos", productos);
		model.addAttribute("totalEtiquetas", etiquetas);
		return "hoja/hojaA4";
	}

	// Agrega un producto desde el listado: POST /hoja/agregar/5
	@PostMapping("/agregar/{id}")
	public String agregarUno(@PathVariable("id") Long id,
			@RequestParam(name = "q", defaultValue = "") String q,
			@RequestParam(name = "pagina", defaultValue = "0") int pagina,
			RedirectAttributes redirect) {
		productoService.agregarAHoja(List.of(id));
		Producto p = productoService.buscarPorId(id);
		if (p != null) {
			redirect.addFlashAttribute("mensaje", "«" + p.getNombre() + "» se agregó a la hoja A4.");
		}
		redirect.addAttribute("q", q);
		redirect.addAttribute("pagina", pagina);
		return "redirect:/productos";
	}

	// Agrega los seleccionados: POST /hoja/agregar
	@PostMapping("/agregar")
	public String agregarVarios(@RequestParam(name = "ids", required = false) List<Long> ids,
			@RequestParam(name = "q", defaultValue = "") String q,
			@RequestParam(name = "pagina", defaultValue = "0") int pagina,
			RedirectAttributes redirect) {
		if (ids == null || ids.isEmpty()) {
			redirect.addFlashAttribute("error", "Marcá al menos un producto.");
		} else {
			productoService.agregarAHoja(ids);
			redirect.addFlashAttribute("mensaje", ids.size() == 1 ? "Se agregó 1 producto a la hoja A4."
					: "Se agregaron " + ids.size() + " productos a la hoja A4.");
		}
		redirect.addAttribute("q", q);
		redirect.addAttribute("pagina", pagina);
		return "redirect:/productos";
	}

	// Recibe la página editada: POST /hoja
	// accion = guardar | imprimir | porcentaje | pagina:N | quitar:ID | vaciar
	@PostMapping
	public String procesar(@RequestParam(name = "ids", required = false) List<Long> ids,
			@RequestParam(name = "precios", required = false) List<String> precios,
			@RequestParam(name = "copias", required = false) List<String> copias,
			@RequestParam(name = "pagina", defaultValue = "0") int pagina,
			@RequestParam(name = "porcentaje", defaultValue = "") String porcentaje,
			@RequestParam(name = "redondeo", defaultValue = "1") int redondeo,
			@RequestParam(name = "formato", required = false) String formato,
			@RequestParam(name = "accion", defaultValue = "guardar") String accion,
			RedirectAttributes redirect) {

		if ("vaciar".equals(accion)) {
			productoService.vaciarHoja();
			redirect.addFlashAttribute("mensaje", "La hoja quedó vacía. Los productos siguen guardados.");
			return "redirect:/hoja";
		}

		// Siempre se guardan primero los precios y copias editados en esta página
		List<String> errores = productoService.actualizarHoja(ids, precios, copias);
		if (!errores.isEmpty()) {
			redirect.addFlashAttribute("errores", errores);
		}
		redirect.addAttribute("pagina", pagina);

		if (accion.startsWith("pagina:")) {
			redirect.addAttribute("pagina", entero(accion.substring("pagina:".length())));
			return "redirect:/hoja";
		}
		if (accion.startsWith("quitar:")) {
			productoService.quitarDeHoja(Long.valueOf(accion.substring("quitar:".length())));
			redirect.addFlashAttribute("mensaje", "Se quitó de la hoja.");
			return "redirect:/hoja";
		}
		if ("porcentaje".equals(accion)) {
			BigDecimal pct = leerPorcentaje(porcentaje);
			if (pct == null) {
				redirect.addFlashAttribute("error", "Escribí un porcentaje entre -90 y 500, por ejemplo 10 o -5.");
			} else {
				int cambiados = productoService.ajustarPreciosHoja(pct, REDONDEOS.contains(redondeo) ? redondeo : 1);
				redirect.addFlashAttribute("mensaje", "Se cambiaron " + cambiados + " precios de la hoja ("
						+ (pct.signum() > 0 ? "+" : "") + pct.stripTrailingZeros().toPlainString() + " %).");
			}
			return "redirect:/hoja";
		}
		if ("imprimir".equals(accion) && errores.isEmpty()) {
			return "redirect:/imprimir/hoja?formato=" + FormatoEtiqueta.de(formato).getClave();
		}
		if (errores.isEmpty()) {
			redirect.addFlashAttribute("mensaje", "Precios guardados.");
		}
		return "redirect:/hoja";
	}

	// "10", "+10", "-5", "7,5" → BigDecimal; null si no es válido
	private static BigDecimal leerPorcentaje(String texto) {
		String t = texto.trim().replace(" ", "").replace("%", "").replace(',', '.');
		if (!t.matches("[+-]?\\d{1,3}(\\.\\d{1,2})?")) {
			return null;
		}
		BigDecimal pct = new BigDecimal(t);
		boolean enRango = pct.signum() != 0 && pct.compareTo(new BigDecimal("-90")) >= 0 && pct.compareTo(new BigDecimal("500")) <= 0;
		return enRango ? pct : null;
	}

	private static int entero(String texto) {
		try {
			return Math.max(0, Integer.parseInt(texto.trim()));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
