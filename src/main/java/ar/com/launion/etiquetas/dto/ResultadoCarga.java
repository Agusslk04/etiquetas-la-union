package ar.com.launion.etiquetas.dto;

import java.util.ArrayList;
import java.util.List;

import ar.com.launion.etiquetas.entity.Carga;

/**
 * Lo que pasó al guardar los productos de un Excel.
 * La carga es null si no había ningún producto nuevo para guardar.
 */
public class ResultadoCarga {

	private Carga carga;

	private int nuevos;
	private int yaCargados;
	private int repetidos;
	private int conError;

	// Mensajes para mostrar (con tope, para que una lista enorme no tape la pantalla)
	private final List<String> filasConError = new ArrayList<>();
	private final List<String> productosYaCargados = new ArrayList<>();
	private final List<String> codigosRepetidos = new ArrayList<>();

	public Carga getCarga() {
		return carga;
	}

	public void setCarga(Carga carga) {
		this.carga = carga;
	}

	public int getNuevos() {
		return nuevos;
	}

	public void setNuevos(int nuevos) {
		this.nuevos = nuevos;
	}

	public int getYaCargados() {
		return yaCargados;
	}

	public void setYaCargados(int yaCargados) {
		this.yaCargados = yaCargados;
	}

	public int getRepetidos() {
		return repetidos;
	}

	public void setRepetidos(int repetidos) {
		this.repetidos = repetidos;
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

	public List<String> getProductosYaCargados() {
		return productosYaCargados;
	}

	public List<String> getCodigosRepetidos() {
		return codigosRepetidos;
	}

}
