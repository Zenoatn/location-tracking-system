package br.edu.maua.client;

import br.edu.maua.client.interfaces.MainWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ClientApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Usa o tema padrão do Swing se o tema do sistema não estiver disponível.
            }
            new MainWindow().setVisible(true);
        });
    }
}
