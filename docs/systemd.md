# Ampliación · Supervisor como servicio systemd

## Entorno

- Linux con systemd. El usuario `alumnat` **no tiene sudo** (pide contraseña),
  así que se usa una **unidad de usuario** (`systemctl --user`): el servicio
  se ejecuta con la cuenta del alumno, sin privilegios, tal como pide el
  enunciado.
- La unidad **no se habilita** (no tiene sección `[Install]`): no puede
  arrancar sola al iniciar el ordenador. `systemctl --user is-enabled`
  devuelve `static`.

## Fichero `systemd/practica01.service`

    [Unit]
    Description=Supervisor multiproces - Practica 01

    [Service]
    Type=oneshot
    WorkingDirectory=/home/alumnat/Escriptori/Servicios y procesos/Practica_01/supervisor
    ExecStart=/usr/bin/java -jar "/home/alumnat/Escriptori/Servicios y procesos/Practica_01/supervisor/target/supervisor.jar"
    TimeoutStartSec=90s
    KillMode=control-group
    StandardOutput=journal
    StandardError=journal

Diferencias con el ejemplo del enunciado: no hay `User=` (en una unidad de
usuario el servicio ya se ejecuta con la cuenta del usuario) y la ruta del
`.jar` va entre comillas porque contiene espacios.

| Directiva | Por qué |
|---|---|
| `Type=oneshot` | El supervisor hace una tarea finita y termina. systemd espera a que acabe y registra su código. |
| `TimeoutStartSec=90s` | Mayor que el límite interno de cada hijo (5 s) más la gracia (2 s) y la pipe: systemd no corta nunca una ejecución normal (~6 s). |
| `KillMode=control-group` | Si se para el servicio, systemd envía la señal a **todos** los procesos del grupo de control (supervisor y workers). |
| `StandardOutput/Error=journal` | Las líneas que el supervisor reemite (con rol y PID) quedan en el journal. |

## Órdenes

    # Instalación (sin habilitar)
    mkdir -p ~/.config/systemd/user
    cp systemd/practica01.service ~/.config/systemd/user/
    systemctl --user daemon-reload

    # Ejecución y consulta
    systemctl --user start practica01.service
    systemctl --user status practica01.service --no-pager
    journalctl --user -u practica01.service --no-pager
    ps -eo pid,ppid,cmd | grep '[P]rocessForkJoinDemo'

    # Parada a medias
    systemctl --user start --no-block practica01.service
    systemctl --user stop practica01.service

Equivalente con una unidad de sistema (si hubiera permisos):
`sudo cp ... /etc/systemd/system/`, `sudo systemctl daemon-reload`,
`sudo systemctl start practica01.service`, `sudo journalctl -u practica01.service`,
y entonces sí con `User=alumnat`.

## Resultado de la ejecución normal

    × practica01.service - Supervisor multiproces - Practica 01
         Loaded: loaded (/home/alumnat/.config/systemd/user/practica01.service; static)
         Active: failed (Result: exit-code)
        Process: 62708 ExecStart=/usr/bin/java -jar ... (code=exited, status=1/FAILURE)

Journal (fragmento):

    [SUPERVISOR pid=62708 ppid=3546 thread=main] inici
    [SUPERVISOR pid=62708 ppid=3546 thread=pool-1-thread-2] TIMEOUT: el fill 62736 supera 5000ms
    [SUPERVISOR pid=62708 ppid=3546 thread=main] pipe: producer pid=62853 --stdout--> SUPERVISOR --stdin--> consumer pid=62854
    [SUPERVISOR pid=62708 ppid=3546 thread=io-stdout] 17:55:05.789 [stdout pid=62854] COUNT=5 CHARS=18
    [SUPERVISOR pid=62708 ppid=3546 thread=main] fi: 5 processos, 2 fallits

- El PPID del supervisor es **3546**: el gestor `systemd --user`, no una shell.
- El servicio queda en `failed` con `status=1` porque la demo incluye dos
  hijos que fallan **a propósito** (BETA por timeout y GAMMA con código 7). Es
  el resultado esperado: el supervisor informa del fallo con su código de
  retorno y systemd lo refleja. Si todos los hijos acabaran bien, sería
  `status=0/SUCCESS` e `inactive (dead)`.
- Después de la ejecución, `ps` no encuentra ningún proceso de la práctica.

## Resultado de la parada a medias

Con el servicio en marcha (ALPHA y BETA trabajando), `systemctl --user stop`:

    --- workers antes del stop:
      63039   63007 ... worker BETA 10 0
      63040   63007 ... worker ALPHA 2 0
    --- workers después del stop:
    (ninguno)

    [SUPERVISOR pid=63007 ppid=3546 thread=shutdown-hook] fill 63040 aturat de forma ordenada
    practica01.service: Main process exited, code=exited, status=143/n/a
    Stopped practica01.service - Supervisor multiproces - Practica 01.

systemd envía SIGTERM a todo el grupo de control; el shutdown hook del
supervisor también para a sus hijos. No queda ningún worker vivo.

## ¿Por qué `Type=oneshot` y no un servicio residente?

- **oneshot**: el supervisor recibe un trabajo finito (tres workers y una
  pipe), lo ejecuta y termina. systemd considera el servicio "activándose"
  mientras trabaja y, cuando acaba, registra el código de salida. Es lo que
  corresponde a esta práctica.
- **Residente** (`Type=simple`/`notify`, normalmente con `Restart=`): el
  proceso se queda vivo indefinidamente esperando trabajo (una cola, un
  socket, un directorio…). Necesitaría una fuente continua de tareas y un
  bucle principal que no acaba, y systemd lo reiniciaría si muriera. Con el
  supervisor actual no tendría sentido: acabaría enseguida y systemd lo
  vería como un fallo o lo reiniciaría en bucle.
- El supervisor no se "daemoniza" (no hace fork y deja el padre): se queda en
  primer plano, de modo que los workers son descendientes del proceso
  principal del servicio y están dentro de su grupo de control.
