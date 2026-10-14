// SupervisorTest: batería de pruebas automáticas (JUnit 5) del supervisor.
// Cubre los casos que pide el enunciado: éxito, orden inexistente, stderr,
// timeout, stdin, pipe y cancelación, más el pool de dos workers.
// Los hijos se lanzan con el mismo java y classpath que los tests, así que no
// necesitan Internet ni nada instalado aparte de Java: son reproducibles.
package practica01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)   // ningún test puede quedarse colgado más de 30 s
class SupervisorTest {

    // Límite generoso para los casos que no deben llegar al timeout.
    private static final Duration LIMITE = Duration.ofSeconds(10);

    // Comprueba que el proceso con este PID ya no existe (no ha quedado huérfano).
    private static void assertMuerto(long pid) {
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false),
                "el proceso " + pid + " sigue vivo");
    }

    // Éxito: un worker que acaba bien da código 0, sin timeout, con su salida
    // capturada y con el supervisor (este proceso) como padre.
    @Test
    void exito() throws Exception {
        Resultado r = Supervisor.ejecutar(Supervisor.ordenPara("worker", "TEST", "1", "0"), List.of(), LIMITE);

        assertEquals(0, r.codigo());
        assertFalse(r.timeout());
        assertTrue(r.exito());
        assertEquals(ProcessHandle.current().pid(), r.ppid());
        assertEquals(1, r.stdout().size());
        assertTrue(r.stdout().get(0).contains("[WORKER-TEST"));
        assertTrue(r.stdout().get(0).contains("pas 1/1"));
        assertTrue(r.stderr().isEmpty());
        assertMuerto(r.pid());
    }

    // Orden inexistente: ProcessBuilder no puede crear el proceso y lanza una
    // IOException; no se queda colgado ni crea nada.
    @Test
    void ordenInexistente() {
        assertThrows(IOException.class, () ->
                Supervisor.ejecutar(List.of("orden-que-no-existe-xyz"), List.of(), LIMITE));
    }

    // stderr: un worker que acaba con código 7 deja el error en stderr y el
    // resultado lo marca como fallido (actividad B).
    @Test
    void stderrYCodigoDeError() throws Exception {
        Resultado r = Supervisor.ejecutar(Supervisor.ordenPara("worker", "TEST", "1", "7"), List.of(), LIMITE);

        assertEquals(7, r.codigo());
        assertFalse(r.exito());
        assertEquals(1, r.stderr().size());
        assertTrue(r.stderr().get(0).contains("[stderr pid=" + r.pid() + "]"));
        assertTrue(r.stderr().get(0).contains("codi 7"));
    }

    // Timeout: un worker de 10 pasos con un límite de 1 s se para de forma
    // ordenada (SIGTERM -> código 143) y no queda vivo (actividad D).
    @Test
    void timeout() throws Exception {
        Resultado r = Supervisor.ejecutar(Supervisor.ordenPara("worker", "TEST", "10", "0"),
                List.of(), Duration.ofSeconds(1));

        assertTrue(r.timeout());
        assertFalse(r.exito());
        assertEquals(143, r.codigo());   // 128 + 15 (SIGTERM)
        assertTrue(r.duracion().compareTo(Duration.ofSeconds(5)) < 0, "no ha esperado al worker entero");
        assertMuerto(r.pid());
    }

    // stdin: el supervisor escribe cinco nombres en el stdin del consumer y lo
    // cierra (EOF); el consumer responde COUNT=5 CHARS=18.
    @Test
    void stdin() throws Exception {
        Resultado r = Supervisor.ejecutar(Supervisor.ordenPara("consumer"),
                List.of("Ana", "Joan", "Pep", "Marc", "Laia"), LIMITE);

        assertEquals(0, r.codigo());
        assertEquals(1, r.stdout().size());
        assertTrue(r.stdout().get(0).endsWith("COUNT=5 CHARS=18"));
    }

    // stdin vacío: si se cierra el stdin sin escribir nada, el consumer recibe
    // EOF enseguida y responde COUNT=0 (no se queda esperando).
    @Test
    void stdinVacio() throws Exception {
        Resultado r = Supervisor.ejecutar(Supervisor.ordenPara("consumer"), List.of(), LIMITE);

        assertFalse(r.timeout());
        assertTrue(r.stdout().get(0).endsWith("COUNT=0 CHARS=0"));
    }

    // Pipe: producer -> supervisor -> consumer. Los dos son hijos de este
    // proceso, el producer genera 5 líneas y el consumer las cuenta.
    @Test
    void pipe() throws Exception {
        List<Resultado> rs = Supervisor.ejecutarPipe(LIMITE);
        Resultado productor = rs.get(0);
        Resultado consumidor = rs.get(1);

        assertTrue(productor.exito());
        assertTrue(consumidor.exito());
        assertEquals(ProcessHandle.current().pid(), productor.ppid());
        assertEquals(ProcessHandle.current().pid(), consumidor.ppid());
        assertEquals(5, productor.stdout().size());
        assertTrue(consumidor.stdout().get(0).endsWith("COUNT=5 CHARS=18"));
    }

    // Cancelación: mientras un worker largo trabaja, se llama a lo mismo que
    // ejecuta el shutdown hook; el worker se para de forma ordenada y no queda
    // ningún proceso hijo vivo.
    @Test
    void cancelacion() throws Exception {
        CompletableFuture<Resultado> enMarcha = CompletableFuture.supplyAsync(() -> {
            try {
                return Supervisor.ejecutar(Supervisor.ordenPara("worker", "TEST", "20", "0"),
                        List.of(), Duration.ofSeconds(25));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // Espera a que el worker exista (sea hijo de este proceso).
        while (ProcessHandle.current().children().findAny().isEmpty()) {
            Thread.sleep(50);
        }
        Thread.sleep(500);

        Supervisor.cancelarActivos();
        Resultado r = enMarcha.get();

        assertFalse(r.timeout());           // no ha sido por tiempo, sino por cancelación
        assertEquals(143, r.codigo());      // SIGTERM
        assertTrue(r.duracion().compareTo(Duration.ofSeconds(10)) < 0);
        assertMuerto(r.pid());
    }

    // Pool: los tres workers de la demo se ejecutan (máximo dos a la vez) y se
    // recogen los tres resultados: ALPHA bien, BETA timeout y GAMMA código 7.
    @Test
    void poolDeWorkers() throws Exception {
        List<Resultado> rs = Supervisor.ejecutarWorkers(Duration.ofSeconds(5));

        assertEquals(3, rs.size());
        assertTrue(rs.get(0).exito());                 // ALPHA
        assertTrue(rs.get(1).timeout());               // BETA
        assertEquals(7, rs.get(2).codigo());           // GAMMA
        for (Resultado r : rs) {
            assertMuerto(r.pid());
        }
    }
}
