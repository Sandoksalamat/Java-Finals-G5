package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.service.SchedulingService;
import java.awt.*;
import java.sql.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

public class WorkforceOptimizationPanel extends JPanel {

    private JTable swapTable, empTable;
    private DefaultTableModel swapModel, empModel;
    private JTextField dateField = new JTextField("2026-06-25");
    private JLabel staffingStatusLabel = new JLabel("STATUS: Initializing...");
    private int currentAdminId;
    private SchedulingService schedulingService;

    public WorkforceOptimizationPanel(int adminId) {
        this.currentAdminId = adminId;
        this.schedulingService = new SchedulingService();
        setLayout(new BorderLayout());
        initUI();
        refreshData();
    }

    private void initUI() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel swapPanel = new JPanel(new BorderLayout(5, 5));
        swapModel = new DefaultTableModel(new Object[]{"ID", "Emp ID", "Target ID", "Reason", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        swapTable = new JTable(swapModel);
        swapPanel.add(new JScrollPane(swapTable), BorderLayout.CENTER);

        JPanel monitorPanel = new JPanel(new BorderLayout());
        staffingStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        staffingStatusLabel.setHorizontalAlignment(JLabel.CENTER);
        monitorPanel.add(staffingStatusLabel, BorderLayout.CENTER);
        
        JPanel relieverPanel = new JPanel(new BorderLayout(5, 5));
        empModel = new DefaultTableModel(new Object[]{"ID", "Name", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 2; }
        };
        empTable = new JTable(empModel);
        empTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        empTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));
        
        relieverPanel.add(new JScrollPane(empTable), BorderLayout.CENTER);
        
        tabbedPane.addTab("Requests", swapPanel);
        tabbedPane.addTab("Monitor", monitorPanel);
        tabbedPane.addTab("Assign", relieverPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Assign");
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("Assign");
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            button.setText("Assign");
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            int row = empTable.getSelectedRow();
            if (row != -1) {
                String empId = empModel.getValueAt(row, 0).toString();
                if (schedulingService.assignReliever(Integer.parseInt(empId), 1, dateField.getText())) {
                    refreshData();
                    JOptionPane.showMessageDialog(null, "Employee " + empId + " assigned successfully!");
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to assign employee.");
                }
            }
            return "Assign";
        }
    }

    public void refreshData() {
        swapModel.setRowCount(0);
        empModel.setRowCount(0);
        loadSwapRequests();
        updateStaffingStatus();
        loadAvailableEmployees();
        this.revalidate();
        this.repaint();
    }

    private void loadSwapRequests() {
        String query = "SELECT id, employee_id, target_employee_id, reason, status FROM shift_swaps WHERE status = 'PENDING'";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                swapModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("employee_id"), rs.getInt("target_employee_id"), rs.getString("reason"), rs.getString("status")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadAvailableEmployees() {
        List<String[]> employees = schedulingService.getAvailableEmployees(dateField.getText());
        for (String[] emp : employees) {
            empModel.addRow(new Object[]{emp[0], emp[1], "Assign"});
        }
    }

    private void updateStaffingStatus() {
   
    String query = "SELECT COUNT(*) FROM shift_assignments WHERE effective_from = ?";
    try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
        ps.setString(1, dateField.getText());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            staffingStatusLabel.setText("STATUS: " + rs.getInt(1) + " active assignments.");
        }
    } catch (SQLException e) { 
        staffingStatusLabel.setText("STATUS: Connection Error"); 
    }
}
}