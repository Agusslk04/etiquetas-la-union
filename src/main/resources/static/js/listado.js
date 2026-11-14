/* Productos: marcar todos y habilitar "Agregar marcados a la hoja" */
(function () {
	'use strict';

	var form = document.getElementById('formLista');
	if (!form) return;

	var todos = document.getElementById('marcarTodos');
	var boton = document.getElementById('agregarMarcados');
	var casillas = Array.prototype.slice.call(form.querySelectorAll('input[name="ids"]'));

	function actualizar() {
		var marcadas = casillas.filter(function (c) { return c.checked; }).length;
		boton.disabled = marcadas === 0;
		todos.checked = marcadas > 0 && marcadas === casillas.length;
		todos.indeterminate = marcadas > 0 && marcadas < casillas.length;
	}

	todos.addEventListener('change', function () {
		casillas.forEach(function (c) { c.checked = todos.checked; });
		actualizar();
	});
	casillas.forEach(function (c) { c.addEventListener('change', actualizar); });

	// Enter en el buscador no debe enviar este formulario
	form.addEventListener('keydown', function (e) {
		if (e.key === 'Enter' && e.target.tagName === 'INPUT') e.preventDefault();
	});

	actualizar();
})();
