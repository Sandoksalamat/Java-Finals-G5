package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.*;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class UserMessagePanel extends UserTablePanel {

    private final JTextField subject = new JTextField(22);
    private final JTextArea body = new JTextArea(2, 34);

    public UserMessagePanel(UserSession s) {
        super(s, "Messages to and from Administrator",
            "SELECT m.id, se.full_name sender, re.full_name receiver, " +
            "       m.subject, m.message_body, m.sent_at, m.read_status " +
            "FROM messages m " +
            "JOIN users se ON m.sender_id = se.id " +
            "JOIN users re ON m.receiver_id = re.id " +
            "WHERE m.sender_id = ? OR m.receiver_id = ? " +
            "ORDER BY m.id DESC",

            "SELECT m.id, se.full_name sender, re.full_name receiver, " +
            "       m.subject, m.message_body, m.sent_at, m.read_status " +
            "FROM messages m " +
            "JOIN users se ON m.sender_id = se.id " +
            "JOIN users re ON m.receiver_id = re.id " +
            "WHERE (m.sender_id = ? OR m.receiver_id = ?) " +
            "  AND (m.subject LIKE ? OR m.message_body LIKE ? OR se.full_name LIKE ?) " +
            "ORDER BY m.id DESC",

            false
        );

        JButton send = UITheme.button("SEND TO ADMIN");
        send.addActionListener(e -> send());

        JPanel compose = new JPanel();
        compose.add(new JLabel("Subject"));
        compose.add(subject);
        compose.add(new JScrollPane(body));
        compose.add(send);

        add(compose, BorderLayout.SOUTH);
    }

    private void send() {
        String findAdmin =
            "SELECT id FROM users " +
            "WHERE role = 'ADMIN' AND status = 'ACTIVE' " +
            "ORDER BY id LIMIT 1";

        String insertMessage =
            "INSERT INTO messages (sender_id, receiver_id, subject, message_body) " +
            "VALUES (?, ?, ?, ?)";

        try (Connection c = Database.getConnection();
             PreparedStatement adminQuery = c.prepareStatement(findAdmin)) {

            try (ResultSet r = adminQuery.executeQuery()) {
                if (!r.next()) throw new SQLException("No active administrator found.");

                try (PreparedStatement insert = c.prepareStatement(insertMessage)) {
                    insert.setInt(1, session.getId());
                    insert.setInt(2, r.getInt(1));
                    insert.setString(3, subject.getText().trim());
                    insert.setString(4, body.getText().trim());
                    insert.executeUpdate();
                }
            }

            AuditService.log(session.getId(), "MESSAGE", "MESSAGES", "Sent message to admin.");
            load("");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}