# Conceptos y preguntas de la defensa

## Executable, proceso, servicio e hilo

| Concepto | Qué es | Ejemplo en esta práctica |
|---|---|---|
| Ejecutable | Fichero en disco con código que se puede ejecutar. No está en marcha. | `/usr/bin/java`, `target/supervisor.jar` |
| Proceso | Programa en ejecución. Tiene su propia memoria y un PID asignado por el sistema operativo. | Cada `java` que lanza el supervisor |
| Hilo | Línea de ejecución dentro de un proceso. Los hilos de un mismo proceso comparten memoria y PID. | `main`, los hilos del pool, los lectores de stdout y stderr |
| Servicio | Proceso gestionado por el sistema (por ejemplo, por systemd), no por un usuario desde la terminal. | Ampliación opcional `practica01.service` |

## PID, PPID e identificador de hilo

- **PID** (*Process ID*): número que el sistema operativo asigna a cada proceso
  en marcha. Es único mientras el proceso vive; cuando muere, el número se puede
  reutilizar.
- **PPID** (*Parent PID*): PID del proceso que ha creado este proceso.
- **`$$`** en la shell contiene el PID de la propia shell.

Experimento (paso 1):

    $ java -jar target/supervisor.jar
    [SUPERVISOR pid=25936 ppid=17731 thread=main] hola

El PPID (17731) es el PID de la terminal (`zsh`), porque es la terminal la
que crea el proceso `java`:

    zsh (pid 17731)
     └── java (pid 25936, ppid 17731)

Al ejecutarlo varias veces **cambia el PID** (cada ejecución es un proceso
nuevo) pero **no el PPID** (el padre es siempre la misma terminal, que sigue
abierta).

`main` es un **hilo**, no un proceso: es el hilo con el que la JVM empieza a
ejecutar el método `main`. Todos los hilos de la JVM comparten el PID del
proceso `java`; cada hilo tiene su propio identificador de hilo (TID, columna
`SPID` de `ps -T`).

## Preguntas de la defensa

1. **¿Qué diferencia hay entre PID, PPID e identificador de hilo?**
   El PID identifica un proceso; el PPID identifica a su padre; el
   identificador de hilo identifica cada hilo dentro de un proceso. Varios
   hilos comparten el mismo PID, pero cada uno tiene su propio TID.
2. **¿Por qué `ProcessBuilder` tiene que recibir una lista de argumentos?**
   *Pendiente (paso de `ProcessBuilder`).*
3. **¿Por qué hay que drenar stdout y stderr concurrentemente?**
   *Pendiente (paso de captura de salidas).*
4. **¿Qué aporta cerrar stdin después de escribir?**
   *Pendiente (paso de stdin).*
5. **¿Qué diferencia hay entre finalización ordenada y forzada?**
   *Pendiente (paso de timeout).*
6. **¿En qué se parece esta solución a Fork-Join y en qué se diferencia?**
   *Pendiente (paso de workers en paralelo).*
