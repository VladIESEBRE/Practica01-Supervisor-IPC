#!/bin/bash
# observar-arbol.sh: observa desde otra terminal el árbol de procesos del
# supervisor mientras se ejecuta. Se lanza ANTES que el supervisor y espera:
#   1. cuando arranca BETA (ALPHA y BETA vivos)  -> pstree
#   2. cuando arranca GAMMA (ALPHA ya ha acabado) -> pstree + ps de los workers
# Solo sirve para observar y documentar (depende de pgrep y pstree, de Linux);
# la aplicación no lo necesita para funcionar.

# Espera hasta que haya un proceso cuya línea de órdenes contenga $1.
esperar() {
    echo "Esperant que arrenqui \"$1\"..."
    while ! pgrep -f "$1" > /dev/null; do sleep 0.2; done
    sleep 1
}

# PID del supervisor: su línea de órdenes acaba en el nombre de la clase.
supervisor() {
    pgrep -f 'ProcessForkJoinDemo$' | head -1
}

esperar "worker BETA"
echo
echo "=== Arbre de processos amb ALPHA i BETA en marxa ==="
pstree -T -p "$(supervisor)"

esperar "worker GAMMA"
echo
echo "=== Arbre de processos quan GAMMA ocupa el lloc d'ALPHA ==="
pstree -T -p "$(supervisor)"
echo
echo "=== ps dels workers vius ==="
ps -eo pid,ppid,stat,etime,cmd | grep '[P]rocessForkJoinDemo worker'
