package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.util.*;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class UserTablePanel extends JPanel {

    protected final UserSession session;
    protected final JTable table = new JTable();

    private final JTextField search = new JTextField(22);
    private final String base, query;
    private final boolean oneId;

    public UserTablePanel(UserSession s, String title, String base, String query, boolean oneId) {
        session = s;
        this.base = base;
        this.query = query;
        this.oneId = oneId;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.title(title), BorderLayout.WEST);

        JPanel actions = new JPanel();
        JButton find = UITheme.button("SEARCH");
        JButton refresh = UITheme.button("REFRESH");
        actions.add(search);
        actions.add(find);
        actions.add(refresh);
        top.add(actions, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        find.addActionListener(e -> load(search.getText()));
        refresh.addActionListener(e -> {
            search.setText("");
            load("");
        });

        load("");
    }

    protected void load(String key) {
        boolean searching = key != null && !key.trim().isEmpty();
        String sql = searching ? query : base;

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            int i = 1;
            p.setInt(i++, session.getId());
            if (!oneId) p.setInt(i++, session.getId());

            if (searching) {
                String q = "%" + key.trim() + "%";
                while (i <= count(sql)) p.setString(i++, q);
            }

            try (ResultSet r = p.executeQuery()) {
                TableUtil.load(table, r);
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private int count(String s) {
        int n = 0;
        for (char c : s.toCharArray())
            if (c == '?') n++;
        return n;
    }
}