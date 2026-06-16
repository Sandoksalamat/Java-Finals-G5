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

public class MyHealthSafetyPanel extends JPanel {

    private final UserSession session;
    private final JTable incidentTable   = new JTable();
    private final JTable restrictionTable = new JTable();

    public MyHealthSafetyPanel(UserSession session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        load();
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.title("My Health & Safety Records"), BorderLayout.WEST);

        JButton refresh = UITheme.button("REFRESH");
        refresh.addActionListener(e -> load());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(refresh);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JPanel buildContent() {
        incidentTable.setRowHeight(22);
        incidentTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        incidentTable.setEnabled(false);

        restrictionTable.setRowHeight(22);
        restrictionTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        restrictionTable.setEnabled(false);

        JPanel incPanel = new JPanel(new BorderLayout(4, 4));
        incPanel.setBorder(BorderFactory.createTitledBorder("My Reported Incidents"));
        incPanel.add(new JScrollPane(incidentTable), BorderLayout.CENTER);

        JPanel resPanel = new JPanel(new BorderLayout(4, 4));
        resPanel.setBorder(BorderFactory.createTitledBorder("My Active Medical Restrictions"));
        resPanel.add(new JScrollPane(restrictionTable), BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.add(incPanel, BorderLayout.CENTER);
        content.add(resPanel, BorderLayout.SOUTH);
        return content;
    }

    private JLabel buildFooter() {
        JLabel note = new JLabel(
            "  Incident reports and restrictions are managed by HR / Safety Officer. " +
            "Contact HR for updates or corrections."
        );
        note.setFont(new Font("SansSerif", Font.ITALIC, 11));
        note.setForeground(Color.GRAY);
        return note;
    }

    private void load() {
        loadMyIncidents();
        loadMyRestrictions();
    }

    private void loadMyIncidents() {
        String sql =
            "SELECT i.id, i.incident_date, i.incident_type, i.location, " +
            "       i.description, i.immediate_action, i.status, i.created_at " +
            "FROM health_safety_incidents i " +
            "JOIN employees e ON i.employee_id = e.id " +
            "WHERE e.user_id = ? " +
            "ORDER BY i.incident_date DESC";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, session.getId());
            try (ResultSet rs = p.executeQuery()) {
                fillTable(incidentTable, rs, new String[]{
                    "ID","Date","Type","Location",
                    "Description","Immediate Action","Status","Created At"
                });
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
        incidentTable.setEnabled(false);
    }

    private void loadMyRestrictions() {
        String sql =
            "SELECT mr.id, mr.restriction_type, mr.start_date, mr.end_date, " +
            "       mr.prescribed_by, mr.details, mr.status, mr.admin_remarks " +
            "FROM medical_restrictions mr " +
            "JOIN employees e ON mr.employee_id = e.id " +
            "WHERE e.user_id = ? " +
            "ORDER BY mr.status ASC, mr.start_date DESC";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, session.getId());
            try (ResultSet rs = p.executeQuery()) {
                fillTable(restrictionTable, rs, new String[]{
                    "ID","Restriction Type","Start Date","End Date",
                    "Prescribed By","Details","Status","Admin Remarks"
                });
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
        restrictionTable.setEnabled(false);
    }

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
}