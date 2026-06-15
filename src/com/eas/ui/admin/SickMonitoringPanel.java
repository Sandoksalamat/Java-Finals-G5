package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.CsvExporter;
import com.eas.util.Dialogs;
import com.eas.util.UITheme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class SickMonitoringPanel extends JPanel {

    private final UserSession session;

    private final JTable     table       = new JTable();
    private final JTextField searchField = new JTextField(20);

    private final JTextField idField          = new JTextField(6);
    private final JTextField employeeIdField  = new JTextField(10);
    private final JTextField reportDateField  = new JTextField(10);
    private final JTextField diagnosisField   = new JTextField(20);
    private final JTextField doctorField      = new JTextField(20);
    private final JTextArea  recommendArea    = new JTextArea(3, 20);
    private final JTextField recoveryField    = new JTextField(5);
    private final JTextField statusField      = new JTextField(10);
    private final JTextField remarksField     = new JTextField(20);

    public SickMonitoringPanel(UserSession session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildTable(),     BorderLayout.CENTER);
        add(buildFormPanel(), BorderLayout.SOUTH);

        clearForm(); // Establishes clean base baseline values (e.g. Current ISO Date)
        load("");
    }

    // ── Top Bar ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.title("Sick Employee Monitoring"), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(new JLabel("Search:"));
        actions.add(searchField);

        JButton btnSearch  = UITheme.button("SEARCH");
        JButton btnRefresh = UITheme.button("REFRESH");
        JButton btnExport  = UITheme.button("EXPORT CSV");

        btnSearch.addActionListener(e -> load(searchField.getText()));
        btnRefresh.addActionListener(e -> { searchField.setText(""); load(""); });
        btnExport.addActionListener(e -> export());

        actions.add(btnSearch);
        actions.add(btnRefresh);
        actions.add(btnExport);
        top.add(actions, BorderLayout.EAST);

        return top;
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    private JScrollPane buildTable() {
        table.setRowHeight(24);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setAutoCreateRowSorter(true);

        // Row colour: red = ACTIVE, green = CLEARED/RECOVERED
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    int modelRow = t.convertRowIndexToModel(row);
                    Object statusVal = t.getModel().getValueAt(modelRow, 9);
                    String status = statusVal == null ? "" : statusVal.toString();
                    if ("ACTIVE".equalsIgnoreCase(status)) {
                        setBackground(new Color(255, 224, 224));
                    } else if ("CLEARED".equalsIgnoreCase(status) || "RECOVERED".equalsIgnoreCase(status)) {
                        setBackground(new Color(220, 255, 220));
                    } else {
                        setBackground(Color.WHITE);
                    }
                } else {
                    setBackground(t.getSelectionBackground());
                }
                return this;
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateForm();
        });

        return new JScrollPane(table);
    }

    // ── Form ──────────────────────────────────────────────────────────────────

    private JPanel buildFormPanel() {
        idField.setEditable(false);
        recommendArea.setLineWrap(true);
        recommendArea.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Sick Record Entry / Edit"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets   = new Insets(4, 6, 4, 6);
        gbc.fill     = GridBagConstraints.HORIZONTAL;
        gbc.anchor   = GridBagConstraints.WEST;

        // Row 0 & 1 Layout Distribution
        addFormField(form, gbc, 0, "ID",             idField);
        addFormField(form, gbc, 1, "Employee ID",     employeeIdField);
        addFormField(form, gbc, 2, "Report Date",     reportDateField);
        addFormField(form, gbc, 3, "Diagnosis",       diagnosisField);
        addFormField(form, gbc, 4, "Doctor Name",     doctorField);

        // Recommendation Multi-Line Field Handling
        gbc.gridx = 5; 
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        form.add(new JLabel("Doctor Recommendation"), gbc);
        
        gbc.gridy = 1;
        JScrollPane recScroll = new JScrollPane(recommendArea);
        recScroll.setPreferredSize(new Dimension(200, 50));
        form.add(recScroll, gbc);

        addFormField(form, gbc, 6, "Recovery Days", recoveryField);
        addFormField(form, gbc, 7, "Status",        statusField);
        addFormField(form, gbc, 8, "Admin Remarks", remarksField);

        // Action Operational Triggers
        JButton btnAdd     = UITheme.button("ADD");
        JButton btnUpdate  = UITheme.button("UPDATE");
        JButton btnDelete  = UITheme.button("DELETE");
        JButton btnCleared = UITheme.button("MARK CLEARED");
        JButton btnClear   = UITheme.button("CLEAR FORM");

        btnAdd.addActionListener(e     -> insertRecord());
        btnUpdate.addActionListener(e  -> updateRecord());
        btnDelete.addActionListener(e  -> deleteRecord());
        btnCleared.addActionListener(e -> markCleared());
        btnClear.addActionListener(e   -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnCleared);
        buttons.add(btnClear);

        // Reset tracking flags inside explicit component bounds
        gbc.gridx = 0; 
        gbc.gridy = 2; 
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 6, 4, 6);
        form.add(buttons, gbc);

        JPanel outer = new JPanel(new BorderLayout(4, 4));
        outer.add(form, BorderLayout.CENTER);

        JLabel hint = new JLabel(
            "  Status: ACTIVE | RECOVERING | CLEARED | EXTENDED    " +
            "  Date format: YYYY-MM-DD    " +
            "  RED = currently sick    GREEN = cleared/recovered"
        );
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        outer.add(hint, BorderLayout.SOUTH);

        return outer;
    }

    private void addFormField(JPanel p, GridBagConstraints g, int col, String label, JComponent field) {
        g.gridwidth = 1;
        g.gridheight = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        
        g.gridx = col;
        g.gridy = 0;
        p.add(new JLabel(label), g);
        
        g.gridy = 1;
        p.add(field, g);
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    private void load(String key) {
        boolean searching = key != null && !key.trim().isEmpty();

        String baseSql =
            "SELECT sr.id, sr.employee_id, u.full_name, e.employee_no, " +
            "       sr.report_date, sr.diagnosis, sr.doctor_name, " +
            "       sr.recommendation, sr.recovery_days, sr.status, " +
            "       sr.expected_return, sr.admin_remarks, sr.created_at " +
            "FROM sick_records sr " +
            "JOIN employees e ON sr.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "ORDER BY sr.report_date DESC";

        String searchSql =
            "SELECT sr.id, sr.employee_id, u.full_name, e.employee_no, " +
            "       sr.report_date, sr.diagnosis, sr.doctor_name, " +
            "       sr.recommendation, sr.recovery_days, sr.status, " +
            "       sr.expected_return, sr.admin_remarks, sr.created_at " +
            "FROM sick_records sr " +
            "JOIN employees e ON sr.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "WHERE u.full_name LIKE ? OR e.employee_no LIKE ? " +
            "   OR sr.diagnosis LIKE ? OR sr.doctor_name LIKE ? OR sr.status LIKE ? " +
            "ORDER BY sr.report_date DESC";

        String sql = searching ? searchSql : baseSql;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            if (searching) {
                String q = "%" + key.trim() + "%";
                for (int i = 1; i <= 5; i++) p.setString(i, q);
            }

            try (ResultSet rs = p.executeQuery()) {
                loadIntoTable(rs);
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void loadIntoTable(ResultSet rs) throws SQLException {
        String[] headers = {
            "ID", "Emp ID", "Full Name", "Emp No", "Report Date",
            "Diagnosis", "Doctor", "Recommendation", "Recovery Days",
            "Status", "Expected Return", "Admin Remarks", "Created At"
        };

        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        while (rs.next()) {
            Object[] row = new Object[cols];
            for (int i = 0; i < cols; i++) row[i] = rs.getObject(i + 1);
            model.addRow(row);
        }

        table.setModel(model);
        applyExpectedReturnRenderer();
    }

    private void applyExpectedReturnRenderer() {
        if (table.getColumnCount() < 11) return;
        table.getColumnModel().getColumn(10).setCellRenderer(
            new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                    super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                    if (!sel && val != null) {
                        try {
                            LocalDate ret   = LocalDate.parse(val.toString());
                            long daysLeft   = ChronoUnit.DAYS.between(LocalDate.now(), ret);
                            if (daysLeft > 0) {
                                setText(val + " (" + daysLeft + " days left)");
                                setForeground(Color.RED);
                            } else {
                                setForeground(new Color(0, 128, 0));
                            }
                        } catch (Exception ignored) {
                            setForeground(Color.BLACK);
                        }
                    }
                    return this;
                }
            }
        );
    }

    // ── Form Population ───────────────────────────────────────────────────────

    private void populateForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int m = table.convertRowIndexToModel(row);
        DefaultTableModel dm = (DefaultTableModel) table.getModel();

        idField.setText(         safeStr(dm.getValueAt(m, 0)));
        employeeIdField.setText( safeStr(dm.getValueAt(m, 1)));
        reportDateField.setText( safeStr(dm.getValueAt(m, 4)));
        diagnosisField.setText(  safeStr(dm.getValueAt(m, 5)));
        doctorField.setText(     safeStr(dm.getValueAt(m, 6)));
        recommendArea.setText(   safeStr(dm.getValueAt(m, 7)));
        recoveryField.setText(   safeStr(dm.getValueAt(m, 8)));
        statusField.setText(     safeStr(dm.getValueAt(m, 9)));
        remarksField.setText(    safeStr(dm.getValueAt(m, 11)));
    }

    private String safeStr(Object o) { return o == null ? "" : o.toString(); }

    // ── CRUD Operations ───────────────────────────────────────────────────────

    private void insertRecord() {
        if (employeeIdField.getText().trim().isEmpty() || recoveryField.getText().trim().isEmpty()) {
            Dialogs.error(this, "Employee ID and Recovery Days fields cannot be blank.");
            return;
        }

        String sql =
            "INSERT INTO sick_records " +
            "(employee_id, report_date, diagnosis, doctor_name, " +
            " recommendation, recovery_days, status, admin_remarks) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1,    Integer.parseInt(employeeIdField.getText().trim()));
            p.setString(2, reportDateField.getText().trim());
            p.setString(3, diagnosisField.getText().trim());
            p.setString(4, doctorField.getText().trim());
            p.setString(5, recommendArea.getText().trim());
            p.setInt(6,    Integer.parseInt(recoveryField.getText().trim()));
            p.setString(7, statusField.getText().trim().isEmpty() ? "ACTIVE" : statusField.getText().trim());
            p.setString(8, remarksField.getText().trim());

            p.executeUpdate();
            AuditService.log(session.getId(), "CREATE", "SICK_MONITORING",
                "Added sick record for employee #" + employeeIdField.getText().trim());
            clearForm();
            load("");
            Dialogs.info(this, "Sick record added successfully.");

        } catch (NumberFormatException nfe) {
            Dialogs.error(this, "Employee ID and Recovery Days must be numeric integers.");
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void updateRecord() {
        if (idField.getText().isEmpty()) {
            Dialogs.error(this, "Select a record from the table first.");
            return;
        }
        String sql =
            "UPDATE sick_records SET " +
            "employee_id=?, report_date=?, diagnosis=?, doctor_name=?, " +
            "recommendation=?, recovery_days=?, status=?, admin_remarks=? " +
            "WHERE id=?";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1,    Integer.parseInt(employeeIdField.getText().trim()));
            p.setString(2, reportDateField.getText().trim());
            p.setString(3, diagnosisField.getText().trim());
            p.setString(4, doctorField.getText().trim());
            p.setString(5, recommendArea.getText().trim());
            p.setInt(6,    Integer.parseInt(recoveryField.getText().trim()));
            p.setString(7, statusField.getText().trim());
            p.setString(8, remarksField.getText().trim());
            p.setInt(9,    Integer.parseInt(idField.getText().trim()));

            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "SICK_MONITORING",
                "Updated sick record #" + idField.getText());
            clearForm();
            load("");
            Dialogs.info(this, "Sick record updated.");

        } catch (NumberFormatException nfe) {
            Dialogs.error(this, "Employee ID and Recovery Days must be numeric integers.");
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void deleteRecord() {
        if (idField.getText().isEmpty()) {
            Dialogs.error(this, "Select a record to delete.");
            return;
        }
        if (!Dialogs.confirm(this, "Delete sick record #" + idField.getText() + "?")) return;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM sick_records WHERE id=?")) {

            p.setInt(1, Integer.parseInt(idField.getText().trim()));
            p.executeUpdate();
            AuditService.log(session.getId(), "DELETE", "SICK_MONITORING",
                "Deleted sick record #" + idField.getText());
            clearForm();
            load("");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void markCleared() {
        if (idField.getText().isEmpty()) {
            Dialogs.error(this, "Select a record to mark as cleared.");
            return;
        }
        if (!Dialogs.confirm(this, "Mark sick record #" + idField.getText() + " as CLEARED?")) return;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE sick_records SET status='CLEARED' WHERE id=?")) {

            p.setInt(1, Integer.parseInt(idField.getText().trim()));
            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "SICK_MONITORING",
                "Marked sick record #" + idField.getText() + " as CLEARED.");
            clearForm();
            load("");
            Dialogs.info(this, "Record marked as CLEARED.");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void clearForm() {
        idField.setText("");
        employeeIdField.setText("");
        reportDateField.setText(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        diagnosisField.setText("");
        doctorField.setText("");
        recommendArea.setText("");
        recoveryField.setText("");
        statusField.setText("ACTIVE");
        remarksField.setText("");
        table.clearSelection();
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    private void export() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("sick_monitoring.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                CsvExporter.export(table, fc.getSelectedFile());
                Dialogs.info(this, "CSV exported successfully.");
            } catch (Exception ex) {
                Dialogs.error(this, ex.getMessage());
            }
        }
    }
}