package ar.com.launion.etiquetas.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Errores que pueden pasar antes de llegar a un controlador.
 */
@ControllerAdvice
public class ErroresGenerales {

	// El Excel pesa más de 10 MB
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public String archivoMuyGrande(RedirectAttributes redirect) {
		redirect.addFlashAttribute("error", "El archivo pesa más de 10 MB. Dividilo en partes más chicas.");
		return "redirect:/cargas";
	}

}
