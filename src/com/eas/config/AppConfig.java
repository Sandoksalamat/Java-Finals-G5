package com.eas.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (FileInputStream in = new FileInputStream("config/db.properties")) {
            props.load(in);
        } catch (IOException ex) {
            props.setProperty("db.url",
                "jdbc:mysql://localhost:3306/employee_attendance_system" +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila"
            );
            props.setProperty("db.user",     "root");
            props.setProperty("db.password", "");
        }
    }

    private AppConfig() {}

    public static String get(String key) {
        return props.getProperty(key, "");
    }
}