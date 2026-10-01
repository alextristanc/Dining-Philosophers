import javax.swing.*;
import java.awt.*;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Cena de los filósofos (Capítulo 6). Cada solución sigue el pseudocódigo de la presentación:
 *  Fig. 6.12  semáforos fork[5]                  (puede haber interbloqueo)
 *  Fig. 6.13  semáforos fork[5] + room = 4       (sin interbloqueo)
 *  Fig. 6.14  monitor con ForkReady[5] / fork[5]
 *  Fig. 6.17  monitor con state[5] / needFork[5]
 */
public class Cena extends JFrame {
    static final int N = 5;
    static final int PENSANDO = 0, HAMBRIENTO = 1, COMIENDO = 2;
    static final String[] ESTADOS = {"PENSANDO", "HAMBRIENTO", "COMIENDO"};
    static final Color[] COLORES = {new Color(0x4CAF50), new Color(0xFFC107), new Color(0xF44336)};
    static final String[] SOLUCIONES = {
            "Fig. 6.12 - Semáforos (puede haber interbloqueo)",
            "Fig. 6.13 - Semáforos + room = 4",
            "Fig. 6.14 - Monitor (ForkReady)",
            "Fig. 6.17 - Monitor (estados)"};

    volatile int velocidad = 1000;
    volatile Simulacion sim;

    final PanelMesa panel = new PanelMesa();
    final JTextArea log = new JTextArea(8, 30);
    final JLabel aviso = new JLabel(" ", SwingConstants.CENTER);
    final JButton btnIniciar = new JButton("Iniciar");
    final JButton btnDetener = new JButton("Detener");
    final JComboBox<String> combo = new JComboBox<>(SOLUCIONES);
    long hambreTotalDesde = 0;

    public Cena() {
        super("Cena de los filósofos");
        JSlider slider = new JSlider(200, 2000, 1000);
        slider.setInverted(true);
        slider.addChangeListener(e -> velocidad = slider.getValue());

        btnIniciar.addActionListener(e -> iniciar());
        btnDetener.addActionListener(e -> detener());
        btnDetener.setEnabled(false);

        JPanel controles = new JPanel();
        controles.add(combo);
        controles.add(btnIniciar);
        controles.add(btnDetener);
        controles.add(new JLabel("Velocidad:"));
        controles.add(slider);

        aviso.setFont(aviso.getFont().deriveFont(Font.BOLD, 16f));
        aviso.setForeground(Color.RED);
        log.setEditable(false);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(controles, BorderLayout.CENTER);
        norte.add(aviso, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        add(new JScrollPane(log), BorderLayout.SOUTH);

        new javax.swing.Timer(50, e -> { detectarInterbloqueo(); panel.repaint(); }).start();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 800);
        setLocationRelativeTo(null);
    }

    void escribir(String s) {
        SwingUtilities.invokeLater(() -> {
            log.append(s + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    void iniciar() {
        detener();
        log.setText("");
        aviso.setText(" ");
        hambreTotalDesde = 0;
        sim = new Simulacion(combo.getSelectedIndex());
        sim.arrancar();
        combo.setEnabled(false);
        btnIniciar.setEnabled(false);
        btnDetener.setEnabled(true);
    }

    void detener() {
        if (sim != null) sim.parar();
        combo.setEnabled(true);
        btnIniciar.setEnabled(true);
        btnDetener.setEnabled(false);
    }

    /** Todos hambrientos y nadie comiendo durante 3 s seguidos = interbloqueo. */
    void detectarInterbloqueo() {
        Simulacion s = sim;
        if (s == null || !s.activa) return;
        boolean todos = true;
        for (int i = 0; i < N; i++) if (s.estado.get(i) != HAMBRIENTO) todos = false;
        if (!todos) { hambreTotalDesde = 0; aviso.setText(" "); return; }
        long ahora = System.currentTimeMillis();
        if (hambreTotalDesde == 0) hambreTotalDesde = ahora;
        else if (ahora - hambreTotalDesde > 3000) aviso.setText("¡INTERBLOQUEO! Los 5 filósofos esperan para siempre");
    }

    interface Solucion {
        void getForks(int pid) throws InterruptedException;
        void releaseForks(int pid);
    }

    /** Una corrida: estado compartido + solución elegida + los 5 hilos (parbegin). */
    class Simulacion {
        final AtomicIntegerArray estado = new AtomicIntegerArray(N);   // lo que se dibuja
        final AtomicIntegerArray duenio = new AtomicIntegerArray(N);   // quién tiene cada tenedor (-1 libre)
        final Thread[] hilos = new Thread[N];
        final Solucion sol;
        volatile boolean activa = true;

        Simulacion(int tipo) {
            for (int i = 0; i < N; i++) duenio.set(i, -1);
            switch (tipo) {
                case 0: sol = new Sem612(false); break;
                case 1: sol = new Sem612(true); break;
                case 2: sol = new Monitor614(); break;
                default: sol = new Monitor617(); break;
            }
        }

        void arrancar() {            // parbegin(philosopher(0), ..., philosopher(4))
            for (int i = 0; i < N; i++) {
                final int k = i;
                hilos[i] = new Thread(() -> philosopher(k), "Filosofo-" + i);
                hilos[i].start();
            }
        }

        void parar() {
            activa = false;
            for (Thread t : hilos) if (t != null) t.interrupt();
        }

        void dormir(int base) throws InterruptedException {
            Thread.sleep(base / 2 + new Random().nextInt(base));
        }

        // while (true) { think(); get_forks(i); eat(); release_forks(i); }
        void philosopher(int i) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    estado.set(i, PENSANDO);
                    dormir(velocidad);                       // think();
                    estado.set(i, HAMBRIENTO);
                    escribir("F" + i + " tiene hambre");
                    sol.getForks(i);
                    estado.set(i, COMIENDO);
                    escribir("F" + i + " COME");
                    dormir(velocidad);                       // eat();
                    sol.releaseForks(i);
                }
            } catch (InterruptedException e) {
                // simulación detenida
            }
        }

        // ---------- Fig. 6.12 y 6.13: semáforos ----------
        // semaphore fork[5] = {1};   (6.13 agrega: semaphore room = {4};)
        class Sem612 implements Solucion {
            final Semaphore[] fork = new Semaphore[N];
            final Semaphore room = new Semaphore(4);
            final boolean conRoom;

            Sem612(boolean conRoom) {
                this.conRoom = conRoom;
                for (int i = 0; i < N; i++) fork[i] = new Semaphore(1);
            }

            public void getForks(int i) throws InterruptedException {
                int der = (i + 1) % N;
                if (conRoom) room.acquire();                 // wait(room);
                fork[i].acquire();                           // wait(fork[i]);
                duenio.set(i, i);
                // Pausa que NO está en la diapositiva: hace visible el interbloqueo de la Fig. 6.12
                Thread.sleep(velocidad / 2);
                fork[der].acquire();                         // wait(fork[(i+1) mod 5]);
                duenio.set(der, i);
            }

            public void releaseForks(int i) {
                int der = (i + 1) % N;
                duenio.set(der, -1);
                fork[der].release();                         // signal(fork[(i+1) mod 5]);
                duenio.set(i, -1);
                fork[i].release();                           // signal(fork[i]);
                if (conRoom) room.release();                 // signal(room);
            }
        }

        // ---------- Fig. 6.14: monitor dining_controller (ForkReady) ----------
        class Monitor614 implements Solucion {
            final ReentrantLock monitor = new ReentrantLock();
            final Condition[] forkReady = new Condition[N];  // cond ForkReady[5]
            final boolean[] fork = new boolean[N];           // boolean fork[5] = {true}

            Monitor614() {
                for (int i = 0; i < N; i++) { forkReady[i] = monitor.newCondition(); fork[i] = true; }
            }

            public void getForks(int pid) throws InterruptedException {
                int left = pid;
                int right = (pid + 1) % N;
                monitor.lockInterruptibly();
                try {
                    if (!fork[left]) forkReady[left].await();     // cwait(ForkReady[left]);
                    fork[left] = false;
                    duenio.set(left, pid);
                    if (!fork[right]) forkReady[right].await();   // cwait(ForkReady[right]);
                    fork[right] = false;
                    duenio.set(right, pid);
                } finally {
                    monitor.unlock();
                }
            }

            public void releaseForks(int pid) {
                int left = pid;
                int right = (pid + 1) % N;
                monitor.lock();
                try {
                    duenio.set(left, -1);
                    if (!monitor.hasWaiters(forkReady[left])) fork[left] = true;   // empty(ForkReady[left])
                    else forkReady[left].signal();                                  // csignal(ForkReady[left])
                    duenio.set(right, -1);
                    if (!monitor.hasWaiters(forkReady[right])) fork[right] = true;
                    else forkReady[right].signal();
                } finally {
                    monitor.unlock();
                }
            }
        }

        // ---------- Fig. 6.17: monitor dining_controller (estados) ----------
        class Monitor617 implements Solucion {
            final ReentrantLock monitor = new ReentrantLock();
            final Condition[] needFork = new Condition[N];   // cond needFork[5]
            final int[] state = new int[N];                  // enum states {thinking, hungry, eating}

            Monitor617() {
                for (int i = 0; i < N; i++) { needFork[i] = monitor.newCondition(); state[i] = PENSANDO; }
            }

            int mod(int x) { return Math.floorMod(x, N); }

            public void getForks(int pid) throws InterruptedException {
                monitor.lockInterruptibly();
                try {
                    state[pid] = HAMBRIENTO;
                    // La diapositiva usa "if"; en Java (monitor tipo Mesa) se usa "while" para re-verificar.
                    while (state[mod(pid + 1)] == COMIENDO || state[mod(pid - 1)] == COMIENDO)
                        needFork[pid].await();                        // cwait(needFork[pid]);
                    state[pid] = COMIENDO;
                    duenio.set(pid, pid);
                    duenio.set(mod(pid + 1), pid);
                } finally {
                    monitor.unlock();
                }
            }

            public void releaseForks(int pid) {
                monitor.lock();
                try {
                    state[pid] = PENSANDO;
                    duenio.set(pid, -1);
                    duenio.set(mod(pid + 1), -1);
                    // dar al vecino derecho (mayor) la oportunidad de comer
                    if (state[mod(pid + 1)] == HAMBRIENTO && state[mod(pid + 2)] != COMIENDO)
                        needFork[mod(pid + 1)].signal();              // csignal(needFork[pid+1]);
                    // dar al vecino izquierdo (menor) la oportunidad de comer
                    else if (state[mod(pid - 1)] == HAMBRIENTO && state[mod(pid - 2)] != COMIENDO)
                        needFork[mod(pid - 1)].signal();              // csignal(needFork[pid-1]);
                } finally {
                    monitor.unlock();
                }
            }
        }
    }

    class PanelMesa extends JPanel {
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Simulacion s = sim;
            int cx = getWidth() / 2, cy = getHeight() / 2;
            int R = Math.min(cx, cy) - 70;

            g.setColor(new Color(0xA8DCEB));
            g.fillOval(cx - R + 40, cy - R + 40, 2 * (R - 40), 2 * (R - 40));

            // tenedor i queda entre el filósofo i y el i+1
            for (int i = 0; i < N; i++) {
                double a = Math.toRadians(-90 + i * 72 + 36);
                double px = cx + (R - 55) * Math.cos(a), py = cy + (R - 55) * Math.sin(a);
                int d = s == null ? -1 : s.duenio.get(i);
                if (d >= 0) {
                    double ao = Math.toRadians(-90 + d * 72);
                    px = cx + (R - 55) * Math.cos(ao) * 0.9 + (px - cx) * 0.1;
                    py = cy + (R - 55) * Math.sin(ao) * 0.9 + (py - cy) * 0.1;
                    g.setColor(Color.BLACK);
                } else g.setColor(Color.GRAY);
                g.setStroke(new BasicStroke(5));
                g.drawLine((int) px, (int) py, (int) (px + 22 * Math.cos(a)), (int) (py + 22 * Math.sin(a)));
                g.drawString("T" + i, (int) px - 6, (int) py - 6);
            }

            for (int i = 0; i < N; i++) {
                double a = Math.toRadians(-90 + i * 72);
                int x = (int) (cx + R * Math.cos(a)), y = (int) (cy + R * Math.sin(a));
                int e = s == null ? PENSANDO : s.estado.get(i);
                g.setColor(COLORES[e]);
                g.fillOval(x - 38, y - 38, 76, 76);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2));
                g.drawOval(x - 38, y - 38, 76, 76);
                g.drawString("P" + i, x - 8, y - 4);
                g.drawString(ESTADOS[e], x - g.getFontMetrics().stringWidth(ESTADOS[e]) / 2, y + 14);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cena().setVisible(true));
    }
}