package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.service.SchedulingService;
import java.awt.*;
import java.sql.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

public class WorkforceOptimizationPanel extends JPanel {

    private JTable swapTable, empTable, historyTable, openTable, availTable;
    private DefaultTableModel swapModel, empModel, historyModel, openModel, availModel;
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
        initRequestsTab(tabbedPane);
        initMonitorTab(tabbedPane);
        initAssignTab(tabbedPane);
        initHistoryTab(tabbedPane);
        initAvailabilityTab(tabbedPane);
        initOpenShiftsTab(tabbedPane);
        tabbedPane.addTab("Report", new StaffingCoverageReportPanel());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initRequestsTab(JTabbedPane tp) {
        swapModel = new DefaultTableModel(new Object[]{"ID", "Emp ID", "Target ID", "Reason", "Approve", "Reject"}, 0);
        swapTable = new JTable(swapModel);
        swapTable.getColumn("Approve").setCellRenderer(new ButtonRenderer());
        swapTable.getColumn("Approve").setCellEditor(new RequestEditor(new JCheckBox(), "APPROVED"));
        swapTable.getColumn("Reject").setCellRenderer(new ButtonRenderer());
        swapTable.getColumn("Reject").setCellEditor(new RequestEditor(new JCheckBox(), "REJECTED"));
        tp.addTab("Requests", new JScrollPane(swapTable));
    }

    private void initAssignTab(JTabbedPane tp) {
        empModel = new DefaultTableModel(new Object[]{"ID", "Name", "Status", "Assign"}, 0);
        empTable = new JTable(empModel);
        empTable.getColumn("Assign").setCellRenderer(new ButtonRenderer());
        empTable.getColumn("Assign").setCellEditor(new AssignEditor(new JCheckBox()));
        tp.addTab("Assign", new JScrollPane(empTable));
    }

    private void initMonitorTab(JTabbedPane tp) {
        tp.addTab("Monitor", new JLabel("System Ready", JLabel.CENTER));
    }

    private void initHistoryTab(JTabbedPane tp) {
        historyModel = new DefaultTableModel(new Object[]{"ID", "Old", "New", "Reason", "Date"}, 0);
        historyTable = new JTable(historyModel);
        tp.addTab("History", new JScrollPane(historyTable));
    }

    private void initAvailabilityTab(JTabbedPane tp) {
        availModel = new DefaultTableModel(new Object[]{"Emp ID", "Date", "Status"}, 0);
        availTable = new JTable(availModel);
        tp.addTab("Availability", new JScrollPane(availTable));
    }

    private void initOpenShiftsTab(JTabbedPane tp) {
        openModel = new DefaultTableModel(new Object[]{"ID", "Shift ID", "Date"}, 0);
        openTable = new JTable(openModel);
        tp.addTab("Open Shifts", new JScrollPane(openTable));
    }

    private void checkStaffingLevels() {
        String query = "SELECT dsr.shift_id, dsr.department_id, dsr.min_required_staff, " +
                       "(SELECT COUNT(*) FROM shift_assignments sa WHERE sa.shift_id = dsr.shift_id) as assigned_count " +
                       "FROM department_staffing_requirements dsr " +
                       "HAVING assigned_count < dsr.min_required_staff";

        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            StringBuilder warnings = new StringBuilder();
            while (rs.next()) {
                warnings.append("Dept: ").append(rs.getInt("department_id"))
                        .append(" | Shift: ").append(rs.getInt("shift_id"))
                        .append(" (Required: ").append(rs.getInt("min_required_staff"))
                        .append(", Assigned: ").append(rs.getInt("assigned_count")).append(")\n");
            }

            if (warnings.length() > 0) {
                JOptionPane.showMessageDialog(this, 
                    "WARNING: Insufficient staffing detected:\n" + warnings.toString(), 
                    "Staffing Alert", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public class StaffingCoverageReportPanel extends JPanel {
        private JTable reportTable;
        private DefaultTableModel reportModel;

        public StaffingCoverageReportPanel() {
            setLayout(new BorderLayout());
            reportModel = new DefaultTableModel(new Object[]{"Shift ID", "Date", "Status", "Assigned Staff"}, 0);
            reportTable = new JTable(reportModel);
            add(new JScrollPane(reportTable), BorderLayout.CENTER);
            
            JButton refreshBtn = new JButton("Generate Report");
            refreshBtn.addActionListener(e -> generateReport());
            add(refreshBtn, BorderLayout.SOUTH);
        }

        private void generateReport() {
            reportModel.setRowCount(0);
            String query = "SELECT s.shift_id, s.shift_date, " +
                           "(SELECT COUNT(*) FROM shift_assignments sa WHERE sa.shift_id = s.shift_id) as assigned " +
                           "FROM open_shifts s";
            
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                
                while (rs.next()) {
                    int assigned = rs.getInt("assigned");
                    String status = (assigned > 0) ? "FILLED" : "VACANT";
                    reportModel.addRow(new Object[]{
                        rs.getInt("shift_id"), 
                        rs.getString("shift_date"), 
                        status, 
                        assigned
                    });
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            setText(t.getColumnName(c));
            return this;
        }
    }

    class RequestEditor extends DefaultCellEditor {
        private JButton btn = new JButton();
        private String status;
        public RequestEditor(JCheckBox c, String status) { super(c); this.status = status;
            btn.addActionListener(e -> {
                int id = (int) swapModel.getValueAt(swapTable.getSelectedRow(), 0);
                try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE shift_swaps SET status=? WHERE id=?")) {
                    ps.setString(1, status); ps.setInt(2, id); ps.executeUpdate(); refreshData();
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        }
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            btn.setText(t.getColumnName(c));
            return btn;
        }
    }

    class AssignEditor extends DefaultCellEditor {
        private JButton btn = new JButton("Assign");
        public AssignEditor(JCheckBox c) { super(c);
            btn.addActionListener(e -> JOptionPane.showMessageDialog(null, "Assign logic active"));
        }
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) { return btn; }
    }

    public void refreshData() {
        swapModel.setRowCount(0); empModel.setRowCount(0); 
        historyModel.setRowCount(0); openModel.setRowCount(0); availModel.setRowCount(0);

        try (Connection conn = Database.getConnection()) {
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM shift_swaps")) {
                while (rs.next()) swapModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("employee_id"), rs.getInt("target_employee_id"), rs.getString("reason"), "Approve", "Reject"});
            }
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM employees")) {
                while (rs.next()) empModel.addRow(new Object[]{rs.getInt("id"), rs.getString("employee_no"), rs.getString("employment_status"), "Assign"});
            }
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM schedule_history")) {
                while (rs.next()) historyModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("old_employee_id"), rs.getInt("new_employee_id"), rs.getString("reason"), rs.getString("change_date")});
            }
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM employee_availability")) {
                while (rs.next()) availModel.addRow(new Object[]{rs.getInt("employee_id"), rs.getString("unavailable_date"), rs.getString("status")});
            }
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM open_shifts")) {
                while (rs.next()) openModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("shift_id"), rs.getString("shift_date")});
            }
            checkStaffingLevels();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}