# Inventario de procesos

## Cómo se ha observado

El supervisor se lanza con una pausa temporal de 60 s para que dé tiempo a
observarlo desde otra terminal:

    # Terminal 1
    java -cp target/supervisor.jar practica01.ProcessForkJoinDemo

    # Terminal 2
    ps -eo pid,ppid,stat,etime,cmd | grep ProcessForkJoinDemo
    pstree -p <PID_DEL_SUPERVISOR>
    ps -T -p <PID_DEL_SUPERVISOR>

Se lanza con `-cp ... practica01.ProcessForkJoinDemo` en vez de `-jar` para
que el nombre de la clase aparezca en la línea de órdenes y `grep` lo encuentre.

## Salidas observadas

### `ps -eo pid,ppid,stat,etime,cmd`

    (pendiente)

### `pstree -p`

    (pendiente)

### `ps -T -p`

    (pendiente)

## Interpretación

*(pendiente: estado STAT, línea de grep, hilos de la JVM)*

## Mapa de procesos

| Rol | PID | PPID | Estado observado | Código de retorno |
|---|---|---|---|---|
| Supervisor | | | | |
| Worker Alpha | | | | |
| Worker Beta | | | | |
| Worker Gamma | | | | |

## Portabilidad de las órdenes

| Orden | Linux | macOS | Windows | Notas |
|---|---|---|---|---|
| `ps -eo pid,ppid,stat,etime,cmd` | Sí | Parcial | No | `ps` es estándar POSIX, pero las columnas disponibles varían (en macOS es `command` en vez de `cmd`). |
| `pstree -p` | Sí, si está instalado el paquete `psmisc` | No viene por defecto | No | Herramienta auxiliar, no estándar. |
| `ps -T` | Sí | No | No | Opción específica de `procps` de Linux. |
| `echo $$`, `echo $?` | Sí | Sí | No (cmd/PowerShell usan otra sintaxis) | Variables de shells POSIX. |
| `ProcessHandle` (Java) | Sí | Sí | Sí | API de Java 9+, portable. |

Por eso la aplicación obtiene el PID, el PPID y los descendientes con
`ProcessHandle` y **no depende de `ps` ni de `pstree` para funcionar**: estas
órdenes solo se usan para observar y documentar.
