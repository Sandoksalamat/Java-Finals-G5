package com.eas.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OffSiteAccomplishment {
    private int id;
    private int employeeId;
    private LocalDate attendanceDate;
    private String accomplishmentText;
    private String documentPath;
    private LocalDateTime submittedAt;
    private String verificationStatus;
    private Integer verifiedBy;
    private LocalDateTime verifiedAt;
    private String managerRemarks;

    public OffSiteAccomplishment() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public String getAccomplishmentText() { return accomplishmentText; }
    public void setAccomplishmentText(String accomplishmentText) { this.accomplishmentText = accomplishmentText; }

    public String getDocumentPath() { return documentPath; }
    public void setDocumentPath(String documentPath) { this.documentPath = documentPath; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public Integer getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Integer verifiedBy) { this.verifiedBy = verifiedBy; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getManagerRemarks() { return managerRemarks; }
    public void setManagerRemarks(String managerRemarks) { this.managerRemarks = managerRemarks; }
}