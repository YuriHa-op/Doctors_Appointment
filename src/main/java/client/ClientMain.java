package client;

import main.MainFrame;
import net.ClinicService;

import javax.swing.*;
import java.rmi.Naming;

public class ClientMain {

    public static void main(String[] args) {
        try {
            String serverIp = JOptionPane.showInputDialog(
                    null,
                    "Enter server IP address:",
                    "Server Connection",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (serverIp == null) {
                return;
            }

            serverIp = serverIp.trim();
            if (serverIp.isEmpty()) {
                serverIp = "localhost";
            }

            ClinicService clinicService =
                    (ClinicService) Naming.lookup("rmi://" + serverIp + ":1099/ClinicService");

            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {}

                MainFrame frame = new MainFrame(clinicService);
                frame.showLogin();
                frame.setVisible(true);
            });
        } catch (Exception e) {
            System.err.println("Failed to connect to RMI server: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Cannot connect to server. Make sure the server is running.\n" + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
