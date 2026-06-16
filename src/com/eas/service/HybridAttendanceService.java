package com.eas.service;

import com.eas.config.Database;
import com.eas.model.OffSiteRequest;
import com.eas.model.OffSiteAccomplishment;
import com.eas.model.HybridReportRow;

import java.sql.*;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

public class HybridAttendanceService {

    public boolean submitOffSiteRequest(OffSiteRequest request) {
        String sql = "INSERT INTO offsite_requests (employee_id, request_type, start_date, end_date, destination_or_location, purpose) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, request.getId());
            pstmt.setString(2, request.getRequestType());
            pstmt.setDate(3, java.sql.Date.valueOf(request.getStartDate()));
            pstmt.setDate(4, java.sql.Date.valueOf(request.getEndDate()));
            pstmt.setString(5, request.getDestinationOrLocation());
            pstmt.setString(6, request.getPurpose());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, 
        "Database Error: " + e.getMessage(), 
        "SQL Exception Triggered", 
        javax.swing.JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean reviewOffSiteRequest(int requestId, int adminUserId, String status, String remarks) {
        String sql = "UPDATE offsite_requests SET status = ?, reviewed_by = ?, reviewed_at = NOW(), admin_remarks = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, adminUserId);
            pstmt.setString(3, remarks);
            pstmt.setInt(4, requestId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean processMobileClockEvent(int employeeId, String logType, double latitude, double longitude, String attachmentPath) {
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement masterStmt = null;
        PreparedStatement logStmt = null;
        ResultSet rs = null;

        LocalDate today = LocalDate.now();

        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            String checkApprovalSql = "SELECT request_type FROM offsite_requests WHERE employee_id = ? AND ? BETWEEN start_date AND end_date AND status = 'APPROVED' LIMIT 1";
            checkStmt = conn.prepareStatement(checkApprovalSql);
            checkStmt.setInt(1, employeeId);
            checkStmt.setDate(2, Date.valueOf(today));
            rs = checkStmt.executeQuery();

            String verifiedClassification = "PRESENT";
            if (rs.next()) {
                verifiedClassification = rs.getString("request_type"); 
            } else {
                System.out.println("Clocking rejected: No approved off-site template entry authorized for today.");
                conn.rollback();
                return false;
            }

            String findRecordSql = "SELECT id FROM attendance_records WHERE employee_id = ? AND attendance_date = ?";
            PreparedStatement findStmt = conn.prepareStatement(findRecordSql);
            findStmt.setInt(1, employeeId);
            findStmt.setDate(2, Date.valueOf(today));
            ResultSet recordRs = findStmt.executeQuery();

            int attendanceId;
            if (recordRs.next()) {
                attendanceId = recordRs.getInt("id");
                String updateField = logType.equalsIgnoreCase("TIME_IN") ? "clock_in = NOW()" : "clock_out = NOW()";
                String updateMasterSql = "UPDATE attendance_records SET " + updateField + ", attendance_status = ? WHERE id = ?";
                masterStmt = conn.prepareStatement(updateMasterSql);
                masterStmt.setString(1, verifiedClassification);
                masterStmt.setInt(2, attendanceId);
                masterStmt.executeUpdate();
            } else {
                String insertMasterSql = "INSERT INTO attendance_records (employee_id, attendance_date, clock_in, attendance_status, source) VALUES (?, ?, NOW(), ?, 'MANUAL')";
                masterStmt = conn.prepareStatement(insertMasterSql, Statement.RETURN_GENERATED_KEYS);
                masterStmt.setInt(1, employeeId);
                masterStmt.setDate(2, Date.valueOf(today));
                masterStmt.setString(3, verifiedClassification);
                masterStmt.executeUpdate();
                
                ResultSet genKeys = masterStmt.getGeneratedKeys();
                if (genKeys.next()) {
                    attendanceId = genKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to capture parent key sequence generation.");
                }
            }

            String insertLogSql = "INSERT INTO attendance_logs (attendance_id, employee_id, log_type, logged_at, latitude, longitude, attachment_proof_path) VALUES (?, ?, ?, NOW(), ?, ?, ?)";
            logStmt = conn.prepareStatement(insertLogSql);
            logStmt.setInt(1, attendanceId);
            logStmt.setInt(2, employeeId);
            logStmt.setString(3, logType.toUpperCase());
            pstmtSetDoubleOrNull(logStmt, 4, latitude);
            pstmtSetDoubleOrNull(logStmt, 5, longitude);
            logStmt.setString(6, attachmentPath);
            logStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (checkStmt != null) checkStmt.close();
                if (masterStmt != null) masterStmt.close();
                if (logStmt != null) logStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean submitAccomplishment(OffSiteAccomplishment report) {
        String sql = "INSERT INTO offsite_accomplishments (employee_id, attendance_date, accomplishment_text, document_path) " +
                     "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE accomplishment_text = ?, document_path = ?, verification_status = 'PENDING'";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, report.getId());
            pstmt.setDate(2, Date.valueOf(report.getAttendanceDate()));
            pstmt.setString(3, report.getAccomplishmentText());
            pstmt.setString(4, report.getDocumentPath());
            
            pstmt.setString(5, report.getAccomplishmentText());
            pstmt.setString(6, report.getDocumentPath());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean verifyAccomplishment(int employeeId, LocalDate date, int managerUserId, String status, String remarks) {
        String updateAccomplishSql = "UPDATE offsite_accomplishments SET verification_status = ?, verified_by = ?, verified_at = NOW(), manager_remarks = ? WHERE employee_id = ? AND attendance_date = ?";
        String updateAttendanceRecordSql = "UPDATE attendance_records SET is_offsite_verified = ? WHERE employee_id = ? AND attendance_date = ?";
        
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt1 = conn.prepareStatement(updateAccomplishSql);
                 PreparedStatement pstmt2 = conn.prepareStatement(updateAttendanceRecordSql)) {

                pstmt1.setString(1, status);
                pstmt1.setInt(2, managerUserId);
                pstmt1.setString(3, remarks);
                pstmt1.setInt(4, employeeId);
                pstmt1.setDate(5, Date.valueOf(date));
                pstmt1.executeUpdate();

                int flagValue = status.equalsIgnoreCase("VERIFIED") ? 1 : 0;
                pstmt2.setInt(1, flagValue);
                pstmt2.setInt(2, employeeId);
                pstmt2.setDate(3, Date.valueOf(date));
                pstmt2.executeUpdate();
                
                conn.commit();
                return true;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public List<HybridReportRow> getHybridReportingData() {
        List<HybridReportRow> reportList = new ArrayList<>();
        String sql = "SELECT * FROM v_hybrid_field_reporting ORDER BY attendance_date DESC, employee_name ASC";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                HybridReportRow row = new HybridReportRow();
                row.setAttendanceDate(rs.getDate("attendance_date").toLocalDate());
                row.setEmployeeNo(rs.getString("employee_no"));
                row.setEmployeeName(rs.getString("employee_name"));
                row.setDepartmentName(rs.getString("department_name"));
                row.setDutyClassification(rs.getString("duty_classification"));
                
                Timestamp inTime = rs.getTimestamp("clock_in");
                if (inTime != null) row.setClockIn(inTime.toLocalDateTime());
                
                Timestamp outTime = rs.getTimestamp("clock_out");
                if (outTime != null) row.setClockOut(outTime.toLocalDateTime());
                
                row.setApprovedDestination(rs.getString("approved_destination"));
                row.setAccomplishmentText(rs.getString("accomplishment_text"));
                row.setAttachedProof(rs.getString("attached_proof"));
                row.setSupervisorVerification(rs.getString("supervisor_verification"));
                
                reportList.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reportList;
    }

    private void pstmtSetDoubleOrNull(PreparedStatement pstmt, int index, double value) throws SQLException {
        if (value == 0.0) {
            pstmt.setNull(index, Types.DECIMAL);
        } else {
            pstmt.setDouble(index, value);
        }
    }
}