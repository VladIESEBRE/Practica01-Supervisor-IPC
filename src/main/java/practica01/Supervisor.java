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
import java.util.concurrent.TimeUnit;

public final class Supervisor {

    // Tiempo que se da a un hijo para terminar de forma ordenada (después de
    // destroy) antes de matarlo de forma forzada (destroyForcibly).
    private static final Duration GRACIA = Duration.ofSeconds(2);

    // Constructor privado: solo se usa el método estático run().
    private Supervisor() {
    }

    // Demo del supervisor: ejecuta tres workers (uno bien, uno que se pasa de
    // tiempo y uno que falla) y el consumer, con un límite de 5 s cada uno.
    // Termina con 0 si todos han ido bien o 1 si alguno ha fallado.
    public static int run() throws Exception {
        Log.info("inici");
        Duration limite = Duration.ofSeconds(5);
        List<Resultado> resultados = new ArrayList<>();
        resultados.add(ejecutar(ordenPara("worker", "ALPHA", "2", "0"), List.of(), limite));
        resultados.add(ejecutar(ordenPara("worker", "BETA", "10", "0"), List.of(), limite));
        resultados.add(ejecutar(ordenPara("worker", "GAMMA", "3", "7"), List.of(), limite));
        resultados.add(ejecutar(ordenPara("consumer"),
                List.of("Ana", "Joan", "Pep", "Marc", "Laia"), limite));

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
    // la vez con dos hilos y espera a que termine como mucho "limite"; si se
    // pasa, lo para (timeout). Devuelve un Resultado con todo.
    static Resultado ejecutar(List<String> orden, List<String> entrada, Duration limite)
            throws Exception {
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

        // JOIN con límite: waitFor(tiempo) devuelve false si el hijo no ha
        // terminado dentro del límite.
        boolean timeout = false;
        if (!hijo.waitFor(limite.toMillis(), TimeUnit.MILLISECONDS)) {
            timeout = true;
            Log.error("TIMEOUT: el fill " + hijo.pid() + " supera " + limite.toMillis() + "ms");
            aturar(hijo);
        }
        int codigo = hijo.exitValue(); // el hijo ya ha terminado: leemos su código
        lectorOut.join(); // esperamos a que los lectores acaben
        lectorErr.join();

        Resultado resultado = new Resultado(
                orden,
                hijo.pid(),
                ProcessHandle.current().pid(), // el padre del hijo es este proceso
                codigo,
                timeout, // de momento no hay timeout (paso 8)
                Duration.between(inicio, Instant.now()),
                lectorOut.getLineas(),
                lectorErr.getLineas());

        Log.info("resultat: " + resultado.resumen());
        return resultado;
    }

    // Para un proceso hijo y sus descendientes: primero de forma ordenada
    // (destroy = SIGTERM) y, si al cabo de GRACIA no ha terminado, de forma
    // forzada (destroyForcibly = SIGKILL). Así no quedan procesos huérfanos.
    static void aturar(Process hijo) throws InterruptedException {
        // Apuntamos los descendientes ANTES de parar al hijo: si el hijo muere
        // primero, sus hijos quedan huérfanos y ya no los encontraríamos.
        List<ProcessHandle> descendientes = hijo.descendants().toList();

        descendientes.forEach(ProcessHandle::destroy); // SIGTERM a los nietos
        hijo.destroy(); // SIGTERM al hijo
        if (hijo.waitFor(GRACIA.toMillis(), TimeUnit.MILLISECONDS)) {
            Log.info("fill " + hijo.pid() + " aturat de forma ordenada");
            return;
        }

        descendientes.forEach(ProcessHandle::destroyForcibly); // SIGKILL a los nietos
        hijo.destroyForcibly(); // SIGKILL al hijo
        hijo.waitFor();
        Log.error("fill " + hijo.pid() + " aturat de forma forçada");
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