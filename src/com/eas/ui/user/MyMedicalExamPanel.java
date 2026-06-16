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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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

        JButton refresh = UITheme.button("REFRESH");
        refresh.addActionListener(e -> load());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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

        table.setEnabled(false);
        table.setRowHeight(22);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
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