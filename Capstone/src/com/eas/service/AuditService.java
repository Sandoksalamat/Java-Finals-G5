package com.eas.service;

import com.eas.config.Database;
import java.sql.*;

public final class AuditService {

    private AuditService() {}

    public static void log(Integer userId, String action, String module, String description) {
        String sql =
            "INSERT INTO audit_logs (user_id, action_type, module_name, description) " +
            "VALUES (?, ?, ?, ?)";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            if (userId == null) p.setNull(1, Types.INTEGER);
            else                p.setInt(1, userId);

            p.setString(2, action);
            p.setString(3, module);
            p.setString(4, description);
            p.executeUpdate();

        } catch (Exception ignored) {}
    }
}