# Diseño y decisiones

## Visión general

Un único programa (`supervisor.jar`, clase `practica01.ProcessForkJoinDemo`)
que hace un rol u otro según el primer argumento:

| Argumentos | Rol | Responsabilidad |
|---|---|---|
| *(ninguno)* | SUPERVISOR | Lanza los demás procesos, lee sus salidas, controla el tiempo, los finaliza si hace falta y agrega los resultados |
| `worker <NOMBRE> <pasos> <código>` | WORKER | Simula una tarea independiente: hace `<pasos>` pasos de 1 s, informa por stdout/stderr y termina con `<código>` |
| `producer` | PRODUCER | Escribe cinco nombres por stdout |
| `consumer` | CONSUMER | Lee stdin hasta EOF y responde `COUNT=<líneas> CHARS=<caracteres>` |

## Árbol de procesos previsto

Solo el supervisor se lanza desde la terminal; todos los demás son hijos suyos:

    zsh
     └── java ... ProcessForkJoinDemo                 SUPERVISOR
          ├── java ... ProcessForkJoinDemo worker ALPHA ...
          ├── java ... ProcessForkJoinDemo worker BETA ...
          ├── java ... ProcessForkJoinDemo worker GAMMA ...
          ├── java ... ProcessForkJoinDemo producer
          └── java ... ProcessForkJoinDemo consumer

## Supervisor y worker

| | Supervisor | Worker |
|---|---|---|
| Instancias | 1 | 3 (ALPHA, BETA, GAMMA) |
| Quién lo lanza | El usuario (o systemd) | El supervisor, con `ProcessBuilder` |
| Función | Organizar y vigilar | Hacer una tarea y devolver un código |
| Conoce a otros procesos | Sí, a todos sus hijos | No |

El worker representa cualquier programa externo (un compresor, un conversor,
`ping`…). Se implementa como un programa simulado para controlar su duración,
sus salidas y su código de retorno, y así poder provocar errores y timeouts de
forma reproducible.

## Flujo de una ejecución completa

1. El usuario lanza el supervisor.
2. El supervisor crea un pool de **2 hilos**: como máximo dos workers a la vez.
3. Cada hilo lanza un worker con `ProcessBuilder` (**fork**). Nace un proceso
   hijo con PID propio y PPID = PID del supervisor.
4. Mientras el worker trabaja, el supervisor lee **concurrentemente** su stdout
   y su stderr y añade marca temporal y origen a cada línea.
5. Espera a que acabe con un **timeout**. Si lo supera: finalización ordenada
   (`destroy()`, SIGTERM) y, si no responde, forzada (`destroyForcibly()`,
   SIGKILL), también de sus descendientes.
6. Guarda el resultado en un objeto inmutable: orden, PID, PPID, código,
   duración, salidas y timeout (**join**).
7. El tercer worker espera en la cola del pool hasta que queda un hilo libre.
8. **Pipe**: lanza el producer y el consumer, lee las líneas del stdout del
   producer y las escribe en el stdin del consumer; cuando el producer acaba,
   cierra el stdin del consumer (EOF) y recoge `COUNT=5 CHARS=18`.
9. Muestra el resumen y termina con un código de retorno.

## Decisión: un solo programa con un argumento de rol

El enunciado exige que cada tarea sea un **proceso real con PID propio**, no un
hilo. Alternativas consideradas:

| Opción | Ventajas | Inconvenientes |
|---|---|---|
| A. Lanzar órdenes del sistema (`sleep`, `ls`…) | No hay que programar los hijos | No se controla el código de retorno ni el formato de los mensajes; depende del sistema |
| B. Un programa separado para cada rol | Separación clara | Cuatro artefactos que compilar y mantener; formato de log duplicado |
| **C. Un solo programa con rol por argumento** ✅ | Una sola compilación; la misma clase `Log` para todos; control total de duración, salidas y código; solo necesita Java (pruebas reproducibles sin Internet) | El `main` tiene que validar los argumentos |

Motivos adicionales:

- La orden de observación del enunciado (`ps ... | grep ProcessForkJoinDemo`)
  encuentra todos los procesos, porque todos llevan el nombre de la clase en
  la línea de órdenes.
- Sin argumentos el programa actúa como supervisor, de modo que
  `java -jar supervisor.jar` funciona tal cual desde systemd (ampliación
  opcional).

## Relación con Fork-Join

| Fork-Join | Esta práctica |
|---|---|
| **Fork**: dividir el trabajo en subtareas | El supervisor lanza un proceso hijo para cada tarea |
| **Join**: esperar las subtareas y combinar los resultados | El supervisor espera a cada proceso (`waitFor`) y agrega sus resultados |
| Las subtareas son **hilos** de un `ForkJoinPool`, dentro del mismo proceso y compartiendo memoria | Las subtareas son **procesos** con PID y memoria propios; se comunican solo por stdin/stdout/stderr y el código de retorno |
