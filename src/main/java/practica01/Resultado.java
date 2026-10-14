// Resultado: todo lo que el supervisor sabe de la ejecución de un proceso hijo
// (orden, PID, PPID, código de retorno, duración, salidas y si ha habido
// timeout). Es un record: inmutable, una vez creado no se puede modificar.
package practica01;

import java.time.Duration;
import java.util.List;

public record Resultado(
        List<String> orden,     // ejecutable y argumentos
        long pid,               // PID del hijo
        long ppid,              // PID de su padre (el supervisor)
        int codigo,             // código de retorno
        boolean timeout,        // true si se ha tenido que parar por tiempo
        Duration duracion,      // tiempo total desde start() hasta el final
        List<String> stdout,    // líneas de stdout, con hora y origen
        List<String> stderr) {  // líneas de stderr, con hora y origen

    // Constructor compacto: se ejecuta al crear el record. Hace copias
    // inmutables de las listas, para que nadie pueda cambiarlas desde fuera.
    public Resultado {
        orden = List.copyOf(orden);
        stdout = List.copyOf(stdout);
        stderr = List.copyOf(stderr);
    }

    // true si el hijo ha acabado bien: código 0 y sin timeout.
    public boolean exito() {
        return codigo == 0 && !timeout;
    }

    // Resumen de una línea para mostrar en el log.
    public String resumen() {
        return "pid=" + pid + " ppid=" + ppid
                + " codi=" + codigo
                + " timeout=" + (timeout ? "sí" : "no")
                + " durada=" + duracion.toMillis() + "ms"
                + " stdout=" + stdout.size() + " línies"
                + " stderr=" + stderr.size() + " línies"
                + " ordre=" + orden.subList(3, orden.size());
    }
}