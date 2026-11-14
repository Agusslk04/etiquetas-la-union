package ar.com.launion.etiquetas.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ar.com.launion.etiquetas.service.CodigoBarrasService;

@Controller
public class CodigoBarrasController {

	private static final MediaType SVG = MediaType.valueOf("image/svg+xml");

	private final CodigoBarrasService codigoBarrasService;

	public CodigoBarrasController(CodigoBarrasService codigoBarrasService) {
		this.codigoBarrasService = codigoBarrasService;
	}

	// Imagen del código: GET /codigo-barras?valor=7790070012345
	@GetMapping("/codigo-barras")
	public ResponseEntity<String> svg(@RequestParam(name = "valor", defaultValue = "") String valor) {
		return ResponseEntity.ok()
				.contentType(SVG)
				.cacheControl(CacheControl.maxAge(Duration.ofDays(30)))
				.body(codigoBarrasService.generarSvg(valor.trim()));
	}

}
