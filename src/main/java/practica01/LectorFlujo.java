
// LectorFlujo: hilo que lee un tubo de salida de un proceso hijo (stdout o
// stderr) línea a línea, hasta que el hijo lo cierra. Cada línea se guarda con
// la hora y el origen, y se reemite por la salida del supervisor.
// Se usa un hilo para cada tubo para leer los dos A LA VEZ: si no, un tubo
// lleno podría bloquear al hijo y al supervisor (actividad C).
package practica01;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class LectorFlujo extends Thread {
    // Formato de la marca temporal: horas:minutos:segundos.milisegundos
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final InputStream flujo;                     // tubo del que leemos
    private final String origen;                         // "stdout" o "stderr"
    private final long pid;                              // PID del hijo que escribe
    private final List<String> lineas = new ArrayList<>(); // líneas leídas, ya marcadas

    // Prepara el hilo. Se llamará "io-stdout" o "io-stderr" (actividad A).
    public LectorFlujo(InputStream flujo, String origen, long pid) {
        super("io-" + origen);
        this.flujo = flujo;
        this.origen = origen;
        this.pid = pid;
    }

    // Lo que hace el hilo al arrancar: lee línea a línea hasta EOF
    // (readLine() devuelve null cuando el hijo termina y el tubo se cierra).
    // Cada línea se marca con hora y origen, se guarda y se reemite.
    @Override
    public void run() {
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(flujo, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                String marcada = LocalTime.now().format(HORA)
                        + " [" + origen + " pid=" + pid + "] " + linea;
                lineas.add(marcada);
                if (origen.equals("stderr")) {
                    Log.error(marcada);
                } else {
                    Log.info(marcada);
                }
            }
        } catch (IOException e) {
            Log.error("error llegint " + origen + " del pid " + pid + ": " + e.getMessage());
        }
    }

        // Devuelve una copia de las líneas leídas. Hay que llamarlo después de
    // join(), cuando el hilo ya ha terminado de leer.
    public List<String> getLineas() {
        return List.copyOf(lineas);
    }
}