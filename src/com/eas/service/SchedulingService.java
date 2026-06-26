package com.eas.service;

import com.eas.config.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchedulingService {

    public boolean hasScheduleConflict(int employeeId, String date) {
        String assignmentQuery = "SELECT COUNT(*) FROM shift_assignments WHERE employee_id = ? AND effective_from = ?";
        String availabilityQuery = "SELECT COUNT(*) FROM employee_availability WHERE employee_id = ? AND unavailable_date = ? AND status = 'UNAVAILABLE'";

        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(assignmentQuery)) {
                ps.setInt(1, employeeId);
                ps.setString(2, date);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) return true;
            }
            try (PreparedStatement ps = conn.prepareStatement(availabilityQuery)) {
                ps.setInt(1, employeeId);
                ps.setString(2, date);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public List<String[]> getAvailableEmployees(String date) {
        List<String[]> employees = new ArrayList<>();
        String query = "SELECT e.id, u.full_name FROM employees e " +
                       "JOIN users u ON e.user_id = u.id " +
                       "WHERE e.id NOT IN (SELECT employee_id FROM shift_assignments WHERE effective_from = ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, date);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                employees.add(new String[]{String.valueOf(rs.getInt("id")), rs.getString("full_name")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employees;
    }

    public boolean assignReliever(int employeeId, int shiftId, String date) {
        if (hasScheduleConflict(employeeId, date)) return false;

        String insertQuery = "INSERT INTO shift_assignments (employee_id, shift_id, effective_from) VALUES (?, ?, ?)";
        String updateShiftQuery = "UPDATE open_shifts SET is_taken = 1 WHERE shift_id = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                ps.setInt(1, employeeId);
                ps.setInt(2, shiftId);
                ps.setString(3, date);
                ps.executeUpdate();
            }
            
            try (PreparedStatement ps2 = conn.prepareStatement(updateShiftQuery)) {
                ps2.setInt(1, shiftId);
                ps2.executeUpdate();
            }
            
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean volunteerForShift(int employeeId, int shiftId, String date) {
        return assignReliever(employeeId, shiftId, date);
    }
}