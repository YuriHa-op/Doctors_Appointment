package view;

import client.ClinicCallbackImpl;
import controller.PatientController;
import model.ActionResult;
import model.ScheduleModel;
import model.ScheduleResult;
import model.UserListResult;
import model.UserModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PatientView extends JPanel {

    private final PatientController controller;
    private final String patientId;
    private final NavigationHandler navigationHandler;
    private final ClinicCallbackImpl callbackImpl;
    private ClinicCallbackImpl.NotificationListener notificationListener;

    // Doctor list
    private JTable doctorTable;
    private DefaultTableModel doctorTableModel;
    private JTextField searchField;

    // Cached full doctor list for local filtering
    private List<UserModel> allDoctors = new ArrayList<>();

    public PatientView(String patientId, PatientController controller,
                       NavigationHandler navigationHandler, ClinicCallbackImpl callbackImpl) {
        this.controller = controller;
        this.patientId = patientId;
        this.navigationHandler = navigationHandler;
        this.callbackImpl = callbackImpl;
        initComponents();
        loadDoctors();
        registerCallback();
    }

    private void registerCallback() {
        if (callbackImpl != null) {
            notificationListener = (eventType, message) -> {
                // Auto-refresh doctor list and show notification
                loadDoctors();
                JOptionPane.showMessageDialog(PatientView.this, message,
                        "Notification", JOptionPane.INFORMATION_MESSAGE);
            };
            callbackImpl.addListener(notificationListener);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        //HEADER
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(33, 150, 243));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel headerLabel = new JLabel("Patient Dashboard - " + patientId);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);

        add(headerPanel, BorderLayout.NORTH);

        // ─SEARCH BAR
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JLabel searchLabel = new JLabel("Search Doctor (name / specialty): ");
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        searchPanel.add(searchLabel, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.addActionListener(e -> filterDoctors()); // Enter key also triggers search
        searchPanel.add(searchField, BorderLayout.CENTER);

        JButton searchButton = createStyledButton("Search", new Color(33, 150, 243));
        searchButton.setPreferredSize(new Dimension(100, 35));
        searchButton.addActionListener(e -> filterDoctors());
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Wrap header + search together
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // DOCTOR TABLE 
        String[] columns = {"Doctor ID", "Name", "Specialty", "Contact"};
        doctorTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        doctorTable = new JTable(doctorTableModel);
        doctorTable.setFont(new Font("Arial", Font.PLAIN, 13));
        doctorTable.setRowHeight(28);
        doctorTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        doctorTable.getTableHeader().setBackground(new Color(187, 222, 251));
        doctorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Double-click a doctor row 
        doctorTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && doctorTable.getSelectedRow() != -1) {
                    showDoctorSlots();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(doctorTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Doctors (double-click to view available slots)"));
        add(scrollPane, BorderLayout.CENTER);

        //BUTTON PANEL 
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton refreshButton = createStyledButton("Refresh", new Color(33, 150, 243));
        refreshButton.addActionListener(e -> loadDoctors());
        buttonPanel.add(refreshButton);

        JButton viewSlotsButton = createStyledButton("View Slots", new Color(76, 175, 80));
        viewSlotsButton.addActionListener(e -> showDoctorSlots());
        buttonPanel.add(viewSlotsButton);

        JButton myApptsButton = createStyledButton("My Appointments", new Color(255, 152, 0));
        myApptsButton.addActionListener(e -> showMyAppointments());
        buttonPanel.add(myApptsButton);

        JButton logoutButton = createStyledButton("Logout", new Color(96, 125, 139));
        logoutButton.addActionListener(e -> navigationHandler.logout());
        buttonPanel.add(logoutButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(160, 40));
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        return button;
    }

    //  LOAD & FILTER DOCTORS
    private void loadDoctors() {
        UserListResult doctorsResult = controller.getDoctors();
        allDoctors.clear();
        if (doctorsResult.isSuccess()) {
            allDoctors = doctorsResult.getUsers();
        }
        filterDoctors();
    }

    private void filterDoctors() {
        String query = searchField.getText().trim().toLowerCase();
        doctorTableModel.setRowCount(0);
        for (UserModel d : allDoctors) {
            String name = d.getName() != null ? d.getName().toLowerCase() : "";
            String specialty = d.getSpecialty() != null ? d.getSpecialty().toLowerCase() : "";
            if (query.isEmpty() || name.contains(query) || specialty.contains(query)) {
                doctorTableModel.addRow(new Object[]{
                        d.getId(),
                        d.getName(),
                        d.getSpecialty(),
                        d.getContact()
                });
            }
        }
    }

    //  VIEW DOCTOR SLOTS
    private void showDoctorSlots() {
        int row = doctorTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a doctor first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String doctorId = (String) doctorTableModel.getValueAt(row, 0);
        String doctorName = (String) doctorTableModel.getValueAt(row, 1);

        ScheduleResult slotsResult = controller.getAvailableSlots(doctorId);
        if (!slotsResult.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load slots: " + slotsResult.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<ScheduleModel> slots = slotsResult.getSchedules();
        if (slots.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No available slots for Dr. " + doctorName + ".",
                    "No Slots", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Build a dialog with a table of available slots
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Available Slots - Dr. " + doctorName, true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // Slot table
        String[] cols = {"Slot ID", "Date", "Start Time", "End Time"};
        DefaultTableModel slotModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (ScheduleModel s : slots) {
            slotModel.addRow(new Object[]{
                    s.getId(),
                    s.getDate(),
                    s.getStartTime(),
                    s.getEndTime()
            });
        }
        JTable slotTable = new JTable(slotModel);
        slotTable.setFont(new Font("Arial", Font.PLAIN, 13));
        slotTable.setRowHeight(25);
        slotTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        slotTable.getTableHeader().setBackground(new Color(200, 230, 201));
        slotTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        dialog.add(new JScrollPane(slotTable), BorderLayout.CENTER);

        // Book button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton bookBtn = createStyledButton("Book Selected", new Color(76, 175, 80));
        bookBtn.addActionListener(e -> {
            int sel = slotTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Select a slot to book.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String scheduleId = (String) slotModel.getValueAt(sel, 0);
            String date = (String) slotModel.getValueAt(sel, 1);
            String time = slotModel.getValueAt(sel, 2) + " - " + slotModel.getValueAt(sel, 3);

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Book appointment with Dr. " + doctorName + "\n"
                            + "Date: " + date + "\nTime: " + time + "\n\nConfirm?",
                    "Confirm Booking", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                ActionResult bookResult = controller.bookSlot(patientId, scheduleId);
                JOptionPane.showMessageDialog(dialog, bookResult.getMessage(),
                        "Book", JOptionPane.INFORMATION_MESSAGE);
                if (bookResult.isSuccess()) {
                    // Remove booked row from slot table
                    slotModel.removeRow(sel);
                }
            }
        });
        btnPanel.add(bookBtn);

        JButton closeBtn = createStyledButton("Close", new Color(96, 125, 139));
        closeBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(closeBtn);

        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    //  MY APPOINTMENTS WINDOW
    private void showMyAppointments() {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "My Appointments", true);
        dialog.setSize(750, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // Header
        JPanel hdr = new JPanel();
        hdr.setBackground(new Color(33, 150, 243));
        hdr.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel hdrLbl = new JLabel("My Appointments");
        hdrLbl.setFont(new Font("Arial", Font.BOLD, 18));
        hdrLbl.setForeground(Color.WHITE);
        hdr.add(hdrLbl);
        dialog.add(hdr, BorderLayout.NORTH);

        // Appointment table
        String[] cols = {"Appointment ID", "Doctor ID", "Doctor Name", "Date",
                "Start Time", "End Time", "Status"};
        DefaultTableModel apptModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable apptTable = new JTable(apptModel);
        apptTable.setFont(new Font("Arial", Font.PLAIN, 13));
        apptTable.setRowHeight(25);
        apptTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        apptTable.getTableHeader().setBackground(new Color(187, 222, 251));
        apptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Load appointments into table
        Runnable refreshAppts = () -> {
            apptModel.setRowCount(0);
            ScheduleResult apptResult = controller.getPatientSchedules(patientId);
            if (apptResult.isSuccess()) {
                List<ScheduleModel> list = apptResult.getSchedules();
                for (ScheduleModel s : list) {
                    apptModel.addRow(new Object[]{
                            s.getId(),
                            s.getDoctorId(),
                            s.getDoctorName(),
                            s.getDate(),
                            s.getStartTime(),
                            s.getEndTime(),
                            s.getStatus()
                    });
                }
            }
        };
        refreshAppts.run();

        dialog.add(new JScrollPane(apptTable), BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));

        JButton reschedBtn = createStyledButton("Reschedule", new Color(255, 152, 0));
        reschedBtn.addActionListener(e -> {
            int sel = apptTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Select an appointment to reschedule.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String oldId = (String) apptModel.getValueAt(sel, 0);
            String docId = (String) apptModel.getValueAt(sel, 1);
            String status = (String) apptModel.getValueAt(sel, 6);
            if (!("Booked".equals(status) || "Rescheduled".equals(status))) {
                JOptionPane.showMessageDialog(dialog,
                        "Only booked or rescheduled appointments can be rescheduled.",
                        "Cannot Reschedule", JOptionPane.WARNING_MESSAGE);
                return;
            }
            rescheduleFromDialog(dialog, oldId, docId);
            refreshAppts.run();
        });
        btnPanel.add(reschedBtn);

        JButton cancelBtn = createStyledButton("Cancel Appointment", new Color(244, 67, 54));
        cancelBtn.addActionListener(e -> {
            int sel = apptTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Select an appointment to cancel.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String scheduleId = (String) apptModel.getValueAt(sel, 0);
                String status = (String) apptModel.getValueAt(sel, 6);
                if (!("Booked".equals(status) || "Rescheduled".equals(status))) {
                JOptionPane.showMessageDialog(dialog,
                    "Only booked or rescheduled appointments can be cancelled.",
                    "Cannot Cancel", JOptionPane.WARNING_MESSAGE);
                return;
                }
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Cancel appointment " + scheduleId + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                ActionResult cancelResult = controller.cancelBooking(patientId, scheduleId);
                JOptionPane.showMessageDialog(dialog, cancelResult.getMessage(),
                        "Cancel", JOptionPane.INFORMATION_MESSAGE);
                refreshAppts.run();
            }
        });
        btnPanel.add(cancelBtn);

        JButton refreshBtn = createStyledButton("Refresh", new Color(33, 150, 243));
        refreshBtn.addActionListener(e -> refreshAppts.run());
        btnPanel.add(refreshBtn);

        JButton closeBtn = createStyledButton("Close", new Color(96, 125, 139));
        closeBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(closeBtn);

        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // Reschedule helper called from the My Appointments dialog
    private void rescheduleFromDialog(JDialog parent, String oldScheduleId, String doctorId) {
        ScheduleResult slotsResult2 = controller.getAvailableSlots(doctorId);
        if (!slotsResult2.isSuccess()) {
            JOptionPane.showMessageDialog(parent,
                    "Failed to load slots: " + slotsResult2.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<ScheduleModel> slots = slotsResult2.getSchedules();
        if (slots.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "No available slots for this doctor.",
                    "No Slots", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] descriptions = new String[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            ScheduleModel s = slots.get(i);
            descriptions[i] = s.getDate() + "  "
                    + s.getStartTime() + " - "
                    + s.getEndTime()
                    + "  [" + s.getId() + "]";
        }

        String selected = (String) JOptionPane.showInputDialog(parent,
                "Select a new time slot:",
                "Reschedule",
                JOptionPane.QUESTION_MESSAGE,
                null, descriptions, descriptions[0]);
        if (selected == null) return;

        int bs = selected.lastIndexOf("[");
        int be = selected.lastIndexOf("]");
        String newId = selected.substring(bs + 1, be);

        ActionResult rescheduleResult = controller.reschedule(patientId, oldScheduleId, newId);
        JOptionPane.showMessageDialog(parent, rescheduleResult.getMessage(),
                "Reschedule", JOptionPane.INFORMATION_MESSAGE);
    }
}
