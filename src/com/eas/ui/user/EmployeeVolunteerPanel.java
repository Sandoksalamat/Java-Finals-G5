package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.service.SchedulingService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class EmployeeVolunteerPanel extends JPanel {

    private JTable openShiftTable;
    private DefaultTableModel tableModel;
    private SchedulingService schedulingService;
    private int currentUserId;

    public EmployeeVolunteerPanel(int userId) {
        this.currentUserId = userId;
        this.schedulingService = new SchedulingService();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        loadOpenShifts();
    }

    private void initUI() {
        JLabel title = new JLabel("Available Open Shifts", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 16));
        add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Shift ID", "Date", "Department ID"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        openShiftTable = new JTable(tableModel);
        add(new JScrollPane(openShiftTable), BorderLayout.CENTER);

        JButton volunteerBtn = new JButton("Volunteer for Selected Shift");
        volunteerBtn.addActionListener(e -> handleVolunteer());
        add(volunteerBtn, BorderLayout.SOUTH);
    }

    private void loadOpenShifts() {
        tableModel.setRowCount(0);
        String query = "SELECT dsr.shift_id, dsr.department_id, dsr.min_required_staff, " +
                       "(SELECT COUNT(*) FROM shift_assignments sa WHERE sa.shift_id = dsr.shift_id) as assigned_count " +
                       "FROM department_staffing_requirements dsr " +
                       "HAVING assigned_count < dsr.min_required_staff";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("shift_id"), 
                    "2026-06-26", 
                    rs.getInt("department_id")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleVolunteer() {
        int row = openShiftTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a shift from the table.");
            return;
        }

        int shiftId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        String date = tableModel.getValueAt(row, 1).toString();

        if (schedulingService.volunteerForShift(currentUserId, shiftId, date)) {
            JOptionPane.showMessageDialog(this, "Volunteer request sent successfully!");
            loadOpenShifts();
        } else {
            JOptionPane.showMessageDialog(this, "Conflict detected or request failed.");
        }
    }
}