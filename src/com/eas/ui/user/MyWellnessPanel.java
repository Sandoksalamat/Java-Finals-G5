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

public class MyWellnessPanel extends JPanel {

    private final UserSession session;
    private final JTable enrolledTable  = new JTable();
    private final JTable upcomingTable  = new JTable();

    public MyWellnessPanel(UserSession session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        load();
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.title("My Wellness Program History"), BorderLayout.WEST);

        JButton refresh = UITheme.button("REFRESH");
        refresh.addActionListener(e -> load());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(refresh);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JPanel buildContent() {
        // My enrollment records
        JPanel myPanel = new JPanel(new BorderLayout(4, 4));
        myPanel.setBorder(BorderFactory.createTitledBorder("My Enrollments & Attendance"));
        enrolledTable.setRowHeight(22);
        enrolledTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        enrolledTable.setEnabled(false);
        myPanel.add(new JScrollPane(enrolledTable), BorderLayout.CENTER);

        // Upcoming programs (all employees can see what's coming)
        JPanel upPanel = new JPanel(new BorderLayout(4, 4));
        upPanel.setBorder(BorderFactory.createTitledBorder("Upcoming & Active Wellness Programs"));
        upcomingTable.setRowHeight(22);
        upcomingTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        upcomingTable.setEnabled(false);
        upPanel.add(new JScrollPane(upcomingTable), BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.add(myPanel,  BorderLayout.CENTER);
        content.add(upPanel,  BorderLayout.SOUTH);
        return content;
    }

    private JLabel buildFooter() {
        JLabel note = new JLabel(
            "  Contact HR to enroll in or inquire about a wellness program."
        );
        note.setFont(new Font("SansSerif", Font.ITALIC, 11));
        note.setForeground(Color.GRAY);
        return note;
    }

    private void load() {
        loadMyEnrollments();
        loadUpcoming();
        enrolledTable.setEnabled(false);
        upcomingTable.setEnabled(false);
    }

    private void loadMyEnrollments() {
        String sql =
            "SELECT wp.id, w.program_title, w.program_type, w.facilitator, " +
            "       w.venue, w.program_date, wp.status, wp.remarks " +
            "FROM wellness_participants wp " +
            "JOIN wellness_programs w ON wp.program_id = w.id " +
            "JOIN employees e         ON wp.employee_id = e.id " +
            "WHERE e.user_id = ? " +
            "ORDER BY w.program_date DESC";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, session.getId());
            try (ResultSet rs = p.executeQuery()) {
                fillTable(enrolledTable, rs, new String[]{
                    "Record ID", "Program", "Type", "Facilitator",
                    "Venue", "Date", "My Status", "Remarks"
                });
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void loadUpcoming() {
        String sql =
            "SELECT id, program_title, program_type, facilitator, venue, " +
            "       program_date, duration_hours, max_participants, status " +
            "FROM wellness_programs " +
            "WHERE status IN ('SCHEDULED','ONGOING') " +
            "ORDER BY program_date ASC";

        try (Connection c  = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            try (ResultSet rs = p.executeQuery()) {
                fillTable(upcomingTable, rs, new String[]{
                    "ID", "Title", "Type", "Facilitator", "Venue",
                    "Date", "Duration (hrs)", "Max Participants", "Status"
                });
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void fillTable(JTable target, ResultSet rs, String[] headers)
            throws SQLException {
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        while (rs.next()) {
            Object[] row = new Object[cols];
            for (int i = 0; i < cols; i++) row[i] = rs.getObject(i + 1);
            model.addRow(row);
        }
        target.setModel(model);
    }
}