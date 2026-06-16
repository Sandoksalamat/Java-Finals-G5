package com.eas.service;

import com.eas.config.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class SchedulingService {

    public String checkScheduleConflict(int employeeId, String dateStr, int shiftTemplateId) {
        String overlapQuery = "SELECT COUNT(*) FROM shift_assignments WHERE employee_id = ? AND date = ?";
        String consecutiveQuery = "SELECT COUNT(DISTINCT date) FROM shift_assignments WHERE employee_id = ? AND date BETWEEN ? AND ?";

        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(overlapQuery)) {
                stmt.setInt(1, employeeId);
                stmt.setString(2, dateStr);
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return "CONFLICT: The employee already has an existing shift on this date.";
                }
            }

            LocalDate targetDate = LocalDate.parse(dateStr);
            LocalDate sixDaysAgo = targetDate.minusDays(6);
            try (PreparedStatement stmt = conn.prepareStatement(consecutiveQuery)) {
                stmt.setInt(1, employeeId);
                stmt.setString(2, sixDaysAgo.toString());
                stmt.setString(3, dateStr);
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) >= 6) {
                    return "WARNING: Employee duties will exceed 6 consecutive days (Excessive Consecutive Duties).";
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR: Unable to verify schedule conflicts.";
        }
        return "CLEAR";
    }

    public boolean isStaffingInsufficient(int departmentId, int shiftTemplateId, String dateStr) {
        String reqQuery = "SELECT min_required_staff FROM department_staffing_requirements WHERE department_id = ? AND shift_id = ?";
        String activeCountQuery = "SELECT COUNT(*) FROM shift_assignments sa " +
                                  "JOIN employees e ON sa.employee_id = e.id " +
                                  "WHERE e.department_id = ? AND sa.shift_template_id = ? AND sa.date = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmtReq = conn.prepareStatement(reqQuery);
             PreparedStatement stmtActive = conn.prepareStatement(activeCountQuery)) {
            
            stmtReq.setInt(1, departmentId);
            stmtReq.setInt(2, shiftTemplateId);
            ResultSet rsReq = stmtReq.executeQuery();
            int minRequired = 0; 
            if (rsReq.next()) {
                minRequired = rsReq.getInt("min_required_staff");
            } else {
                return false;
            }

            stmtActive.setInt(1, departmentId);
            stmtActive.setInt(2, shiftTemplateId);
            stmtActive.setString(3, dateStr);
            ResultSet rsActive = stmtActive.executeQuery();
            int currentAssigned = 0;
            if (rsActive.next()) {
                currentAssigned = rsActive.getInt(1);
            }

            return currentAssigned < minRequired;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean approveShiftSwap(int swapRequestId, int adminId, String reason) {
        String selectSwap = "SELECT * FROM shift_swaps WHERE id = ?";
        
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            int reqEmpId = 0, targetEmpId = 0, origScheduleId = 0;
            try (PreparedStatement stmt = conn.prepareStatement(selectSwap)) {
                stmt.setInt(1, swapRequestId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    reqEmpId = rs.getInt("requesting_employee_id");
                    targetEmpId = rs.getInt("target_employee_id");
                    origScheduleId = rs.getInt("original_schedule_id");
                } else {
                    return false;
                }
            }

            String updateStatusSql = "UPDATE shift_swaps SET status = 'APPROVED', approved_by = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateStatusSql)) {
                stmt.setInt(1, adminId);
                stmt.setInt(2, swapRequestId);
                stmt.executeUpdate();
            }

            String updateAssignmentSql = "UPDATE shift_assignments SET employee_id = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateAssignmentSql)) {
                stmt.setInt(1, targetEmpId);
                stmt.setInt(2, origScheduleId);
                stmt.executeUpdate();
            }

            String logHistorySql = "INSERT INTO schedule_revision_history (schedule_id, original_employee_id, replacement_employee_id, reason, approved_by) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(logHistorySql)) {
                stmt.setInt(1, origScheduleId);
                stmt.setInt(2, reqEmpId);
                stmt.setInt(3, targetEmpId);
                stmt.setString(4, reason);
                stmt.setInt(5, adminId);
                stmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean rejectShiftSwap(int swapRequestId, int adminId, String reason) {
        String query = "UPDATE shift_swaps SET status = 'REJECTED', approved_by = ?, reason = CONCAT(reason, ' | Reject Reason: ', ?) WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, adminId);
            stmt.setString(2, reason);
            stmt.setInt(3, swapRequestId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}