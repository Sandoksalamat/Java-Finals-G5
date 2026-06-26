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
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class MedicalExamPanel extends JPanel {

    private final UserSession session;

    // ── Table ─────────────────────────────────────────────────────────────────
    private final JTable     table       = new JTable();
    private final JTextField searchField = new JTextField(20);

    // ── Form — identity ───────────────────────────────────────────────────────
    private final JTextField  idField         = new JTextField(5);
    private final JTextField  empIdField      = new JTextField(8);
    private final JComboBox<String> typeBox   = new JComboBox<>(new String[]{
        "PRE_EMPLOYMENT", "ANNUAL", "RETURN_TO_WORK", "SPECIAL"
    });
    private final JTextField  examDateField   = new JTextField(10);
    private final JTextField  physicianField  = new JTextField(20);
    private final JTextField  facilityField   = new JTextField(20);

    // ── Form — vitals ─────────────────────────────────────────────────────────
    private final JComboBox<String> bloodTypeBox = new JComboBox<>(new String[]{
        "", "A+","A-","AB+","AB-","B+","B-","O+","O-"
    });
    private final JTextField  bpField         = new JTextField(8);
    private final JTextField  hrField         = new JTextField(5);
    private final JTextField  heightField     = new JTextField(6);
    private final JTextField  weightField     = new JTextField(6);

    // ── Form — test results ───────────────────────────────────────────────────
    private final JTextField  visionLField    = new JTextField(8);
    private final JTextField  visionRField    = new JTextField(8);
    private final JTextField  hearingLField   = new JTextField(10);
    private final JTextField  hearingRField   = new JTextField(10);
    private final JTextField  xrayField       = new JTextField(12);
    private final JTextField  urineField      = new JTextField(12);
    private final JTextField  cbcField        = new JTextField(12);

    // ── Form — summary ────────────────────────────────────────────────────────
    private final JTextArea   findingsArea    = new JTextArea(2, 22);
    private final JTextArea   recommendArea   = new JTextArea(2, 22);
    private final JCheckBox   fitBox          = new JCheckBox("Fit to Work", true);
    private final JComboBox<String> statusBox = new JComboBox<>(new String[]{
        "PENDING", "COMPLETED", "FLAGGED"
    });
    private final JTextField  remarksField    = new JTextField(20);

    public MedicalExamPanel(UserSession session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildTopBar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.SOUTH);

        styleTable();
        load("");
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.title("Medical Examination Records"), BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(new JLabel("Search:"));
        right.add(searchField);

        JButton btnSearch  = UITheme.button("SEARCH");
        JButton btnRefresh = UITheme.button("REFRESH");
        JButton btnExport  = UITheme.button("EXPORT CSV");
        JButton btnView    = UITheme.button("VIEW");

        btnSearch.addActionListener(e  -> load(searchField.getText()));
        btnRefresh.addActionListener(e -> { searchField.setText(""); load(""); });
        btnExport.addActionListener(e  -> export());
        btnView.addActionListener(e    -> showSelectedRecord());

        right.add(btnSearch);
        right.add(btnRefresh);
        right.add(btnView);
        right.add(btnExport);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    // ── Table styling ─────────────────────────────────────────────────────────

    private void styleTable() {
        table.setRowHeight(24);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Colour by status: FLAGGED=red, COMPLETED=green, PENDING=yellow
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    int mr = t.convertRowIndexToModel(row);
                    Object sv = t.getModel().getValueAt(mr, 20); // status col
                    String s  = sv == null ? "" : sv.toString();
                    switch (s) {
                        case "FLAGGED":   setBackground(new Color(255, 224, 224)); break;
                        case "COMPLETED": setBackground(new Color(220, 255, 220)); break;
                        default:          setBackground(new Color(255, 243, 200));
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
    }

    // ── Form ──────────────────────────────────────────────────────────────────

    private JPanel buildForm() {
        idField.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
            "Examination Record Entry / Edit"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets    = new Insets(3, 5, 3, 5);
        g.fill      = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 1;

        // Row 0-1: identity fields
        addField(form, g, 0,  "ID",           idField);
        addField(form, g, 1,  "Employee ID",  empIdField);
        addField(form, g, 2,  "Exam Type",    typeBox);
        addField(form, g, 3,  "Exam Date",    examDateField);
        addField(form, g, 4,  "Physician",    physicianField);
        addField(form, g, 5,  "Facility",     facilityField);

        // Row 2-3: vitals
        addField(form, g, 0,  "Blood Type",   bloodTypeBox);
        advanceRow(g);
        addField(form, g, 1,  "BP (mmHg)",    bpField);
        addField(form, g, 2,  "Heart Rate",   hrField);
        addField(form, g, 3,  "Height (cm)",  heightField);
        addField(form, g, 4,  "Weight (kg)",  weightField);

        // Row 4-5: test results
        addField(form, g, 0,  "Vision L",     visionLField);
        advanceRow(g);
        addField(form, g, 1,  "Vision R",     visionRField);
        addField(form, g, 2,  "Hearing L",    hearingLField);
        addField(form, g, 3,  "Hearing R",    hearingRField);
        addField(form, g, 4,  "Chest X-Ray",  xrayField);
        addField(form, g, 5,  "Urinalysis",   urineField);
        addField(form, g, 6,  "CBC",          cbcField);

        // Row 6-7: summary
        addField(form, g, 0,  "Status",       statusBox);
        advanceRow(g);
        addField(form, g, 1,  "Fit to Work",  fitBox);
        addField(form, g, 2,  "Admin Remarks", remarksField);

        // Text areas span more space
        addTextArea(form, g, 3, "Physician Findings", findingsArea);
        addTextArea(form, g, 4, "Recommendations",    recommendArea);

        // Buttons
        JButton btnAdd      = UITheme.button("ADD");
        JButton btnUpdate   = UITheme.button("UPDATE");
        JButton btnComplete = UITheme.button("MARK COMPLETED");
        JButton btnFlag     = UITheme.button("FLAG");
        JButton btnDelete   = UITheme.button("DELETE");
        JButton btnClear    = UITheme.button("CLEAR");

        btnAdd.addActionListener(e      -> insertRecord());
        btnUpdate.addActionListener(e   -> updateRecord());
        btnComplete.addActionListener(e -> setStatus("COMPLETED"));
        btnFlag.addActionListener(e     -> setStatus("FLAGGED"));
        btnDelete.addActionListener(e   -> deleteRecord());
        btnClear.addActionListener(e    -> clearForm());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnAdd); btns.add(btnUpdate);
        btns.add(btnComplete); btns.add(btnFlag);
        btns.add(btnDelete); btns.add(btnClear);

        g.gridx = 0; g.gridy = g.gridy + 2; g.gridwidth = 12;
        form.add(btns, g);

        JLabel hint = new JLabel(
            "  GREEN = Completed    RED = Flagged (needs attention)    " +
            "YELLOW = Pending    Date format: YYYY-MM-DD"
        );
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        g.gridy++;
        form.add(hint, g);

        return form;
    }

    // Track current label row for the multi-row form layout
    private int labelRow = 0;

    private void advanceRow(GridBagConstraints g) {
        labelRow += 2;
    }

    private void addField(JPanel p, GridBagConstraints g, int col,
                          String label, JComponent field) {
        g.gridx    = col;
        g.gridy    = labelRow;
        g.gridwidth = 1;
        p.add(new JLabel(label), g);
        g.gridy = labelRow + 1;
        p.add(field, g);
    }

    private void addTextArea(JPanel p, GridBagConstraints g, int col,
                             String label, JTextArea area) {
        g.gridx    = col;
        g.gridy    = labelRow;
        g.gridwidth = 1;
        p.add(new JLabel(label), g);
        g.gridy = labelRow + 1;
        p.add(new JScrollPane(area), g);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void load(String key) {
        boolean s = key != null && !key.trim().isEmpty();

        String base =
            "SELECT me.id, me.employee_id, u.full_name, e.employee_no, " +
            "       me.exam_type, me.exam_date, me.examining_physician, " +
            "       me.medical_facility, me.blood_type, me.blood_pressure, " +
            "       me.heart_rate, me.height_cm, me.weight_kg, " +
            "       me.vision_left, me.vision_right, " +
            "       me.hearing_left, me.hearing_right, " +
            "       me.chest_xray, me.urinalysis, me.cbc, " +
            "       me.status, me.fit_to_work, " +
            "       me.findings, me.recommendations, me.admin_remarks, me.created_at " +
            "FROM medical_examinations me " +
            "JOIN employees e ON me.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "ORDER BY me.exam_date DESC";

        String search =
            "SELECT me.id, me.employee_id, u.full_name, e.employee_no, " +
            "       me.exam_type, me.exam_date, me.examining_physician, " +
            "       me.medical_facility, me.blood_type, me.blood_pressure, " +
            "       me.heart_rate, me.height_cm, me.weight_kg, " +
            "       me.vision_left, me.vision_right, " +
            "       me.hearing_left, me.hearing_right, " +
            "       me.chest_xray, me.urinalysis, me.cbc, " +
            "       me.status, me.fit_to_work, " +
            "       me.findings, me.recommendations, me.admin_remarks, me.created_at " +
            "FROM medical_examinations me " +
            "JOIN employees e ON me.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "WHERE u.full_name LIKE ? OR e.employee_no LIKE ? " +
            "   OR me.exam_type LIKE ? OR me.examining_physician LIKE ? " +
            "   OR me.status LIKE ? " +
            "ORDER BY me.exam_date DESC";

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
            "Exam Type", "Exam Date", "Physician", "Facility",
            "Blood Type", "BP", "Heart Rate", "Height (cm)", "Weight (kg)",
            "Vision L", "Vision R", "Hearing L", "Hearing R",
            "Chest X-Ray", "Urinalysis", "CBC",
            "Status", "Fit to Work",
            "Findings", "Recommendations", "Admin Remarks", "Created At"
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
                    // col 21 = fit_to_work BOOLEAN → Yes/No
                    if (i == 21) row[i] = rs.getInt(i + 1) == 1 ? "Yes" : "No";
                    else         row[i] = rs.getObject(i + 1);
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

        idField.setText(        safeStr(dm.getValueAt(m, 0)));
        empIdField.setText(     safeStr(dm.getValueAt(m, 1)));
        typeBox.setSelectedItem(safeStr(dm.getValueAt(m, 4)));
        examDateField.setText(  safeStr(dm.getValueAt(m, 5)));
        physicianField.setText( safeStr(dm.getValueAt(m, 6)));
        facilityField.setText(  safeStr(dm.getValueAt(m, 7)));

        String bt = safeStr(dm.getValueAt(m, 8));
        bloodTypeBox.setSelectedItem(bt.isEmpty() ? "" : bt);

        bpField.setText(        safeStr(dm.getValueAt(m, 9)));
        hrField.setText(        safeStr(dm.getValueAt(m, 10)));
        heightField.setText(    safeStr(dm.getValueAt(m, 11)));
        weightField.setText(    safeStr(dm.getValueAt(m, 12)));
        visionLField.setText(   safeStr(dm.getValueAt(m, 13)));
        visionRField.setText(   safeStr(dm.getValueAt(m, 14)));
        hearingLField.setText(  safeStr(dm.getValueAt(m, 15)));
        hearingRField.setText(  safeStr(dm.getValueAt(m, 16)));
        xrayField.setText(      safeStr(dm.getValueAt(m, 17)));
        urineField.setText(     safeStr(dm.getValueAt(m, 18)));
        cbcField.setText(       safeStr(dm.getValueAt(m, 19)));
        statusBox.setSelectedItem(safeStr(dm.getValueAt(m, 20)));
        fitBox.setSelected(     "Yes".equals(safeStr(dm.getValueAt(m, 21))));
        findingsArea.setText(   safeStr(dm.getValueAt(m, 22)));
        recommendArea.setText(  safeStr(dm.getValueAt(m, 23)));
        remarksField.setText(   safeStr(dm.getValueAt(m, 24)));
    }


    // ── View selected record ───────────────────────────────────────────────────

    private void showSelectedRecord() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            Dialogs.error(this, "Select a medical examination record first.");
            return;
        }

        int row = table.convertRowIndexToModel(selectedRow);
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        String html = "<html><body style='font-family:SansSerif; padding:18px;'>" +
            "<h1 style='margin-bottom:4px;'>Medical Examination Result</h1>" +
            "<p style='color:#666;'>Record ID: " + html(model.getValueAt(row, 0)) + "</p>" +
            section("Employee Information") +
            item("Employee ID", model.getValueAt(row, 1)) +
            item("Employee Name", model.getValueAt(row, 2)) +
            item("Employee Number", model.getValueAt(row, 3)) +
            section("Examination Information") +
            item("Exam Type", model.getValueAt(row, 4)) +
            item("Exam Date", model.getValueAt(row, 5)) +
            item("Physician", model.getValueAt(row, 6)) +
            item("Medical Facility", model.getValueAt(row, 7)) +
            section("Vital Signs") +
            item("Blood Type", model.getValueAt(row, 8)) +
            item("Blood Pressure", model.getValueAt(row, 9)) +
            item("Heart Rate", model.getValueAt(row, 10)) +
            item("Height", valueWithUnit(model.getValueAt(row, 11), " cm")) +
            item("Weight", valueWithUnit(model.getValueAt(row, 12), " kg")) +
            section("Test Results") +
            item("Vision — Left", model.getValueAt(row, 13)) +
            item("Vision — Right", model.getValueAt(row, 14)) +
            item("Hearing — Left", model.getValueAt(row, 15)) +
            item("Hearing — Right", model.getValueAt(row, 16)) +
            item("Chest X-Ray", model.getValueAt(row, 17)) +
            item("Urinalysis", model.getValueAt(row, 18)) +
            item("CBC", model.getValueAt(row, 19)) +
            section("Assessment") +
            item("Status", model.getValueAt(row, 20)) +
            item("Fit to Work", model.getValueAt(row, 21)) +
            item("Physician Findings", model.getValueAt(row, 22)) +
            item("Recommendations", model.getValueAt(row, 23)) +
            item("Admin Remarks", model.getValueAt(row, 24)) +
            item("Created At", model.getValueAt(row, 25)) +
            "</body></html>";

        JEditorPane resultPane = new JEditorPane("text/html", html);
        resultPane.setEditable(false);
        resultPane.setCaretPosition(0);

        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "Medical Examination Result",
            java.awt.Dialog.ModalityType.MODELESS
        );
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.add(new JScrollPane(resultPane), BorderLayout.CENTER);

        JButton closeButton = UITheme.button("CLOSE");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(closeButton);
        dialog.add(controls, BorderLayout.SOUTH);

        dialog.setSize(760, 650);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String section(String title) {
        return "<h2 style='margin-top:22px; border-bottom:1px solid #ccc; padding-bottom:5px;'>" +
               html(title) + "</h2>";
    }

    private String item(String label, Object value) {
        String text = value == null || value.toString().trim().isEmpty()
            ? "—" : html(value);
        return "<table width='100%' cellpadding='5' cellspacing='0'>" +
               "<tr><td width='32%' valign='top'><b>" + html(label) +
               "</b></td><td valign='top'>" + text + "</td></tr></table>";
    }

    private String valueWithUnit(Object value, String unit) {
        if (value == null || value.toString().trim().isEmpty()) return "";
        return value.toString() + unit;
    }

    private String html(Object value) {
        if (value == null) return "";
        return value.toString()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("\n", "<br>");
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    private void insertRecord() {
        if (empIdField.getText().trim().isEmpty()
                || examDateField.getText().trim().isEmpty()) {
            Dialogs.error(this, "Employee ID and Exam Date are required.");
            return;
        }

        String sql =
            "INSERT INTO medical_examinations " +
            "(employee_id, exam_type, exam_date, examining_physician, medical_facility, " +
            " blood_type, blood_pressure, heart_rate, height_cm, weight_kg, " +
            " vision_left, vision_right, hearing_left, hearing_right, " +
            " chest_xray, urinalysis, cbc, " +
            " findings, recommendations, fit_to_work, status, admin_remarks, created_by) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1,    Integer.parseInt(empIdField.getText().trim()));
            p.setString(2, typeBox.getSelectedItem().toString());
            p.setString(3, examDateField.getText().trim());
            p.setString(4, physicianField.getText().trim());
            p.setString(5, facilityField.getText().trim());

            String bt = bloodTypeBox.getSelectedItem().toString().trim();
            if (bt.isEmpty()) p.setNull(6, Types.VARCHAR);
            else              p.setString(6, bt);

            p.setString(7,   bpField.getText().trim());
            setNullableInt(p, 8, hrField.getText().trim());
            setNullableDecimal(p, 9,  heightField.getText().trim());
            setNullableDecimal(p, 10, weightField.getText().trim());
            p.setString(11, visionLField.getText().trim());
            p.setString(12, visionRField.getText().trim());
            p.setString(13, hearingLField.getText().trim());
            p.setString(14, hearingRField.getText().trim());
            p.setString(15, xrayField.getText().trim());
            p.setString(16, urineField.getText().trim());
            p.setString(17, cbcField.getText().trim());
            p.setString(18, findingsArea.getText().trim());
            p.setString(19, recommendArea.getText().trim());
            p.setBoolean(20, fitBox.isSelected());
            p.setString(21, statusBox.getSelectedItem().toString());
            p.setString(22, remarksField.getText().trim());
            p.setInt(23,   session.getId());

            p.executeUpdate();
            AuditService.log(session.getId(), "CREATE", "MEDICAL_EXAM",
                typeBox.getSelectedItem() + " exam added for employee #"
                + empIdField.getText().trim());
            clearForm();
            load("");
            Dialogs.info(this, "Medical examination record added.");

        } catch (NumberFormatException nfe) {
            Dialogs.error(this, "Employee ID must be numeric. Heart Rate, Height and Weight must be numbers.");
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
            "UPDATE medical_examinations SET " +
            "employee_id=?, exam_type=?, exam_date=?, examining_physician=?, medical_facility=?, " +
            "blood_type=?, blood_pressure=?, heart_rate=?, height_cm=?, weight_kg=?, " +
            "vision_left=?, vision_right=?, hearing_left=?, hearing_right=?, " +
            "chest_xray=?, urinalysis=?, cbc=?, " +
            "findings=?, recommendations=?, fit_to_work=?, status=?, admin_remarks=? " +
            "WHERE id=?";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1,    Integer.parseInt(empIdField.getText().trim()));
            p.setString(2, typeBox.getSelectedItem().toString());
            p.setString(3, examDateField.getText().trim());
            p.setString(4, physicianField.getText().trim());
            p.setString(5, facilityField.getText().trim());

            String bt = bloodTypeBox.getSelectedItem().toString().trim();
            if (bt.isEmpty()) p.setNull(6, Types.VARCHAR);
            else              p.setString(6, bt);

            p.setString(7,   bpField.getText().trim());
            setNullableInt(p, 8, hrField.getText().trim());
            setNullableDecimal(p, 9,  heightField.getText().trim());
            setNullableDecimal(p, 10, weightField.getText().trim());
            p.setString(11, visionLField.getText().trim());
            p.setString(12, visionRField.getText().trim());
            p.setString(13, hearingLField.getText().trim());
            p.setString(14, hearingRField.getText().trim());
            p.setString(15, xrayField.getText().trim());
            p.setString(16, urineField.getText().trim());
            p.setString(17, cbcField.getText().trim());
            p.setString(18, findingsArea.getText().trim());
            p.setString(19, recommendArea.getText().trim());
            p.setBoolean(20, fitBox.isSelected());
            p.setString(21, statusBox.getSelectedItem().toString());
            p.setString(22, remarksField.getText().trim());
            p.setInt(23,    Integer.parseInt(idField.getText().trim()));

            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "MEDICAL_EXAM",
                "Updated exam record #" + idField.getText());
            clearForm();
            load("");
            Dialogs.info(this, "Record updated.");

        } catch (NumberFormatException nfe) {
            Dialogs.error(this, "Employee ID must be numeric. Heart Rate, Height and Weight must be numbers.");
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void setStatus(String newStatus) {
        if (idField.getText().isEmpty()) {
            Dialogs.error(this, "Select a record first.");
            return;
        }
        String label = "COMPLETED".equals(newStatus) ? "mark as completed" : "flag";
        if (!Dialogs.confirm(this,
                "Are you sure you want to " + label +
                " exam record #" + idField.getText() + "?")) return;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "UPDATE medical_examinations SET status=? WHERE id=?")) {

            p.setString(1, newStatus);
            p.setInt(2, Integer.parseInt(idField.getText().trim()));
            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "MEDICAL_EXAM",
                "Set exam #" + idField.getText() + " to " + newStatus);
            clearForm();
            load("");
            Dialogs.info(this, "Status updated to " + newStatus + ".");

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
                "Delete exam record #" + idField.getText() + "? This cannot be undone."))
            return;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "DELETE FROM medical_examinations WHERE id=?")) {

            p.setInt(1, Integer.parseInt(idField.getText().trim()));
            p.executeUpdate();
            AuditService.log(session.getId(), "DELETE", "MEDICAL_EXAM",
                "Deleted exam record #" + idField.getText());
            clearForm();
            load("");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void clearForm() {
        labelRow = 0;
        idField.setText("");
        empIdField.setText("");
        typeBox.setSelectedIndex(0);
        examDateField.setText(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        physicianField.setText("");
        facilityField.setText("");
        bloodTypeBox.setSelectedIndex(0);
        bpField.setText("");
        hrField.setText("");
        heightField.setText("");
        weightField.setText("");
        visionLField.setText("");
        visionRField.setText("");
        hearingLField.setText("");
        hearingRField.setText("");
        xrayField.setText("");
        urineField.setText("");
        cbcField.setText("");
        findingsArea.setText("");
        recommendArea.setText("");
        fitBox.setSelected(true);
        statusBox.setSelectedIndex(0);
        remarksField.setText("");
        table.clearSelection();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setNullableInt(PreparedStatement p, int idx, String val)
            throws SQLException {
        if (val == null || val.isEmpty()) p.setNull(idx, Types.INTEGER);
        else                              p.setInt(idx, Integer.parseInt(val));
    }

    private void setNullableDecimal(PreparedStatement p, int idx, String val)
            throws SQLException {
        if (val == null || val.isEmpty()) p.setNull(idx, Types.DECIMAL);
        else                              p.setDouble(idx, Double.parseDouble(val));
    }

    private String safeStr(Object o) { return o == null ? "" : o.toString(); }

    private void export() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("medical_examinations.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                CsvExporter.export(table, fc.getSelectedFile());
                Dialogs.info(this, "CSV exported.");
            } catch (Exception ex) {
                Dialogs.error(this, ex.getMessage());
            }
        }
    }
}   