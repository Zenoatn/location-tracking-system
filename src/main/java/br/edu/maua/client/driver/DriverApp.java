package br.edu.maua.client.driver;

import javax.swing.SwingUtilities;

public final class DriverApp {
    private DriverApp() { }
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new DriverLoginWindow().setVisible(true)); }
}
