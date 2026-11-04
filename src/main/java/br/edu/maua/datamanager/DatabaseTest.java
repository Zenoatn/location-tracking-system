package br.edu.maua.datamanager;

import java.sql.Connection;

public class DatabaseTest {

    public static void main(String[] args) {

        try (Connection connection =
                DatabaseConnection.getConnection()) {

            System.out.println("MySQL conectado com sucesso!");
            System.out.println("Banco: " + connection.getCatalog());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
