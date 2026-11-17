# Etiquetas La Unión

Sistema web (Spring Boot MVC + Thymeleaf + PostgreSQL) para imprimir etiquetas y carteles de precio a color:

| Tamaño | Diseño | Hoja A4 | Por hoja |
|---|---|---|---|
| **Etiqueta 13 × 8 cm** | naranja, con código de barras y fecha | horizontal | 4 (2 × 2) |
| **Etiqueta 9 × 5 cm** | naranja, con código de barras y fecha | vertical | 10 (2 × 5) |
| **Etiqueta 6,5 × 3 cm** | naranja, con código de barras y fecha | vertical | 27 (3 × 9) |
| **Cartel A4 oferta** | azul, título *Ofertas de fin de semana* | horizontal | 1 (hoja entera) |
| **Cartel media A4 oferta** | azul, título *Ofertas de fin de semana* | vertical | 2 (uno arriba del otro) |
| **Cartel media A4** | azul, sin título (precios normales) | vertical | 2 (uno arriba del otro) |

Los carteles usan el fondo de los Word de La Unión (`static/img/cartel-oferta.jpg` y `cartel-precio.jpg`): el nombre va en el recuadro blanco (o arriba, en el que no tiene título) y el precio en el amarillo. Llevan la fecha chiquita abajo. Si se imprime un solo cartel de media A4, sale en la mitad de arriba de la hoja.

El tamaño se elige con los botones **Etiquetas / Carteles** en *Nueva etiqueta*, *Hoja A4*, *Carga Excel* y en la barra de la vista de impresión. Cada compu recuerda el último que usó.

## Cómo se usa

| Pantalla | Para qué |
|---|---|
| **Nueva etiqueta** | Cargás nombre, precio y código de barras, y ves la etiqueta en vivo. *Imprimir etiqueta* o *Agregar a hoja A4*. Si escaneás un código ya guardado, se completan el nombre y el precio solos. |
| **Carga Excel** | Subís un Excel (nombre, precio, código) y elegís: **Solo imprimir** (imprime lo que trae el archivo, no guarda nada) o **Guardar productos** (guarda los nuevos; los que ya están cargados no se repiten). Cada carga guardada queda en el historial para reimprimirla. Hay una plantilla para descargar. |
| **Productos** | Todo lo guardado, de a 15 por página. Buscar, volver a imprimir, editar, borrar, o marcar varios y mandarlos a la hoja. |
| **Hoja A4** | Los productos para imprimir juntos, de a 15 por página. Editás precios y copias (se guardan al pasar de página) o cambiás un % todos los precios de la hoja con redondeo. Cada tamaño muestra cuántas hojas salen. |

La etiqueta lleva la **fecha del día** en que se imprime.

## Corriendo como sistema

Está instalado como tarea de Windows **"Etiquetas La Union"**:

- Arranca solo cuando el usuario entra a Windows, sin ventanas.
- Cada 5 minutos revisa que siga vivo y, si se cerró, lo vuelve a abrir.
- Funciona aunque la notebook esté con batería. Tiene que estar prendida y sin suspender.

| Archivo | Para qué |
|---|---|
| `Actualizar.bat` | Baja la última versión de GitHub y la deja funcionando (ver abajo). |
| `Instalar como sistema.bat` | Lo instala o lo **actualiza** después de cambiar el código (recompila y lo reinicia). *Como administrador* arranca al prender la compu, aunque nadie inicie sesión. |
| `Iniciar Etiquetas.bat` | Lo prende (si estaba detenido) y abre el navegador. |
| `Detener Etiquetas.bat` | Lo apaga y lo deja en pausa hasta que se use *Iniciar*. |
| `Quitar del sistema.bat` | Lo apaga y deja de arrancar solo. Los datos quedan. |

- El programa instalado vive en `sistema\` y el registro está en `sistema\logs\etiquetas.log`.
- PostgreSQL tiene que estar iniciado (servicio `postgresql-x64-18`, arranca con Windows).
- La base `etiquetas_launion` se crea sola. Usuario y clave: `src/main/resources/application.properties`.

## Actualizar (en la compu del supermercado)

`Actualizar.bat` se puede llevar en un pendrive: copialo a la carpeta del sistema (donde está `Iniciar Etiquetas.bat`) y hacé doble clic, o ejecutalo directo desde el pendrive (busca la carpeta solo).

1. Pide permiso de administrador (aceptá el cartel de Windows).
2. Baja la versión nueva de GitHub. Si no hay internet, avisa y no cambia nada.
3. Respalda la base en `respaldos\etiquetas_launion-antes-de-actualizar-FECHA.dump` (guarda los últimos 10).
4. Reemplaza el código, recompila y reinicia el sistema. Los productos, la hoja y las cargas de Excel no se tocan.

- Si en esa compu alguien cambió archivos a mano, los guarda en `respaldos\cambios-a-mano-FECHA.patch` y pregunta antes de reemplazarlos.
- Si la carpeta fue copiada a mano (sin Git), la conecta a GitHub sola.
- Si el sistema no está instalado en esa compu, ofrece instalarlo en `C:\EtiquetasLaUnion`.
- Después de actualizar, en las otras compus alcanza con recargar la página (Ctrl + F5 si algo se ve raro).

## Usarlo desde otras compus de la red

Cualquier compu de la misma red entra con `http://IP-DE-ESTA-COMPU:8081`. La dirección exacta aparece abajo de todo en el panel.

La primera vez hay que abrir el puerto en el Firewall de esta compu: clic derecho en **`Permitir acceso en red.bat`** → *Ejecutar como administrador*.

- El permiso es solo para equipos de la **misma red**.
- No pide usuario ni contraseña: lo usa cualquiera conectado a ese WiFi.
- La hoja A4 es la misma para todos.

## El Excel

- Columnas: **Nombre del producto · Precio · Código de barras** (en cualquier orden si tienen títulos; sin títulos se toman A, B y C).
- Los títulos se reconocen aunque digan *Descripción*, *Producto*, *PVP*, *EAN*, *Código*, etc.
- Precios como número de Excel o como texto (`1.299,50`). Los códigos largos que Excel muestra como `7,79E+12` se leen completos.
- Las filas con datos faltantes se informan (*Fila 7: falta el precio*) y no frenan al resto. Hasta 3.000 productos por archivo.
- Al guardar, el código de barras identifica al producto: si ya existe no se carga de nuevo ni se cambia su precio, y si el mismo código aparece dos veces en el archivo se toma solo la primera fila.

## Al imprimir

- Usá Chrome o Edge. La hoja (A4 horizontal o vertical, sin márgenes) ya viene configurada para cada tamaño.
- Si sale corrido: *Escala → Predeterminada* y *Márgenes → Ninguno*.
- Etiquetas: cortá siguiendo las marcas grises de las esquinas.
- Carteles: ocupan toda la hoja, como en el Word; la impresora puede dejar un borde blanco de unos milímetros. Los de media A4 se cortan por la mitad.

## Estructura (MVC)

```
entity/                    Producto, Carga (cada Excel subido y sus productos)
repository/                ProductoRepository, CargaRepository
service/ (+ implement/)    ProductoService, CargaService, CodigoBarrasService (EAN-13 / Code 128 en SVG)
util/                      Precio, LectorExcel, PlantillaExcel (Apache POI), FormatoEtiqueta (los 6 tamaños)
controller/                EtiquetaController, CargaController, ProductoController, HojaController,
                           ImpresionController, CodigoBarrasController
templates/                 vistas Thymeleaf (fragments/etiqueta.html = etiquetas y carteles,
                           fragments/formato.html = botones de tamaño)
static/css/etiqueta.css    diseño de etiquetas y carteles en milímetros
static/css/impresion.css   cómo se acomodan en la hoja A4
```

Tests: `mvnw test` (usan una base en memoria, no tocan PostgreSQL).
