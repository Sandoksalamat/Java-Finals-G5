package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.util.CsvExporter;
import com.eas.util.Dialogs;
import com.eas.util.UITheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class SafetyChecklistReviewPanel extends JPanel {

    private final UserSession session;

    // ── Top: per-employee completion summary ─────────────────────────────────
    private final JTable summaryTable = new JTable();
    private final JComboBox<String> deptFilter = new JComboBox<>(new String[]{
        "All Departments", "Office", "Laboratory", "Warehouse"
    });

    // ── Bottom: item-level detail for the selected employee/department ───────
    private final JTable detailTable = new JTable();

    public SafetyChecklistReviewPanel(UserSession session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildTopBar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            buildSummarySection(),
            buildDetailSection());
        split.setResizeWeight(0.55);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);

        loadSummary();
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.title("Occupational Safety Checklist Review"), BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(new JLabel("Department:"));
        right.add(deptFilter);

        JButton btnRefresh = UITheme.button("REFRESH");
        JButton btnExport   = UITheme.button("EXPORT SUMMARY CSV");

        deptFilter.addActionListener(e -> loadSummary());
        btnRefresh.addActionListener(e -> loadSummary());
        btnExport.addActionListener(e -> exportTable(summaryTable, "safety_checklist_summary.csv"));

        right.add(btnRefresh);
        right.add(btnExport);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    // ── Summary section ───────────────────────────────────────────────────────

    private JPanel buildSummarySection() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(
            "Employee Checklist Completion Summary"));

        summaryTable.setRowHeight(24);
        summaryTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        summaryTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        summaryTable.setAutoCreateRowSorter(true);
        summaryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Colour by completion: all checked = green, some checked = yellow, none = red
        summaryTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    int mr = t.convertRowIndexToModel(row);
                    Object checkedObj = t.getModel().getValueAt(mr, 4); // checked_count
                    Object totalObj   = t.getModel().getValueAt(mr, 5); // total_count
                    try {
                        int checked = Integer.parseInt(checkedObj.toString());
                        int total   = Integer.parseInt(totalObj.toString());
                        if (total == 0) {
                            setBackground(Color.WHITE);
                        } else if (checked == total) {
                            setBackground(new Color(220, 255, 220)); // green
                        } else if (checked == 0) {
                            setBackground(new Color(255, 224, 224)); // red
                        } else {
                            setBackground(new Color(255, 243, 200)); // yellow
                        }
                    } catch (Exception ex) {
                        setBackground(Color.WHITE);
                    }
                } else {
                    setBackground(t.getSelectionBackground());
                }
                return this;
            }
        });

        summaryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadDetailForSelection();
        });

        panel.add(new JScrollPane(summaryTable), BorderLayout.CENTER);
        return panel;
    }

    // ── Detail section ────────────────────────────────────────────────────────

    private JPanel buildDetailSection() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(
            "Item-Level Detail  —  select an employee row above"));

        detailTable.setRowHeight(22);
        detailTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        detailTable.setAutoCreateRowSorter(true);

        // Colour by checked state
        detailTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    int mr = t.convertRowIndexToModel(row);
                    Object cv = t.getModel().getValueAt(mr, 2); // is_checked col
                    String c  = cv == null ? "" : cv.toString();
                    setBackground("Yes".equals(c)
                        ? new Color(220, 255, 220)
                        : new Color(255, 224, 224));
                } else {
                    setBackground(t.getSelectionBackground());
                }
                return this;
            }
        });

        JButton btnExportDetail = UITheme.button("EXPORT DETAIL CSV");
        btnExportDetail.addActionListener(e -> exportTable(detailTable, "safety_checklist_detail.csv"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(btnExportDetail);

        panel.add(new JScrollPane(detailTable), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadSummary() {
        String dept = (String) deptFilter.getSelectedItem();
        boolean filtered = dept != null && !"All Departments".equals(dept);

        String base =
            "SELECT e.employee_no, u.full_name, sc.department_type, " +
            "       SUM(sc.is_checked) AS checked_count, " +
            "       COUNT(*) AS total_count, " +
            "       MAX(sc.checked_at) AS last_updated " +
            "FROM safety_checklist_records sc " +
            "JOIN employees e ON sc.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "GROUP BY e.id, sc.department_type " +
            "ORDER BY u.full_name, sc.department_type";

        String filteredSql =
            "SELECT e.employee_no, u.full_name, sc.department_type, " +
            "       SUM(sc.is_checked) AS checked_count, " +
            "       COUNT(*) AS total_count, " +
            "       MAX(sc.checked_at) AS last_updated " +
            "FROM safety_checklist_records sc " +
            "JOIN employees e ON sc.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "WHERE sc.department_type = ? " +
            "GROUP BY e.id, sc.department_type " +
            "ORDER BY u.full_name";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(filtered ? filteredSql : base)) {

            if (filtered) p.setString(1, dept);

            try (ResultSet rs = p.executeQuery()) {
                fillTable(summaryTable, rs, new String[]{
                    "Employee No", "Full Name", "Department",
                    "Checked Count", "Total Items", "Last Updated"
                });
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }

        fillTable(detailTable, null, new String[]{
            "Item Label", "Department", "Checked", "Last Updated"
        });
    }

    private void loadDetailForSelection() {
        int row = summaryTable.getSelectedRow();
        if (row < 0) return;
        int m = summaryTable.convertRowIndexToModel(row);
        DefaultTableModel dm = (DefaultTableModel) summaryTable.getModel();

        String employeeNo = safeStr(dm.getValueAt(m, 0));
        String department = safeStr(dm.getValueAt(m, 2));

        String sql =
            "SELECT sc.item_label, sc.department_type, sc.is_checked, sc.checked_at " +
            "FROM safety_checklist_records sc " +
            "JOIN employees e ON sc.employee_id = e.id " +
            "WHERE e.employee_no = ? AND sc.department_type = ? " +
            "ORDER BY sc.item_label";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, employeeNo);
            p.setString(2, department);

            try (ResultSet rs = p.executeQuery()) {
                fillDetailTable(rs);
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void fillDetailTable(ResultSet rs) {
        String[] headers = { "Item Label", "Department", "Checked", "Last Updated" };
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("item_label"),
                    rs.getString("department_type"),
                    rs.getBoolean("is_checked") ? "Yes" : "No",
                    rs.getTimestamp("checked_at")
                });
            }
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
        }

        detailTable.setModel(model);
    }

    // ── Generic table filler ──────────────────────────────────────────────────

    private void fillTable(JTable target, ResultSet rs, String[] headers) {
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (rs != null) {
            try {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 0; i < cols; i++) row[i] = rs.getObject(i + 1);
                    model.addRow(row);
                }
            } catch (SQLException ex) {
                Dialogs.error(this, ex.getMessage());
            }
        }

        target.setModel(model);
    }

    private String safeStr(Object o) { return o == null ? "" : o.toString(); }

    // ── CSV Export ────────────────────────────────────────────────────────────

    private void exportTable(JTable t, String filename) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(filename));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                CsvExporter.export(t, fc.getSelectedFile());
                Dialogs.info(this, "CSV exported.");
            } catch (Exception ex) {
                Dialogs.error(this, ex.getMessage());
            }
        }
    }
}