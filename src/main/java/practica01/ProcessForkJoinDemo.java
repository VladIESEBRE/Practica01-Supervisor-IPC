// ProcessForkJoinDemo: punto de entrada del programa. De momento solo imprime
// quién es (rol, PID, PPID e hilo) para comprobar que el proyecto compila
// y que sabemos obtener la identidad del proceso con la API ProcessHandle.
package practica01;

public class ProcessForkJoinDemo {
    public static void main(String[] args) {
        ProcessHandle yo = ProcessHandle.current();                       // este proceso
        long pid = yo.pid();                                              // mi identificador
        long ppid = yo.parent().map(ProcessHandle::pid).orElse(-1L);      // el de mi padre
        String hilo = Thread.currentThread().getName();                   // hilo actual

        System.out.printf("[SUPERVISOR pid=%d ppid=%d thread=%s] hola%n", pid, ppid, hilo);
    }
}
