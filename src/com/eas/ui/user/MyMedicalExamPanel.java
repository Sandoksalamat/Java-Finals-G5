package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.util.Dialogs;
import com.eas.util.UITheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class MyMedicalExamPanel extends JPanel {

    private final UserSession session;
    private final JTable table = new JTable();

    public MyMedicalExamPanel(UserSession session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildTopBar(),          BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildFooter(),          BorderLayout.SOUTH);

        load();
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.title("My Medical Examination Records"), BorderLayout.WEST);

        JButton viewResult = UITheme.button("VIEW RESULT");
        JButton refresh = UITheme.button("REFRESH");
        viewResult.addActionListener(e -> showSelectedResult());
        refresh.addActionListener(e -> load());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(viewResult);
        right.add(refresh);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JLabel buildFooter() {
        JLabel note = new JLabel(
            "  Medical examination records are managed by HR. " +
            "Contact HR for corrections or to request your latest results."
        );
        note.setFont(new Font("SansSerif", Font.ITALIC, 11));
        note.setForeground(Color.GRAY);
        return note;
    }

    private void load() {
        String sql =
            "SELECT me.id, me.exam_type, me.exam_date, me.examining_physician, " +
            "       me.medical_facility, me.blood_type, me.blood_pressure, " +
            "       me.heart_rate, me.height_cm, me.weight_kg, " +
            "       me.vision_left, me.vision_right, " +
            "       me.hearing_left, me.hearing_right, " +
            "       me.chest_xray, me.urinalysis, me.cbc, " +
            "       me.fit_to_work, me.findings, me.recommendations, " +
            "       me.status, me.admin_remarks " +
            "FROM medical_examinations me " +
            "JOIN employees e ON me.employee_id = e.id " +
            "WHERE e.user_id = ? " +
            "ORDER BY me.exam_date DESC";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, session.getId());
            try (ResultSet rs = p.executeQuery()) {
                fillTable(rs);
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }

        table.setEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }


    private void showSelectedResult() {
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
            section("Examination Information") +
            item("Exam Type", model.getValueAt(row, 1)) +
            item("Exam Date", model.getValueAt(row, 2)) +
            item("Physician", model.getValueAt(row, 3)) +
            item("Medical Facility", model.getValueAt(row, 4)) +
            section("Vital Signs") +
            item("Blood Type", model.getValueAt(row, 5)) +
            item("Blood Pressure", model.getValueAt(row, 6)) +
            item("Heart Rate", model.getValueAt(row, 7)) +
            item("Height", valueWithUnit(model.getValueAt(row, 8), " cm")) +
            item("Weight", valueWithUnit(model.getValueAt(row, 9), " kg")) +
            section("Test Results") +
            item("Vision — Left", model.getValueAt(row, 10)) +
            item("Vision — Right", model.getValueAt(row, 11)) +
            item("Hearing — Left", model.getValueAt(row, 12)) +
            item("Hearing — Right", model.getValueAt(row, 13)) +
            item("Chest X-Ray", model.getValueAt(row, 14)) +
            item("Urinalysis", model.getValueAt(row, 15)) +
            item("CBC", model.getValueAt(row, 16)) +
            section("Assessment") +
            item("Fit to Work", model.getValueAt(row, 17)) +
            item("Physician Findings", model.getValueAt(row, 18)) +
            item("Recommendations", model.getValueAt(row, 19)) +
            item("Status", model.getValueAt(row, 20)) +
            item("Admin Remarks", model.getValueAt(row, 21)) +
            "</body></html>";

        JEditorPane resultPane = new JEditorPane("text/html", html);
        resultPane.setEditable(false);
        resultPane.setCaretPosition(0);

        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "My Medical Examination Result",
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

        dialog.setSize(720, 620);
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
               "<tr><td width='34%' valign='top'><b>" + html(label) +
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

    private void fillTable(ResultSet rs) {
        String[] headers = {
            "ID", "Exam Type", "Exam Date", "Physician", "Facility",
            "Blood Type", "BP", "Heart Rate", "Height (cm)", "Weight (kg)",
            "Vision L", "Vision R", "Hearing L", "Hearing R",
            "Chest X-Ray", "Urinalysis", "CBC",
            "Fit to Work", "Findings", "Recommendations",
            "Status", "Admin Remarks"
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
                    // col 17 = fit_to_work BOOLEAN → Yes/No
                    if (i == 17) row[i] = rs.getInt(i + 1) == 1 ? "Yes" : "No";
                    else         row[i] = rs.getObject(i + 1);
                }
                model.addRow(row);
            }
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
        }

        table.setModel(model);
    }
}