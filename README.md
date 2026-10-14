# Práctica 01 · Supervisor de procesos e IPC

MP 0490 · Programación de servicios y procesos · AEA1 · RA1

Aplicación Java que actúa como **supervisor**: crea procesos externos reales
(cada uno con su propio PID), controla su entrada y salida, gestiona timeouts
y conecta procesos mediante una pipe implementada por el propio programa.

Roles lógicos: `SUPERVISOR`, `WORKER`, `PRODUCER` y `CONSUMER`. Todos los
mensajes de diagnóstico siguen este formato:

    [ROL pid=<PID> ppid=<PPID> thread=<hilo>] mensaje

## Requisitos

- JDK 21
- Maven 3 (probado con 3.8.7)
- Linux (las órdenes de observación `ps` y `pstree` son de Linux; el programa
  en sí no depende de ellas)
- No necesita conexión a Internet para ejecutarse ni para pasar los tests
  (solo la primera compilación descarga las dependencias de Maven)

## Estructura

    supervisor/
    ├── pom.xml                      configuración de Maven (Java 21, JUnit, jar ejecutable)
    ├── src/main/java/practica01/    código fuente
    ├── src/test/java/practica01/    tests automáticos (JUnit 5)
    └── docs/                        documentación e informe

## Compilar

    mvn package

Genera `target/supervisor.jar`.

## Ejecutar

    java -jar target/supervisor.jar

Para observar el proceso con `ps ... | grep ProcessForkJoinDemo` hay que
lanzarlo indicando la clase, porque con `-jar` el nombre de la clase no
aparece en la línea de órdenes:

    java -cp target/supervisor.jar practica01.ProcessForkJoinDemo

### Roles

El mismo programa hace un rol u otro según el primer argumento:

| Argumentos | Rol | Qué hace |
|---|---|---|
| *(ninguno)* | SUPERVISOR | Coordina los procesos hijos |
| `worker <NOMBRE> <pasos> <código>` | WORKER | Hace `<pasos>` pasos de 1 s y termina con `<código>` |
| `producer` | PRODUCER | Escribe cinco nombres por stdout |
| `consumer` | CONSUMER | Lee stdin hasta EOF y escribe `COUNT=<líneas> CHARS=<caracteres>` |

Ejemplo:

    java -cp target/supervisor.jar practica01.ProcessForkJoinDemo worker BETA 3 0

### Observar el árbol de procesos

En otra terminal, **antes** de lanzar el supervisor:

    ./scripts/observar-arbol.sh

Espera a que arranquen los workers y muestra el árbol de procesos con
`pstree` y el estado con `ps` (solo Linux; la aplicación no lo necesita).

## Tests

    mvn test

9 tests JUnit (éxito, orden inexistente, stderr, timeout, stdin, pipe,
cancelación y pool), sin Internet. Detalle en
[docs/pruebas-manuales.md](docs/pruebas-manuales.md#paso-11--tests-automáticos-junit).

## Como servicio systemd (ampliación)

Unidad de usuario `Type=oneshot` en `systemd/practica01.service`. Instalación,
órdenes y resultados en [docs/systemd.md](docs/systemd.md).

## Documentación

Informe completo (resumen, decisiones, evidencias con capturas, mapa de
procesos, actividades A–F, criterios, preguntas de la defensa, incidencias y
uso de IA): **[Informe_Practica01.docx](Informe_Practica01.docx)**.

Detalle de cada paso:

| Documento | Contenido |
|---|---|
| [docs/diseno.md](docs/diseno.md) | Diseño, flujo y decisiones |
| [docs/conceptos.md](docs/conceptos.md) | Conceptos teóricos y respuestas a las preguntas de la defensa |
| [docs/pruebas-manuales.md](docs/pruebas-manuales.md) | Pruebas manuales de cada paso y tests automáticos |
| [docs/systemd.md](docs/systemd.md) | Ampliación: el supervisor como servicio systemd |
| [docs/inventario.md](docs/inventario.md) | Observación de procesos con `ps` y `pstree`, mapa de procesos |
| [docs/Incidencias.md](docs/Incidencias.md) | Registro de incidencias |
| [docs/uso-ia.md](docs/uso-ia.md) | Declaración de autoría y de uso de IA |

## Estado

- [x] Paso 1 · Repositorio y estructura
- [x] Paso 2 · Inventario de procesos
- [x] Paso 3 · Log con PID/PPID/hilo y rol WORKER
- [x] Paso 4 · El supervisor lanza un worker con `ProcessBuilder`
- [x] Paso 5 · Captura concurrente de stdout y stderr
- [x] Paso 6a · Rol CONSUMER (stdin y EOF)
- [x] Paso 6b · Rol PRODUCER
- [x] Paso 6c · El supervisor envía datos por stdin y cierra el flujo
- [x] Paso 7 · Resultado inmutable
- [x] Paso 8a · Timeout con finalización ordenada y forzada
- [x] Paso 8b · Cancelación con shutdown hook (sin procesos huérfanos)
- [x] Paso 9 · Pipe productor → supervisor → consumidor
- [x] Paso 10 · Tres workers con un máximo de dos simultáneos
- [x] Paso 11 · Tests automáticos (9 tests JUnit)
- [x] Ampliación · Servicio systemd (`Type=oneshot`)
- [x] Informe final y actividades A–F
