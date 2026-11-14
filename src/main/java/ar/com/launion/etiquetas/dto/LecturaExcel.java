package ar.com.launion.etiquetas.dto;

import java.util.ArrayList;
import java.util.List;

import ar.com.launion.etiquetas.entity.Producto;

/**
 * Opción "Solo imprimir": las etiquetas tal como vienen en el Excel,
 * sin guardar nada en la base de datos.
 */
public class LecturaExcel {

	// Productos armados en memoria (no se guardan)
	private final List<Producto> etiquetas = new ArrayList<>();

	private int conError;
	private final List<String> filasConError = new ArrayList<>();

	public List<Producto> getEtiquetas() {
		return etiquetas;
	}

	public int getConError() {
		return conError;
	}

	public void setConError(int conError) {
		this.conError = conError;
	}

	public List<String> getFilasConError() {
		return filasConError;
	}

}
