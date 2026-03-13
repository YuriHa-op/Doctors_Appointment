package main;

import client.ClinicCallbackImpl;
import controller.DoctorController;
import controller.PatientController;
import controller.LoginController;
import model.LoginModel;
import net.ClinicService;
import view.DoctorView;
import view.LoginView;
import view.NavigationHandler;
import view.PatientView;

import javax.swing.*;
import java.awt.*;

/**
 * Main application frame that manages navigation between Login and Dashboard views.
 * Also manages the RMI callback lifecycle for real-time server notifications.
 */
public class MainFrame extends JFrame implements LoginView.MainFrameAccess, NavigationHandler {

    private static final String LOGIN_VIEW = "LOGIN";
    private static final String DOCTOR_DASHBOARD = "DOCTOR_DASHBOARD";
    private static final String PATIENT_DASHBOARD = "PATIENT_DASHBOARD";

    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final ClinicService clinicService;

    // Login MVC
    private final LoginController loginController;
    private final LoginView loginView;

    // Current user info
    private String currentUserId;
    private String currentUserRole;

    // Callback for server notifications
    private ClinicCallbackImpl callbackImpl;

    public MainFrame(ClinicService clinicService) {
        this.clinicService = clinicService;

        setTitle("WellnessLink - Clinic Appointment Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 550);
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Login MVC
        loginController = new LoginController(clinicService);
        loginView = new LoginView(loginController, this);

        mainPanel.add(loginView, LOGIN_VIEW);
        add(mainPanel);
    }

    /** Called by ClientMain after construction */
    public void showLogin() {
        cardLayout.show(mainPanel, LOGIN_VIEW);
        setTitle("WellnessLink - Login");
    }

    // LoginView.MainFrameAccess callback
    @Override
    public void onLoginSuccess(LoginModel model) {
        currentUserId = model.getUserId();
        currentUserRole = model.getRole();

        // Create and register callback for real-time notifications
        try {
            callbackImpl = new ClinicCallbackImpl();
            clinicService.registerCallback(currentUserId, callbackImpl);
        } catch (Exception e) {
            System.err.println("[MainFrame] Failed to register callback: " + e.getMessage());
            callbackImpl = null;
        }

        if ("doctor".equalsIgnoreCase(currentUserRole)) {
            showDoctorDashboard();
        } else {
            showPatientDashboard();
        }
    }

    private void showDoctorDashboard() {
        DoctorController dc = new DoctorController(clinicService);
        DoctorView dv = new DoctorView(currentUserId, dc, this, callbackImpl);
        mainPanel.add(dv, DOCTOR_DASHBOARD);
        cardLayout.show(mainPanel, DOCTOR_DASHBOARD);
        setTitle("WellnessLink - Doctor Dashboard");
        setSize(1000, 650);
        setResizable(true);
        setLocationRelativeTo(null);
    }

    private void showPatientDashboard() {
        PatientController pc = new PatientController(clinicService);
        PatientView pv = new PatientView(currentUserId, pc, this, callbackImpl);
        mainPanel.add(pv, PATIENT_DASHBOARD);
        cardLayout.show(mainPanel, PATIENT_DASHBOARD);
        setTitle("WellnessLink - Patient Dashboard");
        setSize(1000, 650);
        setResizable(true);
        setLocationRelativeTo(null);
    }

    public void logout() {
        // Unregister callback before logging out
        if (currentUserId != null && callbackImpl != null) {
            try {
                clinicService.unregisterCallback(currentUserId);
            } catch (Exception e) {
                System.err.println("[MainFrame] Failed to unregister callback: " + e.getMessage());
            }
            callbackImpl = null;
        }

        currentUserId = null;
        currentUserRole = null;
        setSize(500, 550);
        setResizable(false);
        setLocationRelativeTo(null);
        showLogin();
    }
}
