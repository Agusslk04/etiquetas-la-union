/* =========================================================
   Carga de Excel: arrastrar y soltar, mostrar el archivo elegido
   y evitar que se envíe dos veces. Dos botones: Solo imprimir / Guardar.
   ========================================================= */
(function () {
	'use strict';

	var zona = document.getElementById('zonaArchivo');
	if (!zona) return;

	var input = document.getElementById('archivo');
	var nombre = document.getElementById('nombreArchivo');
	var detalle = document.getElementById('detalleArchivo');
	var form = document.getElementById('formExcel');
	var botones = Array.prototype.slice.call(form.querySelectorAll('button[name="accion"]'));
	var textos = botones.map(function (b) { return b.querySelector('span').textContent; });
	var textoNombre = nombre.textContent;
	var textoDetalle = detalle.textContent;
	var enviando = false;

	function tamanio(bytes) {
		if (bytes < 1024 * 1024) return Math.max(1, Math.round(bytes / 1024)) + ' KB';
		return (bytes / 1024 / 1024).toFixed(1).replace('.', ',') + ' MB';
	}

	function mostrar() {
		var archivo = input.files && input.files[0];
		zona.classList.toggle('con-archivo', !!archivo);
		nombre.textContent = archivo ? archivo.name : textoNombre;
		detalle.textContent = archivo ? tamanio(archivo.size) + ' · clic para cambiarlo' : textoDetalle;
		botones.forEach(function (b) { b.disabled = !archivo; });
	}

	['dragenter', 'dragover'].forEach(function (evento) {
		zona.addEventListener(evento, function (e) {
			e.preventDefault();
			zona.classList.add('arrastrando');
		});
	});
	['dragleave', 'drop'].forEach(function (evento) {
		zona.addEventListener(evento, function (e) {
			e.preventDefault();
			zona.classList.remove('arrastrando');
		});
	});
	zona.addEventListener('drop', function (e) {
		if (e.dataTransfer && e.dataTransfer.files.length) {
			input.files = e.dataTransfer.files;
			mostrar();
		}
	});

	// Si el archivo se suelta fuera de la zona, que el navegador no lo abra
	['dragover', 'drop'].forEach(function (evento) {
		window.addEventListener(evento, function (e) { e.preventDefault(); });
	});

	input.addEventListener('change', mostrar);

	// No se deshabilitan los botones: el navegador tiene que mandar cuál se apretó
	form.addEventListener('submit', function (e) {
		if (enviando) {
			e.preventDefault();
			return;
		}
		enviando = true;
		form.classList.add('enviando');
		var boton = e.submitter;
		if (boton && boton.querySelector('span')) {
			boton.querySelector('span').textContent = boton.value === 'imprimir' ? 'Preparando etiquetas…' : 'Guardando productos…';
		}
	});

	// Al volver con "atrás", todo tiene que quedar como al principio
	window.addEventListener('pageshow', function () {
		enviando = false;
		form.classList.remove('enviando');
		botones.forEach(function (b, i) { b.querySelector('span').textContent = textos[i]; });
		mostrar();
	});
})();
