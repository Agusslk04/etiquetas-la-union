package ar.com.launion.etiquetas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ar.com.launion.etiquetas.config.CrearBaseDeDatos;

@SpringBootApplication
public class EtiquetasApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(EtiquetasApplication.class);
		// Antes de conectarse, crea la base en PostgreSQL si todavía no existe
		app.addListeners(new CrearBaseDeDatos());
		app.run(args);
	}

}
