package com.eas.ui.admin;

import com.eas.model.HybridReportRow;
import com.eas.service.HybridAttendanceService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class HybridReportPanel extends JPanel {
    private final HybridAttendanceService service = new HybridAttendanceService();
    private JTable reportingTable;
    private DefaultTableModel tableModel;

    public HybridReportPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Hybrid Work, Field Assignment & Off-Site Master Summary Report Log");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        JButton refreshBtn = new JButton("Force Refresh Ledger Logs");
        
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(refreshBtn, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        String[] columnHeaders = {
            "Date", "Emp ID", "Full Name", "Department", "Classification", 
            "Clock In", "Clock Out", "Destination Location Target", 
            "Accomplishment Summary Log", "Audit Proof Reference Path", "Manager Signoff"
        };

        tableModel = new DefaultTableModel(columnHeaders, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; } 
        };

        reportingTable = new JTable(tableModel);
        reportingTable.getTableHeader().setReorderingAllowed(false);
        reportingTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        add(new JScrollPane(reportingTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> loadReportRecordsPipeline());
        loadReportRecordsPipeline();
    }

    public void loadReportRecordsPipeline() {
        tableModel.setRowCount(0);
        List<HybridReportRow> rows = service.getHybridReportingData();

        for (HybridReportRow r : rows) {
            tableModel.addRow(new Object[]{
                r.getAttendanceDate(),
                r.getEmployeeNo(),
                r.getEmployeeName(),
                r.getDepartmentName(),
                r.getDutyClassification(),
                r.getClockIn() != null ? r.getClockIn().toLocalTime() : "--:--",
                r.getClockOut() != null ? r.getClockOut().toLocalTime() : "--:--",
                r.getApprovedDestination() != null ? r.getApprovedDestination() : "N/A (Office/Home Based)",
                r.getAccomplishmentText() != null ? r.getAccomplishmentText() : "[Missing Accomplishment Filing]",
                r.getAttachedProof() != null ? r.getAttachedProof() : "[No Supporting Attachment Filed]",
                r.getSupervisorVerification() != null ? r.getSupervisorVerification() : "UNVERIFIED"
            });
        }
    }
}