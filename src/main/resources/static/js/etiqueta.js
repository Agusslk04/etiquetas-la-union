/* =========================================================
   Etiquetas: ajusta el tamaño del texto para que siempre entre,
   escala las vistas previas y formatea precios (es-AR).
   ========================================================= */
(function () {
	'use strict';

	// ---------- Ajuste de texto ----------

	function entra(caja, interno) {
		var estilo = getComputedStyle(caja);
		var ancho = caja.clientWidth - parseFloat(estilo.paddingLeft) - parseFloat(estilo.paddingRight);
		var alto = caja.clientHeight - parseFloat(estilo.paddingTop) - parseFloat(estilo.paddingBottom);
		return interno.scrollWidth <= ancho + 0.5 && interno.offsetHeight <= alto + 0.5;
	}

	// Busca el tamaño de letra más grande (en mm) que entra en la caja
	function ajustar(caja, interno, maxMm, minMm) {
		if (!caja || !interno) return;
		interno.style.fontSize = maxMm + 'mm';
		if (entra(caja, interno)) return;
		var bajo = minMm, alto = maxMm;
		for (var i = 0; i < 12; i++) {
			var medio = (bajo + alto) / 2;
			interno.style.fontSize = medio + 'mm';
			if (entra(caja, interno)) bajo = medio; else alto = medio;
		}
		interno.style.fontSize = bajo + 'mm';
	}

	// Tamaños de letra máximo y mínimo (mm) de cada formato (data-formato de la etiqueta)
	var MEDIDAS = {
		grande: { nombre: [7.4, 3.2], precio: [26, 8], codigo: [2.5, 1.4] },
		mini: { nombre: [4.6, 2.2], precio: [17, 6], codigo: [1.95, 1.1] },
		chica: { nombre: [3.15, 1.5], precio: [10.2, 3.75], codigo: [1.35, 1] },
		// Carteles: la media A4 es el A4 achicado (x 0,707)
		'oferta-a4': { nombre: [13, 5], precio: [86, 20] },
		'media-oferta': { nombre: [9.2, 3.5], precio: [61, 14] },
		media: { nombre: [15, 4], precio: [50, 12] }
	};

	function ajustarEtiqueta(et) {
		var m = MEDIDAS[et.dataset.formato] || MEDIDAS.grande;
		var nombre = et.querySelector('.et-nombre');
		ajustar(nombre, nombre && nombre.firstElementChild, m.nombre[0], m.nombre[1]);
		var precio = et.querySelector('.et-precio');
		ajustar(precio, precio && precio.firstElementChild, m.precio[0], m.precio[1]);
		// Los carteles no tienen código de barras
		var codigo = et.querySelector('.et-codigo');
		if (codigo && m.codigo) ajustar(codigo, codigo.querySelector('span'), m.codigo[0], m.codigo[1]);
	}

	function ajustarTodas() {
		document.querySelectorAll('.etiqueta').forEach(ajustarEtiqueta);
	}

	// ---------- Escala de la vista previa (sin deformar) ----------

	// data-escala-mm="130": las etiquetas del marco usan la misma escala (la de 13 cm),
	// así la de 9 x 5 y la de 6,5 x 3 se ven más chicas, como salen impresas.
	// Los carteles (más anchos que 13 cm) se achican hasta ocupar el ancho del marco.
	// Se muestra la que no está oculta.
	var MM = 96 / 25.4;

	function escalar() {
		document.querySelectorAll('[data-escala]').forEach(function (marco) {
			var hijo = Array.prototype.find.call(marco.querySelectorAll('.etiqueta'), function (e) { return e.offsetWidth > 0; });
			if (!hijo || !hijo.offsetWidth) return;
			var referencia = Math.max(Number(marco.dataset.escalaMm) * MM || 0, hijo.offsetWidth);
			var s = marco.clientWidth / referencia;
			marco.style.setProperty('--escala', s);
			marco.style.height = (hijo.offsetHeight * s) + 'px';
			hijo.style.left = ((marco.clientWidth - hijo.offsetWidth * s) / 2) + 'px';
		});
	}

	// ---------- Precios ----------

	// Acepta 1299 · 1.299 · 1299,5 · 1.299,50 · $ 1.299 · 1299.50
	function parsearPrecio(texto) {
		var t = String(texto || '').replace(/[$\s ]/g, '');
		if (!t || !/^[0-9.,]+$/.test(t)) return null;
		var coma = t.lastIndexOf(','), punto = t.lastIndexOf('.');
		var normal;
		if (coma >= 0 && punto >= 0) {
			var decimal = coma > punto ? ',' : '.';
			var miles = decimal === ',' ? '.' : ',';
			normal = t.split(miles).join('').replace(decimal, '.');
		} else if (coma >= 0) {
			normal = t.indexOf(',') !== coma ? t.split(',').join('') : t.replace(',', '.');
		} else if (punto >= 0) {
			var sonMiles = t.indexOf('.') !== punto || t.length - punto - 1 === 3;
			normal = sonMiles ? t.split('.').join('') : t;
		} else {
			normal = t;
		}
		var n = Number(normal);
		return isFinite(n) ? Math.round(n * 100) / 100 : null;
	}

	function partesPrecio(n) {
		var centavosTotales = Math.round((n || 0) * 100);
		var entero = Math.floor(centavosTotales / 100);
		var cent = centavosTotales % 100;
		return {
			entero: entero.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.'),
			centavos: cent ? (cent < 10 ? '0' + cent : String(cent)) : ''
		};
	}

	function precioEditable(n) {
		var p = partesPrecio(n);
		return p.centavos ? p.entero + ',' + p.centavos : p.entero;
	}

	// ---------- Arranque ----------

	function listo() {
		ajustarTodas();
		escalar();
	}

	if (document.fonts && document.fonts.ready) {
		document.fonts.ready.then(listo);
	} else {
		window.addEventListener('load', listo);
	}
	window.addEventListener('resize', escalar);

	window.Etiquetas = {
		ajustarEtiqueta: ajustarEtiqueta,
		ajustarTodas: ajustarTodas,
		escalar: escalar,
		parsearPrecio: parsearPrecio,
		partesPrecio: partesPrecio,
		precioEditable: precioEditable
	};
})();
