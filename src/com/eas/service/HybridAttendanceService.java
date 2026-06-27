package com.eas.service;

import com.eas.config.Database;
import com.eas.model.HybridReportRow;
import com.eas.model.OffSiteAccomplishment;
import com.eas.model.OffSiteRequest;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HybridAttendanceService {

    private int resolveEmployeeId(Connection conn, int userId) throws SQLException {
        String sql = "SELECT id FROM employees WHERE user_id = ? AND employment_status = 'ACTIVE'";
        try (PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, userId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    throw new SQLException("No active employee profile is linked to this account.");
                }
                return r.getInt("id");
            }
        }
    }

    public boolean submitOffSiteRequest(int userId, OffSiteRequest request) {
        String sql = "INSERT INTO offsite_requests (employee_id, request_type, start_date, end_date, destination_or_location, purpose) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection()) {
            int employeeId = resolveEmployeeId(conn, userId);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, employeeId);
                pstmt.setString(2, request.getRequestType());
                pstmt.setDate(3, java.sql.Date.valueOf(request.getStartDate()));
                pstmt.setDate(4, java.sql.Date.valueOf(request.getEndDate()));
                pstmt.setString(5, request.getDestinationOrLocation());
                pstmt.setString(6, request.getPurpose());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

    public boolean processMobileClockEvent(int userId, String logType, Double latitude, Double longitude, String attachmentPath) {
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement findStmt = null;
        PreparedStatement masterStmt = null;
        PreparedStatement logStmt = null;
        ResultSet rs = null;
        ResultSet recordRs = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            int employeeId = resolveEmployeeId(conn, userId);
            String employeeIdStr = String.valueOf(employeeId);

            String checkApprovalSql = "SELECT request_type FROM offsite_requests WHERE employee_id = ? AND CURDATE() BETWEEN start_date AND end_date AND status = 'APPROVED' LIMIT 1";
            checkStmt = conn.prepareStatement(checkApprovalSql);
            checkStmt.setInt(1, employeeId);
            rs = checkStmt.executeQuery();
            String verifiedClassification;
            if (rs.next()) {
                verifiedClassification = rs.getString("request_type");
            } else {
                System.out.println("Clocking rejected: No approved off-site request found today for employee_id=" + employeeIdStr);
                conn.rollback();
                return false;
            }

            // FIX: SQL now uses CURDATE() directly, so the second '?' placeholder
            // (which was never being bound) has been removed entirely.
            String findRecordSql = "SELECT id, clock_in, clock_out FROM attendance_records WHERE employee_id = ? AND attendance_date = CURDATE()";
            findStmt = conn.prepareStatement(findRecordSql);
            findStmt.setInt(1, employeeId);
            recordRs = findStmt.executeQuery();
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
                String insertMasterSql = "INSERT INTO attendance_records (employee_id, attendance_date, clock_in, attendance_status, source) VALUES (?, CURDATE(), NOW(), ?, 'MANUAL')";
                masterStmt = conn.prepareStatement(insertMasterSql, Statement.RETURN_GENERATED_KEYS);
                masterStmt.setInt(1, employeeId);
                masterStmt.setString(2, verifiedClassification);
                masterStmt.executeUpdate();

                try (ResultSet genKeys = masterStmt.getGeneratedKeys()) {
                    if (genKeys.next()) {
                        attendanceId = genKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to capture parent key sequence generation.");
                    }
                }
            }

            String insertLogSql = "INSERT INTO attendance_logs (attendance_id, employee_id, log_type, latitude, longitude, attachment_proof_path, logged_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
            logStmt = conn.prepareStatement(insertLogSql);
            logStmt.setInt(1, attendanceId);
            logStmt.setInt(2, employeeId);
            logStmt.setString(3, logType.toUpperCase());
            setNullableDouble(logStmt, 4, latitude);
            setNullableDouble(logStmt, 5, longitude);
            logStmt.setString(6, attachmentPath);
            logStmt.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (recordRs != null) recordRs.close();
                if (checkStmt != null) checkStmt.close();
                if (findStmt != null) findStmt.close();
                if (masterStmt != null) masterStmt.close();
                if (logStmt != null) logStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean submitAccomplishment(int userId, OffSiteAccomplishment report) {
        String sql = "INSERT INTO offsite_accomplishments (employee_id, attendance_date, accomplishment_text, document_path) " +
                     "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE accomplishment_text = ?, document_path = ?, verification_status = 'PENDING'";
        try (Connection conn = Database.getConnection()) {
            int employeeId = resolveEmployeeId(conn, userId);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, employeeId);
                pstmt.setDate(2, Date.valueOf(report.getAttendanceDate()));
                pstmt.setString(3, report.getAccomplishmentText());
                pstmt.setString(4, report.getDocumentPath());

                pstmt.setString(5, report.getAccomplishmentText());
                pstmt.setString(6, report.getDocumentPath());

                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean verifyAccomplishment(int employeeId, LocalDate date, int managerUserId, String status, String remarks) {
        String employeeIdStr = String.valueOf(employeeId);

        String updateAccomplishSql = "UPDATE offsite_accomplishments SET verification_status = ?, verified_by = ?, verified_at = NOW(), manager_remarks = ? WHERE employee_id = ? AND DATE(attendance_date) = ?";
        String updateAttendanceRecordSql = "UPDATE attendance_records SET is_offsite_verified = ? WHERE employee_id = ? AND DATE(attendance_date) = ?";

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
                int accomplishRows = pstmt1.executeUpdate();

                if (accomplishRows == 0) {
                    conn.rollback();
                    System.out.println("verifyAccomplishment: NO MATCH in offsite_accomplishments for employee_id="
                            + employeeIdStr + ", date=" + date);
                    return false;
                }

                int flagValue = status.equalsIgnoreCase("VERIFIED") ? 1 : 0;
                pstmt2.setInt(1, flagValue);
                pstmt2.setInt(2, employeeId);
                pstmt2.setDate(3, Date.valueOf(date));
                int attendanceRows = pstmt2.executeUpdate();

                if (attendanceRows == 0) {
                    System.out.println("verifyAccomplishment: warning - no matching attendance_records row for employee_id="
                            + employeeIdStr + ", date=" + date + " (offsite_accomplishments was still updated)");
                }

                conn.commit();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
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
                row.setEmployeeNo(rs.getString("employee_id"));
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

    private void setNullableDouble(PreparedStatement pstmt, int index, Double value) throws SQLException {
        if (value == null) {
            pstmt.setNull(index, Types.DECIMAL);
        } else {
            pstmt.setDouble(index, value);
        }
    }
}