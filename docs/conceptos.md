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

## Llamar a un método no crea un proceso

Un proceso no se "declara" en el código: nace cuando se pide al sistema
operativo que **ejecute un programa**. Lo puede pedir la shell (cuando el
usuario escribe una orden) o un programa Java con `ProcessBuilder.start()`.

| Acción | ¿Proceso nuevo? | PID |
|---|---|---|
| El supervisor llama `Worker.run(...)` directamente | No: el código del worker se ejecuta dentro del proceso del supervisor | El mismo que el supervisor |
| El supervisor ejecuta `new ProcessBuilder(... "worker" ...).start()` | Sí (**fork**) | Nuevo, con PPID = PID del supervisor |

Por eso `Worker.java` no contiene nada que lo haga "ser un proceso": es el
mismo código, y lo que decide si es un proceso aparte es **cómo se ejecuta**.

## Por qué los workers usan `Thread.sleep`

El worker simula un trabajo real (comprimir, convertir…) que tarda un rato.
`Thread.sleep(1000)` detiene el hilo un segundo en cada paso. Sin esta pausa,
el worker acabaría en milisegundos y no se podría observar con `ps`, superar
un timeout ni comprobar que solo se ejecutan dos workers a la vez.

## Códigos de retorno y señales

Cuando un proceso termina, devuelve a su padre un **código de retorno**
(*exit code*), un número entre 0 y 255. En la shell se consulta con `echo $?`
justo después de la orden y **en la misma terminal**.

| Código | Significado |
|---|---|
| 0 | Éxito |
| 1–125 | Error definido por el propio programa (por ejemplo, el worker que acaba con código 7 de la actividad B) |
| 126 | El fichero existe, pero no es ejecutable |
| 127 | Orden no encontrada |
| 128 + N | El proceso ha terminado por la **señal N** |

Una **señal** es un aviso que el sistema operativo envía a un proceso. Las
más importantes para esta práctica (`kill -l` las lista todas):

| Señal | Número | Quién la envía | ¿El proceso puede reaccionar? | Código de retorno |
|---|---|---|---|---|
| `SIGINT` | 2 | Ctrl+C en la terminal | Sí (puede hacer limpieza) | 130 |
| `SIGTERM` | 15 | `kill PID`, systemd, `Process.destroy()` en Java | Sí → **finalización ordenada** | 143 |
| `SIGKILL` | 9 | `kill -9 PID`, `Process.destroyForcibly()` en Java | No, el kernel lo mata de inmediato → **finalización forzada** | 137 |

Experimento (paso 2): al parar el supervisor con Ctrl+C, `echo $?` devuelve
**130 = 128 + 2 (SIGINT)**.

## stdin, stdout y stderr

Todo proceso nace con tres flujos abiertos, identificados por un número
(*descriptor de fichero*):

| Descriptor | Nombre | En Java | Uso |
|---|---|---|---|
| 0 | stdin (entrada estándar) | `System.in` | Datos que recibe el proceso |
| 1 | stdout (salida estándar) | `System.out` | Resultados |
| 2 | stderr (salida de errores) | `System.err` | Errores y diagnósticos |

En la terminal los dos de salida se ven mezclados, pero son **independientes**.
Con redirecciones de la shell se pueden separar: `2>/dev/null` descarta
stderr, `>fichero` guarda stdout en un fichero, `<fichero` lee stdin de un
fichero. Cuando un proceso lo crea otro programa (como el supervisor con
`ProcessBuilder`), es el padre quien recibe estos flujos y tiene que leerlos
o escribirlos.

## Por qué `System.exit(codigo)`

Si el método `main` termina normalmente, la JVM acaba siempre con código 0
(o con 1 si se escapa una excepción). Para devolver un código concreto al
padre, como el 7 de la actividad B, hay que llamar a `System.exit(codigo)`.

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
