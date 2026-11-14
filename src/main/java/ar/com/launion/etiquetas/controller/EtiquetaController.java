package ar.com.launion.etiquetas.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.com.launion.etiquetas.dto.EtiquetaForm;
import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.service.CodigoBarrasService;
import ar.com.launion.etiquetas.service.ProductoService;
import ar.com.launion.etiquetas.util.FormatoEtiqueta;
import ar.com.launion.etiquetas.util.Precio;

@Controller
public class EtiquetaController {

	private final ProductoService productoService;
	private final CodigoBarrasService codigoBarrasService;

	public EtiquetaController(ProductoService productoService, CodigoBarrasService codigoBarrasService) {
		this.productoService = productoService;
		this.codigoBarrasService = codigoBarrasService;
	}

	// Panel de carga: GET /
	@GetMapping("/")
	public String nueva(Model model) {
		model.addAttribute("form", new EtiquetaForm());
		return "etiquetas/formEtiqueta";
	}

	// Mismo panel con un producto guardado: GET /productos/editar/5
	@GetMapping("/productos/editar/{id}")
	public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
		Producto producto = productoService.buscarPorId(id);
		if (producto == null) {
			redirect.addFlashAttribute("error", "Ese producto ya no existe.");
			return "redirect:/productos";
		}
		model.addAttribute("form", EtiquetaForm.desde(producto));
		return "etiquetas/formEtiqueta";
	}

	// Recibe el panel: POST /etiquetas/guardar  (accion = imprimir | hoja | guardar)
	@PostMapping("/etiquetas/guardar")
	public String guardar(@ModelAttribute("form") EtiquetaForm form, BindingResult result,
			@RequestParam(name = "accion", defaultValue = "imprimir") String accion,
			@RequestParam(name = "formato", required = false) String formato,
			RedirectAttributes redirect) {

		validar(form, result);
		Producto producto = null;
		if (!result.hasErrors()) {
			try {
				producto = productoService.guardar(form);
			} catch (IllegalArgumentException e) {
				result.rejectValue("codigoBarras", "duplicado", e.getMessage());
			}
		}
		if (result.hasErrors()) {
			return "etiquetas/formEtiqueta";
		}

		switch (accion) {
		case "hoja":
			productoService.agregarAHoja(List.of(producto.getIdProducto()));
			redirect.addFlashAttribute("mensaje", "«" + producto.getNombre() + "» se agregó a la hoja A4.");
			return "redirect:/";
		case "guardar":
			redirect.addFlashAttribute("mensaje", "«" + producto.getNombre() + "» se guardó.");
			return "redirect:/productos";
		default:
			// El tamaño elegido en el panel (13 x 8 o 8 x 4)
			return "redirect:/imprimir/producto/" + producto.getIdProducto() + "?formato=" + FormatoEtiqueta.de(formato).getClave();
		}
	}

	private void validar(EtiquetaForm form, BindingResult result) {
		String nombre = form.getNombre() == null ? "" : form.getNombre().trim();
		if (nombre.isEmpty()) {
			result.rejectValue("nombre", "vacio", "Escribí el nombre del producto.");
		} else if (nombre.length() > 60) {
			result.rejectValue("nombre", "largo", "El nombre puede tener hasta 60 caracteres.");
		}

		BigDecimal precio = Precio.parsear(form.getPrecio());
		if (precio == null) {
			result.rejectValue("precio", "invalido", "Escribí un precio, por ejemplo 1299 o 1.299,50.");
		} else if (!Precio.esValido(precio)) {
			result.rejectValue("precio", "rango", "El precio tiene que ser mayor a cero.");
		}

		String codigo = form.getCodigoBarras() == null ? "" : form.getCodigoBarras().trim();
		form.setCodigoBarras(codigo);
		if (codigo.isEmpty()) {
			result.rejectValue("codigoBarras", "vacio", "Escaneá o escribí el código de barras.");
		} else if (!codigoBarrasService.esValido(codigo)) {
			result.rejectValue("codigoBarras", "invalido", "Solo números y letras, sin espacios (hasta 40).");
		}
	}

}
