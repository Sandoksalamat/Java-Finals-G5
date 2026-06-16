package com.eas.service;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.util.PasswordUtil;
import java.sql.*;

public class AuthService {

    public UserSession login(String username, String password) throws SQLException {
        String sql =
            "SELECT id, username, password_hash, role, full_name, email, status " +
            "FROM users " +
            "WHERE username = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, username.trim());

            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || !PasswordUtil.hash(password).equals(r.getString("password_hash")))
                    return null;

                if (!"ACTIVE".equals(r.getString("status")))
                    throw new SQLException("This account is inactive. Contact the attendance administrator.");

                UserSession session = new UserSession(
                    r.getInt("id"),
                    r.getString("username"),
                    r.getString("full_name"),
                    r.getString("role"),
                    r.getString("email")
                );

                AuditService.log(session.getId(), "LOGIN", "AUTHENTICATION", "Logged into attendance system.");
                return session;
            }
        }
    }

    public void registerEmployee(
            String username, String password, String fullName,
            String email,    String phone,    String employeeNo,
            String address,  String emergency) throws SQLException {

        if (username.trim().length() < 4
                || password.length() < 6
                || fullName.trim().isEmpty()
                || email.trim().isEmpty()
                || employeeNo.trim().isEmpty())
            throw new SQLException(
                "Complete required fields. " +
                "Username needs 4 characters and password needs 6 characters."
            );

        String insertUser =
            "INSERT INTO users (username, password_hash, role, full_name, email, phone, status) " +
            "VALUES (?, ?, 'EMPLOYEE', ?, ?, ?, 'ACTIVE')";

        String insertEmployee =
            "INSERT INTO employees (user_id, employee_no, address, emergency_contact, employment_status) " +
            "VALUES (?, ?, ?, ?, 'ACTIVE')";

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            try {
                int userId;

                try (PreparedStatement p = c.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, username.trim());
                    p.setString(2, PasswordUtil.hash(password));
                    p.setString(3, fullName.trim());
                    p.setString(4, email.trim());
                    p.setString(5, phone.trim());
                    p.executeUpdate();

                    try (ResultSet keys = p.getGeneratedKeys()) {
                        keys.next();
                        userId = keys.getInt(1);
                    }
                }

                try (PreparedStatement p = c.prepareStatement(insertEmployee)) {
                    p.setInt(1, userId);
                    p.setString(2, employeeNo.trim());
                    p.setString(3, address.trim());
                    p.setString(4, emergency.trim());
                    p.executeUpdate();
                }

                c.commit();
                AuditService.log(userId, "REGISTER", "ACCOUNTS", "New employee account registered.");

            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public void changePassword(int userId, String oldPassword, String newPassword) throws SQLException {
        if (newPassword.length() < 6)
            throw new SQLException("New password must have at least 6 characters.");

        String selectHash = "SELECT password_hash FROM users WHERE id = ?";
        String updateHash = "UPDATE users SET password_hash = ? WHERE id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(selectHash)) {

            p.setInt(1, userId);

            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || !PasswordUtil.hash(oldPassword).equals(r.getString(1)))
                    throw new SQLException("Current password is incorrect.");
            }

            try (PreparedStatement u = c.prepareStatement(updateHash)) {
                u.setString(1, PasswordUtil.hash(newPassword));
                u.setInt(2, userId);
                u.executeUpdate();
            }

            AuditService.log(userId, "CHANGE_PASSWORD", "PROFILE", "Changed account password.");
        }
    }
}