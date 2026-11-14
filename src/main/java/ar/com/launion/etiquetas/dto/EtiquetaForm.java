package ar.com.launion.etiquetas.dto;

import java.math.BigDecimal;

import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.util.Precio;

/**
 * Datos que carga el usuario en el panel. El precio llega como texto
 * para aceptar "1.299,50", "1299" o "$ 1.299".
 */
public class EtiquetaForm {

	private Long idProducto;
	private String nombre;
	private String precio;
	private String codigoBarras;

	public EtiquetaForm() {
		super();
	}

	public static EtiquetaForm desde(Producto p) {
		EtiquetaForm form = new EtiquetaForm();
		form.setIdProducto(p.getIdProducto());
		form.setNombre(p.getNombre());
		form.setPrecio(p.getPrecioEditable());
		form.setCodigoBarras(p.getCodigoBarras());
		return form;
	}

	// ===== Para la vista previa =====

	public String getPrecioEntero() {
		BigDecimal valor = Precio.parsear(precio);
		return Precio.entero(valor == null ? BigDecimal.ZERO : valor);
	}

	public String getPrecioCentavos() {
		return Precio.centavos(Precio.parsear(precio));
	}

	// ===== Getters y setters =====

	public Long getIdProducto() {
		return idProducto;
	}

	public void setIdProducto(Long idProducto) {
		this.idProducto = idProducto;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getPrecio() {
		return precio;
	}

	public void setPrecio(String precio) {
		this.precio = precio;
	}

	public String getCodigoBarras() {
		return codigoBarras;
	}

	public void setCodigoBarras(String codigoBarras) {
		this.codigoBarras = codigoBarras;
	}

}
