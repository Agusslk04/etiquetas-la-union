package ar.com.launion.etiquetas.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ar.com.launion.etiquetas.util.Precio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "producto")
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_producto")
	private Long idProducto;

	@Column(name = "nombre", length = 80, nullable = false)
	private String nombre;

	@Column(name = "precio", precision = 12, scale = 2, nullable = false)
	private BigDecimal precio;

	@Column(name = "codigo_barras", length = 40, nullable = false, unique = true)
	private String codigoBarras;

	// ===== Hoja A4 (cola de impresión) =====
	@Column(name = "en_hoja", nullable = false)
	private boolean enHoja = false;

	@Column(name = "copias", nullable = false)
	private int copias = 1;

	@Column(name = "fecha_en_hoja")
	private LocalDateTime fechaEnHoja;

	// ===== Auditoría =====
	@Column(name = "fecha_alta", nullable = false, updatable = false)
	private LocalDateTime fechaAlta;

	@Column(name = "fecha_modificacion", nullable = false)
	private LocalDateTime fechaModificacion;

	public Producto() {
		super();
	}

	@PrePersist
	private void alCrear() {
		LocalDateTime ahora = LocalDateTime.now();
		if (fechaAlta == null) {
			fechaAlta = ahora;
		}
		if (fechaModificacion == null) {
			fechaModificacion = ahora;
		}
	}

	// ===== Precio formateado para las vistas =====

	public String getPrecioEntero() {
		return Precio.entero(precio);
	}

	public String getPrecioCentavos() {
		return Precio.centavos(precio);
	}

	public String getPrecioEditable() {
		return Precio.editable(precio);
	}

	public String getPrecioTexto() {
		return Precio.texto(precio);
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

	public BigDecimal getPrecio() {
		return precio;
	}

	public void setPrecio(BigDecimal precio) {
		this.precio = precio;
	}

	public String getCodigoBarras() {
		return codigoBarras;
	}

	public void setCodigoBarras(String codigoBarras) {
		this.codigoBarras = codigoBarras;
	}

	public boolean isEnHoja() {
		return enHoja;
	}

	public void setEnHoja(boolean enHoja) {
		this.enHoja = enHoja;
	}

	public int getCopias() {
		return copias;
	}

	public void setCopias(int copias) {
		this.copias = copias;
	}

	public LocalDateTime getFechaEnHoja() {
		return fechaEnHoja;
	}

	public void setFechaEnHoja(LocalDateTime fechaEnHoja) {
		this.fechaEnHoja = fechaEnHoja;
	}

	public LocalDateTime getFechaAlta() {
		return fechaAlta;
	}

	public void setFechaAlta(LocalDateTime fechaAlta) {
		this.fechaAlta = fechaAlta;
	}

	public LocalDateTime getFechaModificacion() {
		return fechaModificacion;
	}

	public void setFechaModificacion(LocalDateTime fechaModificacion) {
		this.fechaModificacion = fechaModificacion;
	}

}
