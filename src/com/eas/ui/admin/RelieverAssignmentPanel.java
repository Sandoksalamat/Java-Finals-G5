package com.eas.ui.admin;

import com.eas.service.SchedulingService;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class RelieverAssignmentPanel extends JPanel {
    private JTable empTable;
    private DefaultTableModel empModel;
    private JTextField dateField = new JTextField("2026-06-25"); // Palitan ng dynamic date kung kailangan
    private SchedulingService schedulingService;

    public RelieverAssignmentPanel() {
        this.schedulingService = new SchedulingService();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // UI Setup
        empModel = new DefaultTableModel(new Object[]{"ID", "Name"}, 0);
        empTable = new JTable(empModel);
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Target Date (YYYY-MM-DD):"));
        topPanel.add(dateField);
        
        JButton searchBtn = new JButton("Find Available");
        searchBtn.addActionListener(e -> loadAvailableEmployees());
        topPanel.add(searchBtn);

        JButton assignBtn = new JButton("Assign as Reliever");
        assignBtn.addActionListener(e -> handleAssignment());

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(empTable), BorderLayout.CENTER);
        add(assignBtn, BorderLayout.SOUTH);
    }

    private void loadAvailableEmployees() {
        empModel.setRowCount(0);
        // Kinukuha natin ang listahan mula sa SchedulingService
        for (String[] emp : schedulingService.getAvailableEmployees(dateField.getText())) {
            empModel.addRow(emp);
        }
    }

    private void handleAssignment() {
        int row = empTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee first.");
            return;
        }

        int empId = Integer.parseInt(empModel.getValueAt(row, 0).toString());
        // Default shiftId as 1 for demonstration
        int shiftId = 1; 
        
        if (schedulingService.assignReliever(empId, shiftId, dateField.getText())) {
            JOptionPane.showMessageDialog(this, "Reliever successfully assigned!");
            loadAvailableEmployees(); // Refresh list
        } else {
            JOptionPane.showMessageDialog(this, "Assignment failed! Conflict detected.");
        }
    }
}