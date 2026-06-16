package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.CsvExporter;
import com.eas.util.Dialogs;
import com.eas.util.UITheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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

public class ReturnToWorkPanel extends JPanel {

    private final UserSession session;

    // ── Table ─────────────────────────────────────────────────────────────────
    private final JTable      table       = new JTable();
    private final JTextField  searchField = new JTextField(20);

    // ── Form fields ───────────────────────────────────────────────────────────
    private final JTextField  idField          = new JTextField(5);
    private final JTextField  employeeIdField  = new JTextField(8);
    private final JTextField  sickRecordIdField = new JTextField(8);
    private final JTextField  clearanceDateField = new JTextField(10);
    private final JTextField  physicianField   = new JTextField(22);
    private final JTextField  facilityField    = new JTextField(22);
    private final JCheckBox   fitBox           = new JCheckBox("Fit to Work", true);
    private final JComboBox<String> statusBox  = new JComboBox<>(new String[]{
        "PENDING", "APPROVED", "REJECTED"
    });
    private final JTextArea   restrictionsArea = new JTextArea(2, 22);
    private final JTextArea   remarksArea      = new JTextArea(2, 22);

    public ReturnToWorkPanel(UserSession session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildTable(),     BorderLayout.CENTER);
        add(buildForm(),      BorderLayout.SOUTH);

        load("");
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.title("Return-to-Work Clearances"), BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(new JLabel("Search:"));
        right.add(searchField);

        JButton btnSearch  = UITheme.button("SEARCH");
        JButton btnRefresh = UITheme.button("REFRESH");
        JButton btnExport  = UITheme.button("EXPORT CSV");

        btnSearch.addActionListener(e  -> load(searchField.getText()));
        btnRefresh.addActionListener(e -> { searchField.setText(""); load(""); });
        btnExport.addActionListener(e  -> export());

        right.add(btnSearch);
        right.add(btnRefresh);
        right.add(btnExport);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    private JScrollPane buildTable() {
        table.setRowHeight(24);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setAutoCreateRowSorter(true);

        // Colour by status
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    int mr = t.convertRowIndexToModel(row);
                    Object sv = t.getModel().getValueAt(mr, 11); // status col
                    String s  = sv == null ? "" : sv.toString();
                    switch (s) {
                        case "APPROVED": setBackground(new Color(220, 255, 220)); break; // green
                        case "REJECTED": setBackground(new Color(255, 224, 224)); break; // red
                        default:         setBackground(new Color(255, 243, 200));         // yellow = PENDING
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

    private JPanel buildForm() {
        idField.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Clearance Entry / Edit"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets    = new Insets(3, 5, 3, 5);
        g.fill      = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 1;

        addField(form, g, 0, "ID",               idField);
        addField(form, g, 1, "Employee ID",       employeeIdField);
        addField(form, g, 2, "Sick Record ID",    sickRecordIdField);
        addField(form, g, 3, "Clearance Date",    clearanceDateField);
        addField(form, g, 4, "Physician",         physicianField);
        addField(form, g, 5, "Medical Facility",  facilityField);

        // Fit-to-work checkbox
        g.gridx = 6; g.gridy = 0;
        form.add(new JLabel("Fit to Work"), g);
        g.gridy = 1;
        form.add(fitBox, g);

        addField(form, g, 7, "Status", statusBox);

        // Restrictions text area
        g.gridx = 8; g.gridy = 0;
        form.add(new JLabel("Work Restrictions"), g);
        g.gridy = 1;
        form.add(new JScrollPane(restrictionsArea), g);

        // Remarks text area
        g.gridx = 9; g.gridy = 0;
        form.add(new JLabel("Admin Remarks"), g);
        g.gridy = 1;
        form.add(new JScrollPane(remarksArea), g);

        // Buttons
        JButton btnAdd     = UITheme.button("ADD");
        JButton btnUpdate  = UITheme.button("UPDATE");
        JButton btnApprove = UITheme.button("APPROVE");
        JButton btnReject  = UITheme.button("REJECT");
        JButton btnDelete  = UITheme.button("DELETE");
        JButton btnClear   = UITheme.button("CLEAR");

        btnAdd.addActionListener(e     -> insertRecord());
        btnUpdate.addActionListener(e  -> updateRecord());
        btnApprove.addActionListener(e -> setStatus("APPROVED"));
        btnReject.addActionListener(e  -> setStatus("REJECTED"));
        btnDelete.addActionListener(e  -> deleteRecord());
        btnClear.addActionListener(e   -> clearForm());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnAdd);
        btns.add(btnUpdate);
        btns.add(btnApprove);
        btns.add(btnReject);
        btns.add(btnDelete);
        btns.add(btnClear);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 11;
        form.add(btns, g);

        // Hint
        JLabel hint = new JLabel(
            "  GREEN = Approved    RED = Rejected    YELLOW = Pending    " +
            "Sick Record ID is optional — link to a sick_records row if applicable."
        );
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        g.gridy = 3;
        form.add(hint, g);

        return form;
    }

    private void addField(JPanel p, GridBagConstraints g, int col,
                          String label, JComponent field) {
        g.gridx    = col;
        g.gridy    = 0;
        g.gridwidth = 1;
        p.add(new JLabel(label), g);
        g.gridy = 1;
        p.add(field, g);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void load(String key) {
        boolean s = key != null && !key.trim().isEmpty();

        String base =
            "SELECT r.id, r.employee_id, u.full_name, e.employee_no, " +
            "       r.sickness_record_id, r.clearance_date, r.physician_name, " +
            "       r.medical_facility, r.fit_to_work, r.restrictions, " +
            "       r.remarks, r.status, r.review_date, r.created_at " +
            "FROM return_to_work_clearances r " +
            "JOIN employees e ON r.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "ORDER BY r.clearance_date DESC";

        String search =
            "SELECT r.id, r.employee_id, u.full_name, e.employee_no, " +
            "       r.sickness_record_id, r.clearance_date, r.physician_name, " +
            "       r.medical_facility, r.fit_to_work, r.restrictions, " +
            "       r.remarks, r.status, r.review_date, r.created_at " +
            "FROM return_to_work_clearances r " +
            "JOIN employees e ON r.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "WHERE u.full_name LIKE ? OR e.employee_no LIKE ? " +
            "   OR r.physician_name LIKE ? OR r.medical_facility LIKE ? " +
            "   OR r.status LIKE ? " +
            "ORDER BY r.clearance_date DESC";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(s ? search : base)) {

            if (s) {
                String q = "%" + key.trim() + "%";
                for (int i = 1; i <= 5; i++) p.setString(i, q);
            }

            try (ResultSet rs = p.executeQuery()) {
                fillTable(rs);
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void fillTable(ResultSet rs) {
        String[] headers = {
            "ID", "Emp ID", "Full Name", "Emp No",
            "Sick Record ID", "Clearance Date", "Physician",
            "Medical Facility", "Fit to Work", "Restrictions",
            "Remarks", "Status", "Review Date", "Created At"
        };

        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 0; i < cols; i++) {
                    // Convert fit_to_work tinyint(1) → readable Yes/No
                    if (i == 8) {
                        row[i] = rs.getInt(i + 1) == 1 ? "Yes" : "No";
                    } else {
                        row[i] = rs.getObject(i + 1);
                    }
                }
                model.addRow(row);
            }
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
        }

        table.setModel(model);
    }

    // ── Form population ───────────────────────────────────────────────────────

    private void populateForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int m = table.convertRowIndexToModel(row);
        DefaultTableModel dm = (DefaultTableModel) table.getModel();

        idField.setText(           safeStr(dm.getValueAt(m, 0)));
        employeeIdField.setText(   safeStr(dm.getValueAt(m, 1)));
        sickRecordIdField.setText( safeStr(dm.getValueAt(m, 4)));
        clearanceDateField.setText(safeStr(dm.getValueAt(m, 5)));
        physicianField.setText(    safeStr(dm.getValueAt(m, 6)));
        facilityField.setText(     safeStr(dm.getValueAt(m, 7)));
        fitBox.setSelected(        "Yes".equals(safeStr(dm.getValueAt(m, 8))));
        restrictionsArea.setText(  safeStr(dm.getValueAt(m, 9)));
        remarksArea.setText(       safeStr(dm.getValueAt(m, 10)));
        statusBox.setSelectedItem( safeStr(dm.getValueAt(m, 11)));
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    private void insertRecord() {
        if (employeeIdField.getText().trim().isEmpty()
                || clearanceDateField.getText().trim().isEmpty()) {
            Dialogs.error(this, "Employee ID and Clearance Date are required.");
            return;
        }

        String sql =
            "INSERT INTO return_to_work_clearances " +
            "(employee_id, sickness_record_id, clearance_date, physician_name, " +
            " medical_facility, fit_to_work, restrictions, remarks, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, Integer.parseInt(employeeIdField.getText().trim()));

            String sickId = sickRecordIdField.getText().trim();
            if (sickId.isEmpty()) p.setNull(2, Types.INTEGER);
            else                  p.setInt(2, Integer.parseInt(sickId));

            p.setString(3, clearanceDateField.getText().trim());
            p.setString(4, physicianField.getText().trim());
            p.setString(5, facilityField.getText().trim());
            p.setBoolean(6, fitBox.isSelected());
            p.setString(7, restrictionsArea.getText().trim());
            p.setString(8, remarksArea.getText().trim());
            p.setString(9, statusBox.getSelectedItem().toString());

            p.executeUpdate();
            AuditService.log(session.getId(), "CREATE", "RTW_CLEARANCE",
                "Added RTW clearance for employee #" + employeeIdField.getText().trim());
            clearForm();
            load("");
            Dialogs.info(this, "Return-to-work clearance record added.");

        } catch (NumberFormatException nfe) {
            Dialogs.error(this, "Employee ID and Sick Record ID must be numeric.");
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void updateRecord() {
        if (idField.getText().isEmpty()) {
            Dialogs.error(this, "Select a record to update.");
            return;
        }

        String sql =
            "UPDATE return_to_work_clearances SET " +
            "employee_id=?, sickness_record_id=?, clearance_date=?, physician_name=?, " +
            "medical_facility=?, fit_to_work=?, restrictions=?, remarks=?, status=? " +
            "WHERE id=?";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, Integer.parseInt(employeeIdField.getText().trim()));

            String sickId = sickRecordIdField.getText().trim();
            if (sickId.isEmpty()) p.setNull(2, Types.INTEGER);
            else                  p.setInt(2, Integer.parseInt(sickId));

            p.setString(3,  clearanceDateField.getText().trim());
            p.setString(4,  physicianField.getText().trim());
            p.setString(5,  facilityField.getText().trim());
            p.setBoolean(6, fitBox.isSelected());
            p.setString(7,  restrictionsArea.getText().trim());
            p.setString(8,  remarksArea.getText().trim());
            p.setString(9,  statusBox.getSelectedItem().toString());
            p.setInt(10,    Integer.parseInt(idField.getText().trim()));

            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "RTW_CLEARANCE",
                "Updated RTW clearance #" + idField.getText());
            clearForm();
            load("");
            Dialogs.info(this, "Record updated.");

        } catch (NumberFormatException nfe) {
            Dialogs.error(this, "Employee ID and Sick Record ID must be numeric.");
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    // One-click approve or reject — also stamps reviewed_by and review_date
    private void setStatus(String newStatus) {
        if (idField.getText().isEmpty()) {
            Dialogs.error(this, "Select a record first.");
            return;
        }
        String label = "APPROVED".equals(newStatus) ? "approve" : "reject";
        if (!Dialogs.confirm(this,
                "Are you sure you want to " + label +
                " clearance #" + idField.getText() + "?")) return;

        String sql =
            "UPDATE return_to_work_clearances " +
            "SET status=?, reviewed_by=?, review_date=NOW() " +
            "WHERE id=?";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, newStatus);
            p.setInt(2, session.getId());
            p.setInt(3, Integer.parseInt(idField.getText().trim()));

            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "RTW_CLEARANCE",
                newStatus + " RTW clearance #" + idField.getText());
            clearForm();
            load("");
            Dialogs.info(this, "Clearance " + newStatus.toLowerCase() + ".");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void deleteRecord() {
        if (idField.getText().isEmpty()) {
            Dialogs.error(this, "Select a record to delete.");
            return;
        }
        if (!Dialogs.confirm(this,
                "Delete RTW clearance #" + idField.getText() + "? This cannot be undone."))
            return;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "DELETE FROM return_to_work_clearances WHERE id=?")) {

            p.setInt(1, Integer.parseInt(idField.getText().trim()));
            p.executeUpdate();
            AuditService.log(session.getId(), "DELETE", "RTW_CLEARANCE",
                "Deleted RTW clearance #" + idField.getText());
            clearForm();
            load("");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void clearForm() {
        idField.setText("");
        employeeIdField.setText("");
        sickRecordIdField.setText("");
        clearanceDateField.setText(
            LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        physicianField.setText("");
        facilityField.setText("");
        fitBox.setSelected(true);
        statusBox.setSelectedIndex(0);
        restrictionsArea.setText("");
        remarksArea.setText("");
        table.clearSelection();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String safeStr(Object o) { return o == null ? "" : o.toString(); }

    private void export() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("return_to_work_clearances.csv"));
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