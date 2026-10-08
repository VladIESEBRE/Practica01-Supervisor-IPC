# Pruebas manuales

Ejecuciones hechas desde la terminal para comprobar cada rol por separado,
antes de que el supervisor los lance. Las pruebas automáticas (JUnit) están
en `src/test/java`.

## Paso 3 · Roles y códigos de retorno

Cada orden va seguida de `; echo $?` para ver el código de retorno en la
misma terminal.

| Orden (`java -cp target/supervisor.jar practica01.ProcessForkJoinDemo ...`) | Código esperado | Código obtenido |
|---|---|---|
| *(sin argumentos)* | 0 | 0 |
| `worker BETA 3 0` | 0 | 0 |
| `worker GAMMA 2 7` | 7 | 7 |
| `worker GAMMA 2 7 2>/dev/null` | 7 | 7 |
| `patata` | 2 | 2 |

Salida completa:

    $ java -cp target/supervisor.jar practica01.ProcessForkJoinDemo ; echo $?
    [SUPERVISOR pid=60358 ppid=17731 thread=main] hola, soc el supervisor
    0

    $ java -cp target/supervisor.jar practica01.ProcessForkJoinDemo worker BETA 3 0 ; echo $?
    [WORKER-BETA pid=60738 ppid=17731 thread=main] pas 1/3
    [WORKER-BETA pid=60738 ppid=17731 thread=main] pas 2/3
    [WORKER-BETA pid=60738 ppid=17731 thread=main] pas 3/3
    0

![Worker BETA](capturas/paso3-worker-beta.png)

    $ java -cp target/supervisor.jar practica01.ProcessForkJoinDemo worker GAMMA 2 7 ; echo $?
    [WORKER-GAMMA pid=61135 ppid=17731 thread=main] pas 1/2
    [WORKER-GAMMA pid=61135 ppid=17731 thread=main] pas 2/2
    [WORKER-GAMMA pid=61135 ppid=17731 thread=main] acabo amb error, codi 7
    7

![Worker GAMMA con código 7](capturas/paso3-worker-gamma-codi7.png)

    $ java -cp target/supervisor.jar practica01.ProcessForkJoinDemo worker GAMMA 2 7 2>/dev/null ; echo $?
    [WORKER-GAMMA pid=61480 ppid=17731 thread=main] pas 1/2
    [WORKER-GAMMA pid=61480 ppid=17731 thread=main] pas 2/2
    7

![Worker GAMMA sin stderr](capturas/paso3-worker-gamma-sin-stderr.png)

    $ java -cp target/supervisor.jar practica01.ProcessForkJoinDemo patata ; echo $?
    [SUPERVISOR pid=61835 ppid=17731 thread=main] rol desconegut: patata
    2

### Conclusiones

- **Cada ejecución es un proceso distinto** (PIDs 60358, 60738, 61135…) y
  todos tienen como padre la shell (PPID 17731), porque de momento se lanzan
  desde la terminal. Cuando el supervisor los lance, el PPID será el del
  supervisor.
- **El código de retorno llega al padre**: el worker GAMMA termina con
  `System.exit(7)` y la shell recibe un 7. Es la base de la actividad B: el
  supervisor tendrá que recoger este código y dejar el error explícito.
- **stdout y stderr son flujos separados**: con `2>/dev/null` se descarta
  stderr (descriptor 2) y desaparece solo la línea `acabo amb error`, que se
  escribe con `Log.error()` → `System.err`. Las líneas `pas i/N` (stdout,
  descriptor 1) se siguen viendo. Por eso el supervisor tendrá que leer los
  dos flujos por separado.
- Un rol desconocido no provoca una excepción: se informa por stderr y se
  devuelve el código 2.

## Paso 4 · El supervisor lanza un worker con `ProcessBuilder`

El supervisor construye la orden como **lista** y la ejecuta con
`ProcessBuilder.start()`. Con `inheritIO()` el hijo escribe directamente en la
misma terminal. Después espera con `waitFor()` y recoge el código de retorno.

    $ java -cp target/supervisor.jar practica01.ProcessForkJoinDemo ; echo $?
    [SUPERVISOR pid=99981 ppid=17731 thread=main] inici
    [SUPERVISOR pid=99981 ppid=17731 thread=main] llançant: [/usr/lib/jvm/java-21-openjdk-amd64/bin/java, -cp, target/supervisor.jar, practica01.ProcessForkJoinDemo, worker, BETA, 10, 0]
    [SUPERVISOR pid=99981 ppid=17731 thread=main] fill creat amb pid=100020
    [WORKER-BETA pid=100020 ppid=99981 thread=main] pas 1/10
    ...
    [WORKER-BETA pid=100020 ppid=99981 thread=main] pas 10/10
    [SUPERVISOR pid=99981 ppid=17731 thread=main] fill pid=100020 ha acabat amb codi 0
    0

![Supervisor lanza worker](capturas/paso4-supervisor-lanza-worker.png)

Árbol de procesos observado desde otra terminal mientras el worker trabajaba
(`pstree -p 99981`, resumido; `{java}` son hilos):

    java(99981)            SUPERVISOR
     ├── java(100020)      WORKER-BETA   (+ 17 hilos {java} propios)
     └── 19 hilos {java}   del supervisor

![pstree supervisor y worker](capturas/paso4-pstree-supervisor-worker.png)

### Conclusiones

- El worker es un **proceso nuevo** (PID 100020) cuyo **PPID es el
  supervisor** (99981), no la shell. El supervisor sigue teniendo como padre
  la shell (17731):

      zsh (17731) → supervisor (99981) → worker BETA (100020)

- `pstree` muestra `java(100020)` **sin llaves**: es un proceso hijo, con sus
  propios hilos. Las entradas `{java}` son hilos.
- El código de retorno del worker (0) llega al supervisor con `waitFor()`, del
  mismo modo que a la shell con `echo $?`.
- Supervisor y worker escriben en la misma terminal porque, con
  `inheritIO()`, el hijo **hereda los descriptores 0, 1 y 2** del padre. Que
  compartan pantalla no los convierte en el mismo proceso.
