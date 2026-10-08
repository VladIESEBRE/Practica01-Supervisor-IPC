// Supervisor: rol SUPERVISOR. Lanza otros procesos (hijos) a partir del mismo
// programa y espera a que acaben.
// Versión 1: lanza un solo worker, deja que escriba directamente en la
// terminal (inheritIO) y recoge su código de retorno.
package practica01;

import java.util.ArrayList;
import java.util.List;

public final class Supervisor {
    // Constructor privado: solo se usa el método estático run().
    private Supervisor() {}

    // Lanza el worker BETA como proceso hijo, espera a que termine,
    // muestra su código de retorno y devuelve 0.
    public static int run() throws Exception {
        Log.info("inici");
        List<String> orden = ordenPara("worker", "BETA", "10", "0");
        Log.info("llançant: " + orden);

        ProcessBuilder pb = new ProcessBuilder(orden);
        pb.inheritIO();                  // el hijo escribe en la misma terminal que el supervisor
        Process hijo = pb.start();       // FORK: aquí el sistema operativo crea el proceso hijo
        Log.info("fill creat amb pid=" + hijo.pid());

        int codigo = hijo.waitFor();     // JOIN: el supervisor se queda esperando a que acabe
        Log.info("fill pid=" + hijo.pid() + " ha acabat amb codi " + codigo);
        return 0;
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