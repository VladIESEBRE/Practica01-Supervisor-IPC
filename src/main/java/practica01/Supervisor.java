// Supervisor: rol SUPERVISOR. Lanza otros procesos (hijos) a partir del mismo
// programa y espera a que acaben.
// Versión 1: lanza un solo worker, deja que escriba directamente en la
// terminal (inheritIO) y recoge su código de retorno.
package practica01;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Supervisor {
    // Constructor privado: solo se usa el método estático run().
    private Supervisor() {}

    // Demo del supervisor: ejecuta un worker (sin datos de entrada) y después
    // un consumer al que se le envían cinco nombres por stdin.
    public static int run() throws Exception {
        Log.info("inici");
        ejecutar(ordenPara("worker", "GAMMA", "3", "7"), List.of());
        ejecutar(ordenPara("consumer"), List.of("Ana", "Joan", "Pep", "Marc", "Laia"));
        Log.info("fi");
        return 0;
    }

    // Lanza un proceso hijo con la orden indicada, le escribe las líneas de
    // "entrada" por stdin y cierra el tubo (EOF), lee su stdout y su stderr a
    // la vez con dos hilos y espera a que termine. Devuelve su código de retorno.
    static int ejecutar(List<String> orden, List<String> entrada) throws Exception {
        Log.info("llançant: " + orden);
        Process hijo = new ProcessBuilder(orden).start();   // FORK
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

        int codigo = hijo.waitFor();   // JOIN con el proceso hijo
        lectorOut.join();              // esperamos a que los lectores acaben
        lectorErr.join();

        Log.info("fill pid=" + hijo.pid() + " ha acabat amb codi " + codigo
                + " (stdout=" + lectorOut.getLineas().size() + " línies, stderr="
                + lectorErr.getLineas().size() + " línies)");
        return codigo;
    }

    // Construye la orden para ejecutar ESTE MISMO programa con otro rol:
    //   <ruta de java> -cp <classpath> practica01.ProcessForkJoinDemo <args...>
    // Usa el mismo java y el mismo classpath con los que se está ejecutando el supervisor.
    static List<String> ordenPara(String... args) {
        String java = ProcessHandle.current().info().command().orElse("java");
        String classpath = System.getProperty("java.class.path");
        List<String> orden = new ArrayList<>(List.of(
                java, "-cp", classpath, ProcessForkJoinDemo.class.getName()));
        orden.addAll(List.of(args));
        return orden;
    }
}