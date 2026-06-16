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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class HealthSafetyPanel extends JPanel {

    private final UserSession session;

    // ── Incident table & form ────────────────────────────────────────────────
    private final JTable     incidentTable    = new JTable();
    private final JTextField searchField      = new JTextField(20);

    private final JTextField incIdField       = new JTextField(5);
    private final JTextField incEmpIdField    = new JTextField(8);
    private final JTextField incDateField     = new JTextField(10);
    private final JComboBox<String> incTypeBox = new JComboBox<>(new String[]{
        "NEAR_MISS","MINOR_INJURY","MAJOR_INJURY",
        "ILLNESS","PROPERTY_DAMAGE","FIRE","OTHER"
    });
    private final JTextField incLocationField = new JTextField(20);
    private final JTextArea  incDescArea      = new JTextArea(2, 22);
    private final JTextArea  incActionArea    = new JTextArea(2, 22);
    private final JTextArea  incInvestArea    = new JTextArea(2, 22);
    private final JComboBox<String> incStatusBox = new JComboBox<>(new String[]{
        "REPORTED","UNDER_INVESTIGATION","CLOSED"
    });

    // ── Restriction table & form ──────────────────────────────────────────────
    private final JTable     restrictTable    = new JTable();

    private final JTextField resIdField       = new JTextField(5);
    private final JTextField resEmpIdField    = new JTextField(8);
    private final JTextField resIncIdField    = new JTextField(8);
    private final JTextField resRtwIdField    = new JTextField(8);
    private final JTextField resTypeField     = new JTextField(20);
    private final JTextField resStartField    = new JTextField(10);
    private final JTextField resEndField      = new JTextField(10);
    private final JTextField resPrescField    = new JTextField(20);
    private final JTextArea  resDetailsArea   = new JTextArea(2, 22);
    private final JComboBox<String> resStatusBox = new JComboBox<>(new String[]{
        "ACTIVE","LIFTED","EXPIRED"
    });
    private final JTextField resRemarksField  = new JTextField(20);

    public HealthSafetyPanel(UserSession session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildTopBar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            buildIncidentSection(),
            buildRestrictionSection());
        split.setResizeWeight(0.55);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);

        loadIncidents("");
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.title("Health & Safety Report"), BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(new JLabel("Search:"));
        right.add(searchField);

        JButton btnSearch  = UITheme.button("SEARCH");
        JButton btnRefresh = UITheme.button("REFRESH");
        JButton btnExport  = UITheme.button("EXPORT INCIDENTS CSV");

        btnSearch.addActionListener(e  -> loadIncidents(searchField.getText()));
        btnRefresh.addActionListener(e -> { searchField.setText(""); loadIncidents(""); });
        btnExport.addActionListener(e  -> exportTable(incidentTable, "hs_incidents.csv"));

        right.add(btnSearch);
        right.add(btnRefresh);
        right.add(btnExport);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    // ── Incident section ──────────────────────────────────────────────────────

    private JPanel buildIncidentSection() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Reported Incidents"));

        incidentTable.setRowHeight(24);
        incidentTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        incidentTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        incidentTable.setAutoCreateRowSorter(true);
        incidentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Colour by status
        incidentTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    int mr = t.convertRowIndexToModel(row);
                    Object sv = t.getModel().getValueAt(mr, 9); // status col
                    String s  = sv == null ? "" : sv.toString();
                    switch (s) {
                        case "REPORTED":             setBackground(new Color(255, 224, 224)); break; // red
                        case "UNDER_INVESTIGATION":  setBackground(new Color(255, 243, 200)); break; // yellow
                        case "CLOSED":               setBackground(new Color(220, 255, 220)); break; // green
                        default:                     setBackground(Color.WHITE);
                    }
                } else {
                    setBackground(t.getSelectionBackground());
                }
                return this;
            }
        });

        incidentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateIncidentForm();
                loadRestrictionsForSelectedIncident();
            }
        });

        panel.add(new JScrollPane(incidentTable), BorderLayout.CENTER);
        panel.add(buildIncidentForm(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildIncidentForm() {
        incIdField.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Incident Entry / Edit"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets    = new Insets(3, 5, 3, 5);
        g.fill      = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 1;

        addField(form, g, 0, "ID",           incIdField);
        addField(form, g, 1, "Employee ID",  incEmpIdField);
        addField(form, g, 2, "Date",         incDateField);
        addField(form, g, 3, "Type",         incTypeBox);
        addField(form, g, 4, "Location",     incLocationField);
        addField(form, g, 5, "Status",       incStatusBox);

        addTextArea(form, g, 6, "Description",          incDescArea);
        addTextArea(form, g, 7, "Immediate Action",     incActionArea);
        addTextArea(form, g, 8, "Investigation Notes",  incInvestArea);

        JButton btnAdd        = UITheme.button("ADD");
        JButton btnUpdate     = UITheme.button("UPDATE");
        JButton btnInvestigate = UITheme.button("MARK INVESTIGATING");
        JButton btnClose      = UITheme.button("CLOSE INCIDENT");
        JButton btnDelete     = UITheme.button("DELETE");
        JButton btnClear      = UITheme.button("CLEAR");

        btnAdd.addActionListener(e         -> insertIncident());
        btnUpdate.addActionListener(e      -> updateIncident());
        btnInvestigate.addActionListener(e -> setIncidentStatus("UNDER_INVESTIGATION"));
        btnClose.addActionListener(e       -> setIncidentStatus("CLOSED"));
        btnDelete.addActionListener(e      -> deleteIncident());
        btnClear.addActionListener(e       -> clearIncidentForm());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnAdd); btns.add(btnUpdate);
        btns.add(btnInvestigate); btns.add(btnClose);
        btns.add(btnDelete); btns.add(btnClear);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 10;
        form.add(btns, g);

        JLabel hint = new JLabel(
            "  RED = Reported    YELLOW = Under Investigation    GREEN = Closed    " +
            "Selecting an incident filters restrictions below."
        );
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        g.gridy = 3;
        form.add(hint, g);

        return form;
    }

    // ── Restriction section ───────────────────────────────────────────────────

    private JPanel buildRestrictionSection() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(
            "Medical Restrictions  —  select an incident above to filter, " +
            "or REFRESH to show all"));

        restrictTable.setRowHeight(22);
        restrictTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        restrictTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        restrictTable.setAutoCreateRowSorter(true);

        // Colour by status
        restrictTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    int mr = t.convertRowIndexToModel(row);
                    Object sv = t.getModel().getValueAt(mr, 9); // status col
                    String s  = sv == null ? "" : sv.toString();
                    switch (s) {
                        case "ACTIVE":  setBackground(new Color(255, 224, 224)); break; // red
                        case "LIFTED":  setBackground(new Color(220, 255, 220)); break; // green
                        case "EXPIRED": setBackground(new Color(220, 220, 220)); break; // grey
                        default:        setBackground(Color.WHITE);
                    }
                } else {
                    setBackground(t.getSelectionBackground());
                }
                return this;
            }
        });

        restrictTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateRestrictionForm();
        });

        panel.add(new JScrollPane(restrictTable), BorderLayout.CENTER);
        panel.add(buildRestrictionForm(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRestrictionForm() {
        resIdField.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Medical Restriction Entry / Edit"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets    = new Insets(3, 5, 3, 5);
        g.fill      = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 1;

        addField(form, g, 0,  "ID",               resIdField);
        addField(form, g, 1,  "Employee ID",       resEmpIdField);
        addField(form, g, 2,  "Incident ID",       resIncIdField);
        addField(form, g, 3,  "RTW Clearance ID",  resRtwIdField);
        addField(form, g, 4,  "Restriction Type",  resTypeField);
        addField(form, g, 5,  "Start Date",        resStartField);
        addField(form, g, 6,  "End Date",          resEndField);
        addField(form, g, 7,  "Prescribed By",     resPrescField);
        addField(form, g, 8,  "Status",            resStatusBox);
        addField(form, g, 9,  "Admin Remarks",     resRemarksField);
        addTextArea(form, g, 10, "Details",        resDetailsArea);

        JButton btnAdd    = UITheme.button("ADD");
        JButton btnUpdate = UITheme.button("UPDATE");
        JButton btnLift   = UITheme.button("MARK LIFTED");
        JButton btnDelete = UITheme.button("DELETE");
        JButton btnAllRes = UITheme.button("SHOW ALL RESTRICTIONS");
        JButton btnExport = UITheme.button("EXPORT CSV");
        JButton btnClear  = UITheme.button("CLEAR");

        btnAdd.addActionListener(e    -> insertRestriction());
        btnUpdate.addActionListener(e -> updateRestriction());
        btnLift.addActionListener(e   -> liftRestriction());
        btnDelete.addActionListener(e -> deleteRestriction());
        btnAllRes.addActionListener(e -> loadAllRestrictions());
        btnExport.addActionListener(e -> exportTable(restrictTable, "medical_restrictions.csv"));
        btnClear.addActionListener(e  -> clearRestrictionForm());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnAdd); btns.add(btnUpdate); btns.add(btnLift);
        btns.add(btnDelete); btns.add(btnAllRes);
        btns.add(btnExport); btns.add(btnClear);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 12;
        form.add(btns, g);

        JLabel hint = new JLabel(
            "  RED = Active restriction    GREEN = Lifted    GREY = Expired    " +
            "Incident ID and RTW Clearance ID are optional links."
        );
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        g.gridy = 3;
        form.add(hint, g);

        return form;
    }

    // ── GridBag helpers ───────────────────────────────────────────────────────

    private void addField(JPanel p, GridBagConstraints g, int col,
                          String label, JComponent field) {
        g.gridx = col; g.gridy = 0; g.gridwidth = 1;
        p.add(new JLabel(label), g);
        g.gridy = 1;
        p.add(field, g);
    }

    private void addTextArea(JPanel p, GridBagConstraints g, int col,
                             String label, JTextArea area) {
        g.gridx = col; g.gridy = 0; g.gridwidth = 1;
        p.add(new JLabel(label), g);
        g.gridy = 1;
        p.add(new JScrollPane(area), g);
    }

    // ── Load incidents ────────────────────────────────────────────────────────

    private void loadIncidents(String key) {
        boolean s = key != null && !key.trim().isEmpty();

        String base =
            "SELECT i.id, i.employee_id, u.full_name, e.employee_no, " +
            "       i.incident_date, i.incident_type, i.location, " +
            "       i.description, i.immediate_action, i.status, " +
            "       i.investigation_notes, i.created_at " +
            "FROM health_safety_incidents i " +
            "JOIN employees e ON i.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "ORDER BY i.incident_date DESC";

        String search =
            "SELECT i.id, i.employee_id, u.full_name, e.employee_no, " +
            "       i.incident_date, i.incident_type, i.location, " +
            "       i.description, i.immediate_action, i.status, " +
            "       i.investigation_notes, i.created_at " +
            "FROM health_safety_incidents i " +
            "JOIN employees e ON i.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "WHERE u.full_name LIKE ? OR e.employee_no LIKE ? " +
            "   OR i.incident_type LIKE ? OR i.location LIKE ? OR i.status LIKE ? " +
            "ORDER BY i.incident_date DESC";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(s ? search : base)) {

            if (s) {
                String q = "%" + key.trim() + "%";
                for (int i = 1; i <= 5; i++) p.setString(i, q);
            }

            try (ResultSet rs = p.executeQuery()) {
                fillTable(incidentTable, rs, new String[]{
                    "ID","Emp ID","Full Name","Emp No","Date","Type",
                    "Location","Description","Immediate Action",
                    "Status","Investigation Notes","Created At"
                });
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    // ── Load restrictions (filtered by incident or all) ───────────────────────

    private void loadRestrictionsForSelectedIncident() {
        int incidentId = getSelectedIncidentId();
        if (incidentId < 0) { loadAllRestrictions(); return; }

        String sql =
            "SELECT mr.id, mr.employee_id, u.full_name, e.employee_no, " +
            "       mr.incident_id, mr.rtw_clearance_id, mr.restriction_type, " +
            "       mr.start_date, mr.end_date, mr.status, " +
            "       mr.prescribed_by, mr.details, mr.admin_remarks, mr.created_at " +
            "FROM medical_restrictions mr " +
            "JOIN employees e ON mr.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "WHERE mr.incident_id = ? " +
            "ORDER BY mr.start_date DESC";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, incidentId);
            try (ResultSet rs = p.executeQuery()) {
                fillTable(restrictTable, rs, restrictionHeaders());
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void loadAllRestrictions() {
        String sql =
            "SELECT mr.id, mr.employee_id, u.full_name, e.employee_no, " +
            "       mr.incident_id, mr.rtw_clearance_id, mr.restriction_type, " +
            "       mr.start_date, mr.end_date, mr.status, " +
            "       mr.prescribed_by, mr.details, mr.admin_remarks, mr.created_at " +
            "FROM medical_restrictions mr " +
            "JOIN employees e ON mr.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "ORDER BY mr.status ASC, mr.start_date DESC";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            try (ResultSet rs = p.executeQuery()) {
                fillTable(restrictTable, rs, restrictionHeaders());
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private String[] restrictionHeaders() {
        return new String[]{
            "ID","Emp ID","Full Name","Emp No",
            "Incident ID","RTW ID","Restriction Type",
            "Start Date","End Date","Status",
            "Prescribed By","Details","Admin Remarks","Created At"
        };
    }

    // ── Generic table filler ──────────────────────────────────────────────────

    private void fillTable(JTable target, ResultSet rs, String[] headers) {
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
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
        target.setModel(model);
    }

    // ── Form population ───────────────────────────────────────────────────────

    private void populateIncidentForm() {
        int row = incidentTable.getSelectedRow();
        if (row < 0) return;
        int m = incidentTable.convertRowIndexToModel(row);
        DefaultTableModel dm = (DefaultTableModel) incidentTable.getModel();

        incIdField.setText(       safeStr(dm.getValueAt(m, 0)));
        incEmpIdField.setText(    safeStr(dm.getValueAt(m, 1)));
        incDateField.setText(     safeStr(dm.getValueAt(m, 4)));
        incTypeBox.setSelectedItem(safeStr(dm.getValueAt(m, 5)));
        incLocationField.setText( safeStr(dm.getValueAt(m, 6)));
        incDescArea.setText(      safeStr(dm.getValueAt(m, 7)));
        incActionArea.setText(    safeStr(dm.getValueAt(m, 8)));
        incStatusBox.setSelectedItem(safeStr(dm.getValueAt(m, 9)));
        incInvestArea.setText(    safeStr(dm.getValueAt(m, 10)));
    }

    private void populateRestrictionForm() {
        int row = restrictTable.getSelectedRow();
        if (row < 0) return;
        int m = restrictTable.convertRowIndexToModel(row);
        DefaultTableModel dm = (DefaultTableModel) restrictTable.getModel();

        resIdField.setText(      safeStr(dm.getValueAt(m, 0)));
        resEmpIdField.setText(   safeStr(dm.getValueAt(m, 1)));
        resIncIdField.setText(   safeStr(dm.getValueAt(m, 4)));
        resRtwIdField.setText(   safeStr(dm.getValueAt(m, 5)));
        resTypeField.setText(    safeStr(dm.getValueAt(m, 6)));
        resStartField.setText(   safeStr(dm.getValueAt(m, 7)));
        resEndField.setText(     safeStr(dm.getValueAt(m, 8)));
        resStatusBox.setSelectedItem(safeStr(dm.getValueAt(m, 9)));
        resPrescField.setText(   safeStr(dm.getValueAt(m, 10)));
        resDetailsArea.setText(  safeStr(dm.getValueAt(m, 11)));
        resRemarksField.setText( safeStr(dm.getValueAt(m, 12)));
    }

    // ── Incident CRUD ─────────────────────────────────────────────────────────

    private void insertIncident() {
        if (incEmpIdField.getText().trim().isEmpty()
                || incDateField.getText().trim().isEmpty()
                || incDescArea.getText().trim().isEmpty()) {
            Dialogs.error(this, "Employee ID, Date and Description are required.");
            return;
        }

        String sql =
            "INSERT INTO health_safety_incidents " +
            "(employee_id, incident_date, incident_type, location, description, " +
            " immediate_action, investigation_notes, status, reported_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1,    Integer.parseInt(incEmpIdField.getText().trim()));
            p.setString(2, incDateField.getText().trim());
            p.setString(3, incTypeBox.getSelectedItem().toString());
            p.setString(4, incLocationField.getText().trim());
            p.setString(5, incDescArea.getText().trim());
            p.setString(6, incActionArea.getText().trim());
            p.setString(7, incInvestArea.getText().trim());
            p.setString(8, incStatusBox.getSelectedItem().toString());
            p.setInt(9,    session.getId());

            p.executeUpdate();
            AuditService.log(session.getId(), "CREATE", "HS_INCIDENT",
                "Reported incident for employee #" + incEmpIdField.getText().trim());
            clearIncidentForm();
            loadIncidents("");
            Dialogs.info(this, "Incident reported.");

        } catch (NumberFormatException nfe) {
            Dialogs.error(this, "Employee ID must be numeric.");
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void updateIncident() {
        if (incIdField.getText().isEmpty()) {
            Dialogs.error(this, "Select an incident to update.");
            return;
        }

        String sql =
            "UPDATE health_safety_incidents SET " +
            "employee_id=?, incident_date=?, incident_type=?, location=?, description=?, " +
            "immediate_action=?, investigation_notes=?, status=?, investigated_by=? " +
            "WHERE id=?";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1,    Integer.parseInt(incEmpIdField.getText().trim()));
            p.setString(2, incDateField.getText().trim());
            p.setString(3, incTypeBox.getSelectedItem().toString());
            p.setString(4, incLocationField.getText().trim());
            p.setString(5, incDescArea.getText().trim());
            p.setString(6, incActionArea.getText().trim());
            p.setString(7, incInvestArea.getText().trim());
            p.setString(8, incStatusBox.getSelectedItem().toString());
            p.setInt(9,    session.getId());
            p.setInt(10,   Integer.parseInt(incIdField.getText().trim()));

            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "HS_INCIDENT",
                "Updated incident #" + incIdField.getText());
            clearIncidentForm();
            loadIncidents("");
            Dialogs.info(this, "Incident updated.");

        } catch (NumberFormatException nfe) {
            Dialogs.error(this, "Employee ID must be numeric.");
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void setIncidentStatus(String newStatus) {
        if (incIdField.getText().isEmpty()) {
            Dialogs.error(this, "Select an incident first.");
            return;
        }
        String label = "CLOSED".equals(newStatus) ? "close" : "mark as under investigation";
        if (!Dialogs.confirm(this,
                "Are you sure you want to " + label +
                " incident #" + incIdField.getText() + "?")) return;

        String sql =
            "UPDATE health_safety_incidents " +
            "SET status=?, investigated_by=? WHERE id=?";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, newStatus);
            p.setInt(2, session.getId());
            p.setInt(3, Integer.parseInt(incIdField.getText().trim()));

            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "HS_INCIDENT",
                "Set incident #" + incIdField.getText() + " to " + newStatus);
            clearIncidentForm();
            loadIncidents("");
            Dialogs.info(this, "Incident status updated to " + newStatus + ".");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void deleteIncident() {
        if (incIdField.getText().isEmpty()) {
            Dialogs.error(this, "Select an incident to delete.");
            return;
        }
        if (!Dialogs.confirm(this,
                "Delete incident #" + incIdField.getText() +
                "? All linked restrictions will also be unlinked.")) return;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "DELETE FROM health_safety_incidents WHERE id=?")) {

            p.setInt(1, Integer.parseInt(incIdField.getText().trim()));
            p.executeUpdate();
            AuditService.log(session.getId(), "DELETE", "HS_INCIDENT",
                "Deleted incident #" + incIdField.getText());
            clearIncidentForm();
            loadIncidents("");
            loadAllRestrictions();

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void clearIncidentForm() {
        incIdField.setText("");
        incEmpIdField.setText("");
        incDateField.setText(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        incTypeBox.setSelectedIndex(0);
        incLocationField.setText("");
        incDescArea.setText("");
        incActionArea.setText("");
        incInvestArea.setText("");
        incStatusBox.setSelectedIndex(0);
        incidentTable.clearSelection();
    }

    // ── Restriction CRUD ──────────────────────────────────────────────────────

    private void insertRestriction() {
        if (resEmpIdField.getText().trim().isEmpty()
                || resTypeField.getText().trim().isEmpty()
                || resStartField.getText().trim().isEmpty()) {
            Dialogs.error(this, "Employee ID, Restriction Type and Start Date are required.");
            return;
        }

        String sql =
            "INSERT INTO medical_restrictions " +
            "(employee_id, incident_id, rtw_clearance_id, restriction_type, " +
            " start_date, end_date, prescribed_by, details, status, admin_remarks) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, Integer.parseInt(resEmpIdField.getText().trim()));

            setNullableInt(p, 2, resIncIdField.getText().trim());
            setNullableInt(p, 3, resRtwIdField.getText().trim());

            p.setString(4, resTypeField.getText().trim());
            p.setString(5, resStartField.getText().trim());

            String end = resEndField.getText().trim();
            if (end.isEmpty()) p.setNull(6, Types.DATE);
            else               p.setString(6, end);

            p.setString(7, resPrescField.getText().trim());
            p.setString(8, resDetailsArea.getText().trim());
            p.setString(9, resStatusBox.getSelectedItem().toString());
            p.setString(10, resRemarksField.getText().trim());

            p.executeUpdate();
            AuditService.log(session.getId(), "CREATE", "MEDICAL_RESTRICTION",
                "Added restriction for employee #" + resEmpIdField.getText().trim());
            clearRestrictionForm();
            loadAllRestrictions();
            Dialogs.info(this, "Medical restriction added.");

        } catch (NumberFormatException nfe) {
            Dialogs.error(this, "Employee ID, Incident ID and RTW ID must be numeric.");
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void updateRestriction() {
        if (resIdField.getText().isEmpty()) {
            Dialogs.error(this, "Select a restriction to update.");
            return;
        }

        String sql =
            "UPDATE medical_restrictions SET " +
            "employee_id=?, incident_id=?, rtw_clearance_id=?, restriction_type=?, " +
            "start_date=?, end_date=?, prescribed_by=?, details=?, " +
            "status=?, admin_remarks=? WHERE id=?";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, Integer.parseInt(resEmpIdField.getText().trim()));
            setNullableInt(p, 2, resIncIdField.getText().trim());
            setNullableInt(p, 3, resRtwIdField.getText().trim());
            p.setString(4, resTypeField.getText().trim());
            p.setString(5, resStartField.getText().trim());

            String end = resEndField.getText().trim();
            if (end.isEmpty()) p.setNull(6, Types.DATE);
            else               p.setString(6, end);

            p.setString(7,  resPrescField.getText().trim());
            p.setString(8,  resDetailsArea.getText().trim());
            p.setString(9,  resStatusBox.getSelectedItem().toString());
            p.setString(10, resRemarksField.getText().trim());
            p.setInt(11,    Integer.parseInt(resIdField.getText().trim()));

            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "MEDICAL_RESTRICTION",
                "Updated restriction #" + resIdField.getText());
            clearRestrictionForm();
            loadAllRestrictions();
            Dialogs.info(this, "Restriction updated.");

        } catch (NumberFormatException nfe) {
            Dialogs.error(this, "Employee ID, Incident ID and RTW ID must be numeric.");
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void liftRestriction() {
        if (resIdField.getText().isEmpty()) {
            Dialogs.error(this, "Select a restriction to lift.");
            return;
        }
        if (!Dialogs.confirm(this,
                "Mark restriction #" + resIdField.getText() + " as LIFTED?")) return;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "UPDATE medical_restrictions SET status='LIFTED' WHERE id=?")) {

            p.setInt(1, Integer.parseInt(resIdField.getText().trim()));
            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "MEDICAL_RESTRICTION",
                "Lifted restriction #" + resIdField.getText());
            clearRestrictionForm();
            loadAllRestrictions();
            Dialogs.info(this, "Restriction marked as LIFTED.");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void deleteRestriction() {
        if (resIdField.getText().isEmpty()) {
            Dialogs.error(this, "Select a restriction to delete.");
            return;
        }
        if (!Dialogs.confirm(this,
                "Delete restriction #" + resIdField.getText() + "?")) return;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "DELETE FROM medical_restrictions WHERE id=?")) {

            p.setInt(1, Integer.parseInt(resIdField.getText().trim()));
            p.executeUpdate();
            AuditService.log(session.getId(), "DELETE", "MEDICAL_RESTRICTION",
                "Deleted restriction #" + resIdField.getText());
            clearRestrictionForm();
            loadAllRestrictions();

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void clearRestrictionForm() {
        resIdField.setText("");
        resEmpIdField.setText("");
        resIncIdField.setText("");
        resRtwIdField.setText("");
        resTypeField.setText("");
        resStartField.setText(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        resEndField.setText("");
        resPrescField.setText("");
        resDetailsArea.setText("");
        resStatusBox.setSelectedIndex(0);
        resRemarksField.setText("");
        restrictTable.clearSelection();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getSelectedIncidentId() {
        int row = incidentTable.getSelectedRow();
        if (row < 0) return -1;
        int m = incidentTable.convertRowIndexToModel(row);
        Object val = incidentTable.getModel().getValueAt(m, 0);
        try { return Integer.parseInt(val.toString()); }
        catch (Exception e) { return -1; }
    }

    private void setNullableInt(PreparedStatement p, int idx, String val)
            throws SQLException {
        if (val == null || val.isEmpty()) p.setNull(idx, Types.INTEGER);
        else                              p.setInt(idx, Integer.parseInt(val));
    }

    private String safeStr(Object o) { return o == null ? "" : o.toString(); }

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