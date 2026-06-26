package com.eas.auth;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuthService;
import com.eas.ui.admin.AdminDashboardFrame;
import com.eas.ui.user.UserDashboardFrame;
import com.eas.util.*;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

public class LoginFrame extends JFrame {

    private final JTextField     username = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);

    public LoginFrame() {
        setTitle("GR 5 - Employee Attendance System | Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(540, 430);
        setLocationRelativeTo(null);

        JButton login    = UITheme.button("LOGIN");
        JButton register = UITheme.button("REGISTER EMPLOYEE ACCOUNT");
        JButton test     = UITheme.button("TEST DATABASE CONNECTION");
        login.addActionListener(e    -> login());
        register.addActionListener(e -> { dispose(); new RegisterFrame().setVisible(true); });
        test.addActionListener(e     -> testConnection());
        getRootPane().setDefaultButton(login);

        JLabel heading = UITheme.title("EMPLOYEE ATTENDANCE SYSTEM");
        heading.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(30, 15, 20, 15));
        form.add(new JLabel("Username"));
        form.add(username);
        form.add(new JLabel("Password"));
        form.add(password);
        form.add(login);
        form.add(register);
        form.add(test);

        JLabel hint = new JLabel("Demo Admin: admin / Admin@123    Demo Employee: employee1 / User@123");
        hint.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(28, 50, 25, 50));
        root.add(heading, BorderLayout.NORTH);
        root.add(form,    BorderLayout.CENTER);
        root.add(hint,    BorderLayout.SOUTH);

        add(root);
    }

    private void login() {
        try {
            UserSession session = new AuthService().login(
                username.getText(),
                new String(password.getPassword())
            );

            if (session == null) {
                Dialogs.error(this, "Invalid username or password.");
                return;
            }

            dispose();
            if (session.isAdmin()) new AdminDashboardFrame(session).setVisible(true);
            else                   new UserDashboardFrame(session).setVisible(true);

        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void testConnection() {
        try {
            boolean ok = Database.testConnection();
            Dialogs.info(this, ok ? "Database connection successful." : "Database connection failed.");
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}