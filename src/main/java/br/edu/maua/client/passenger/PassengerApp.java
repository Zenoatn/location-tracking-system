package br.edu.maua.client.passenger;

import javax.swing.SwingUtilities;

public final class PassengerApp {
    private PassengerApp() { }
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new PassengerWindow().setVisible(true)); }
}
