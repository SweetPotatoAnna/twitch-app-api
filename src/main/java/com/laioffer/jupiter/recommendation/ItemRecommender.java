package com.laioffer.jupiter.recommendation;

import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.db.MySQLDBUtil;
import com.laioffer.jupiter.db.MySQLException;
import com.laioffer.jupiter.entities.Game;
import com.laioffer.jupiter.entities.Item;
import com.laioffer.jupiter.entities.ItemType;
import com.laioffer.jupiter.external.TwitchClient;
import com.laioffer.jupiter.external.TwitchException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ItemRecommender {
    private static int recommendation_game_limit;
    private static int per_game_recommendation_limit;
    private static int total_recommendation_limit;

    public ItemRecommender() {
        Properties prop = new Properties();
        String propFileName = "config.properties";

        InputStream inputStream = MySQLDBUtil.class.getClassLoader().getResourceAsStream(propFileName);
        try {
            prop.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        recommendation_game_limit = Integer.valueOf(prop.getProperty("recommendation_game_limit"));
        per_game_recommendation_limit = Integer.valueOf(prop.getProperty("per_game_recommendation_limit"));
        total_recommendation_limit = Integer.valueOf(prop.getProperty("total_recommendation_limit"));
    }

    // Return a list of Item objects for the given type. Types are one of [Stream, Video, Clip]. Add items are related
    // to the top games provided in the argument
    private List<Item> recommendByTopGames (ItemType type, List<Game> topGames) throws RecommendationException {
        List<Item> recommendItems = new ArrayList<>();
        TwitchClient twitchClient = new TwitchClient();

        outerloop:
        for(Game game: topGames) {
            List<Item> items;
            try {
                items = twitchClient.searchByType(game.getId(), type, per_game_recommendation_limit);
            } catch (TwitchException e) {
                throw new RecommendationException("Failed to get recommendation result.");
            }
            for (Item item: items) {
                if (recommendItems.size() == total_recommendation_limit) break outerloop;
                recommendItems.add(item);
            }
        }
        return recommendItems;
    }

    // Return a list of Item objects for the given type. Types are one of [Stream, Video, Clip]. All items are related
    // to the items previously favorited by the user. E.g., if a user favorited some videos about game "Just Chatting",
    // then it will return some other videos about the same game.
    private List<Item> recommendByFavoriteHistory (ItemType type, Set<String> favoriteItemIds, List<String> favoriteGameIds)
            throws RecommendationException {
        List<Item> recommendItems = new ArrayList<>();

        // Count the favorite game IDs from the database for the given user. E.g. if the favorited game ID list is
        // ["1234", "2345", "2345", "3456"], the returned Map is {"1234": 1, "2345": 2, "3456": 1}
        Map<String, Integer> countGameIds = new HashMap<>();
        for (String gameId: favoriteGameIds) {
            countGameIds.put(gameId, countGameIds.getOrDefault(gameId, 0) + 1);
        }

        // Sort the game Id by count. E.g. if the input is {"1234": 1, "2345": 2, "3456": 1}, the returned Map is
        // {"2345": 2, "1234": 1, "3456": 1}
        List<Map.Entry<String, Integer>> sortedFavoriteGameIds = new ArrayList<>(countGameIds.entrySet());
        sortedFavoriteGameIds.sort((Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2)
                -> Integer.compare(e2.getValue(), e1.getValue()));

        if (sortedFavoriteGameIds.size() > recommendation_game_limit) sortedFavoriteGameIds.subList(0, recommendation_game_limit);

        TwitchClient twitchClient = new TwitchClient();
        // Search Twitch based on the favorite game IDs returned in the last step.
        outerloop:
        for(Map.Entry<String, Integer> eachGame: sortedFavoriteGameIds) {
            List<Item> items;
            try {
                items = twitchClient.searchByType(eachGame.getKey(), type, per_game_recommendation_limit);
            } catch (TwitchException e) {
                throw new RecommendationException("Failed to get recommendation result.");
            }
            for (Item item: items) {
                if (recommendItems.size() == total_recommendation_limit) {
                    break outerloop;
                }
                if (!favoriteItemIds.contains(item.getId())) {
                    recommendItems.add(item);
                }
            }
        }

        return recommendItems;
    }

    // Return a map of Item objects as the recommendation result. Keys of the may are [Stream, Video, Clip]. Each key
    // is corresponding to a list of Items objects, each item object is a recommended item based on the previous
    // favorite records by the user.
    public Map<String, List<Item>> recommendItemsByUser (String userId) throws RecommendationException {
        Map<String, List<Item>> recommendItemMap = new HashMap<>();
        Set<String> favoriteItemIds;
        Map<String, List<String>> favoriteGameIdMap;
        try (MySQLConnection conn = new MySQLConnection()) {
            favoriteItemIds = conn.getFavoriteItemIds(userId);
            favoriteGameIdMap = conn.getFavoriteGameIds(favoriteItemIds);
        } catch (MySQLException e) {
            throw new RecommendationException("Failed to get user favorite history for recommendation.");
        }

        for (Map.Entry<String, List<String>> each: favoriteGameIdMap.entrySet()) {
            ItemType type = ItemType.valueOf(each.getKey());
            List<String> gameIds = each.getValue();
            if (gameIds.size() == 0) {
                List<Game> topGames;
                try {
                    topGames = new TwitchClient().topGames(recommendation_game_limit);
                } catch (TwitchException e) {
                    throw new RecommendationException("Failed to get game data for recommendation.");
                }
                recommendItemMap.put(type.toString(),
                        recommendByTopGames(type, topGames));
            } else {
                recommendItemMap.put(type.toString(),
                        recommendByFavoriteHistory(type, favoriteItemIds, gameIds));
            }
        }
        return recommendItemMap;
    }

    // Return a map of Item objects as the recommendation result. Keys of the may are [Stream, Video, Clip]. Each key
    // is corresponding to a list of Items objects, each item object is a recommended item based on the top games
    // currently on Twitch.
    public Map<String, List<Item>> recommendItemsByDefault () throws RecommendationException {
        Map<String, List<Item>> recommendItemMap = new HashMap<>();
        TwitchClient twitchClient = new TwitchClient();
        List<Game> topGames;
        try {
            topGames = twitchClient.topGames(recommendation_game_limit);
        } catch (TwitchException e) {
            throw new RecommendationException("Failed to get game data for recommendation.");
        }

        for(ItemType type: ItemType.values()) {
            recommendItemMap.put(type.toString(), recommendByTopGames(type, topGames));
        }
        return recommendItemMap;
    }
}
