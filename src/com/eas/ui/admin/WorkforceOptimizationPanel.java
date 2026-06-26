package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.service.SchedulingService;
import java.awt.*;
import java.sql.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

public class WorkforceOptimizationPanel extends JPanel {

    private JTable swapTable, empTable, historyTable;
    private DefaultTableModel swapModel, empModel, historyModel;
    private JTextField dateField = new JTextField("2026-06-26");
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

        // 1. Requests Tab (Fixed: Action column added)
        swapModel = new DefaultTableModel(new Object[]{"ID", "Emp ID", "Target ID", "Reason", "Status", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 5; }
        };
        swapTable = new JTable(swapModel);
        swapTable.getColumn("Action").setCellRenderer(new SwapButtonRenderer());
        swapTable.getColumn("Action").setCellEditor(new SwapButtonEditor(new JCheckBox()));
        tabbedPane.addTab("Requests", new JScrollPane(swapTable));

        // 2. Monitor Tab (Feature #10: Report button)
        JPanel monitorPanel = new JPanel(new BorderLayout());
        staffingStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        staffingStatusLabel.setHorizontalAlignment(JLabel.CENTER);
        JButton btnReport = new JButton("Generate Staffing Coverage Report");
        btnReport.addActionListener(e -> generateReport());
        monitorPanel.add(staffingStatusLabel, BorderLayout.CENTER);
        monitorPanel.add(btnReport, BorderLayout.SOUTH);
        tabbedPane.addTab("Monitor", monitorPanel);

        // 3. Assign Tab
        empModel = new DefaultTableModel(new Object[]{"ID", "Name", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 2; }
        };
        empTable = new JTable(empModel);
        empTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        empTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));
        tabbedPane.addTab("Assign", new JScrollPane(empTable));

        // 4. History Tab (Feature #9)
        historyModel = new DefaultTableModel(new Object[]{"Assign ID", "Old Emp", "New Emp", "Reason", "Admin", "Date"}, 0);
        historyTable = new JTable(historyModel);
        tabbedPane.addTab("History", new JScrollPane(historyTable));

        add(tabbedPane, BorderLayout.CENTER);
    }

    // --- Methods for Features ---

    private void generateReport() {
        String query = "SELECT s.shift_name, COUNT(sa.id) as filled, dsr.min_required_staff as required " +
                       "FROM department_staffing_requirements dsr " +
                       "LEFT JOIN shift_assignments sa ON dsr.shift_id = sa.shift_id " +
                       "LEFT JOIN shift_templates s ON dsr.shift_id = s.id " +
                       "GROUP BY dsr.shift_id";
        StringBuilder report = new StringBuilder("--- Staffing Coverage Report ---\n\n");
        try (Connection conn = Database.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                report.append("Shift: ").append(rs.getString("shift_name"))
                      .append(" | Filled: ").append(rs.getInt("filled"))
                      .append(" / Required: ").append(rs.getInt("required")).append("\n");
            }
            JOptionPane.showMessageDialog(this, report.toString());
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadHistory() {
        historyModel.setRowCount(0);
        String query = "SELECT shift_assignment_id, old_employee_id, new_employee_id, reason, changed_by_admin_id, change_date FROM schedule_history ORDER BY change_date DESC";
        try (Connection conn = Database.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                historyModel.addRow(new Object[]{rs.getInt("shift_assignment_id"), rs.getInt("old_employee_id"), rs.getInt("new_employee_id"), rs.getString("reason"), rs.getInt("changed_by_admin_id"), rs.getTimestamp("change_date")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void refreshData() {
        swapModel.setRowCount(0);
        empModel.setRowCount(0);
        loadSwapRequests();
        loadHistory();
        updateStaffingStatus();
        loadAvailableEmployees();
        checkStaffingLevels();
        this.revalidate();
        this.repaint();
    }

    // --- Button Handlers ---
    class SwapButtonRenderer extends JPanel implements TableCellRenderer {
        public SwapButtonRenderer() { setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0)); add(new JButton("Approve")); add(new JButton("Reject")); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) { return this; }
    }

    class SwapButtonEditor extends DefaultCellEditor {
        private JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        private JButton btnApprove = new JButton("Approve");
        private JButton btnReject = new JButton("Reject");
        public SwapButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            btnApprove.addActionListener(e -> { fireEditingStopped(); updateSwapStatus(true); });
            btnReject.addActionListener(e -> { fireEditingStopped(); updateSwapStatus(false); });
            panel.add(btnApprove); panel.add(btnReject);
        }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) { return panel; }
        public Object getCellEditorValue() { return "Action"; }
    }

    private void updateSwapStatus(boolean approved) {
    int row = swapTable.getSelectedRow();
    if (row == -1) return;
    int swapId = (int) swapModel.getValueAt(row, 0);
    
    // 1. I-update ang status ng swap
    String query = "UPDATE shift_swaps SET status = ? WHERE id = ?";
    try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
        ps.setString(1, approved ? "APPROVED" : "REJECTED");
        ps.setInt(2, swapId);
        ps.executeUpdate();
        
        
        if (approved) {
            String histQuery = "INSERT INTO schedule_history (shift_assignment_id, old_employee_id, new_employee_id, reason, changed_by_admin_id, change_date) VALUES (?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement psHist = conn.prepareStatement(histQuery)) {
                
                psHist.setInt(1, swapId); 
                psHist.setInt(2, (int) swapModel.getValueAt(row, 1)); // old_employee_id
                psHist.setInt(3, (int) swapModel.getValueAt(row, 2)); // new_employee_id
                psHist.setString(4, (String) swapModel.getValueAt(row, 3)); // reason
                psHist.setInt(5, currentAdminId); // changed_by_admin_id
                psHist.executeUpdate();
            }
        }
        
        
    } catch (SQLException e) { e.printStackTrace(); }
}
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) { setText("Assign"); return this; }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button = new JButton("Assign");
        public ButtonEditor(JCheckBox checkBox) { super(checkBox); button.addActionListener(e -> fireEditingStopped()); }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) { return button; }
        public Object getCellEditorValue() {
            int row = empTable.getSelectedRow();
            if (row != -1) {
                String empId = empModel.getValueAt(row, 0).toString();
                if (schedulingService.assignReliever(Integer.parseInt(empId), 1, dateField.getText())) {
                    refreshData();
                    JOptionPane.showMessageDialog(null, "Assigned successfully!");
                }
            }
            return "Assign";
        }
    }

    private void checkStaffingLevels() {
        String query = "SELECT dsr.shift_id FROM department_staffing_requirements dsr " +
                       "LEFT JOIN shift_assignments sa ON dsr.shift_id = sa.shift_id AND sa.effective_from = ? " +
                       "GROUP BY dsr.shift_id, dsr.min_required_staff HAVING COUNT(sa.id) < dsr.min_required_staff";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, dateField.getText());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "WARNING: Shift ID " + rs.getInt("shift_id") + " is understaffed!", "Staffing Alert", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadSwapRequests() {
        String query = "SELECT id, employee_id, target_employee_id, reason, status FROM shift_swaps WHERE status = 'PENDING'";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(query); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                swapModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("employee_id"), rs.getInt("target_employee_id"), rs.getString("reason"), rs.getString("status"), "Action"});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadAvailableEmployees() {
        List<String[]> employees = schedulingService.getAvailableEmployees(dateField.getText());
        for (String[] emp : employees) empModel.addRow(new Object[]{emp[0], emp[1], "Assign"});
    }

    private void updateStaffingStatus() {
        String query = "SELECT COUNT(*) FROM shift_assignments WHERE effective_from = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, dateField.getText());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) staffingStatusLabel.setText("STATUS: " + rs.getInt(1) + " active assignments.");
        } catch (SQLException e) { staffingStatusLabel.setText("STATUS: Connection Error"); }
    }
}