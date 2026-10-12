import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/** Registro de eventos con estilo de pergamino griego. Solo muestra texto; no toca la lógica. */
public class PanelRegistro extends JPanel {

    private static final Color PERGAMINO = new Color(245, 230, 200);
    private static final Color MARRON = new Color(92, 58, 30);
    private static final Color DORADO = new Color(230, 190, 90);

    private final JTextPane area = new JTextPane();
    private final StyledDocument doc = area.getStyledDocument();
    private int contador = 0;

    public PanelRegistro() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(330, 0));
        setBackground(MARRON);

        // Encabezado
        JLabel titulo = new JLabel("\u03A6  Pergamino de los filósofos  \u03A6", SwingConstants.CENTER);
        titulo.setOpaque(true);
        titulo.setBackground(MARRON);
        titulo.setForeground(DORADO);
        titulo.setFont(new Font(Font.SERIF, Font.BOLD, 17));
        titulo.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        add(titulo, BorderLayout.NORTH);

        // Pergamino
        area.setEditable(false);
        area.setBackground(PERGAMINO);
        area.setMargin(new Insets(8, 10, 8, 10));
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createMatteBorder(3, 3, 3, 3, DORADO));
        add(sp, BorderLayout.CENTER);

        // Pie con botón limpiar
        JButton limpiar = new JButton("Limpiar pergamino");
        limpiar.setFont(new Font(Font.SERIF, Font.BOLD, 13));
        limpiar.setBackground(DORADO);
        limpiar.setForeground(MARRON);
        limpiar.setFocusPainted(false);
        limpiar.addActionListener(e -> limpiar());
        JPanel pie = new JPanel();
        pie.setBackground(MARRON);
        pie.add(limpiar);
        add(pie, BorderLayout.SOUTH);
    }

    public void limpiar() {
        try { doc.remove(0, doc.getLength()); } catch (BadLocationException e) { }
        contador = 0;
    }

    /** Debe llamarse desde el hilo de Swing. */
    public void agregar(String texto) {
        contador++;

        SimpleAttributeSet num = new SimpleAttributeSet();
        StyleConstants.setFontFamily(num, Font.MONOSPACED);
        StyleConstants.setFontSize(num, 11);
        StyleConstants.setForeground(num, new Color(150, 120, 80));

        SimpleAttributeSet at = new SimpleAttributeSet();
        StyleConstants.setFontFamily(at, Font.SERIF);
        StyleConstants.setFontSize(at, 14);
        Color c = MARRON;
        if (texto.contains("entró")) {
            c = new Color(120, 60, 140);                       // morado: entra al comedor
        } else if (texto.contains("wait(")) {
            c = new Color(190, 110, 20);                       // naranja: pide recurso
        } else if (texto.contains("signal(")) {
            c = new Color(40, 90, 160);                        // azul: libera recurso
        } else if (texto.contains("tomó")) {
            c = new Color(30, 120, 60);                        // verde: obtuvo tenedor
        } else if (texto.endsWith(" come")) {
            c = new Color(20, 100, 40);
            StyleConstants.setBold(at, true);                  // come
        } else if (texto.endsWith(" piensa")) {
            c = new Color(110, 110, 110);
            StyleConstants.setItalic(at, true);                // piensa
        }
        StyleConstants.setForeground(at, c);

        try {
            doc.insertString(doc.getLength(), String.format("%03d  ", contador), num);
            doc.insertString(doc.getLength(), texto + "\n", at);
            if (doc.getLength() > 30000) {                     // evita que crezca sin límite
                String t = doc.getText(0, doc.getLength());
                doc.remove(0, t.indexOf('\n', 10000) + 1);
            }
        } catch (BadLocationException e) { }
        area.setCaretPosition(doc.getLength());
    }
}