package view;

import controller.LoginController;
import model.LoginModel;
import model.RegisterModel;

import javax.swing.*;
import java.awt.*;

// Login and registration view — unified in view alongside DoctorView and PatientView.
public class LoginView extends JPanel {

    private final LoginController controller;
    private final MainFrameAccess mainFrame;

    // Login form fields
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;

    // Callback interface so LoginView can notify MainFrame after a successful login
    public interface MainFrameAccess {
        void onLoginSuccess(LoginModel model);
    }

    public LoginView(LoginController controller, MainFrameAccess mainFrame) {
        this.controller = controller;
        this.mainFrame  = mainFrame;
        setLayout(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        // Header banner
        JLabel titleLabel = new JLabel("WellnessLink", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(0, 123, 255));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(titleLabel, BorderLayout.NORTH);

        add(buildLoginPanel(), BorderLayout.CENTER);
    }

    // ── LOGIN PANEL ──

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 80, 30, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel subtitle = new JLabel("Sign In", SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(subtitle, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Username:"), gbc);
        loginUsernameField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(loginUsernameField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        loginPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        panel.add(loginPasswordField, gbc);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(0, 123, 255));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(loginBtn, gbc);

        JButton registerBtn = new JButton("Register");
        registerBtn.setBackground(new Color(40, 167, 69));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        gbc.gridy = 4;
        panel.add(registerBtn, gbc);

        loginBtn.addActionListener(e -> performLogin());
        registerBtn.addActionListener(e -> showRegisterWindow());

        return panel;
    }

    // ── REGISTER DIALOG ──

    private void showRegisterWindow() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Create Account - WellnessLink",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 420);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        JTextField regUsernameField  = new JTextField(20);
        JPasswordField regPasswordField = new JPasswordField(20);
        JTextField regNameField      = new JTextField(20);
        JTextField regContactField   = new JTextField(20);
        JComboBox<String> regRoleCombo = new JComboBox<>(new String[]{"patient", "doctor"});
        JTextField regSpecialtyField = new JTextField(20);
        JLabel specialtyLabel        = new JLabel("Specialty:");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel subtitle = new JLabel("Create Account", SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(subtitle, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        panel.add(regRoleCombo, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        panel.add(regNameField, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(regUsernameField, gbc);

        gbc.gridy = 4; gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        panel.add(regPasswordField, gbc);

        gbc.gridy = 5; gbc.gridx = 0;
        panel.add(new JLabel("Contact:"), gbc);
        gbc.gridx = 1;
        panel.add(regContactField, gbc);

        gbc.gridy = 6; gbc.gridx = 0;
        panel.add(specialtyLabel, gbc);
        gbc.gridx = 1;
        panel.add(regSpecialtyField, gbc);

        // Specialty only shown for doctors
        specialtyLabel.setVisible(false);
        regSpecialtyField.setVisible(false);

        regRoleCombo.addActionListener(e -> {
            boolean isDoctor = "doctor".equals(regRoleCombo.getSelectedItem());
            specialtyLabel.setVisible(isDoctor);
            regSpecialtyField.setVisible(isDoctor);
            panel.revalidate();
            panel.repaint();
        });

        JButton submitBtn = new JButton("Register");
        submitBtn.setBackground(new Color(40, 167, 69));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        gbc.gridy = 7; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(submitBtn, gbc);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFocusPainted(false);
        gbc.gridy = 8;
        panel.add(cancelBtn, gbc);

        submitBtn.addActionListener(e -> {
            String username  = regUsernameField.getText().trim();
            String password  = new String(regPasswordField.getPassword()).trim();
            String name      = regNameField.getText().trim();
            String contact   = regContactField.getText().trim();
            String role      = (String) regRoleCombo.getSelectedItem();
            String specialty = regSpecialtyField.getText().trim();

            if (username.isEmpty() || password.isEmpty() || name.isEmpty() || contact.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please fill in all required fields.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if ("doctor".equals(role) && specialty.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter your specialty.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            RegisterModel result = controller.register(username, password, role, name, contact, specialty);
            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(dialog, result.getMessage(),
                        "Registration Successful", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                // Pre-fill username so the user can sign in immediately
                loginUsernameField.setText(username);
                loginPasswordField.setText("");
                loginUsernameField.requestFocus();
            } else {
                JOptionPane.showMessageDialog(dialog,
                        result.getMessage() != null ? result.getMessage() : "Registration failed.",
                        "Registration Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.add(panel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // ── LOGIN ACTION ──

    private void performLogin() {
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LoginModel result = controller.login(username, password);
        if (result.isAuthenticated()) {
            mainFrame.onLoginSuccess(result);
        } else {
            JOptionPane.showMessageDialog(this,
                    result.getErrorMessage() != null ? result.getErrorMessage() : "Login failed.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

