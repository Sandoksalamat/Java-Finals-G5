package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.util.*;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class UserHomePanel extends JPanel {

    private final UserSession session;
    private final JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12));

    public UserHomePanel(UserSession s) {
        session = s;

        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JButton refresh = UITheme.button("REFRESH");
        refresh.addActionListener(e -> load());

        add(UITheme.title("My Attendance Dashboard"), BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
        add(refresh, BorderLayout.SOUTH);

        load();
    }

    private void load() {
        cards.removeAll();

        card("Today's Status",
            "SELECT COALESCE(MAX(ar.attendance_status), 'NO RECORD') " +
            "FROM employees e " +
            "LEFT JOIN attendance_records ar ON e.id = ar.employee_id " +
            "    AND ar.attendance_date = CURDATE() " +
            "WHERE e.user_id = ?"
        );

        card("Late Minutes This Month",
            "SELECT COALESCE(SUM(ar.late_minutes), 0) " +
            "FROM employees e " +
            "LEFT JOIN attendance_records ar ON e.id = ar.employee_id " +
            "    AND MONTH(ar.attendance_date) = MONTH(CURDATE()) " +
            "    AND YEAR(ar.attendance_date) = YEAR(CURDATE()) " +
            "WHERE e.user_id = ?"
        );

        card("Pending Leave",
            "SELECT COUNT(*) " +
            "FROM employees e " +
            "JOIN leave_requests l ON e.id = l.employee_id " +
            "WHERE e.user_id = ? AND l.status = 'PENDING'"
        );

        card("Pending OT",
            "SELECT COUNT(*) " +
            "FROM employees e " +
            "JOIN overtime_requests o ON e.id = o.employee_id " +
            "WHERE e.user_id = ? AND o.status = 'PENDING'"
        );

        revalidate();
        repaint();
    }

    private void card(String label, String query) {
        String value = "-";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(query)) {

            p.setInt(1, session.getId());

            try (ResultSet r = p.executeQuery()) {
                if (r.next()) value = r.getString(1);
            }

        } catch (Exception ignored) {}

        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.TEAL),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 23));
        valueLabel.setForeground(UITheme.NAVY);

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(new JLabel(label), BorderLayout.SOUTH);
        cards.add(card);
    }
}