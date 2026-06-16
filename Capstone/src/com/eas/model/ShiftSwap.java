package com.eas.model;

public class ShiftSwap {

    private int id;
    private int requestingEmployeeId;
    private int targetEmployeeId;
    private int originalScheduleId;
    private String status;
    private String reason;
    private int approvedBy;

    public ShiftSwap() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRequestingEmployeeId() { return requestingEmployeeId; }
    public void setRequestingEmployeeId(int id) { this.requestingEmployeeId = id; }
    public int getTargetEmployeeId() { return targetEmployeeId; }
    public void setTargetEmployeeId(int id) { this.targetEmployeeId = id; }
    public int getOriginalScheduleId() { return originalScheduleId; }
    public void setOriginalScheduleId(int id) { this.originalScheduleId = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public int getApprovedBy() { return approvedBy; }
    public void setApprovedBy(int id) { this.approvedBy = id; }
}