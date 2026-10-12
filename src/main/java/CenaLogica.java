import java.util.concurrent.Semaphore;

/** LÓGICA (Figura 6.13). Se añadieron llamadas a "listener" (avisos a la animación)
 *  y "paso()" (pausa opcional para el modo paso a paso). Los wait/signal no cambiaron. */
public class CenaLogica {

    public enum Estado { PENSANDO, ESPERANDO, SENTADO, COMIENDO }

    /** Canal de comunicación con la animación (no afecta la lógica). */
    public interface Listener {
        void filosofo(int i, Estado e);
        void tenedor(int t, int duenio);   // duenio = -1 si queda libre
        default void mensaje(String texto) { }
    }

    static volatile Listener listener = new Listener() {
        public void filosofo(int i, Estado e) { }
        public void tenedor(int t, int duenio) { }
    };

    // ---- Modo paso a paso ----
    static volatile boolean modoPaso = false;
    static final Semaphore pasos = new Semaphore(0, true);   // FIFO: avanza el que llegó primero

    public static void setModoPaso(boolean activo) {
        modoPaso = activo;
        if (activo) pasos.drainPermits(); else pasos.release(1000);   // al salir, libera a los que esperan
    }

    public static void siguientePaso() { pasos.release(); }

    static void paso() throws InterruptedException {
        if (modoPaso) pasos.acquire();
    }

    // ---- Algoritmo ----
    static Semaphore[] fork = new Semaphore[5];   // semaphore fork[5] = {1}
    static Semaphore room = new Semaphore(4);     // semaphore room = {4}

    static void philosopher(int i) throws InterruptedException {
        while (true) {
            think(i);

            paso(); listener.mensaje("F" + i + ": wait(room)");
            listener.filosofo(i, Estado.ESPERANDO);
            room.acquire();                                              // wait(room)
            listener.filosofo(i, Estado.SENTADO);
            listener.mensaje("F" + i + " entró al comedor");

            paso(); listener.mensaje("F" + i + ": wait(fork[" + i + "])");
            fork[i].acquire();                                           // wait(fork[i])
            listener.tenedor(i, i);
            listener.mensaje("F" + i + " tomó tenedor " + i);

            paso(); listener.mensaje("F" + i + ": wait(fork[" + (i + 1) % 5 + "])");
            fork[(i + 1) % 5].acquire();                                 // wait(fork[(i+1) mod 5])
            listener.tenedor((i + 1) % 5, i);
            listener.mensaje("F" + i + " tomó tenedor " + (i + 1) % 5);

            eat(i);

            paso(); listener.mensaje("F" + i + ": signal(fork[" + (i + 1) % 5 + "])");
            listener.tenedor((i + 1) % 5, -1);
            fork[(i + 1) % 5].release();                                 // signal(fork[(i+1) mod 5])

            paso(); listener.mensaje("F" + i + ": signal(fork[" + i + "])");
            listener.tenedor(i, -1);
            fork[i].release();                                           // signal(fork[i])

            paso(); listener.mensaje("F" + i + ": signal(room)");
            room.release();                                              // signal(room)
        }
    }

    static void think(int i) throws InterruptedException {
        listener.filosofo(i, Estado.PENSANDO);
        listener.mensaje("F" + i + " piensa");
        pausa();
    }

    static void eat(int i) throws InterruptedException {
        listener.filosofo(i, Estado.COMIENDO);
        listener.mensaje("F" + i + " come");
        pausa();
    }

    /** En automático duerme un rato; en paso a paso espera al botón. */
    static void pausa() throws InterruptedException {
        if (modoPaso) pasos.acquire();
        else Thread.sleep(1000 + (long) (Math.random() * 2000));
    }

    /** Equivale a parbegin(philosopher(0), ..., philosopher(4)). */
    public static void iniciar() {
        for (int i = 0; i < 5; i++) fork[i] = new Semaphore(1);
        for (int i = 0; i < 5; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try { philosopher(id); } catch (InterruptedException e) { }
            });
            t.setDaemon(true);
            t.start();
        }
    }
}