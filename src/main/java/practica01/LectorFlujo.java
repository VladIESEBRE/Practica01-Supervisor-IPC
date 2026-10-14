// LectorFlujo: hilo que lee un tubo de salida de un proceso hijo (stdout o
// stderr) línea a línea, hasta que el hijo lo cierra. Cada línea se guarda con
// la hora y el origen, y se reemite por la salida del supervisor.
// Se usa un hilo para cada tubo para leer los dos A LA VEZ: si no, un tubo
// lleno podría bloquear al hijo y al supervisor (actividad C).
// Si se le da un destino de reenvío (el stdin de otro proceso), además copia
// cada línea allí: así el supervisor implementa la pipe productor -> consumidor.
package practica01;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
    private final OutputStream reenvio;                  // stdin de otro proceso, o null
    private final List<String> lineas = new ArrayList<>(); // líneas leídas, ya marcadas

    // Prepara un lector normal. Se llamará "io-stdout" o "io-stderr" (actividad A).
    public LectorFlujo(InputStream flujo, String origen, long pid) {
        this(flujo, origen, pid, null);
    }

    // Prepara un lector que además reenvía cada línea a "reenvio" (el stdin de
    // otro proceso) y lo cierra al llegar al EOF. El hilo se llamará "pipe".
    public LectorFlujo(InputStream flujo, String origen, long pid, OutputStream reenvio) {
        super(reenvio == null ? "io-" + origen : "pipe");
        this.flujo = flujo;
        this.origen = origen;
        this.pid = pid;
        this.reenvio = reenvio;
    }

    // Lo que hace el hilo al arrancar: lee línea a línea hasta EOF
    // (readLine() devuelve null cuando el hijo termina y el tubo se cierra).
    // Cada línea se marca con hora y origen, se guarda, se reemite y, si hay
    // destino, se reenvía. Al acabar, el try cierra el destino: le llega EOF.
    @Override
    public void run() {
        try (BufferedReader lector = new BufferedReader(
                     new InputStreamReader(flujo, StandardCharsets.UTF_8));
             BufferedWriter destino = reenvio == null ? null : new BufferedWriter(
                     new OutputStreamWriter(reenvio, StandardCharsets.UTF_8))) {
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
                if (destino != null) {
                    destino.write(linea);   // la línea original, sin marcas
                    destino.newLine();
                    destino.flush();        // que llegue ya al otro proceso
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
