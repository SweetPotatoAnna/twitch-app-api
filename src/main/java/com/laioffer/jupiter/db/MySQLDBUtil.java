package com.laioffer.jupiter.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

public class MySQLDBUtil {

    public static String getMySQLAddress() {
        Properties prop = new Properties();
        String propFileName = "config.properties";

        InputStream inputStream = MySQLDBUtil.class.getClassLoader().getResourceAsStream(propFileName);
        try {
            prop.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String username = prop.getProperty("username");
        String password = prop.getProperty("password");
        final String dbName = prop.getProperty("db_name");
        final String portNum = prop.getProperty("port_number");
        final String instance = prop.getProperty("instance");

        try {
            password = URLEncoder.encode(password, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s&autoReconnect=true&serverTimezone=UTC&" +
                        "createDatabaseIfNotExist=true",
                dbName, portNum, instance, username, password);
    }
}
