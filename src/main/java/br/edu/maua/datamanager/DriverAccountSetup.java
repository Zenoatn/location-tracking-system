package br.edu.maua.datamanager;

import br.edu.maua.common.security.CryptoUtil;
import br.edu.maua.datamanager.dao.UserDao;
import java.awt.GridLayout;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Administrative Swing form for adding a driver account. */
public class DriverAccountSetup {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DriverAccountSetup::showForm);
    }

    private static void showForm() {
        JTextField name = new JTextField(24);
        JTextField email = new JTextField(24);
        JPasswordField password = new JPasswordField(24);
        JPasswordField confirmation = new JPasswordField(24);
        JPanel fields = new JPanel(new GridLayout(0, 1, 6, 6));
        fields.add(new JLabel("Nome do motorista"));
        fields.add(name);
        fields.add(new JLabel("E-mail"));
        fields.add(email);
        fields.add(new JLabel("Senha (mínimo 10 caracteres)"));
        fields.add(password);
        fields.add(new JLabel("Confirme a senha"));
        fields.add(confirmation);

        int choice = JOptionPane.showConfirmDialog(null, fields,
                "Criar conta de motorista", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            password.setText("");
            confirmation.setText("");
            return;
        }

        char[] secret = password.getPassword();
        char[] repeated = confirmation.getPassword();
        password.setText("");
        confirmation.setText("");
        try {
            String cleanName = name.getText().trim();
            String cleanEmail = email.getText().trim();
            if (cleanName.isBlank() || cleanName.length() > 120
                    || cleanEmail.isBlank() || cleanEmail.length() > 150
                    || !cleanEmail.contains("@")) {
                throw new IllegalArgumentException("Informe nome e e-mail válidos.");
            }
            if (secret.length < 10 || !Arrays.equals(secret, repeated)) {
                throw new IllegalArgumentException("As senhas devem coincidir e ter ao menos 10 caracteres.");
            }
            String hash = CryptoUtil.hashPassword(secret);
            new UserDao().createDriver(cleanName, cleanEmail, hash);
            JOptionPane.showMessageDialog(null, "Conta criada com segurança.",
                    "Cadastro concluído", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(),
                    "Não foi possível criar a conta", JOptionPane.ERROR_MESSAGE);
        } catch (Exception exception) {
            exception.printStackTrace();

            JOptionPane.showMessageDialog(null,
            "   Erro ao salvar: " + exception.getMessage(),
            "   Não foi possível criar a conta", JOptionPane.ERROR_MESSAGE);
} finally {
            Arrays.fill(secret, '\0');
            Arrays.fill(repeated, '\0');
        }
    }
}
