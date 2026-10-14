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

## Terminal y proceso no son lo mismo

La terminal (`/dev/pts/N`) es solo el **destino** al que apuntan los
descriptores 0, 1 y 2 de un proceso: un "fichero" especial que representa la
pantalla y el teclado. Muchos procesos pueden escribir en la misma terminal a
la vez (la `zsh` escribe el prompt, el supervisor sus mensajes, el worker los
suyos) y cada uno sigue teniendo su propio PID.

Con `ProcessBuilder.inheritIO()` el hijo **hereda** los descriptores del
padre: su stdout y su stderr apuntan a la misma terminal. Es como dos personas
escribiendo en la misma pizarra: comparten la pizarra, pero siguen siendo dos
personas. Se puede comprobar con:

    ls -l /proc/<PID>/fd

que muestra a dónde apunta cada descriptor (`0`, `1`, `2` → `/dev/pts/1`).

Sin `inheritIO()`, los descriptores del hijo se conectan a **tubos (pipes)**
que lee el supervisor, y el texto del hijo ya no aparece en pantalla
directamente. Es lo que se hará para capturar stdout y stderr.

## Por qué hay que leer stdout y stderr a la vez (actividad C)

Cada tubo tiene un búfer limitado (en Linux, unos 64 KB). Si el hijo escribe
y nadie lee, el búfer se llena y el hijo **se bloquea** en la escritura hasta
que alguien lo vacíe.

Si el supervisor leyera los tubos uno detrás de otro (o hiciera `waitFor()`
antes de leer):

1. El supervisor espera a que se acabe stdout (o a que el hijo termine).
2. El hijo escribe mucho por stderr, nadie lo lee, el búfer se llena y el hijo
   se queda parado.
3. El hijo no termina nunca, así que stdout no se acaba nunca.
4. **Interbloqueo**: cada uno espera al otro para siempre.

**Medida preventiva**: un hilo lector para cada tubo (`LectorFlujo`, hilos
`io-stdout` e `io-stderr`) que empieza a leer justo después de `start()`, de
modo que ningún búfer se llena. Después de `waitFor()` se hace `join()` de los
dos hilos para no perder las últimas líneas.

### Nombres de los flujos de `Process`

Java los nombra desde el punto de vista del supervisor:

| Método | Tubo del hijo | El supervisor… |
|---|---|---|
| `getInputStream()` | stdout | lo lee |
| `getErrorStream()` | stderr | lo lee |
| `getOutputStream()` | stdin | lo escribe |

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

## EOF: fin de la entrada

Un proceso que lee stdin no puede saber por sí mismo cuándo se han acabado
los datos: tiene que recibir un **EOF** (*End Of File*). El EOF llega cuando
**quien escribe cierra su extremo del tubo**:

| Quién escribe | Cómo se produce el EOF |
|---|---|
| El usuario con el teclado | Ctrl+D |
| Un fichero (`< fichero`) | Al llegar al final del fichero |
| Otro proceso por una pipe | Cuando ese proceso termina o cierra el tubo |
| El supervisor (`Process.getOutputStream()`) | Llamando a `close()` |

En Java, `BufferedReader.readLine()` devuelve `null` al recibir el EOF.

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
   Porque así cada argumento llega tal cual al programa, sin ninguna shell en
   medio que interprete caracteres especiales (`;`, `|`, `>`, espacios…). Si
   un argumento viene del exterior, no se puede convertir en una orden nueva
   (inyección de órdenes).
3. **¿Por qué hay que drenar stdout y stderr concurrentemente?**
   Porque cada tubo tiene un búfer limitado: si uno no se lee, se llena, el
   hijo se bloquea escribiendo y no termina nunca, y el supervisor, que
   espera el otro tubo o el final del hijo, también se queda bloqueado
   (interbloqueo). Con un hilo lector por tubo los dos se vacían a la vez.
4. **¿Qué aporta cerrar stdin después de escribir?**
   Cerrar el tubo envía el **EOF** al hijo: le indica que no recibirá más
   datos. Un proceso como el consumer lee hasta EOF, así que sin el cierre se
   quedaría esperando para siempre y no escribiría nunca el resultado (y el
   supervisor, esperándolo con `waitFor()`, también se bloquearía).
5. **¿Qué diferencia hay entre finalización ordenada y forzada?**
   *Pendiente (paso de timeout).*
6. **¿En qué se parece esta solución a Fork-Join y en qué se diferencia?**
   *Pendiente (paso de workers en paralelo).*
