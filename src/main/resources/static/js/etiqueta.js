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

	function ajustarEtiqueta(et) {
		var nombre = et.querySelector('.et-nombre');
		ajustar(nombre, nombre && nombre.firstElementChild, 8.6, 3.4);
		var precio = et.querySelector('.et-precio');
		ajustar(precio, precio && precio.firstElementChild, 19, 7);
		var codigo = et.querySelector('.et-codigo');
		ajustar(codigo, codigo && codigo.querySelector('span'), 2.5, 1.4);
	}

	function ajustarTodas() {
		document.querySelectorAll('.etiqueta').forEach(ajustarEtiqueta);
	}

	// ---------- Escala de la vista previa (sin deformar) ----------

	function escalar() {
		document.querySelectorAll('[data-escala]').forEach(function (marco) {
			var hijo = marco.firstElementChild;
			if (!hijo || !hijo.offsetWidth) return;
			var s = marco.clientWidth / hijo.offsetWidth;
			marco.style.setProperty('--escala', s);
			marco.style.height = (hijo.offsetHeight * s) + 'px';
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
