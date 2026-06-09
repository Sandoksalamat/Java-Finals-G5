package com.eas.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JTable;

public final class CsvExporter {
    private CsvExporter() { }

    public static void export(JTable table, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int column = 0; column < table.getColumnCount(); column++) {
                if (column > 0) writer.write(",");
                writer.write(escape(table.getColumnName(column)));
            }
            writer.newLine();
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int column = 0; column < table.getColumnCount(); column++) {
                    if (column > 0) writer.write(",");
                    Object value = table.getValueAt(row, column);
                    writer.write(escape(value == null ? "" : value.toString()));
                }
                writer.newLine();
            }
        }
    }

    private static String escape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
