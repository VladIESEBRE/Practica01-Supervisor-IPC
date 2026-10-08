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

## Tests

    mvn test

## Documentación

| Documento | Contenido |
|---|---|
| [docs/conceptos.md](docs/conceptos.md) | Conceptos teóricos y respuestas a las preguntas de la defensa |
| [docs/inventario.md](docs/inventario.md) | Observación de procesos con `ps` y `pstree`, mapa de procesos |
| [docs/Incidencias.md](docs/Incidencias.md) | Registro de incidencias |
| [docs/uso-ia.md](docs/uso-ia.md) | Declaración de autoría y de uso de IA |

## Estado

- [x] Paso 1 · Repositorio y estructura
- [x] Paso 2 · Inventario de procesos
- [ ] Roles y log con PID/PPID/hilo
- [ ] Lanzar procesos con `ProcessBuilder`
- [ ] Captura concurrente de stdout y stderr
- [ ] Envío de datos por stdin
- [ ] Resultado inmutable
- [ ] Timeout, cancelación y shutdown hook
- [ ] Pipe productor → supervisor → consumidor
- [ ] Tres workers con un máximo de dos simultáneos
- [ ] Tests
- [ ] Informe final y actividades A–F
