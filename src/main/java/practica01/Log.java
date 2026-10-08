// Log: escribe los mensajes de diagnóstico con el formato del enunciado:
//   [ROL pid=... ppid=... thread=...] mensaje
// Así, de cualquier línea sabemos qué proceso (PID), quién es su padre (PPID)
// y qué hilo la ha escrito. info() va a stdout y error() a stderr.
package practica01;

public final class Log {
    // Rol que aparece en el prefijo. Por defecto SUPERVISOR; los workers lo cambian.
    private static String rol = "SUPERVISOR";

    // Constructor privado: es una clase de utilidades y no se crean objetos de ella.
    private Log() {}

    // Cambia el rol que se mostrará en los mensajes (p. ej. "WORKER-BETA").
    public static void setRol(String nuevoRol) {
        rol = nuevoRol;
    }

    // Escribe un mensaje normal por stdout (salida estándar), con el prefijo.
    public static void info(String mensaje) {
        System.out.println(prefijo() + " " + mensaje);
    }

    // Escribe un mensaje de error por stderr (salida de errores), con el prefijo.
    public static void error(String mensaje) {
        System.err.println(prefijo() + " " + mensaje);
    }

    // Construye el prefijo "[ROL pid=... ppid=... thread=...]" consultando
    // al sistema el PID de este proceso, el de su padre y el nombre del hilo actual.
    private static String prefijo() {
        ProcessHandle yo = ProcessHandle.current();
        long ppid = yo.parent().map(ProcessHandle::pid).orElse(-1L);
        return String.format("[%s pid=%d ppid=%d thread=%s]",
                rol, yo.pid(), ppid, Thread.currentThread().getName());
    }
}