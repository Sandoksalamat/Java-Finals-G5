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
import java.awt.Dimension;
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

public class WellnessProgramPanel extends JPanel {

    private final UserSession session;

    // ── Program table (top) ───────────────────────────────────────────────────
    private final JTable programTable       = new JTable();
    private final JTextField searchField    = new JTextField(20);

    // ── Program form fields ───────────────────────────────────────────────────
    private final JTextField progIdField    = new JTextField(5);
    private final JTextField titleField     = new JTextField(25);
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{
        "HEALTH_SEMINAR", "STRESS_MANAGEMENT",
        "PHYSICAL_FITNESS", "VACCINATION_DRIVE", "OTHER"
    });
    private final JTextField facilitatorField = new JTextField(20);
    private final JTextField venueField       = new JTextField(20);
    private final JTextField dateField        = new JTextField(10);
    private final JTextField durationField    = new JTextField(5);
    private final JTextField maxField         = new JTextField(5);
    private final JComboBox<String> statusBox = new JComboBox<>(new String[]{
        "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
    });
    private final JTextArea descArea = new JTextArea(2, 25);

    // ── Participant table (bottom) ────────────────────────────────────────────
    private final JTable participantTable   = new JTable();
    private final JTextField partEmpIdField = new JTextField(8);
    private final JComboBox<String> partStatusBox = new JComboBox<>(new String[]{
        "ENROLLED", "ATTENDED", "ABSENT", "EXCUSED"
    });
    private final JTextField partRemarksField = new JTextField(20);
    private final JTextField partIdField      = new JTextField(5);

    public WellnessProgramPanel(UserSession session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildTopBar(), BorderLayout.NORTH);

        // Split: program management (top) | participant management (bottom)
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            buildProgramSection(),
            buildParticipantSection());
        split.setResizeWeight(0.55);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);

        loadPrograms("");
    }
    
    // ── Top Search Bar ────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.title("Employee Wellness Program Monitoring"), BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(new JLabel("Search:"));
        right.add(searchField);

        JButton btnSearch  = UITheme.button("SEARCH");
        JButton btnRefresh = UITheme.button("REFRESH");
        JButton btnExport  = UITheme.button("EXPORT CSV");

        btnSearch.addActionListener(e  -> loadPrograms(searchField.getText()));
        btnRefresh.addActionListener(e -> { searchField.setText(""); loadPrograms(""); });
        btnExport.addActionListener(e  -> exportPrograms());

        right.add(btnSearch);
        right.add(btnRefresh);
        right.add(btnExport);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    // ── Program Section ───────────────────────────────────────────────────────

    private JPanel buildProgramSection() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Wellness Programs"));

        // Table
        programTable.setRowHeight(24);
        programTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        programTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        programTable.setAutoCreateRowSorter(true);
        programTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Colour rows by status
        programTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    int mr = t.convertRowIndexToModel(row);
                    Object sv = t.getModel().getValueAt(mr, 8); // status col index
                    String s  = sv == null ? "" : sv.toString();
                    switch (s) {
                        case "SCHEDULED":  setBackground(new Color(220, 235, 255)); break; // blue
                        case "ONGOING":    setBackground(new Color(255, 243, 200)); break; // yellow
                        case "COMPLETED":  setBackground(new Color(220, 255, 220)); break; // green
                        case "CANCELLED":  setBackground(new Color(255, 224, 224)); break; // red
                        default:           setBackground(Color.WHITE);
                    }
                } else {
                    setBackground(t.getSelectionBackground());
                }
                return this;
            }
        });

        // When a program is selected, load its participants below
        programTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateProgramForm();
                loadParticipants();
            }
        });

        panel.add(new JScrollPane(programTable), BorderLayout.CENTER);
        panel.add(buildProgramForm(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildProgramForm() {
        progIdField.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Program Entry / Edit"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets    = new Insets(3, 5, 3, 5);
        g.fill      = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 1;

        addField(form, g, 0, "ID",          progIdField);
        addField(form, g, 1, "Title",        titleField);
        addField(form, g, 2, "Type",         typeBox);
        addField(form, g, 3, "Facilitator",  facilitatorField);
        addField(form, g, 4, "Venue",        venueField);
        addField(form, g, 5, "Date (YYYY-MM-DD)", dateField);
        addField(form, g, 6, "Duration (hrs)", durationField);
        addField(form, g, 7, "Max Participants", maxField);
        addField(form, g, 8, "Status",       statusBox);

        // Description text area
        g.gridx = 9; g.gridy = 0;
        form.add(new JLabel("Description"), g);
        g.gridy = 1;
        JScrollPane ds = new JScrollPane(descArea);
        ds.setPreferredSize(new Dimension(180, 45));
        form.add(ds, g);

        // Buttons
        JButton btnAdd    = UITheme.button("ADD");
        JButton btnUpdate = UITheme.button("UPDATE");
        JButton btnDelete = UITheme.button("DELETE");
        JButton btnClear  = UITheme.button("CLEAR");

        btnAdd.addActionListener(e    -> insertProgram());
        btnUpdate.addActionListener(e -> updateProgram());
        btnDelete.addActionListener(e -> deleteProgram());
        btnClear.addActionListener(e  -> clearProgramForm());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnAdd); btns.add(btnUpdate);
        btns.add(btnDelete); btns.add(btnClear);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 11;
        form.add(btns, g);

        return form;
    }

    // ── Participant Section ───────────────────────────────────────────────────

    private JPanel buildParticipantSection() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(
            "Participants  —  select a program above to manage enrollment"));

        participantTable.setRowHeight(22);
        participantTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        participantTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        participantTable.setAutoCreateRowSorter(true);

        // Colour rows by attendance status
        participantTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    int mr = t.convertRowIndexToModel(row);
                    Object sv = t.getModel().getValueAt(mr, 5); // status col
                    String s  = sv == null ? "" : sv.toString();
                    switch (s) {
                        case "ATTENDED": setBackground(new Color(220, 255, 220)); break;
                        case "ABSENT":   setBackground(new Color(255, 224, 224)); break;
                        case "EXCUSED":  setBackground(new Color(255, 243, 200)); break;
                        default:         setBackground(Color.WHITE);
                    }
                } else {
                    setBackground(t.getSelectionBackground());
                }
                return this;
            }
        });

        participantTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateParticipantForm();
        });

        panel.add(new JScrollPane(participantTable), BorderLayout.CENTER);
        panel.add(buildParticipantForm(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildParticipantForm() {
        partIdField.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Enroll / Update Participant"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets    = new Insets(3, 5, 3, 5);
        g.fill      = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 1;

        addField(form, g, 0, "Record ID",    partIdField);
        addField(form, g, 1, "Employee ID",  partEmpIdField);
        addField(form, g, 2, "Status",       partStatusBox);
        addField(form, g, 3, "Remarks",      partRemarksField);

        JButton btnEnroll  = UITheme.button("ENROLL");
        JButton btnUpdate  = UITheme.button("UPDATE");
        JButton btnRemove  = UITheme.button("REMOVE");
        JButton btnExport  = UITheme.button("EXPORT PARTICIPANTS");
        JButton btnClear   = UITheme.button("CLEAR");

        btnEnroll.addActionListener(e  -> enrollParticipant());
        btnUpdate.addActionListener(e  -> updateParticipant());
        btnRemove.addActionListener(e  -> removeParticipant());
        btnExport.addActionListener(e  -> exportParticipants());
        btnClear.addActionListener(e   -> clearParticipantForm());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnEnroll); btns.add(btnUpdate);
        btns.add(btnRemove); btns.add(btnExport); btns.add(btnClear);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 6;
        form.add(btns, g);

        JLabel hint = new JLabel(
            "  GREEN = Attended    RED = Absent    YELLOW = Excused    " +
            "BLUE = Scheduled    Enter Employee ID to enroll into the selected program."
        );
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        g.gridy = 3;
        form.add(hint, g);

        return form;
    }

    // ── Helper: add label+field column ───────────────────────────────────────

    private void addField(JPanel p, GridBagConstraints g, int col,
                          String label, JComponent field) {
        g.gridx    = col;
        g.gridy    = 0;
        g.gridwidth = 1;
        p.add(new JLabel(label), g);
        g.gridy = 1;
        p.add(field, g);
    }

    // ── Load Programs ─────────────────────────────────────────────────────────

    private void loadPrograms(String key) {
        boolean s = key != null && !key.trim().isEmpty();

        String base =
            "SELECT id, program_title, program_type, facilitator, venue, " +
            "       program_date, duration_hours, max_participants, status " +
            "FROM wellness_programs " +
            "ORDER BY program_date DESC";

        String search =
            "SELECT id, program_title, program_type, facilitator, venue, " +
            "       program_date, duration_hours, max_participants, status " +
            "FROM wellness_programs " +
            "WHERE program_title LIKE ? OR program_type LIKE ? " +
            "   OR facilitator LIKE ? OR venue LIKE ? OR status LIKE ? " +
            "ORDER BY program_date DESC";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(s ? search : base)) {

            if (s) {
                String q = "%" + key.trim() + "%";
                for (int i = 1; i <= 5; i++) p.setString(i, q);
            }

            try (ResultSet rs = p.executeQuery()) {
                fillTable(programTable, rs, new String[]{
                    "ID", "Title", "Type", "Facilitator", "Venue",
                    "Date", "Duration (hrs)", "Max Participants", "Status"
                });
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    // ── Load Participants for selected program ────────────────────────────────

    private void loadParticipants() {
        int programId = getSelectedProgramId();
        if (programId < 0) {
            fillTable(participantTable, null, new String[]{
                "ID", "Program ID", "Employee ID", "Employee No", "Full Name", "Status", "Remarks", "Enrolled At"
            });
            return;
        }

        String sql =
            "SELECT wp.id, wp.program_id, wp.employee_id, " +
            "       e.employee_no, u.full_name, wp.status, wp.remarks, wp.enrolled_at " +
            "FROM wellness_participants wp " +
            "JOIN employees e ON wp.employee_id = e.id " +
            "JOIN users u     ON e.user_id = u.id " +
            "WHERE wp.program_id = ? " +
            "ORDER BY wp.enrolled_at";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, programId);
            try (ResultSet rs = p.executeQuery()) {
                fillTable(participantTable, rs, new String[]{
                    "ID", "Program ID", "Employee ID", "Employee No",
                    "Full Name", "Status", "Remarks", "Enrolled At"
                });
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
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

    // ── Form population ───────────────────────────────────────────────────────

    private void populateProgramForm() {
        int row = programTable.getSelectedRow();
        if (row < 0) return;
        int m = programTable.convertRowIndexToModel(row);
        DefaultTableModel dm = (DefaultTableModel) programTable.getModel();

        progIdField.setText(    safeStr(dm.getValueAt(m, 0)));
        titleField.setText(     safeStr(dm.getValueAt(m, 1)));
        typeBox.setSelectedItem(safeStr(dm.getValueAt(m, 2)));
        facilitatorField.setText(safeStr(dm.getValueAt(m, 3)));
        venueField.setText(     safeStr(dm.getValueAt(m, 4)));
        dateField.setText(      safeStr(dm.getValueAt(m, 5)));
        durationField.setText(  safeStr(dm.getValueAt(m, 6)));
        maxField.setText(       safeStr(dm.getValueAt(m, 7)));
        statusBox.setSelectedItem(safeStr(dm.getValueAt(m, 8)));
    }

    private void populateParticipantForm() {
        int row = participantTable.getSelectedRow();
        if (row < 0) return;
        int m = participantTable.convertRowIndexToModel(row);
        DefaultTableModel dm = (DefaultTableModel) participantTable.getModel();

        partIdField.setText(       safeStr(dm.getValueAt(m, 0)));
        partEmpIdField.setText(    safeStr(dm.getValueAt(m, 2)));
        partStatusBox.setSelectedItem(safeStr(dm.getValueAt(m, 5)));
        partRemarksField.setText(  safeStr(dm.getValueAt(m, 6)));
    }

    // ── Program CRUD ──────────────────────────────────────────────────────────

    private void insertProgram() {
        String sql =
            "INSERT INTO wellness_programs " +
            "(program_title, program_type, facilitator, venue, program_date, " +
            " duration_hours, max_participants, status, description, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, titleField.getText().trim());
            p.setString(2, typeBox.getSelectedItem().toString());
            p.setString(3, facilitatorField.getText().trim());
            p.setString(4, venueField.getText().trim());
            p.setString(5, dateField.getText().trim());
            p.setString(6, durationField.getText().trim().isEmpty() ? null
                           : durationField.getText().trim());
            p.setString(7, maxField.getText().trim().isEmpty() ? null
                           : maxField.getText().trim());
            p.setString(8, statusBox.getSelectedItem().toString());
            p.setString(9, descArea.getText().trim());
            p.setInt(10, session.getId());

            p.executeUpdate();
            AuditService.log(session.getId(), "CREATE", "WELLNESS_PROGRAM",
                "Added wellness program: " + titleField.getText().trim());
            clearProgramForm();
            loadPrograms("");
            Dialogs.info(this, "Wellness program added.");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void updateProgram() {
        if (progIdField.getText().isEmpty()) {
            Dialogs.error(this, "Select a program to update.");
            return;
        }
        String sql =
            "UPDATE wellness_programs SET " +
            "program_title=?, program_type=?, facilitator=?, venue=?, program_date=?, " +
            "duration_hours=?, max_participants=?, status=?, description=? " +
            "WHERE id=?";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, titleField.getText().trim());
            p.setString(2, typeBox.getSelectedItem().toString());
            p.setString(3, facilitatorField.getText().trim());
            p.setString(4, venueField.getText().trim());
            p.setString(5, dateField.getText().trim());
            p.setString(6, durationField.getText().trim().isEmpty() ? null
                           : durationField.getText().trim());
            p.setString(7, maxField.getText().trim().isEmpty() ? null
                           : maxField.getText().trim());
            p.setString(8, statusBox.getSelectedItem().toString());
            p.setString(9, descArea.getText().trim());
            p.setInt(10, Integer.parseInt(progIdField.getText().trim()));

            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "WELLNESS_PROGRAM",
                "Updated wellness program #" + progIdField.getText());
            clearProgramForm();
            loadPrograms("");
            Dialogs.info(this, "Program updated.");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void deleteProgram() {
        if (progIdField.getText().isEmpty()) {
            Dialogs.error(this, "Select a program to delete.");
            return;
        }
        if (!Dialogs.confirm(this,
                "Delete program #" + progIdField.getText() +
                "? All participant records for this program will also be removed.")) return;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "DELETE FROM wellness_programs WHERE id=?")) {

            p.setInt(1, Integer.parseInt(progIdField.getText().trim()));
            p.executeUpdate();
            AuditService.log(session.getId(), "DELETE", "WELLNESS_PROGRAM",
                "Deleted wellness program #" + progIdField.getText());
            clearProgramForm();
            loadPrograms("");
            fillTable(participantTable, null, new String[]{
                "ID","Program ID","Employee ID","Employee No",
                "Full Name","Status","Remarks","Enrolled At"
            });

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void clearProgramForm() {
        progIdField.setText("");
        titleField.setText("");
        typeBox.setSelectedIndex(0);
        facilitatorField.setText("");
        venueField.setText("");
        dateField.setText(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        durationField.setText("");
        maxField.setText("");
        statusBox.setSelectedIndex(0);
        descArea.setText("");
        programTable.clearSelection();
    }

    // ── Participant CRUD ──────────────────────────────────────────────────────

    private void enrollParticipant() {
        int programId = getSelectedProgramId();
        if (programId < 0) {
            Dialogs.error(this, "Select a program from the table above first.");
            return;
        }
        if (partEmpIdField.getText().trim().isEmpty()) {
            Dialogs.error(this, "Enter an Employee ID to enroll.");
            return;
        }

        String sql =
            "INSERT INTO wellness_participants (program_id, employee_id, status, remarks) " +
            "VALUES (?, ?, ?, ?)";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, programId);
            p.setInt(2, Integer.parseInt(partEmpIdField.getText().trim()));
            p.setString(3, partStatusBox.getSelectedItem().toString());
            p.setString(4, partRemarksField.getText().trim());

            p.executeUpdate();
            AuditService.log(session.getId(), "CREATE", "WELLNESS_PARTICIPANT",
                "Enrolled employee #" + partEmpIdField.getText() +
                " in program #" + programId);
            clearParticipantForm();
            loadParticipants();
            Dialogs.info(this, "Employee enrolled.");

        } catch (Exception ex) {
            // Duplicate entry gives a clear message from MySQL
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void updateParticipant() {
        if (partIdField.getText().isEmpty()) {
            Dialogs.error(this, "Select a participant record to update.");
            return;
        }
        String sql =
            "UPDATE wellness_participants SET status=?, remarks=? WHERE id=?";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, partStatusBox.getSelectedItem().toString());
            p.setString(2, partRemarksField.getText().trim());
            p.setInt(3, Integer.parseInt(partIdField.getText().trim()));

            p.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "WELLNESS_PARTICIPANT",
                "Updated participant record #" + partIdField.getText());
            clearParticipantForm();
            loadParticipants();
            Dialogs.info(this, "Participant record updated.");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void removeParticipant() {
        if (partIdField.getText().isEmpty()) {
            Dialogs.error(this, "Select a participant to remove.");
            return;
        }
        if (!Dialogs.confirm(this,
                "Remove participant record #" + partIdField.getText() + "?")) return;

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "DELETE FROM wellness_participants WHERE id=?")) {

            p.setInt(1, Integer.parseInt(partIdField.getText().trim()));
            p.executeUpdate();
            AuditService.log(session.getId(), "DELETE", "WELLNESS_PARTICIPANT",
                "Removed participant record #" + partIdField.getText());
            clearParticipantForm();
            loadParticipants();

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void clearParticipantForm() {
        partIdField.setText("");
        partEmpIdField.setText("");
        partStatusBox.setSelectedIndex(0);
        partRemarksField.setText("");
        participantTable.clearSelection();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getSelectedProgramId() {
        int row = programTable.getSelectedRow();
        if (row < 0) return -1;
        int m = programTable.convertRowIndexToModel(row);
        Object val = programTable.getModel().getValueAt(m, 0);
        try { return Integer.parseInt(val.toString()); }
        catch (Exception e) { return -1; }
    }

    private String safeStr(Object o) { return o == null ? "" : o.toString(); }

    // ── CSV Export ────────────────────────────────────────────────────────────

    private void exportPrograms() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("wellness_programs.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                CsvExporter.export(programTable, fc.getSelectedFile());
                Dialogs.info(this, "Programs exported.");
            } catch (Exception ex) { Dialogs.error(this, ex.getMessage()); }
        }
    }

    private void exportParticipants() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("wellness_participants.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                CsvExporter.export(participantTable, fc.getSelectedFile());
                Dialogs.info(this, "Participants exported.");
            } catch (Exception ex) { Dialogs.error(this, ex.getMessage()); }
        }
    }
}