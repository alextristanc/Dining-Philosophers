import javax.swing.*;
import java.awt.*;

/** Ventana de tutorial por páginas. Solo explica; no toca la lógica. */
public class Tutorial extends JDialog {

    private static final Color PERGAMINO = new Color(245, 230, 200);
    private static final Color MARRON = new Color(92, 58, 30);
    private static final Color DORADO = new Color(230, 190, 90);

    private static final String ABRE = "<html><body style='width:440px; font-family:serif; font-size:13px; color:#5c3a1e'>";
    private static final String CIERRA = "</body></html>";

    private static final String[] TITULOS = {
        "El problema de la cena de los filósofos",
        "El peligro: el interbloqueo",
        "La solución: el comedor (Figura 6.13)",
        "Cómo leer la simulación",
        "Controles"
    };

    private static final String[] TEXTOS = {
        ABRE
        + "<p>Cinco filósofos están sentados alrededor de una mesa redonda. "
        + "Entre cada par de filósofos hay <b>un tenedor</b> (5 en total).</p>"
        + "<p>Cada filósofo repite siempre lo mismo: <b>piensa</b> y luego <b>come</b>. "
        + "Para comer necesita <b>dos tenedores</b>: el de su izquierda y el de su derecha.</p>"
        + "<p>Los filósofos son <b>procesos</b> (hilos) y los tenedores son "
        + "<b>recursos</b> compartidos.</p>" + CIERRA,

        ABRE
        + "<p>Si los cinco toman su tenedor izquierdo al mismo tiempo, cada uno se queda "
        + "esperando el derecho... que tiene su vecino.</p>"
        + "<p>Nadie suelta nada y <b>nadie vuelve a comer</b>. Eso es un "
        + "<b>interbloqueo (deadlock)</b>.</p>"
        + "<p>Esa es la solución ingenua de la Figura 6.12.</p>" + CIERRA,

        ABRE
        + "<p>Se agrega un semáforo <b>room = 4</b>: solo 4 filósofos pueden estar "
        + "sentados a la vez.</p>"
        + "<p>Con 4 filósofos y 5 tenedores, <b>al menos uno</b> siempre consigue sus dos "
        + "tenedores, come y los libera para los demás. Así no hay interbloqueo.</p>"
        + "<p><b>wait</b> pide un permiso (espera si no hay) y <b>signal</b> lo devuelve.</p>" + CIERRA,

        ABRE
        + "<p>El color del aro de cada filósofo indica su estado:</p>"
        + "<p><span style='color:#6aa0f0'>&#9679;</span> <b>Azul:</b> piensa<br>"
        + "<span style='color:#f09a3c'>&#9679;</span> <b>Naranja:</b> espera entrar al comedor<br>"
        + "<span style='color:#e6c800'>&#9679;</span> <b>Amarillo:</b> sentado, buscando tenedores<br>"
        + "<span style='color:#50be64'>&#9679;</span> <b>Verde:</b> está comiendo</p>"
        + "<p>Un tenedor <b>rojo</b> está en uso. Arriba ves cuántos hay en el comedor "
        + "(máximo 4) y a la derecha el pergamino registra cada operación.</p>" + CIERRA,

        ABRE
        + "<p><b>Modo automático:</b> los filósofos actúan solos.</p>"
        + "<p><b>Modo paso a paso:</b> cada clic en <i>Siguiente paso</i> avanza una sola "
        + "acción, para analizar el algoritmo con calma.</p>"
        + "<p>Puedes volver a abrir este tutorial con el botón <i>Ayuda</i>.</p>"
        + "<p>Cuando estés listo, presiona el botón de abajo.</p>" + CIERRA
    };

    private final boolean inicial;
    private int pagina = 0;
    private final JLabel titulo = new JLabel("", SwingConstants.CENTER);
    private final JLabel cuerpo = new JLabel();
    private final JLabel contador = new JLabel("", SwingConstants.CENTER);
    private final JButton anterior = boton("< Anterior");
    private final JButton siguiente = boton("Siguiente >");

    public Tutorial(JFrame padre, boolean inicial) {
        super(padre, "Tutorial", true);
        this.inicial = inicial;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(MARRON);

        titulo.setOpaque(true);
        titulo.setBackground(MARRON);
        titulo.setForeground(DORADO);
        titulo.setFont(new Font(Font.SERIF, Font.BOLD, 20));
        titulo.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
        raiz.add(titulo, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(PERGAMINO);
        centro.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 3, 3, 3, DORADO),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        cuerpo.setVerticalAlignment(SwingConstants.TOP);
        centro.add(cuerpo, BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);

        contador.setForeground(DORADO);
        contador.setFont(new Font(Font.SERIF, Font.BOLD, 13));
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(MARRON);
        pie.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
        pie.add(anterior, BorderLayout.WEST);
        pie.add(contador, BorderLayout.CENTER);
        pie.add(siguiente, BorderLayout.EAST);
        raiz.add(pie, BorderLayout.SOUTH);

        anterior.addActionListener(e -> { pagina--; mostrar(); });
        siguiente.addActionListener(e -> {
            if (pagina == TEXTOS.length - 1) dispose();
            else { pagina++; mostrar(); }
        });

        setContentPane(raiz);
        mostrar();
        setSize(540, 440);
        setResizable(false);
        setLocationRelativeTo(padre);
    }

    private void mostrar() {
        titulo.setText(TITULOS[pagina]);
        cuerpo.setText(TEXTOS[pagina]);
        contador.setText((pagina + 1) + " / " + TEXTOS.length);
        anterior.setEnabled(pagina > 0);
        boolean ultima = pagina == TEXTOS.length - 1;
        siguiente.setText(ultima ? (inicial ? "¡Iniciar!" : "Cerrar") : "Siguiente >");
    }

    private static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font(Font.SERIF, Font.BOLD, 14));
        b.setBackground(DORADO);
        b.setForeground(MARRON);
        b.setFocusPainted(false);
        return b;
    }
}