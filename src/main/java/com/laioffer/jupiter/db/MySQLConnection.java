package com.laioffer.jupiter.db;

import com.laioffer.jupiter.entities.Item;
import com.laioffer.jupiter.entities.ItemType;
import com.laioffer.jupiter.entities.User;

import java.sql.*;
import java.util.*;

public class MySQLConnection implements AutoCloseable {
    private final Connection conn;

    // Create a connection to the MySQL database.
    public MySQLConnection() throws MySQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(MySQLDBUtil.getMySQLAddress());
        } catch (Exception e) {
            e.printStackTrace();
            throw new MySQLException("Failed to connect to Database.");
        }
    }

    @Override
    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Insert a favorite record to the database
    public void setFavoriteItem (Item item, String userId) throws MySQLException {
        if (conn == null) {
            System.out.println("DB connection failed.");
            throw new MySQLException("Failed to connect to Database.");
        }

        // Need to make sure item is added to the database first because the foreign key restriction on
        // item_id(favorite_records) -> id(items)　
        saveItem(item);

        // Using ? and preparedStatement to prevent SQL injection
        String sql = "insert ignore into favorite_records(user_id, item_id) values (?, ?)";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, item.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to save favorite record to Database");
        }
    }

    // Insert an item to the database.
    public void saveItem (Item item)  throws MySQLException {
        if (conn == null) {
            System.out.println("DB connection failed.");
            throw new MySQLException("Failed to connect to Database.");
        }
        try {
            String sql = "insert ignore into items values (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, item.getId());
            statement.setString(2, item.getTitle());
            statement.setString(3, item.getUrl());
            statement.setString(4, item.getThumbnailUrl());
            statement.setString(5, item.getBroadcasterName());
            statement.setString(6, item.getGameId());
            statement.setString(7, item.getType().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to add item to Database.");
        }
    }

    // Remove a favorite record from the database
    public void unsetFavoriteItem(Item item, String userId) throws MySQLException {
        if (conn == null) {
            System.out.println("DB connection failed.");
            throw new MySQLException("Failed to connect to Database.");
        }
        String sql = "delete from favorite_records where user_id = ? and item_id = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, item.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to delete favorite item from Database.");
        }
    }

    // Get favorite item ids for the given user
    public Set<String> getFavoriteItemIds(String userId)  throws MySQLException {
        if (conn == null) {
            System.out.println("DB connection failed.");
            throw new MySQLException("Failed to connect to Database.");
        }
        Set<String> favoriteItemIds = new HashSet<>(); // ??
        String sql = "select item_id from favorite_records where user_id = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String itemId = resultSet.getString("item_id");
                favoriteItemIds.add(itemId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get favorite item ids from Database.");
        }
        return favoriteItemIds;
    }

    // Get favorite items for the given user. The returned map includes three entries like {"Video": [item1, item2,
    // item3], "Stream": [item4, item5, item6], "Clip": [item7, item8, ...]}
    public Map<String, List<Item>>  getFavoriteItems (String userId) throws MySQLException {
        if (conn == null) {
            System.out.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database.");
        }
        Map<String, List<Item>> itemMap = new HashMap<>();
        for (ItemType type: ItemType.values()) {
            itemMap.put(type.toString(), new ArrayList<>());
        }
        Set<String> favoriteItemIds = getFavoriteItemIds(userId);
        String sql = "select * from items where id = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            for (String itemId: favoriteItemIds) {
                statement.setString(1, itemId);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    ItemType type = ItemType.valueOf(resultSet.getString("type"));
                    Item item = new Item.Builder()
                            .id(resultSet.getString("id"))
                            .title(resultSet.getString("title"))
                            .url(resultSet.getString("url"))
                            .thumbnailUrl(resultSet.getString("thumbnail_url"))
                            .broadcasterName(resultSet.getString("broadcaster_name"))
                            .gameId(resultSet.getString("game_id"))
                            .type(type)
                            .build();
                    itemMap.get(type.toString()).add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get favorite items from database.");
        }
        return itemMap;
    }

    // Get favorite game ids for the given user. The returned map includes three entries like {"Video": ["1234",
    // "5678", ...], "Stream": ["abcd", "efgh", ...], "Clip": ["4321", "5678", ...]}
    public Map<String, List<String>> getFavoriteGameIds(Set<String> favoriteItemIds) {
        if (conn == null) {
            System.out.println("Database connection failed.");
            throw new MySQLException("Failed to connect to Database");
        }
        Map<String, List<String>> gameIdMap = new HashMap<>();
        for (ItemType type: ItemType.values()) {
            gameIdMap.put(type.toString(), new ArrayList<>());
        }
        String sql = "select game_id, type from items where id = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            for (String itemId: favoriteItemIds) {
                statement.setString(1, itemId);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    gameIdMap.get(resultSet.getString("type")).add(resultSet.getString("game_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get favorite game ids from Database.");
        }
        return gameIdMap;
    }

    // Add a new user to the database
    public boolean addUser (User user) throws MySQLException {
        if (conn == null) {
            System.out.println("Database connection failed.");
            throw new MySQLException("Failed to connect to Database.");
        }
        String sql = "insert ignore into users value (?, ?, ?, ?)";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, user.getUserId());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getFirstName());
            statement.setString(4, user.getLastName());
            return 1 == statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to add new user information to Database");
        }
    }

    // Verify if the given user Id and password are correct. Returns the user name when it passes
    public String verifyLogin (String userId, String password) throws MySQLException {
        if (conn == null) {
            System.out.println("Database connection failed.");
            throw new MySQLException("Failed to connect to Database.");
        }
        String sql = "select * from users where id = ? and password = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("first_name") + " " + resultSet.getString("last_name");
            }
            return "";
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to verify user id and password from Database.");
        }
    }
}
