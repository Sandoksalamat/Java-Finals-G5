package com.eas.service;

import com.eas.config.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchedulingService {

    public boolean hasScheduleConflict(int employeeId, String date) {
        String query = "SELECT COUNT(*) FROM attendance_records WHERE employee_id = ? AND attendance_date = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, employeeId);
            ps.setString(2, date);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return true; 
        }
    }

    public List<String[]> getAvailableEmployees(String date) {
        List<String[]> employees = new ArrayList<>();
        String query = "SELECT e.id, u.full_name FROM employees e " +
                       "JOIN users u ON e.user_id = u.id " +
                       "JOIN employee_availability ea ON e.id = ea.employee_id " +
                       "WHERE ea.status = 'AVAILABLE' AND ea.unavailable_date != ? " +
                       "AND e.id NOT IN (SELECT employee_id FROM attendance_records WHERE attendance_date = ?)";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, date);
            ps.setString(2, date);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                employees.add(new String[]{String.valueOf(rs.getInt("id")), rs.getString("full_name")});
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return employees;
    }

    public boolean assignReliever(int employeeId, int shiftTemplateId, String date) {
        if (hasScheduleConflict(employeeId, date)) return false;

        String insertQuery = "INSERT INTO shift_assignments (employee_id, shift_template_id, date, status) VALUES (?, ?, ?, 'ASSIGNED')";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertQuery)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, shiftTemplateId);
            ps.setString(3, date);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return false; 
        }
    }

    public boolean approveShiftSwap(int swapRequestId, int adminId, String reason) {
        String updateQuery = "UPDATE shift_swaps SET status = 'APPROVED', approved_by = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateQuery)) {
            ps.setInt(1, adminId);
            ps.setInt(2, swapRequestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return false; 
        }
    }

    public boolean rejectShiftSwap(int swapRequestId, int adminId, String reason) {
        String updateQuery = "UPDATE shift_swaps SET status = 'REJECTED', approved_by = ?, reason = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateQuery)) {
            ps.setInt(1, adminId);
            ps.setString(2, reason);
            ps.setInt(3, swapRequestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return false; 
        }
    }

    public boolean isUnderstaffed(int deptId, int shiftId, String date) {
        String query = "SELECT COUNT(sa.id) as current_count, dsr.min_required_staff " +
                       "FROM department_staffing_requirements dsr " +
                       "LEFT JOIN shift_assignments sa ON sa.date = ? AND sa.shift_template_id = dsr.shift_id " +
                       "WHERE dsr.department_id = ? AND dsr.shift_id = ? GROUP BY dsr.min_required_staff";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, date);
            ps.setInt(2, deptId);
            ps.setInt(3, shiftId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("current_count") < rs.getInt("min_required_staff");
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return false;
    }

    public boolean volunteerForShift(int employeeId, int shiftId, String date) {
        if (hasScheduleConflict(employeeId, date)) return false;

        String query = "INSERT INTO shift_assignments (employee_id, shift_template_id, date, status) VALUES (?, ?, ?, 'PENDING_VOLUNTEER')";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, shiftId);
            ps.setString(3, date);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return false; 
        }
    }
}