import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;

/** ANIMACIÓN: solo dibuja lo que la lógica le avisa. No usa semáforos.
 *  Usa imágenes de la carpeta "imagenes" (raíz del proyecto); si faltan, dibuja figuras. */
public class PanelMesa extends JPanel implements CenaLogica.Listener {

    private final CenaLogica.Estado[] estado = new CenaLogica.Estado[5];
    private final int[] duenio = new int[5];   // dueño de cada tenedor (-1 = libre)

    // Imágenes (null = no encontrada -> se usa figura)
    private final BufferedImage imgFondo = cargar("fondo.png");
    private final BufferedImage imgMesa = cargar("mesa.png");
    private final BufferedImage imgTenedor = cargar("tenedor.png");
    private final BufferedImage imgFilosofo = cargar("filosofo.png");          // genérica
    private final BufferedImage[] imgEstado = {                                 // opcionales por estado
        cargar("filosofo_pensando.png"),
        cargar("filosofo_esperando.png"),
        cargar("filosofo_sentado.png"),
        cargar("filosofo_comiendo.png")
    };

    public PanelMesa() {
        setBackground(Color.WHITE);
        for (int i = 0; i < 5; i++) { estado[i] = CenaLogica.Estado.PENSANDO; duenio[i] = -1; }
    }

    private static BufferedImage cargar(String nombre) {
        try {
            File f = new File("imagenes", nombre);
            return f.exists() ? ImageIO.read(f) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Llamados desde los hilos de los filósofos -> se pasan al hilo de Swing
    @Override public void filosofo(int i, CenaLogica.Estado e) {
        SwingUtilities.invokeLater(() -> { estado[i] = e; repaint(); });
    }

    @Override public void tenedor(int t, int d) {
        SwingUtilities.invokeLater(() -> { duenio[t] = d; repaint(); });
    }

    private Color color(CenaLogica.Estado e) {
        switch (e) {
            case PENSANDO:  return new Color(100, 160, 240);  // azul
            case ESPERANDO: return new Color(240, 150, 60);   // naranja (fuera del comedor)
            case SENTADO:   return new Color(240, 210, 60);   // amarillo (en comedor, sin comer)
            default:        return new Color(80, 190, 100);   // verde (comiendo)
        }
    }

    private String texto(CenaLogica.Estado e) {
        switch (e) {
            case PENSANDO:  return "Piensa";
            case ESPERANDO: return "Espera comedor";
            case SENTADO:   return "Busca tenedores";
            default:        return "Come";
        }
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth(), h = getHeight();

        // Fondo
        if (imgFondo != null) {
            g.drawImage(imgFondo, 0, 0, w, h, null);
        } else {
            g.setPaint(new RadialGradientPaint(w / 2f, h / 2f, Math.max(w, h) * 0.7f,
                    new float[]{0f, 1f}, new Color[]{new Color(255, 248, 225), new Color(176, 140, 100)}));
            g.fillRect(0, 0, w, h);
        }
        double cx = w / 2.0, cy = h / 2.0;
        double R = Math.min(w, h) * 0.34;

        // Mesa
        if (imgMesa != null) {
            g.drawImage(imgMesa, (int) (cx - R * 0.75), (int) (cy - R * 0.75),
                        (int) (R * 1.5), (int) (R * 1.5), null);
        } else {
            g.setColor(new Color(222, 184, 135));
            g.fill(new Ellipse2D.Double(cx - R * 0.75, cy - R * 0.75, R * 1.5, R * 1.5));
        }

        // Comedor: ocupados = sentados + comiendo
        int enComedor = 0;
        for (CenaLogica.Estado e : estado)
            if (e == CenaLogica.Estado.SENTADO || e == CenaLogica.Estado.COMIENDO) enComedor++;
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRoundRect(10, 8, 220, 28, 12, 12);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Comedor (room): " + enComedor + " / 4", 18, 28);

        // Tenedores
        for (int k = 0; k < 5; k++) {
            // Posición dentro de la mesa. Libre: entre dos filósofos.
            // En uso: se acerca a su dueño (izquierdo -14°, derecho +14°), sin salir de la mesa.
            double grados = -90 + k * 72 - 36;
            if (duenio[k] >= 0)
                grados = -90 + duenio[k] * 72 + (k == duenio[k] ? -14 : 14);
            double a = Math.toRadians(grados);
            double px = cx + R * 0.55 * Math.cos(a), py = cy + R * 0.55 * Math.sin(a);
            if (imgTenedor != null) {
                if (duenio[k] >= 0) {                               // halo rojo = en uso
                    g.setColor(new Color(220, 50, 50, 140));
                    g.fill(new Ellipse2D.Double(px - 20, py - 20, 40, 40));
                }
                AffineTransform old = g.getTransform();
                g.translate(px, py);
                g.rotate(a + Math.PI / 2);
                g.drawImage(imgTenedor, -12, -28, 24, 56, null);
                g.setTransform(old);
            } else {
                double dx = Math.cos(a) * 16, dy = Math.sin(a) * 16;
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(duenio[k] >= 0 ? new Color(200, 50, 50) : Color.DARK_GRAY);
                g.drawLine((int) (px - dx), (int) (py - dy), (int) (px + dx), (int) (py + dy));
            }
        }

        // Filósofos
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(-90 + i * 72);
            double x = cx + R * Math.cos(a), y = cy + R * Math.sin(a);
            BufferedImage img = imgEstado[estado[i].ordinal()] != null
                              ? imgEstado[estado[i].ordinal()] : imgFilosofo;
            if (img != null) {
                g.drawImage(img, (int) x - 40, (int) y - 40, 80, 80, null);
                g.setColor(color(estado[i]));                       // aro de color = estado
                g.setStroke(new BasicStroke(5f));
                g.draw(new Ellipse2D.Double(x - 42, y - 42, 84, 84));
            } else {
                g.setColor(color(estado[i]));
                g.fill(new Ellipse2D.Double(x - 32, y - 32, 64, 64));
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Double(x - 32, y - 32, 64, 64));
                g.setFont(new Font("SansSerif", Font.BOLD, 18));
                g.drawString("F" + i, (int) x - 11, (int) y + 6);
            }
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            String t = "F" + i + ": " + texto(estado[i]);
            int tw = g.getFontMetrics().stringWidth(t);
            g.setColor(new Color(255, 255, 255, 210));
            g.fillRoundRect((int) x - tw / 2 - 6, (int) y + 48, tw + 12, 20, 10, 10);
            g.setColor(Color.BLACK);
            g.drawString(t, (int) x - tw / 2, (int) y + 63);
        }
    }
}