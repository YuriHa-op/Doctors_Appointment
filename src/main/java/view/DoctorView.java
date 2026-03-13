package view;

import client.ClinicCallbackImpl;
import controller.DoctorController;
import model.ActionResult;
import model.ScheduleModel;
import model.ScheduleResult;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DoctorView extends JPanel {

    private final DoctorController controller;
    private final String doctorId;
    private final NavigationHandler navigationHandler;
    private final ClinicCallbackImpl callbackImpl;
    private ClinicCallbackImpl.NotificationListener notificationListener;

    private JTable appointmentTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public DoctorView(String doctorId, DoctorController controller,
                      NavigationHandler navigationHandler, ClinicCallbackImpl callbackImpl) {
        this.controller = controller;
        this.doctorId = doctorId;
        this.navigationHandler = navigationHandler;
        this.callbackImpl = callbackImpl;
        initComponents();
        loadAppointments();
        registerCallback();
    }

    private void registerCallback() {
        if (callbackImpl != null) {
            notificationListener = (eventType, message) -> {
                // Auto-refresh schedule table and show notification
                loadAppointments();
                JOptionPane.showMessageDialog(DoctorView.this, message,
                        "Notification", JOptionPane.INFORMATION_MESSAGE);
            };
            callbackImpl.addListener(notificationListener);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(76, 175, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel headerLabel = new JLabel("Doctor's Appointment Schedule - Dr. " + doctorId);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);

        JButton bookedBtn = new JButton("Booked Appointments");
        bookedBtn.setFont(new Font("Arial", Font.BOLD, 13));
        bookedBtn.setBackground(new Color(255, 193, 7));
        bookedBtn.setForeground(Color.WHITE);
        bookedBtn.setFocusPainted(false);
        bookedBtn.setBorderPainted(false);
        bookedBtn.setPreferredSize(new Dimension(170, 38));
        bookedBtn.addActionListener(e -> showBookedSchedules());
        headerPanel.add(bookedBtn);

        // ── SEARCH BAR ──
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JLabel searchLabel = new JLabel("Search (patient name / date yyyy-MM-dd): ");
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        searchPanel.add(searchLabel, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.addActionListener(e -> performSearch());
        searchPanel.add(searchField, BorderLayout.CENTER);

        JButton searchButton = createStyledButton("Search", new Color(33, 150, 243));
        searchButton.setPreferredSize(new Dimension(100, 35));
        searchButton.addActionListener(e -> performSearch());
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Wrap header + search together
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"Appointment ID", "Patient Name", "Date", "Start Time", "End Time", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        appointmentTable = new JTable(tableModel);
        appointmentTable.setFont(new Font("Arial", Font.PLAIN, 13));
        appointmentTable.setRowHeight(25);
        appointmentTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        appointmentTable.getTableHeader().setBackground(new Color(200, 230, 201));

        JScrollPane scrollPane = new JScrollPane(appointmentTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton refreshButton = createStyledButton("Refresh", new Color(33, 150, 243));
        refreshButton.addActionListener(e -> loadAppointments());
        buttonPanel.add(refreshButton);

        JButton createButton = createStyledButton("Create Schedule", new Color(76, 175, 80));
        createButton.addActionListener(e -> createSchedule());
        buttonPanel.add(createButton);

        JButton updateStatusButton = createStyledButton("Update Status", new Color(255, 152, 0));
        updateStatusButton.addActionListener(e -> updateAppointmentStatus());
        buttonPanel.add(updateStatusButton);

        JButton cancelButton = createStyledButton("Cancel Appointment", new Color(244, 67, 54));
        cancelButton.addActionListener(e -> cancelAppointment());
        buttonPanel.add(cancelButton);

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

    private void performSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadAppointments();
            return;
        }
        tableModel.setRowCount(0);
        ScheduleResult searchResult = controller.searchAppointments(doctorId, keyword);
        if (!searchResult.isSuccess()) {
            JOptionPane.showMessageDialog(this, searchResult.getMessage(), "Search", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<ScheduleModel> list = searchResult.getSchedules();
        for (ScheduleModel s : list) {
            tableModel.addRow(new Object[]{
                    s.getId(),
                    displayPatient(s),
                    s.getDate(),
                    s.getStartTime(),
                    s.getEndTime(),
                    s.getStatus()
            });
        }
    }

    private void loadAppointments() {
        tableModel.setRowCount(0);
        ScheduleResult loadResult = controller.getSchedulesByDoctor(doctorId);
        if (!loadResult.isSuccess()) {
            JOptionPane.showMessageDialog(this, loadResult.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<ScheduleModel> list = loadResult.getSchedules();
        for (ScheduleModel s : list) {
            tableModel.addRow(new Object[]{
                    s.getId(),
                    displayPatient(s),
                    s.getDate(),
                    s.getStartTime(),
                    s.getEndTime(),
                    s.getStatus()
            });
        }
    }

    // ── CREATE SCHEDULE DIALOG ──

    private void createSchedule() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Year
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Year:"), gbc);
        JSpinner yearSpinner = new JSpinner(
                new SpinnerNumberModel(LocalDate.now().getYear(), 2024, 2030, 1));
        JSpinner.NumberEditor yearEditor = new JSpinner.NumberEditor(yearSpinner, "#");
        yearSpinner.setEditor(yearEditor);
        gbc.gridx = 1;
        panel.add(yearSpinner, gbc);

        // Month
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Month:"), gbc);
        JSpinner monthSpinner = new JSpinner(
                new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        gbc.gridx = 1;
        panel.add(monthSpinner, gbc);

        // Time slots
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(new JLabel("Select time slots:"), gbc);

        String[] defaultSlots = {
                "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
                "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
        };

        JCheckBox[] slotBoxes = new JCheckBox[defaultSlots.length];
        for (int i = 0; i < defaultSlots.length; i++) {
            slotBoxes[i] = new JCheckBox(defaultSlots[i], true);
            gbc.gridy = 3 + i; gbc.gridwidth = 2;
            panel.add(slotBoxes[i], gbc);
        }

        // Include Saturday
        JCheckBox saturdayBox = new JCheckBox("Include Saturday", false);
        gbc.gridy = 3 + defaultSlots.length; gbc.gridwidth = 2;
        panel.add(saturdayBox, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Create Monthly Schedule", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            int year = (int) yearSpinner.getValue();
            int month = (int) monthSpinner.getValue();

            // Prevent creating schedules for past months
            LocalDate selectedMonth = LocalDate.of(year, month, 1);
            LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
            if (selectedMonth.isBefore(currentMonth)) {
                JOptionPane.showMessageDialog(this,
                        "Cannot create a schedule for a past month.",
                        "Invalid Date", JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<String> selectedSlots = new ArrayList<>();
            for (int i = 0; i < defaultSlots.length; i++) {
                if (slotBoxes[i].isSelected()) {
                    selectedSlots.add(defaultSlots[i]);
                }
            }

            if (selectedSlots.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one time slot.",
                        "No Slots", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ActionResult createResult = controller.createMonthlySchedule(
                    doctorId, year, month, selectedSlots, saturdayBox.isSelected());
            JOptionPane.showMessageDialog(this, createResult.getMessage(),
                    "Create Schedule", JOptionPane.INFORMATION_MESSAGE);
            loadAppointments();
        }
    }

    // ── UPDATE STATUS / TIME ──

    private void updateAppointmentStatus() {
        int selectedRow = appointmentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an appointment first",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String scheduleId = (String) tableModel.getValueAt(selectedRow, 0);
        String date = (String) tableModel.getValueAt(selectedRow, 2);
        String currentStart = (String) tableModel.getValueAt(selectedRow, 3);
        String currentEnd = (String) tableModel.getValueAt(selectedRow, 4);
        String status = (String) tableModel.getValueAt(selectedRow, 5);

        String[] options = {"Update Time", "Half Day", "Off Day"};
        int choice = JOptionPane.showOptionDialog(this,
                "What would you like to do for " + date + "?",
                "Update Schedule",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            // Update time for the selected slot
            ActionResult validation = controller.validateSlotTimeUpdate(status);
            if (!validation.isSuccess()) {
                JOptionPane.showMessageDialog(this,
                        validation.getMessage(),
                        "Cannot Update", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JPanel timePanel = new JPanel(new GridLayout(2, 2, 5, 5));
            JTextField startField = new JTextField(currentStart);
            JTextField endField = new JTextField(currentEnd);
            timePanel.add(new JLabel("Start Time (HH:MM):"));
            timePanel.add(startField);
            timePanel.add(new JLabel("End Time (HH:MM):"));
            timePanel.add(endField);

            int res = JOptionPane.showConfirmDialog(this, timePanel,
                    "Update Slot Time", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.OK_OPTION) {
                String newStart = startField.getText().trim();
                String newEnd = endField.getText().trim();
                if (newStart.isEmpty() || newEnd.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Both times are required.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ActionResult updateResult = controller.updateSlotTime(doctorId, scheduleId, newStart, newEnd);
                JOptionPane.showMessageDialog(this, updateResult.getMessage(),
                        "Update", JOptionPane.INFORMATION_MESSAGE);
                loadAppointments();
            }
        } else if (choice == 1) {
            ActionResult overrideResult1 = controller.overrideDay(doctorId, date, "halfday");
            JOptionPane.showMessageDialog(this, overrideResult1.getMessage(),
                    "Half Day", JOptionPane.INFORMATION_MESSAGE);
            loadAppointments();
        } else if (choice == 2) {
            ActionResult overrideResult2 = controller.overrideDay(doctorId, date, "offday");
            JOptionPane.showMessageDialog(this, overrideResult2.getMessage(),
                    "Off Day", JOptionPane.INFORMATION_MESSAGE);
            loadAppointments();
        }
    }

    // ── CANCEL SINGLE APPOINTMENT

    private void cancelAppointment() {
        int selectedRow = appointmentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an appointment to cancel",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String scheduleId = (String) tableModel.getValueAt(selectedRow, 0);
        String status = (String) tableModel.getValueAt(selectedRow, 5);

        ActionResult validation = controller.validateSlotDeletion(status);
        if (!validation.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                    validation.getMessage(),
                    "Cannot Cancel",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel this appointment (" + scheduleId + ")?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            ActionResult deleteResult = controller.deleteSlotById(doctorId, scheduleId);
            JOptionPane.showMessageDialog(this,
                    deleteResult.getMessage(),
                    "Cancelled",
                    JOptionPane.INFORMATION_MESSAGE);
            loadAppointments();
        }
    }

    // BOOKED/RESCHEDULED APPOINTMENTS

    private void showBookedSchedules() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Booked Appointments - Dr. " + doctorId, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(750, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(5, 5));

        String[] cols = {"ID", "Patient Name", "Date", "Start Time", "End Time", "Status"};
        DefaultTableModel bookedModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable bookedTable = new JTable(bookedModel);
        bookedTable.setFont(new Font("Arial", Font.PLAIN, 13));
        bookedTable.setRowHeight(26);
        bookedTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        bookedTable.getTableHeader().setBackground(new Color(200, 230, 201));
        bookedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Refresh helper
        Runnable refreshBooked = () -> {
            bookedModel.setRowCount(0);
            ScheduleResult bookedResult = controller.getSchedulesByDoctor(doctorId);
            if (bookedResult.isSuccess()) {
                List<ScheduleModel> list = bookedResult.getSchedules();
                for (ScheduleModel s : list) {
                    if ("Booked".equals(s.getStatus()) || "Rescheduled".equals(s.getStatus())) {
                        bookedModel.addRow(new Object[]{
                                s.getId(),
                                displayPatient(s),
                                s.getDate(),
                                s.getStartTime(),
                                s.getEndTime(),
                                s.getStatus()
                        });
                    }
                }
            }
        };
        refreshBooked.run();

        dialog.add(new JScrollPane(bookedTable), BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));

        JButton updateBtn = createStyledButton("Update Time", new Color(255, 152, 0));
        updateBtn.addActionListener(e -> {
            int sel = bookedTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Select an appointment to update.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String sid = (String) bookedModel.getValueAt(sel, 0);
            String status = (String) bookedModel.getValueAt(sel, 5);
            if (!("Booked".equals(status) || "Rescheduled".equals(status))) {
                JOptionPane.showMessageDialog(dialog,
                        "Only booked or rescheduled appointments can be updated.",
                        "Cannot Update", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String curStart = (String) bookedModel.getValueAt(sel, 3);
            String curEnd = (String) bookedModel.getValueAt(sel, 4);

            JPanel timePanel = new JPanel(new GridLayout(2, 2, 5, 5));
            JTextField startField = new JTextField(curStart);
            JTextField endField = new JTextField(curEnd);
            timePanel.add(new JLabel("Start Time (HH:MM):"));
            timePanel.add(startField);
            timePanel.add(new JLabel("End Time (HH:MM):"));
            timePanel.add(endField);

            int res = JOptionPane.showConfirmDialog(dialog, timePanel,
                    "Update Slot Time", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.OK_OPTION) {
                String ns = startField.getText().trim();
                String ne = endField.getText().trim();
                if (ns.isEmpty() || ne.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Both times are required.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ActionResult updateResult2 = controller.updateSlotTime(doctorId, sid, ns, ne);
                JOptionPane.showMessageDialog(dialog, updateResult2.getMessage(),
                        "Update", JOptionPane.INFORMATION_MESSAGE);
                refreshBooked.run();
                loadAppointments();
            }
        });
        btnPanel.add(updateBtn);

        JButton cancelBtn = createStyledButton("Cancel Appointment", new Color(244, 67, 54));
        cancelBtn.addActionListener(e -> {
            int sel = bookedTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Select an appointment to cancel.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String sid = (String) bookedModel.getValueAt(sel, 0);
                String status = (String) bookedModel.getValueAt(sel, 5);
                if (!("Booked".equals(status) || "Rescheduled".equals(status))) {
                JOptionPane.showMessageDialog(dialog,
                    "Only booked or rescheduled appointments can be cancelled.",
                    "Cannot Cancel", JOptionPane.WARNING_MESSAGE);
                return;
                }
            int confirm2 = JOptionPane.showConfirmDialog(dialog,
                    "Are you sure you want to cancel appointment " + sid + "?",
                    "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm2 == JOptionPane.YES_OPTION) {
                ActionResult deleteResult2 = controller.deleteSlotById(doctorId, sid);
                JOptionPane.showMessageDialog(dialog, deleteResult2.getMessage(),
                        "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                refreshBooked.run();
                loadAppointments();
            }
        });
        btnPanel.add(cancelBtn);

        JButton refreshBtn = createStyledButton("Refresh", new Color(33, 150, 243));
        refreshBtn.addActionListener(e -> refreshBooked.run());
        btnPanel.add(refreshBtn);

        JButton closeBtn = createStyledButton("Close", new Color(96, 125, 139));
        closeBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(closeBtn);

        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private String displayPatient(ScheduleModel schedule) {
        String patientName = schedule.getPatientName();
        if (patientName != null && !patientName.trim().isEmpty()) {
            return patientName;
        }
        String patientId = schedule.getPatientId();
        return patientId != null ? patientId : "";
    }
}