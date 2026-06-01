package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.Dialogs;
import com.eas.util.UITheme;
import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class AdminMessagePanel extends ReadOnlyQueryPanel {
    private final UserSession session;
    private final JTextField receiverId = new JTextField(7);
    private final JTextField subject = new JTextField(22);
    private final JTextArea message = new JTextArea(2, 36);

    public AdminMessagePanel(UserSession session) {
        super("Employee Messages and Administrator Replies",
            "SELECT m.id,m.sender_id,s.full_name AS sender,m.receiver_id,r.full_name AS receiver,m.subject,m.message_body,m.sent_at,m.read_status FROM messages m JOIN users s ON m.sender_id=s.id JOIN users r ON m.receiver_id=r.id ORDER BY m.id DESC",
            "SELECT m.id,m.sender_id,s.full_name AS sender,m.receiver_id,r.full_name AS receiver,m.subject,m.message_body,m.sent_at,m.read_status FROM messages m JOIN users s ON m.sender_id=s.id JOIN users r ON m.receiver_id=r.id WHERE s.full_name LIKE ? OR r.full_name LIKE ? OR m.subject LIKE ? ORDER BY m.id DESC",
            "messages");
        this.session = session;
        JPanel compose = new JPanel();
        compose.setBorder(javax.swing.BorderFactory.createTitledBorder("Send Reply or New Message"));
        compose.add(new JLabel("Receiver User ID"));
        compose.add(receiverId);
        compose.add(new JLabel("Subject"));
        compose.add(subject);
        compose.add(new javax.swing.JScrollPane(message));
        JButton useSender = UITheme.button("REPLY TO SELECTED SENDER");
        JButton send = UITheme.button("SEND");
        compose.add(useSender);
        compose.add(send);
        add(compose, BorderLayout.SOUTH);
        useSender.addActionListener(event -> useSelectedSender());
        send.addActionListener(event -> send());
    }

    private void useSelectedSender() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Dialogs.error(this, "Select a message first.");
            return;
        }
        receiverId.setText(table.getValueAt(row, 1).toString());
        subject.setText("RE: " + table.getValueAt(row, 5).toString());
    }

    private void send() {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO messages(sender_id,receiver_id,subject,message_body) VALUES(?,?,?,?)")) {
            statement.setInt(1, session.getId());
            statement.setInt(2, Integer.parseInt(receiverId.getText().trim()));
            statement.setString(3, subject.getText().trim());
            statement.setString(4, message.getText().trim());
            statement.executeUpdate();
            AuditService.log(session.getId(), "MESSAGE", "MESSAGES", "Sent administrator reply.");
            load("");
            Dialogs.info(this, "Message sent.");
        } catch (Exception exception) {
            Dialogs.error(this, exception.getMessage());
        }
    }
}
