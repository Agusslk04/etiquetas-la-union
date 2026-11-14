package ar.com.launion.etiquetas.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Una subida de Excel con la opción "Guardar productos". Guarda qué productos
 * nuevos trajo el archivo (en el mismo orden) para volver a imprimir sus etiquetas.
 */
@Entity
@Table(name = "carga")
public class Carga {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_carga")
	private Long idCarga;

	@Column(name = "nombre_archivo", length = 200, nullable = false)
	private String nombreArchivo;

	@Column(name = "fecha", nullable = false)
	private LocalDateTime fecha;

	@Column(name = "cantidad", nullable = false)
	private int cantidad;

	// Productos nuevos que se guardaron
	@Column(name = "nuevos", nullable = false)
	private int nuevos;

	// Productos del Excel que ya estaban cargados (no se tocaron)
	@Column(name = "sin_cambios", nullable = false)
	private int yaCargados;

	// Códigos que aparecían más de una vez en el mismo archivo
	@Column(name = "repetidos", columnDefinition = "integer not null default 0")
	private int repetidos;

	@Column(name = "con_error", nullable = false)
	private int conError;

	// Solo lo usaban las primeras cargas, cuando el Excel actualizaba precios
	@Column(name = "actualizados", nullable = false)
	private int actualizados;

	@ManyToMany
	@JoinTable(name = "carga_producto",
			joinColumns = @JoinColumn(name = "id_carga"),
			inverseJoinColumns = @JoinColumn(name = "id_producto"))
	@OrderColumn(name = "orden")
	private List<Producto> productos = new ArrayList<>();

	public Carga() {
		super();
	}

	/** Productos de la carga (sin huecos, por si alguno se borró). */
	public List<Producto> getProductosVigentes() {
		return productos.stream().filter(Objects::nonNull).toList();
	}

	/** Los que ya estaban cargados (incluye los "actualizados" de las cargas viejas). */
	public int getYaEstaban() {
		return yaCargados + actualizados;
	}

	// ===== Getters y setters =====

	public Long getIdCarga() {
		return idCarga;
	}

	public void setIdCarga(Long idCarga) {
		this.idCarga = idCarga;
	}

	public String getNombreArchivo() {
		return nombreArchivo;
	}

	public void setNombreArchivo(String nombreArchivo) {
		this.nombreArchivo = nombreArchivo;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public int getCantidad() {
		return cantidad;
	}

	public void setCantidad(int cantidad) {
		this.cantidad = cantidad;
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

	public int getActualizados() {
		return actualizados;
	}

	public void setActualizados(int actualizados) {
		this.actualizados = actualizados;
	}

	public List<Producto> getProductos() {
		return productos;
	}

	public void setProductos(List<Producto> productos) {
		this.productos = productos;
	}

}
