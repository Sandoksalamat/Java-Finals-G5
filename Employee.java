class Employee {
    String employeeName;
    String employeeRank;
    String attendanceStatus[] = {"Present", "Absent"};
    String dayLabels[] = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    
    int employeeID;
    int attendanceCount; // Amount of attended work
    int absenceCount; // amount of absences
    int employeeCount[];

    public Employee(int employeeID, String employeeName, String employeeRank) {
        this.employeeID = employeeID;
        this.employeeName = employeeName;
        this.employeeRank = employeeRank;
    }
}
