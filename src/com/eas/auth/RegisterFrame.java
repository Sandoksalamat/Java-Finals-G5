package com.eas.auth;

import com.eas.service.AuthService;
import com.eas.util.*;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

public class RegisterFrame extends JFrame {

    private final JTextField     user       = new JTextField();
    private final JPasswordField pass       = new JPasswordField();
    private final JTextField     name       = new JTextField();
    private final JTextField     email      = new JTextField();
    private final JTextField     phone      = new JTextField();
    private final JTextField     employeeNo = new JTextField();
    private final JTextField     address    = new JTextField();
    private final JTextField     emergency  = new JTextField();

    public RegisterFrame() {
        setTitle("Employee Attendance System | Register Account");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 570);
        setLocationRelativeTo(null);

        JButton save = UITheme.button("REGISTER");
        JButton back = UITheme.button("BACK TO LOGIN");
        save.addActionListener(e -> save());
        back.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 9));
        form.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));
        addRow(form, "Username *",         user);
        addRow(form, "Password *",         pass);
        addRow(form, "Full Name *",        name);
        addRow(form, "Email *",            email);
        addRow(form, "Phone",              phone);
        addRow(form, "Employee Number *",  employeeNo);
        addRow(form, "Address",            address);
        addRow(form, "Emergency Contact",  emergency);

        JPanel buttons = new JPanel();
        buttons.add(save);
        buttons.add(back);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(20, 45, 20, 45));
        root.add(UITheme.title("REGISTER EMPLOYEE ACCOUNT"), BorderLayout.NORTH);
        root.add(form,    BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        add(root);
    }

    private void addRow(JPanel panel, String label, JComponent field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void save() {
        try {
            new AuthService().registerEmployee(
                user.getText(),
                new String(pass.getPassword()),
                name.getText(),
                email.getText(),
                phone.getText(),
                employeeNo.getText(),
                address.getText(),
                emergency.getText()
            );

            Dialogs.info(this, "Account registered. You can now log in.");
            dispose();
            new LoginFrame().setVisible(true);

        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}