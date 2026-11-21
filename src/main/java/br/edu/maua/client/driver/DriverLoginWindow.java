package br.edu.maua.client.driver;

import br.edu.maua.client.communication.ServerClient;
import br.edu.maua.common.protocol.Protocol;
import com.google.gson.JsonObject;
import java.awt.GridLayout;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public final class DriverLoginWindow extends JFrame {

    private final JTextField email = new JTextField(24);
    private final JPasswordField password = new JPasswordField(24);
    private final JButton login = new JButton("Entrar");

    public DriverLoginWindow() {

        super("Location Tracking — Colaborador");


        setDefaultCloseOperation(EXIT_ON_CLOSE);


        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));


        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 24, 20, 24));
        form.add(new JLabel("E-mail")); 
        form.add(email);
        form.add(new JLabel("Senha")); 
        form.add(password); 
        form.add(login);


        setContentPane(form); 
        
        pack(); 
        
        setLocationRelativeTo(null);


        login.addActionListener(event -> authenticate());


        getRootPane().setDefaultButton(login);
    }




    private void authenticate() {


        login.setEnabled(false);

        String userEmail = email.getText().trim();

        char[] secret = password.getPassword();


        password.setText("");


        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override 
            protected JsonObject doInBackground() throws Exception {
                JsonObject data = new JsonObject(); 

                data.addProperty("email", userEmail);
                data.addProperty("password", new String(secret));

                return new ServerClient().request(request(Protocol.LOGIN, data));
            }


            @Override 
            protected void done() {
                java.util.Arrays.fill(secret, '\0');
                login.setEnabled(true);
                try {
                    JsonObject response = get();

                    if (!Protocol.OK.equals(value(response, "status"))) {

                        JsonObject error = response.getAsJsonObject("error");

                        JOptionPane.showMessageDialog(DriverLoginWindow.this,
                            "Não foi possível entrar: " + error.get("message").getAsString(),
                            "Falha no login", JOptionPane.ERROR_MESSAGE); 
                                
                        return;
                    }



                    JsonObject session = response.getAsJsonObject("data");


                    if (!"DRIVER".equals(value(session, "role"))) {

                        JOptionPane.showMessageDialog(DriverLoginWindow.this,
                            "Esta conta não possui perfil de colaborador.", "Acesso negado", JOptionPane.ERROR_MESSAGE); 
                            
                        return;
                    }


                    new DriverWindow(session).setVisible(true); 
                    dispose();

                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(DriverLoginWindow.this,
                        "Falha de conexão com o servidor. Confira a conexão e tente novamente.",
                        "Erro de conexão", JOptionPane.ERROR_MESSAGE);
                }
                
            }

        };


        worker.execute();
    }



    static JsonObject request(String operation, JsonObject data) {

        JsonObject request = new JsonObject(); 
        request.addProperty("requestId", UUID.randomUUID().toString());
        request.addProperty("operation", operation); 
        request.add("data", data); 

        return request;
    }
    static String value(JsonObject object, String name) {
        
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : null;
    }
}
