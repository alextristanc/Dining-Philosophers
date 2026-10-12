import javax.swing.*;
import java.awt.*;

public class DiningPhilosophers {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Cena de los filósofos (Figura 6.13)");
            PanelMesa panel = new PanelMesa();
            PanelRegistro registro = new PanelRegistro();

            // Controles
            JToggleButton modo = new JToggleButton("Modo: automático");
            JButton siguiente = new JButton("Siguiente paso");
            siguiente.setEnabled(false);
            JButton ayuda = new JButton("Ayuda");
            JPanel barra = new JPanel();
            barra.add(modo);
            barra.add(siguiente);
            barra.add(ayuda);

            modo.addActionListener(e -> {
                boolean paso = modo.isSelected();
                CenaLogica.setModoPaso(paso);
                siguiente.setEnabled(paso);
                modo.setText(paso ? "Modo: paso a paso" : "Modo: automático");
            });
            siguiente.addActionListener(e -> CenaLogica.siguientePaso());
            ayuda.addActionListener(e -> new Tutorial(f, false).setVisible(true));

            f.setLayout(new BorderLayout());
            f.add(panel, BorderLayout.CENTER);
            f.add(registro, BorderLayout.EAST);
            f.add(barra, BorderLayout.SOUTH);
            f.setSize(1020, 700);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            // Conecta lógica con animación y registro
            CenaLogica.listener = new CenaLogica.Listener() {
                public void filosofo(int i, CenaLogica.Estado e) { panel.filosofo(i, e); }
                public void tenedor(int t, int d) { panel.tenedor(t, d); }
                public void mensaje(String texto) {
                    SwingUtilities.invokeLater(() -> registro.agregar(texto));
                }
            };

            new Tutorial(f, true).setVisible(true);   // tutorial: espera a que lo cierres
            CenaLogica.iniciar();                     // luego arranca la simulación
        });
    }
}