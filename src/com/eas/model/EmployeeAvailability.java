package com.eas.model;

public class EmployeeAvailability {
    private int id;
    private int employeeId;
    private String preferredWorkday;
    private String unavailableDate;
    private String requestedRestDay;
    private String status;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int id) { this.employeeId = id; }
    public String getPreferredWorkday() { return preferredWorkday; }
    public void setPreferredWorkday(String day) { this.preferredWorkday = day; }
    public String getUnavailableDate() { return unavailableDate; }
    public void setUnavailableDate(String date) { this.unavailableDate = date; }
    public String getRequestedRestDay() { return requestedRestDay; }
    public void setRequestedRestDay(String date) { this.requestedRestDay = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}