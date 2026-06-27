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
import java.awt.Window;
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

    private final JTable table = new JTable();
    private final JTextField searchField = new JTextField(20);

    private final JTextField idField = new JTextField(5);
    private final JTextField empIdField = new JTextField(8);

    private final JComboBox<String> typeBox = new JComboBox<>(
        new String[]{
            "PRE_EMPLOYMENT",
            "ANNUAL",
            "SPECIAL"
        }
    );

    private final JTextField examDateField = new JTextField(10);
    private final JTextField physicianField = new JTextField(20);
    private final JTextField facilityField = new JTextField(20);

    private final JComboBox<String> bloodTypeBox = new JComboBox<>(
        new String[]{
            "",
            "A+",
            "A-",
            "AB+",
            "AB-",
            "B+",
            "B-",
            "O+",
            "O-"
        }
    );

    private final JTextField bpField = new JTextField(8);
    private final JTextField hrField = new JTextField(5);
    private final JTextField heightField = new JTextField(6);
    private final JTextField weightField = new JTextField(6);

    private final JTextField visionLField = new JTextField(8);
    private final JTextField visionRField = new JTextField(8);
    private final JTextField hearingLField = new JTextField(10);
    private final JTextField hearingRField = new JTextField(10);
    private final JTextField xrayField = new JTextField(12);
    private final JTextField urineField = new JTextField(12);
    private final JTextField cbcField = new JTextField(12);

    private final JTextArea findingsArea = new JTextArea(2, 22);
    private final JTextArea recommendArea = new JTextArea(2, 22);

    private final JCheckBox fitBox =
        new JCheckBox("Fit to Work", true);

    private final JComboBox<String> statusBox = new JComboBox<>(
        new String[]{
            "PENDING",
            "COMPLETED",
            "FLAGGED"
        }
    );

    private final JTextField remarksField = new JTextField(20);

    private int labelRow = 0;

    public MedicalExamPanel(UserSession session) {
        this.session = session;

        setLayout(new BorderLayout(8, 8));
        setBorder(
            BorderFactory.createEmptyBorder(
                12,
                12,
                12,
                12
            )
        );

        add(buildTopBar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.SOUTH);

        styleTable();
        load("");
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());

        top.add(
            UITheme.title("Medical Examination Records"),
            BorderLayout.WEST
        );

        JPanel right = new JPanel(
            new FlowLayout(FlowLayout.RIGHT)
        );

        right.add(new JLabel("Search:"));
        right.add(searchField);

        JButton searchButton = UITheme.button("SEARCH");
        JButton refreshButton = UITheme.button("REFRESH");
        JButton viewButton = UITheme.button("VIEW");
        JButton exportButton = UITheme.button("EXPORT CSV");

        searchButton.addActionListener(
            event -> load(searchField.getText())
        );

        refreshButton.addActionListener(event -> {
            searchField.setText("");
            load("");
        });

        viewButton.addActionListener(
            event -> showSelectedRecord()
        );

        exportButton.addActionListener(
            event -> export()
        );

        right.add(searchButton);
        right.add(refreshButton);
        right.add(viewButton);
        right.add(exportButton);

        top.add(right, BorderLayout.EAST);

        return top;
    }

    private void styleTable() {
        table.setRowHeight(24);

        table.setFont(
            new Font(
                "SansSerif",
                Font.PLAIN,
                12
            )
        );

        table.getTableHeader().setFont(
            new Font(
                "SansSerif",
                Font.BOLD,
                12
            )
        );

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        table.setDefaultRenderer(
            Object.class,
            new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(
                        JTable target,
                        Object value,
                        boolean selected,
                        boolean focused,
                        int row,
                        int column
                ) {
                    super.getTableCellRendererComponent(
                        target,
                        value,
                        selected,
                        focused,
                        row,
                        column
                    );

                    if (!selected) {
                        int modelRow =
                            target.convertRowIndexToModel(row);

                        Object statusValue =
                            target.getModel().getValueAt(
                                modelRow,
                                20
                            );

                        String status =
                            statusValue == null
                                ? ""
                                : statusValue.toString();

                        switch (status) {
                            case "FLAGGED":
                                setBackground(
                                    new Color(255, 224, 224)
                                );
                                break;

                            case "COMPLETED":
                                setBackground(
                                    new Color(220, 255, 220)
                                );
                                break;

                            default:
                                setBackground(
                                    new Color(255, 243, 200)
                                );
                                break;
                        }

                    } else {
                        setBackground(
                            target.getSelectionBackground()
                        );
                    }

                    return this;
                }
            }
        );

        table.getSelectionModel()
            .addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    populateForm();
                }
            });
    }

    private JPanel buildForm() {
        idField.setEditable(false);
        labelRow = 0;

        findingsArea.setLineWrap(true);
        findingsArea.setWrapStyleWord(true);

        recommendArea.setLineWrap(true);
        recommendArea.setWrapStyleWord(true);

        JPanel form = new JPanel(
            new GridBagLayout()
        );

        form.setBorder(
            BorderFactory.createTitledBorder(
                "Examination Record Entry / Edit"
            )
        );

        GridBagConstraints constraints =
            new GridBagConstraints();

        constraints.insets =
            new Insets(3, 5, 3, 5);

        constraints.fill =
            GridBagConstraints.HORIZONTAL;

        constraints.gridwidth = 1;

        addField(
            form,
            constraints,
            0,
            "ID",
            idField
        );

        addField(
            form,
            constraints,
            1,
            "Employee ID",
            empIdField
        );

        addField(
            form,
            constraints,
            2,
            "Exam Type",
            typeBox
        );

        addField(
            form,
            constraints,
            3,
            "Exam Date",
            examDateField
        );

        addField(
            form,
            constraints,
            4,
            "Physician",
            physicianField
        );

        addField(
            form,
            constraints,
            5,
            "Facility",
            facilityField
        );

        advanceRow();

        addField(
            form,
            constraints,
            0,
            "Blood Type",
            bloodTypeBox
        );

        addField(
            form,
            constraints,
            1,
            "BP (mmHg)",
            bpField
        );

        addField(
            form,
            constraints,
            2,
            "Heart Rate",
            hrField
        );

        addField(
            form,
            constraints,
            3,
            "Height (cm)",
            heightField
        );

        addField(
            form,
            constraints,
            4,
            "Weight (kg)",
            weightField
        );

        advanceRow();

        addField(
            form,
            constraints,
            0,
            "Vision L",
            visionLField
        );

        addField(
            form,
            constraints,
            1,
            "Vision R",
            visionRField
        );

        addField(
            form,
            constraints,
            2,
            "Hearing L",
            hearingLField
        );

        addField(
            form,
            constraints,
            3,
            "Hearing R",
            hearingRField
        );

        addField(
            form,
            constraints,
            4,
            "Chest X-Ray",
            xrayField
        );

        addField(
            form,
            constraints,
            5,
            "Urinalysis",
            urineField
        );

        addField(
            form,
            constraints,
            6,
            "CBC",
            cbcField
        );

        advanceRow();

        addField(
            form,
            constraints,
            0,
            "Status",
            statusBox
        );

        addField(
            form,
            constraints,
            1,
            "Fit to Work",
            fitBox
        );

        addField(
            form,
            constraints,
            2,
            "Admin Remarks",
            remarksField
        );

        addTextArea(
            form,
            constraints,
            3,
            "Physician Findings",
            findingsArea
        );

        addTextArea(
            form,
            constraints,
            4,
            "Recommendations",
            recommendArea
        );

        JButton addButton = UITheme.button("ADD");
        JButton updateButton = UITheme.button("UPDATE");

        JButton completeButton =
            UITheme.button("MARK COMPLETED");

        JButton flagButton = UITheme.button("FLAG");
        JButton deleteButton = UITheme.button("DELETE");
        JButton clearButton = UITheme.button("CLEAR");

        addButton.addActionListener(
            event -> insertRecord()
        );

        updateButton.addActionListener(
            event -> updateRecord()
        );

        completeButton.addActionListener(
            event -> setStatus("COMPLETED")
        );

        flagButton.addActionListener(
            event -> setStatus("FLAGGED")
        );

        deleteButton.addActionListener(
            event -> deleteRecord()
        );

        clearButton.addActionListener(
            event -> clearForm()
        );

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(completeButton);
        buttonPanel.add(flagButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);

        constraints.gridx = 0;
        constraints.gridy = labelRow + 2;
        constraints.gridwidth = 12;

        form.add(
            buttonPanel,
            constraints
        );

        JLabel hint = new JLabel(
            "  GREEN = Completed    "
            + "RED = Flagged    "
            + "YELLOW = Pending    "
            + "Date format: YYYY-MM-DD"
        );

        hint.setFont(
            new Font(
                "SansSerif",
                Font.ITALIC,
                11
            )
        );

        hint.setForeground(Color.GRAY);

        constraints.gridy = labelRow + 3;

        form.add(
            hint,
            constraints
        );

        return form;
    }

    private void advanceRow() {
        labelRow += 2;
    }

    private void addField(
            JPanel panel,
            GridBagConstraints constraints,
            int column,
            String label,
            JComponent field
    ) {
        constraints.gridx = column;
        constraints.gridy = labelRow;
        constraints.gridwidth = 1;

        panel.add(
            new JLabel(label),
            constraints
        );

        constraints.gridy = labelRow + 1;

        panel.add(
            field,
            constraints
        );
    }

    private void addTextArea(
            JPanel panel,
            GridBagConstraints constraints,
            int column,
            String label,
            JTextArea area
    ) {
        constraints.gridx = column;
        constraints.gridy = labelRow;
        constraints.gridwidth = 1;

        panel.add(
            new JLabel(label),
            constraints
        );

        constraints.gridy = labelRow + 1;

        panel.add(
            new JScrollPane(area),
            constraints
        );
    }

    private void load(String key) {
        boolean searching =
            key != null
            && !key.trim().isEmpty();

        String selectColumns =
            "SELECT "
            + "me.id, "
            + "me.employee_id, "
            + "u.full_name, "
            + "e.employee_no, "
            + "me.exam_type, "
            + "me.exam_date, "
            + "me.examining_physician, "
            + "me.medical_facility, "
            + "COALESCE("
            + "NULLIF(TRIM(emd.blood_type), ''), "
            + "NULLIF(TRIM(me.blood_type), '')"
            + ") AS blood_type, "
            + "me.blood_pressure, "
            + "me.heart_rate, "
            + "me.height_cm, "
            + "me.weight_kg, "
            + "me.vision_left, "
            + "me.vision_right, "
            + "me.hearing_left, "
            + "me.hearing_right, "
            + "me.chest_xray, "
            + "me.urinalysis, "
            + "me.cbc, "
            + "me.status, "
            + "me.fit_to_work, "
            + "me.findings, "
            + "me.recommendations, "
            + "me.admin_remarks, "
            + "me.created_at ";

        String joins =
            "FROM medical_examinations me "
            + "JOIN employees e "
            + "ON me.employee_id = e.id "
            + "JOIN users u "
            + "ON e.user_id = u.id "
            + "LEFT JOIN employee_medical_details emd "
            + "ON me.employee_id = emd.employee_id ";

        String baseSql =
            selectColumns
            + joins
            + "ORDER BY me.exam_date DESC";

        String searchSql =
            selectColumns
            + joins
            + "WHERE u.full_name LIKE ? "
            + "OR e.employee_no LIKE ? "
            + "OR me.exam_type LIKE ? "
            + "OR me.examining_physician LIKE ? "
            + "OR me.status LIKE ? "
            + "ORDER BY me.exam_date DESC";

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(
                    searching
                        ? searchSql
                        : baseSql
                )
        ) {
            if (searching) {
                String query =
                    "%" + key.trim() + "%";

                for (int index = 1; index <= 5; index++) {
                    statement.setString(
                        index,
                        query
                    );
                }
            }

            try (
                ResultSet resultSet =
                    statement.executeQuery()
            ) {
                fillTable(resultSet);
            }

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }

    private void fillTable(ResultSet resultSet) {
        String[] headers = {
            "ID",
            "Emp ID",
            "Full Name",
            "Emp No",
            "Exam Type",
            "Exam Date",
            "Physician",
            "Facility",
            "Blood Type",
            "BP",
            "Heart Rate",
            "Height (cm)",
            "Weight (kg)",
            "Vision L",
            "Vision R",
            "Hearing L",
            "Hearing R",
            "Chest X-Ray",
            "Urinalysis",
            "CBC",
            "Status",
            "Fit to Work",
            "Findings",
            "Recommendations",
            "Admin Remarks",
            "Created At"
        };

        DefaultTableModel model =
            new DefaultTableModel(headers, 0) {

                @Override
                public boolean isCellEditable(
                        int row,
                        int column
                ) {
                    return false;
                }
            };

        try {
            ResultSetMetaData metadata =
                resultSet.getMetaData();

            int columnCount =
                metadata.getColumnCount();

            while (resultSet.next()) {
                Object[] row =
                    new Object[columnCount];

                for (
                    int index = 0;
                    index < columnCount;
                    index++
                ) {
                    if (index == 21) {
                        row[index] =
                            resultSet.getBoolean(index + 1)
                                ? "Yes"
                                : "No";
                    } else {
                        row[index] =
                            resultSet.getObject(index + 1);
                    }
                }

                model.addRow(row);
            }

        } catch (SQLException exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }

        table.setModel(model);
    }

    private void populateForm() {
        int selectedRow =
            table.getSelectedRow();

        if (selectedRow < 0) {
            return;
        }

        int modelRow =
            table.convertRowIndexToModel(selectedRow);

        DefaultTableModel model =
            (DefaultTableModel) table.getModel();

        idField.setText(
            safeStr(model.getValueAt(modelRow, 0))
        );

        empIdField.setText(
            safeStr(model.getValueAt(modelRow, 1))
        );

        typeBox.setSelectedItem(
            safeStr(model.getValueAt(modelRow, 4))
        );

        examDateField.setText(
            safeStr(model.getValueAt(modelRow, 5))
        );

        physicianField.setText(
            safeStr(model.getValueAt(modelRow, 6))
        );

        facilityField.setText(
            safeStr(model.getValueAt(modelRow, 7))
        );

        String bloodType =
            safeStr(model.getValueAt(modelRow, 8));

        bloodTypeBox.setSelectedItem(
            bloodType.isEmpty()
                ? ""
                : bloodType
        );

        bpField.setText(
            safeStr(model.getValueAt(modelRow, 9))
        );

        hrField.setText(
            safeStr(model.getValueAt(modelRow, 10))
        );

        heightField.setText(
            safeStr(model.getValueAt(modelRow, 11))
        );

        weightField.setText(
            safeStr(model.getValueAt(modelRow, 12))
        );

        visionLField.setText(
            safeStr(model.getValueAt(modelRow, 13))
        );

        visionRField.setText(
            safeStr(model.getValueAt(modelRow, 14))
        );

        hearingLField.setText(
            safeStr(model.getValueAt(modelRow, 15))
        );

        hearingRField.setText(
            safeStr(model.getValueAt(modelRow, 16))
        );

        xrayField.setText(
            safeStr(model.getValueAt(modelRow, 17))
        );

        urineField.setText(
            safeStr(model.getValueAt(modelRow, 18))
        );

        cbcField.setText(
            safeStr(model.getValueAt(modelRow, 19))
        );

        statusBox.setSelectedItem(
            safeStr(model.getValueAt(modelRow, 20))
        );

        fitBox.setSelected(
            "Yes".equalsIgnoreCase(
                safeStr(
                    model.getValueAt(modelRow, 21)
                )
            )
        );

        findingsArea.setText(
            safeStr(model.getValueAt(modelRow, 22))
        );

        recommendArea.setText(
            safeStr(model.getValueAt(modelRow, 23))
        );

        remarksField.setText(
            safeStr(model.getValueAt(modelRow, 24))
        );
    }

    private void showSelectedRecord() {
        int selectedRow =
            table.getSelectedRow();

        if (selectedRow < 0) {
            Dialogs.error(
                this,
                "Select a medical examination record first."
            );

            return;
        }

        int modelRow =
            table.convertRowIndexToModel(selectedRow);

        DefaultTableModel model =
            (DefaultTableModel) table.getModel();

        int employeeId;

        try {
            employeeId = Integer.parseInt(
                safeStr(
                    model.getValueAt(modelRow, 1)
                )
            );

        } catch (NumberFormatException exception) {
            Dialogs.error(
                this,
                "The selected record has an invalid Employee ID."
            );

            return;
        }

        String fullName =
            displayValue(
                model.getValueAt(modelRow, 2)
            );

        String employeeNumber =
            displayValue(
                model.getValueAt(modelRow, 3)
            );

        String medicalProfileHtml =
            loadMedicalProfileHtml(employeeId);

        String html =
            "<html>"
            + "<head>"
            + "<style>"
            + "body {"
            + "font-family: SansSerif;"
            + "background: #ffffff;"
            + "color: #222222;"
            + "margin: 18px;"
            + "}"
            + "h1 {"
            + "font-size: 22px;"
            + "color: #122d48;"
            + "margin-bottom: 4px;"
            + "}"
            + "h2 {"
            + "font-size: 16px;"
            + "color: #157e8b;"
            + "margin-top: 22px;"
            + "border-bottom: 1px solid #cccccc;"
            + "padding-bottom: 4px;"
            + "}"
            + "table {"
            + "width: 100%;"
            + "border-collapse: collapse;"
            + "margin-top: 8px;"
            + "}"
            + "td {"
            + "border: 1px solid #dddddd;"
            + "padding: 7px;"
            + "vertical-align: top;"
            + "}"
            + ".label {"
            + "width: 190px;"
            + "font-weight: bold;"
            + "background: #f4f7fa;"
            + "}"
            + ".subtitle {"
            + "color: #666666;"
            + "margin-bottom: 14px;"
            + "}"
            + "</style>"
            + "</head>"
            + "<body>"

            + "<h1>Medical Examination Result</h1>"

            + "<div class='subtitle'>"
            + escapeHtml(fullName)
            + " &mdash; "
            + escapeHtml(employeeNumber)
            + "</div>"

            + "<h2>Employee and Examination Information</h2>"
            + "<table>"
            + resultRow(
                "Record ID",
                model.getValueAt(modelRow, 0)
            )
            + resultRow(
                "Employee ID",
                model.getValueAt(modelRow, 1)
            )
            + resultRow(
                "Full Name",
                model.getValueAt(modelRow, 2)
            )
            + resultRow(
                "Employee Number",
                model.getValueAt(modelRow, 3)
            )
            + resultRow(
                "Exam Type",
                model.getValueAt(modelRow, 4)
            )
            + resultRow(
                "Exam Date",
                model.getValueAt(modelRow, 5)
            )
            + resultRow(
                "Examining Physician",
                model.getValueAt(modelRow, 6)
            )
            + resultRow(
                "Medical Facility",
                model.getValueAt(modelRow, 7)
            )
            + resultRow(
                "Status",
                model.getValueAt(modelRow, 20)
            )
            + resultRow(
                "Fit to Work",
                model.getValueAt(modelRow, 21)
            )
            + "</table>"

            + "<h2>Vital Signs</h2>"
            + "<table>"
            + resultRow(
                "Blood Type",
                model.getValueAt(modelRow, 8)
            )
            + resultRow(
                "Blood Pressure",
                model.getValueAt(modelRow, 9)
            )
            + resultRow(
                "Heart Rate",
                model.getValueAt(modelRow, 10)
            )
            + resultRow(
                "Height",
                appendUnit(
                    model.getValueAt(modelRow, 11),
                    "cm"
                )
            )
            + resultRow(
                "Weight",
                appendUnit(
                    model.getValueAt(modelRow, 12),
                    "kg"
                )
            )
            + "</table>"

            + "<h2>Medical Test Results</h2>"
            + "<table>"
            + resultRow(
                "Vision - Left",
                model.getValueAt(modelRow, 13)
            )
            + resultRow(
                "Vision - Right",
                model.getValueAt(modelRow, 14)
            )
            + resultRow(
                "Hearing - Left",
                model.getValueAt(modelRow, 15)
            )
            + resultRow(
                "Hearing - Right",
                model.getValueAt(modelRow, 16)
            )
            + resultRow(
                "Chest X-Ray",
                model.getValueAt(modelRow, 17)
            )
            + resultRow(
                "Urinalysis",
                model.getValueAt(modelRow, 18)
            )
            + resultRow(
                "CBC",
                model.getValueAt(modelRow, 19)
            )
            + "</table>"

            + "<h2>Assessment</h2>"
            + "<table>"
            + resultRow(
                "Physician Findings",
                model.getValueAt(modelRow, 22)
            )
            + resultRow(
                "Recommendations",
                model.getValueAt(modelRow, 23)
            )
            + resultRow(
                "Admin Remarks",
                model.getValueAt(modelRow, 24)
            )
            + resultRow(
                "Record Created",
                model.getValueAt(modelRow, 25)
            )
            + "</table>"

            + medicalProfileHtml

            + "</body>"
            + "</html>";

        JEditorPane viewer = new JEditorPane();

        viewer.setContentType("text/html");
        viewer.setEditable(false);
        viewer.setText(html);
        viewer.setCaretPosition(0);

        JScrollPane scrollPane =
            new JScrollPane(viewer);

        scrollPane.setPreferredSize(
            new Dimension(760, 650)
        );

        Window owner =
            SwingUtilities.getWindowAncestor(this);

        JDialog dialog =
            new JDialog(
                owner,
                "Medical Examination Result",
                JDialog.ModalityType.APPLICATION_MODAL
            );

        dialog.setLayout(
            new BorderLayout()
        );

        dialog.add(
            scrollPane,
            BorderLayout.CENTER
        );

        JButton closeButton =
            UITheme.button("CLOSE");

        closeButton.addActionListener(
            event -> dialog.dispose()
        );

        JPanel footer = new JPanel(
            new FlowLayout(FlowLayout.RIGHT)
        );

        footer.add(closeButton);

        dialog.add(
            footer,
            BorderLayout.SOUTH
        );

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String loadMedicalProfileHtml(
            int employeeId
    ) {
        String sql =
            "SELECT "
            + "blood_type, "
            + "allergy, "
            + "existing_condition, "
            + "emergency_notes, "
            + "medical_certificate, "
            + "workplace_injury_report, "
            + "health_declaration, "
            + "wellness_activity "
            + "FROM employee_medical_details "
            + "WHERE employee_id = ?";

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setInt(
                1,
                employeeId
            );

            try (
                ResultSet resultSet =
                    statement.executeQuery()
            ) {
                if (resultSet.next()) {
                    return
                        "<h2>Employee Medical Profile</h2>"
                        + "<table>"
                        + resultRow(
                            "Profile Blood Type",
                            resultSet.getString(
                                "blood_type"
                            )
                        )
                        + resultRow(
                            "Allergies",
                            resultSet.getString(
                                "allergy"
                            )
                        )
                        + resultRow(
                            "Existing Medical Condition",
                            resultSet.getString(
                                "existing_condition"
                            )
                        )
                        + resultRow(
                            "Emergency Medical Notes",
                            resultSet.getString(
                                "emergency_notes"
                            )
                        )
                        + resultRow(
                            "Medical Certificate",
                            resultSet.getString(
                                "medical_certificate"
                            )
                        )
                        + resultRow(
                            "Workplace Injury Report",
                            resultSet.getString(
                                "workplace_injury_report"
                            )
                        )
                        + resultRow(
                            "Health Declaration",
                            resultSet.getString(
                                "health_declaration"
                            )
                        )
                        + resultRow(
                            "Wellness Activity",
                            resultSet.getString(
                                "wellness_activity"
                            )
                        )
                        + "</table>";
                }
            }

        } catch (Exception exception) {
            return
                "<h2>Employee Medical Profile</h2>"
                + "<table>"
                + resultRow(
                    "Profile Details",
                    "Unable to load: "
                    + exception.getMessage()
                )
                + "</table>";
        }

        return
            "<h2>Employee Medical Profile</h2>"
            + "<table>"
            + resultRow(
                "Profile Details",
                "No medical profile details found."
            )
            + "</table>";
    }

    private String resultRow(
            String label,
            Object value
    ) {
        return
            "<tr>"
            + "<td class='label'>"
            + escapeHtml(label)
            + "</td>"
            + "<td>"
            + escapeHtml(displayValue(value))
            + "</td>"
            + "</tr>";
    }

    private String appendUnit(
            Object value,
            String unit
    ) {
        String text =
            safeStr(value).trim();

        if (text.isEmpty()) {
            return "";
        }

        return text + " " + unit;
    }

    private String displayValue(Object value) {
        String text =
            safeStr(value).trim();

        return text.isEmpty()
            ? "—"
            : text;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("\n", "<br>");
    }

    private void insertRecord() {
        if (
            empIdField.getText().trim().isEmpty()
            || examDateField.getText().trim().isEmpty()
        ) {
            Dialogs.error(
                this,
                "Employee ID and Exam Date are required."
            );

            return;
        }

        String sql =
            "INSERT INTO medical_examinations "
            + "("
            + "employee_id, "
            + "exam_type, "
            + "exam_date, "
            + "examining_physician, "
            + "medical_facility, "
            + "blood_type, "
            + "blood_pressure, "
            + "heart_rate, "
            + "height_cm, "
            + "weight_kg, "
            + "vision_left, "
            + "vision_right, "
            + "hearing_left, "
            + "hearing_right, "
            + "chest_xray, "
            + "urinalysis, "
            + "cbc, "
            + "findings, "
            + "recommendations, "
            + "fit_to_work, "
            + "status, "
            + "admin_remarks, "
            + "created_by"
            + ") "
            + "VALUES ("
            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
            + ")";

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setInt(
                1,
                Integer.parseInt(
                    empIdField.getText().trim()
                )
            );

            statement.setString(
                2,
                typeBox.getSelectedItem().toString()
            );

            statement.setString(
                3,
                examDateField.getText().trim()
            );

            statement.setString(
                4,
                physicianField.getText().trim()
            );

            statement.setString(
                5,
                facilityField.getText().trim()
            );

            String bloodType =
                bloodTypeBox
                    .getSelectedItem()
                    .toString()
                    .trim();

            if (bloodType.isEmpty()) {
                statement.setNull(
                    6,
                    Types.VARCHAR
                );
            } else {
                statement.setString(
                    6,
                    bloodType
                );
            }

            statement.setString(
                7,
                bpField.getText().trim()
            );

            setNullableInt(
                statement,
                8,
                hrField.getText().trim()
            );

            setNullableDecimal(
                statement,
                9,
                heightField.getText().trim()
            );

            setNullableDecimal(
                statement,
                10,
                weightField.getText().trim()
            );

            statement.setString(
                11,
                visionLField.getText().trim()
            );

            statement.setString(
                12,
                visionRField.getText().trim()
            );

            statement.setString(
                13,
                hearingLField.getText().trim()
            );

            statement.setString(
                14,
                hearingRField.getText().trim()
            );

            statement.setString(
                15,
                xrayField.getText().trim()
            );

            statement.setString(
                16,
                urineField.getText().trim()
            );

            statement.setString(
                17,
                cbcField.getText().trim()
            );

            statement.setString(
                18,
                findingsArea.getText().trim()
            );

            statement.setString(
                19,
                recommendArea.getText().trim()
            );

            statement.setBoolean(
                20,
                fitBox.isSelected()
            );

            statement.setString(
                21,
                statusBox.getSelectedItem().toString()
            );

            statement.setString(
                22,
                remarksField.getText().trim()
            );

            statement.setInt(
                23,
                session.getId()
            );

            statement.executeUpdate();

            AuditService.log(
                session.getId(),
                "CREATE",
                "MEDICAL_EXAM",
                typeBox.getSelectedItem()
                + " exam added for employee #"
                + empIdField.getText().trim()
            );

            clearForm();
            load("");

            Dialogs.info(
                this,
                "Medical examination record added."
            );

        } catch (NumberFormatException exception) {
            Dialogs.error(
                this,
                "Employee ID must be numeric. "
                + "Heart Rate, Height and Weight "
                + "must also be numbers."
            );

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }

    private void updateRecord() {
        if (idField.getText().trim().isEmpty()) {
            Dialogs.error(
                this,
                "Select a record to update."
            );

            return;
        }

        String sql =
            "UPDATE medical_examinations SET "
            + "employee_id=?, "
            + "exam_type=?, "
            + "exam_date=?, "
            + "examining_physician=?, "
            + "medical_facility=?, "
            + "blood_type=?, "
            + "blood_pressure=?, "
            + "heart_rate=?, "
            + "height_cm=?, "
            + "weight_kg=?, "
            + "vision_left=?, "
            + "vision_right=?, "
            + "hearing_left=?, "
            + "hearing_right=?, "
            + "chest_xray=?, "
            + "urinalysis=?, "
            + "cbc=?, "
            + "findings=?, "
            + "recommendations=?, "
            + "fit_to_work=?, "
            + "status=?, "
            + "admin_remarks=? "
            + "WHERE id=?";

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setInt(
                1,
                Integer.parseInt(
                    empIdField.getText().trim()
                )
            );

            statement.setString(
                2,
                typeBox.getSelectedItem().toString()
            );

            statement.setString(
                3,
                examDateField.getText().trim()
            );

            statement.setString(
                4,
                physicianField.getText().trim()
            );

            statement.setString(
                5,
                facilityField.getText().trim()
            );

            String bloodType =
                bloodTypeBox
                    .getSelectedItem()
                    .toString()
                    .trim();

            if (bloodType.isEmpty()) {
                statement.setNull(
                    6,
                    Types.VARCHAR
                );
            } else {
                statement.setString(
                    6,
                    bloodType
                );
            }

            statement.setString(
                7,
                bpField.getText().trim()
            );

            setNullableInt(
                statement,
                8,
                hrField.getText().trim()
            );

            setNullableDecimal(
                statement,
                9,
                heightField.getText().trim()
            );

            setNullableDecimal(
                statement,
                10,
                weightField.getText().trim()
            );

            statement.setString(
                11,
                visionLField.getText().trim()
            );

            statement.setString(
                12,
                visionRField.getText().trim()
            );

            statement.setString(
                13,
                hearingLField.getText().trim()
            );

            statement.setString(
                14,
                hearingRField.getText().trim()
            );

            statement.setString(
                15,
                xrayField.getText().trim()
            );

            statement.setString(
                16,
                urineField.getText().trim()
            );

            statement.setString(
                17,
                cbcField.getText().trim()
            );

            statement.setString(
                18,
                findingsArea.getText().trim()
            );

            statement.setString(
                19,
                recommendArea.getText().trim()
            );

            statement.setBoolean(
                20,
                fitBox.isSelected()
            );

            statement.setString(
                21,
                statusBox.getSelectedItem().toString()
            );

            statement.setString(
                22,
                remarksField.getText().trim()
            );

            statement.setInt(
                23,
                Integer.parseInt(
                    idField.getText().trim()
                )
            );

            statement.executeUpdate();

            AuditService.log(
                session.getId(),
                "UPDATE",
                "MEDICAL_EXAM",
                "Updated exam record #"
                + idField.getText()
            );

            clearForm();
            load("");

            Dialogs.info(
                this,
                "Record updated."
            );

        } catch (NumberFormatException exception) {
            Dialogs.error(
                this,
                "Employee ID must be numeric. "
                + "Heart Rate, Height and Weight "
                + "must also be numbers."
            );

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }

    private void setStatus(String newStatus) {
        if (idField.getText().trim().isEmpty()) {
            Dialogs.error(
                this,
                "Select a record first."
            );

            return;
        }

        String actionLabel =
            "COMPLETED".equals(newStatus)
                ? "mark as completed"
                : "flag";

        boolean confirmed =
            Dialogs.confirm(
                this,
                "Are you sure you want to "
                + actionLabel
                + " exam record #"
                + idField.getText()
                + "?"
            );

        if (!confirmed) {
            return;
        }

        String sql =
            "UPDATE medical_examinations "
            + "SET status=? "
            + "WHERE id=?";

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setString(
                1,
                newStatus
            );

            statement.setInt(
                2,
                Integer.parseInt(
                    idField.getText().trim()
                )
            );

            statement.executeUpdate();

            AuditService.log(
                session.getId(),
                "UPDATE",
                "MEDICAL_EXAM",
                "Set exam #"
                + idField.getText()
                + " to "
                + newStatus
            );

            clearForm();
            load("");

            Dialogs.info(
                this,
                "Status updated to "
                + newStatus
                + "."
            );

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }

    private void deleteRecord() {
        if (idField.getText().trim().isEmpty()) {
            Dialogs.error(
                this,
                "Select a record to delete."
            );

            return;
        }

        boolean confirmed =
            Dialogs.confirm(
                this,
                "Delete exam record #"
                + idField.getText()
                + "? This cannot be undone."
            );

        if (!confirmed) {
            return;
        }

        String sql =
            "DELETE FROM medical_examinations "
            + "WHERE id=?";

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setInt(
                1,
                Integer.parseInt(
                    idField.getText().trim()
                )
            );

            statement.executeUpdate();

            AuditService.log(
                session.getId(),
                "DELETE",
                "MEDICAL_EXAM",
                "Deleted exam record #"
                + idField.getText()
            );

            clearForm();
            load("");

            Dialogs.info(
                this,
                "Medical examination record deleted."
            );

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }

    private void clearForm() {
        idField.setText("");
        empIdField.setText("");

        typeBox.setSelectedIndex(0);

        examDateField.setText(
            LocalDate.now().format(
                DateTimeFormatter.ISO_DATE
            )
        );

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

    private void setNullableInt(
            PreparedStatement statement,
            int parameterIndex,
            String value
    ) throws SQLException {
        if (
            value == null
            || value.trim().isEmpty()
        ) {
            statement.setNull(
                parameterIndex,
                Types.INTEGER
            );
        } else {
            statement.setInt(
                parameterIndex,
                Integer.parseInt(value.trim())
            );
        }
    }

    private void setNullableDecimal(
            PreparedStatement statement,
            int parameterIndex,
            String value
    ) throws SQLException {
        if (
            value == null
            || value.trim().isEmpty()
        ) {
            statement.setNull(
                parameterIndex,
                Types.DECIMAL
            );
        } else {
            statement.setDouble(
                parameterIndex,
                Double.parseDouble(value.trim())
            );
        }
    }

    private String safeStr(Object value) {
        return value == null
            ? ""
            : value.toString();
    }

    private void export() {
        JFileChooser chooser =
            new JFileChooser();

        chooser.setSelectedFile(
            new File(
                "medical_examinations.csv"
            )
        );

        int result =
            chooser.showSaveDialog(this);

        if (
            result
            == JFileChooser.APPROVE_OPTION
        ) {
            try {
                CsvExporter.export(
                    table,
                    chooser.getSelectedFile()
                );

                Dialogs.info(
                    this,
                    "CSV exported."
                );

            } catch (Exception exception) {
                Dialogs.error(
                    this,
                    exception.getMessage()
                );
            }
        }
    }
}