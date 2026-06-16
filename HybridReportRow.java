package com.eas.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HybridReportRow {
    private LocalDate attendanceDate;
    private String employeeNo;
    private String employeeName;
    private String departmentName;
    private String dutyClassification;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private String approvedDestination;
    private String accomplishmentText;
    private String attachedProof;
    private String supervisorVerification;


    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getDutyClassification() { return dutyClassification; }
    public void setDutyClassification(String dutyClassification) { this.dutyClassification = dutyClassification; }
    public LocalDateTime getClockIn() { return clockIn; }
    public void setClockIn(LocalDateTime clockIn) { this.clockIn = clockIn; }
    public LocalDateTime getClockOut() { return clockOut; }
    public void setClockOut(LocalDateTime clockOut) { this.clockOut = clockOut; }
    public String getApprovedDestination() { return approvedDestination; }
    public void setApprovedDestination(String approvedDestination) { this.approvedDestination = approvedDestination; }
    public String getAccomplishmentText() { return accomplishmentText; }
    public void setAccomplishmentText(String accomplishmentText) { this.accomplishmentText = accomplishmentText; }
    public String getAttachedProof() { return attachedProof; }
    public void setAttachedProof(String attachedProof) { this.attachedProof = attachedProof; }
    public String getSupervisorVerification() { return supervisorVerification; }
    public void setSupervisorVerification(String supervisorVerification) { this.supervisorVerification = supervisorVerification; }
}

