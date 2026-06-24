package com.eas.service;

import com.eas.config.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchedulingService {

    // 1. Conflict Detection: Check kung may record na sa attendance para sa petsang iyon
    public boolean hasScheduleConflict(int employeeId, String date) {
        String query = "SELECT COUNT(*) FROM attendance_records WHERE employee_id = ? AND attendance_date = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, employeeId);
            ps.setString(2, date);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); return true; }
    }

    // 2. Kuhanin ang mga empleyadong walang attendance record sa petsang iyon
    public List<String[]> getAvailableEmployees(String date) {
        List<String[]> employees = new ArrayList<>();
        // JOIN sa users table para makuha ang 'name'
        String query = "SELECT e.id, u.name FROM employees e " +
                       "JOIN users u ON e.user_id = u.id " +
                       "WHERE e.id NOT IN (SELECT employee_id FROM attendance_records WHERE attendance_date = ?)";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, date);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                employees.add(new String[]{String.valueOf(rs.getInt("id")), rs.getString("name")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return employees;
    }

    // 3. Assign ng Reliever
    public boolean assignReliever(int employeeId, int shiftTemplateId, String date) {
        if (hasScheduleConflict(employeeId, date)) return false;

        // Base sa schema mo, shift_assignments ay ang table para sa scheduling
        String insertQuery = "INSERT INTO shift_assignments (employee_id, shift_template_id, date, status) VALUES (?, ?, ?, 'ASSIGNED')";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertQuery)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, shiftTemplateId);
            ps.setString(3, date);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // 4. Approve Shift Swap
    public boolean approveShiftSwap(int swapRequestId, int adminId, String reason) {
        String updateQuery = "UPDATE shift_swaps SET status = 'APPROVED', approved_by = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateQuery)) {
            ps.setInt(1, adminId);
            ps.setInt(2, swapRequestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // 5. Reject Shift Swap
    public boolean rejectShiftSwap(int swapRequestId, int adminId, String reason) {
        String updateQuery = "UPDATE shift_swaps SET status = 'REJECTED', approved_by = ?, reason = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateQuery)) {
            ps.setInt(1, adminId);
            ps.setString(2, reason);
            ps.setInt(3, swapRequestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}