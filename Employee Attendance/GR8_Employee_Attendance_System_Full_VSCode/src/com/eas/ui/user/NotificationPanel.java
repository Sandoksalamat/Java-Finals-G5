package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.util.*;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class NotificationPanel extends UserTablePanel {

    public NotificationPanel(UserSession s) {
        super(s, "My Notifications",
            "SELECT id, title, message_body, notification_type, created_at, read_status " +
            "FROM notifications " +
            "WHERE user_id = ? " +
            "ORDER BY id DESC",

            "SELECT id, title, message_body, notification_type, created_at, read_status " +
            "FROM notifications " +
            "WHERE user_id = ? " +
            "  AND (title LIKE ? OR message_body LIKE ? OR notification_type LIKE ?) " +
            "ORDER BY id DESC",

            true
        );

        JButton mark = UITheme.button("MARK SELECTED READ");
        mark.addActionListener(e -> mark());

        JPanel actions = new JPanel();
        actions.add(mark);

        add(actions, BorderLayout.SOUTH);
    }

    private void mark() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        String sql =
            "UPDATE notifications " +
            "SET read_status = 'READ' " +
            "WHERE id = ? AND user_id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, Integer.parseInt(table.getValueAt(row, 0).toString()));
            p.setInt(2, session.getId());
            p.executeUpdate();
            load("");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}