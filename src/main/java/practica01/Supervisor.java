// Supervisor: rol SUPERVISOR. Lanza otros procesos (hijos) a partir del mismo
// programa, les envía datos por stdin, lee su stdout y su stderr a la vez,
// espera a que acaben y guarda el resultado de cada uno en un Resultado.
package practica01;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Supervisor {
    // Constructor privado: solo se usa el método estático run().
    private Supervisor() {
    }

    // Demo del supervisor: ejecuta un worker y un consumer, guarda sus
    // resultados y termina con 0 si todos han ido bien o 1 si alguno ha fallado.
    public static int run() throws Exception {
        Log.info("inici");
        List<Resultado> resultados = new ArrayList<>();
        resultados.add(ejecutar(ordenPara("worker", "GAMMA", "3", "7"), List.of()));
        resultados.add(ejecutar(ordenPara("consumer"), List.of("Ana", "Joan", "Pep", "Marc", "Laia")));

        int fallidos = 0;
        for (Resultado r : resultados) {
            if (!r.exito()) {
                fallidos++;
                Log.error("FALLADA: " + r.resumen());
            }
        }
        Log.info("fi: " + resultados.size() + " processos, " + fallidos + " fallits");
        return fallidos == 0 ? 0 : 1;
    }

    // Lanza un proceso hijo con la orden indicada, le escribe las líneas de
    // "entrada" por stdin y cierra el tubo (EOF), lee su stdout y su stderr a
    // la vez con dos hilos y espera a que termine. Devuelve un Resultado con todo.
    static Resultado ejecutar(List<String> orden, List<String> entrada) throws Exception {
        Log.info("llançant: " + orden);
        Instant inicio = Instant.now();
        Process hijo = new ProcessBuilder(orden).start(); // FORK
        Log.info("fill creat amb pid=" + hijo.pid());

        // Primero los lectores, para que ningún tubo de salida se llene.
        LectorFlujo lectorOut = new LectorFlujo(hijo.getInputStream(), "stdout", hijo.pid());
        LectorFlujo lectorErr = new LectorFlujo(hijo.getErrorStream(), "stderr", hijo.pid());
        lectorOut.start();
        lectorErr.start();

        // Escribimos en el stdin del hijo. Ojo: getOutputStream() es el STDIN
        // del hijo (para nosotros es "salida", porque escribimos en él).
        // El try (...) cierra el tubo al terminar el bloque: el hijo recibe EOF.
        try (BufferedWriter stdinHijo = new BufferedWriter(
                new OutputStreamWriter(hijo.getOutputStream(), StandardCharsets.UTF_8))) {
            for (String linea : entrada) {
                stdinHijo.write(linea);
                stdinHijo.newLine();
            }
        }
        Log.info("stdin del fill " + hijo.pid() + " tancat (EOF enviat, "
                + entrada.size() + " línies)");

        int codigo = hijo.waitFor(); // JOIN con el proceso hijo
        lectorOut.join(); // esperamos a que los lectores acaben
        lectorErr.join();

        Resultado resultado = new Resultado(
                orden,
                hijo.pid(),
                ProcessHandle.current().pid(), // el padre del hijo es este proceso
                codigo,
                false, // de momento no hay timeout (paso 8)
                Duration.between(inicio, Instant.now()),
                lectorOut.getLineas(),
                lectorErr.getLineas());

        Log.info("resultat: " + resultado.resumen());
        return resultado;
    }

    // Construye la orden para ejecutar ESTE MISMO programa con otro rol:
    // <ruta de java> -cp <classpath> practica01.ProcessForkJoinDemo <args...>
    // Usa el mismo java y el mismo classpath con los que se está ejecutando el
    // supervisor.
    static List<String> ordenPara(String... args) {
        String java = ProcessHandle.current().info().command().orElse("java");
        String classpath = System.getProperty("java.class.path");
        List<String> orden = new ArrayList<>(List.of(
                java, "-cp", classpath, ProcessForkJoinDemo.class.getName()));
        orden.addAll(List.of(args));
        return orden;
    }
}