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

    $ java -cp target/supervisor.jar practica01.ProcessForkJoinDemo worker GAMMA 2 7 ; echo $?
    [WORKER-GAMMA pid=61135 ppid=17731 thread=main] pas 1/2
    [WORKER-GAMMA pid=61135 ppid=17731 thread=main] pas 2/2
    [WORKER-GAMMA pid=61135 ppid=17731 thread=main] acabo amb error, codi 7
    7

    $ java -cp target/supervisor.jar practica01.ProcessForkJoinDemo worker GAMMA 2 7 2>/dev/null ; echo $?
    [WORKER-GAMMA pid=61480 ppid=17731 thread=main] pas 1/2
    [WORKER-GAMMA pid=61480 ppid=17731 thread=main] pas 2/2
    7

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
