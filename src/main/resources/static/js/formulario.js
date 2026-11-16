/* =========================================================
   Panel de carga: vista previa en vivo, búsqueda por código
   y Enter para pasar de un campo al siguiente (sirve con lector).
   ========================================================= */
(function () {
	'use strict';

	var form = document.getElementById('formEtiqueta');
	if (!form) return;

	var nombre = document.getElementById('nombre');
	var precio = document.getElementById('precio');
	var codigo = document.getElementById('codigoBarras');
	var aviso = document.getElementById('avisoCodigo');
	var botonImprimir = document.getElementById('botonImprimir');
	var idActual = (form.querySelector('[name="idProducto"]') || {}).value || '';

	// Hay una vista previa por tamaño (etiquetas y carteles): se actualizan todas
	var etiquetas = Array.prototype.slice.call(document.querySelectorAll('.vista-previa .etiqueta'));
	var previas = Array.prototype.slice.call(document.querySelectorAll('.vista-previa .previa-formato'));
	var radiosFormato = Array.prototype.slice.call(document.querySelectorAll('input[name="formato"][form="formEtiqueta"]'));

	var CODIGO_VALIDO = /^[0-9A-Za-z.\-]{1,40}$/;

	// Los carteles no llevan código de barras: solo se toca lo que cada uno tiene
	function cada(selector, accion) {
		etiquetas.forEach(function (et) {
			var el = et.querySelector(selector);
			if (el) accion(el, et);
		});
	}

	function ajustar() {
		etiquetas.forEach(function (et) { if (et.offsetWidth) Etiquetas.ajustarEtiqueta(et); });
	}

	function pintarNombre() {
		var v = nombre.value.trim();
		cada('.et-nombre span', function (el) {
			el.textContent = v || 'Nombre del producto';
			el.classList.toggle('et-vacio', !v);
		});
		ajustar();
	}

	function pintarPrecio() {
		var n = Etiquetas.parsearPrecio(precio.value);
		var partes = Etiquetas.partesPrecio(n || 0);
		cada('.et-entero', function (el) { el.textContent = partes.entero; });
		cada('.et-centavos', function (el) {
			el.textContent = partes.centavos;
			el.classList.toggle('vacio', !partes.centavos);
		});
		ajustar();
	}

	var esperaCodigo;
	function pintarCodigo() {
		clearTimeout(esperaCodigo);
		esperaCodigo = setTimeout(function () {
			var v = codigo.value.trim();
			var valido = CODIGO_VALIDO.test(v);
			var src = form.dataset.urlCodigo + '?valor=' + encodeURIComponent(valido ? v : '0000000000000');
			cada('.et-codigo', function (caja) {
				caja.classList.toggle('et-vacio', !valido);
				var img = caja.querySelector('img');
				if (img.getAttribute('src') !== src) img.src = src;
				caja.querySelector('span').textContent = valido ? v : 'Código de barras';
			});
			ajustar();
		}, 150);
	}

	// Cambiar de tamaño: muestra la vista previa elegida
	function mostrarFormato() {
		var elegido = (radiosFormato.filter(function (r) { return r.checked; })[0] || {}).value || 'grande';
		previas.forEach(function (p) { p.hidden = p.dataset.formato !== elegido; });
		ajustar();
		Etiquetas.escalar();
	}
	radiosFormato.forEach(function (r) { r.addEventListener('change', mostrarFormato); });

	function mostrarAviso(texto, tipo) {
		if (!aviso) return;
		aviso.textContent = texto || '';
		aviso.className = 'ayuda' + (tipo ? ' ' + tipo : '');
	}

	// Si el código ya está guardado, trae el nombre y el precio
	var ultimoBuscado = null;
	function buscarCodigo() {
		var v = codigo.value.trim();
		if (!CODIGO_VALIDO.test(v) || v === ultimoBuscado) return Promise.resolve();
		ultimoBuscado = v;
		return fetch(form.dataset.urlBuscar + '?codigo=' + encodeURIComponent(v))
			.then(function (r) { return r.ok ? r.json() : { encontrado: false }; })
			.then(function (p) {
				if (!p.encontrado) {
					mostrarAviso('Producto nuevo: se guarda al imprimir.', '');
					return;
				}
				if (String(p.id) === String(idActual)) {
					mostrarAviso('', '');
					return;
				}
				if (idActual) {
					mostrarAviso('Ese código ya es de «' + p.nombre + '».', 'aviso');
					return;
				}
				if (!nombre.value.trim()) {
					nombre.value = p.nombre;
					precio.value = p.precio;
					pintarNombre();
					pintarPrecio();
					mostrarAviso('Ya estaba guardado: se cargaron sus datos. Podés cambiar el precio.', 'ok');
				} else {
					mostrarAviso('Ya está guardado como «' + p.nombre + '»: se va a actualizar.', 'aviso');
				}
			})
			.catch(function () { /* sin conexión: no pasa nada */ });
	}

	// Enter = siguiente campo. En el código, busca y lleva al primer campo vacío o al botón Imprimir
	var campos = [nombre, precio, codigo];
	campos.forEach(function (campo, i) {
		campo.addEventListener('keydown', function (e) {
			if (e.key !== 'Enter') return;
			e.preventDefault();
			if (campo !== codigo) {
				campos[i + 1].focus();
				campos[i + 1].select();
				return;
			}
			buscarCodigo().then(function () {
				var vacio = campos.filter(function (c) { return !c.value.trim(); })[0];
				(vacio || botonImprimir).focus();
			});
		});
	});

	nombre.addEventListener('input', pintarNombre);
	precio.addEventListener('input', pintarPrecio);
	codigo.addEventListener('input', function () {
		ultimoBuscado = null;
		mostrarAviso('', '');
		pintarCodigo();
	});
	codigo.addEventListener('change', buscarCodigo);

	// Al volver con "atrás" el navegador puede restaurar los valores: repintar
	window.addEventListener('pageshow', function () {
		mostrarFormato();
		pintarNombre();
		pintarPrecio();
		pintarCodigo();
	});
})();
