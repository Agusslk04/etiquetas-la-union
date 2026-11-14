# Etiquetas La Unión

Sistema web (Spring Boot MVC + Thymeleaf + PostgreSQL) para imprimir carteles de precio de **13 × 8 cm** a color.

## Cómo se usa

| Pantalla | Para qué |
|---|---|
| **Nueva etiqueta** | Cargás nombre, precio y código de barras, y ves la etiqueta en vivo. *Imprimir etiqueta* o *Agregar a hoja A4*. Si escaneás un código ya guardado, se completan el nombre y el precio solos. |
| **Carga Excel** | Subís un Excel (nombre, precio, código) y elegís: **Solo imprimir** (imprime lo que trae el archivo, no guarda nada) o **Guardar productos** (guarda los nuevos; los que ya están cargados no se repiten). Cada carga guardada queda en el historial para reimprimirla. Hay una plantilla para descargar. |
| **Productos** | Todo lo guardado, de a 15 por página. Buscar, volver a imprimir, editar, borrar, o marcar varios y mandarlos a la hoja. |
| **Hoja A4** | Los productos para imprimir juntos (4 por hoja A4 horizontal), de a 15 por página. Editás precios y copias (se guardan al pasar de página) o cambiás un % todos los precios de la hoja con redondeo. |

La etiqueta lleva la **fecha del día** en que se imprime.

## Corriendo como sistema

Está instalado como tarea de Windows **"Etiquetas La Union"**:

- Arranca solo cuando el usuario entra a Windows, sin ventanas.
- Cada 5 minutos revisa que siga vivo y, si se cerró, lo vuelve a abrir.
- Funciona aunque la notebook esté con batería. Tiene que estar prendida y sin suspender.

| Archivo | Para qué |
|---|---|
| `Instalar como sistema.bat` | Lo instala o lo **actualiza** después de cambiar el código (recompila y lo reinicia). *Como administrador* arranca al prender la compu, aunque nadie inicie sesión. |
| `Iniciar Etiquetas.bat` | Lo prende (si estaba detenido) y abre el navegador. |
| `Detener Etiquetas.bat` | Lo apaga y lo deja en pausa hasta que se use *Iniciar*. |
| `Quitar del sistema.bat` | Lo apaga y deja de arrancar solo. Los datos quedan. |

- El programa instalado vive en `sistema\` y el registro está en `sistema\logs\etiquetas.log`.
- PostgreSQL tiene que estar iniciado (servicio `postgresql-x64-18`, arranca con Windows).
- La base `etiquetas_launion` se crea sola. Usuario y clave: `src/main/resources/application.properties`.

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

- Usá Chrome o Edge. El tamaño (A4 horizontal, sin márgenes) ya viene configurado.
- Si sale corrido: *Escala → Predeterminada* y *Márgenes → Ninguno*.
- Cortá siguiendo las marcas grises de las esquinas.

## Estructura (MVC)

```
entity/                    Producto, Carga (cada Excel subido y sus productos)
repository/                ProductoRepository, CargaRepository
service/ (+ implement/)    ProductoService, CargaService, CodigoBarrasService (EAN-13 / Code 128 en SVG)
util/                      Precio, LectorExcel, PlantillaExcel (Apache POI)
controller/                EtiquetaController, CargaController, ProductoController, HojaController,
                           ImpresionController, CodigoBarrasController
templates/                 vistas Thymeleaf (fragments/etiqueta.html = la etiqueta)
static/css/etiqueta.css    diseño de la etiqueta en milímetros
```

Tests: `mvnw test` (usan una base en memoria, no tocan PostgreSQL).
