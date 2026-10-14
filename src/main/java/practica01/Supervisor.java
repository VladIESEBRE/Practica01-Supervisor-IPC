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

    // Lanza el worker GAMMA como proceso hijo, lee su stdout y su stderr a la
    // vez con dos hilos, espera a que termine y muestra el resumen.
    public static int run() throws Exception {
        Log.info("inici");
        List<String> orden = ordenPara("worker", "GAMMA", "3", "7");
        Log.info("llançant: " + orden);

        ProcessBuilder pb = new ProcessBuilder(orden);   // ya SIN inheritIO()
        Process hijo = pb.start();                       // FORK
        Log.info("fill creat amb pid=" + hijo.pid());

        // Un hilo para cada tubo: se leen los dos a la vez mientras el hijo trabaja.
        // Ojo con los nombres: getInputStream() es el STDOUT del hijo
        // (para nosotros es "entrada", porque lo leemos).
        LectorFlujo lectorOut = new LectorFlujo(hijo.getInputStream(), "stdout", hijo.pid());
        LectorFlujo lectorErr = new LectorFlujo(hijo.getErrorStream(), "stderr", hijo.pid());
        lectorOut.start();
        lectorErr.start();

        int codigo = hijo.waitFor();   // JOIN con el proceso hijo
        lectorOut.join();              // esperamos a que los hilos acaben de leer lo que quede
        lectorErr.join();

        Log.info("fill pid=" + hijo.pid() + " ha acabat amb codi " + codigo
                + " (stdout=" + lectorOut.getLineas().size() + " línies, stderr="
                + lectorErr.getLineas().size() + " línies)");
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