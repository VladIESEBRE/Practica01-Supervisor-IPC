// Worker: simula una tarea independiente. Hace N pasos con una pausa de 1 s
// entre ellos, informa del progreso por stdout y devuelve el código de salida
// que se le indique (0 = éxito; otro valor = error, p. ej. 7 para la actividad B).
package practica01;

public final class Worker {
    // Constructor privado: solo se usa el método estático run().
    private Worker() {}

    // Ejecuta el trabajo del worker: se pone el rol WORKER-<NOMBRE>, escribe
    // "pas i/N" una vez por segundo y devuelve codigoSalida. Si no es 0,
    // avisa del error por stderr.
    public static int run(String nombre, int pasos, int codigoSalida) throws InterruptedException {
        Log.setRol("WORKER-" + nombre.toUpperCase());
        for (int i = 1; i <= pasos; i++) {
            Log.info("pas " + i + "/" + pasos);
            Thread.sleep(1000);
        }
        if (codigoSalida != 0) {
            Log.error("acabo amb error, codi " + codigoSalida);
        }
        return codigoSalida;
    }
}