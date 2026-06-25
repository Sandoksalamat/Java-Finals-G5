package com.eas.ui.user;

import com.eas.model.UserSession;
import java.awt.*; // Siguraduhing nandito ang import
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class MyShiftSwapPanel extends JPanel {

    private final UserSession session;
    private JTable requestTable;
    private DefaultTableModel tableModel;
    private JTextField txtTargetEmployeeId;
    private JTextArea txtReason;
    private int currentEmployeeId = -1;

    public MyShiftSwapPanel(UserSession s) {
        this.session = s;
        setLayout(new BorderLayout()); // Border layout para sa JTabbedPane
        setBackground(Color.WHITE);

        resolveEmployeeId();
        initUI(); // Dito natin pagsasamahin ang lahat
        refreshTableData();
    }

    private void resolveEmployeeId() {
        try (Connection conn = com.eas.config.Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM employees WHERE user_id = ?")) {
            ps.setInt(1, session.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) currentEmployeeId = rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void initUI() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // --- TAB 1: Shift Swap Request ---
        JPanel swapPanel = new JPanel(new BorderLayout(15, 15));
        swapPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        swapPanel.setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Create Shift Exchange Request"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Target Employee ID:"), gbc);
        gbc.gridx = 1;
        txtTargetEmployeeId = new JTextField(15);
        formPanel.add(txtTargetEmployeeId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Justification:"), gbc);
        gbc.gridx = 1;
        txtReason = new JTextArea(3, 20);
        formPanel.add(new JScrollPane(txtReason), gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        JButton btnSubmit = new JButton("SUBMIT REQUEST");
        btnSubmit.addActionListener(e -> submitRequest());
        formPanel.add(btnSubmit, gbc);

        tableModel = new DefaultTableModel(new String[]{"ID", "Target ID", "Reason", "Status"}, 0);
        requestTable = new JTable(tableModel);
        
        swapPanel.add(formPanel, BorderLayout.WEST);
        swapPanel.add(new JScrollPane(requestTable), BorderLayout.CENTER);

        // --- TAB 2: Volunteer ---
        // Ipinapasa natin ang session.getId() para sa volunteer logic
        EmployeeVolunteerPanel volunteerPanel = new EmployeeVolunteerPanel(session.getId());

        tabbedPane.addTab("Shift Swap", swapPanel);
        tabbedPane.addTab("Volunteer for Open Shifts", volunteerPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void submitRequest() {
        // ... (Kopyahin ang dati mong code sa submitRequest dito) ...
        // Siguraduhing tama ang INSERT statement base sa database structure mo
    }

    public void refreshTableData() {
        // ... (Kopyahin ang dati mong code sa refreshTableData dito) ...
    }
}