package com.eas.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OffSiteRequest {
    private int id;
    private int employeeId;
    private String requestType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String destinationOrLocation;
    private String purpose;
    private String status;
    private LocalDateTime filedAt;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String adminRemarks;

    public OffSiteRequest() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getDestinationOrLocation() { return destinationOrLocation; }
    public void setDestinationOrLocation(String destinationOrLocation) { this.destinationOrLocation = destinationOrLocation; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getFiledAt() { return filedAt; }
    public void setFiledAt(LocalDateTime filedAt) { this.filedAt = filedAt; }

    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }
}