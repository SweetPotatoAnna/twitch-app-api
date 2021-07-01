package com.laioffer.jupiter.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.laioffer.jupiter.db.MySQLDBUtil;
import com.laioffer.jupiter.entities.Game;
import com.laioffer.jupiter.entities.Item;
import com.laioffer.jupiter.entities.ItemType;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.*;

public class TwitchClient {
    private static String clientId;
    private static String token;
    private static int searchLimit;
    private static int gameLimit;
    private static int gameLimitMax;
    private static final String TOP_GAME_URL_PATTERN = "https://api.twitch.tv/helix/games/top?first=%s";
    private static final String GAME_SEARCH_URL_PATTERN = "https://api.twitch.tv/helix/games?name=%s";
    private static final String CLIP_SEARCH_URL_PATTERN = "https://api.twitch.tv/helix/clips?game_id=%s&first=%s";
    private static final String STREAM_SEARCH_URL_PATTERN = "https://api.twitch.tv/helix/streams?game_id=%s&first=%s";
    private static final String VIDEO_SEARCH_URL_PATTERN = "https://api.twitch.tv/helix/videos?game_id=%s&first=%s";

    public TwitchClient() {
        Properties prop = new Properties();
        String propFileName = "config.properties";

        InputStream inputStream = MySQLDBUtil.class.getClassLoader().getResourceAsStream(propFileName);
        try {
            prop.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        clientId = prop.getProperty("client_id");
        token = prop.getProperty("token");
        searchLimit = Integer.valueOf(prop.getProperty("search_limit"));
        gameLimit = Integer.valueOf(prop.getProperty("game_limit"));
        gameLimitMax = Integer.valueOf(prop.getProperty("game_limit_max"));
    }

    //  Build the request URL which will be used when calling Twitch APIs, e.g. https://api.twitch.tv/helix/games/top
    //  when trying to get top games.
    private String buildGameURL(String gameName, Integer limit, String url) {
        if (gameName == null) {
            return String.format(url, limit > 0 ? limit : gameLimit);
        }
        try {
            // Encode special characters in URL, e.g. Rick Sun -> Rick%20Sun
            gameName = URLEncoder.encode(gameName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return String.format(url, gameName);
    }

    // Send HTTP request to Twitch Backend based on the given URL, and returns the body of the HTTP response returned
    // from Twitch backend.
    private String searchTwitch(String url) throws TwitchException {
        // Define the response handler to parse and return HTTP response body returned from Twitch
        ResponseHandler<String> responseHandler = (response) -> {
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("Response Status: " + response.getStatusLine().getStatusCode());
                throw new TwitchException("Failed to get response from Twitch.");
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new TwitchException("Failed to get response from Twitch.");
            }
            JSONObject jsonObject = new JSONObject(EntityUtils.toString(entity));
            return jsonObject.getJSONArray("data").toString();
        };

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Define the HTTP request, TOKEN and CLIENT_ID are used for user authentication on Twitch backend
            HttpGet request = new HttpGet(url);
            request.setHeader("Client-Id", clientId);
            request.setHeader("Authorization", token);
            return httpClient.execute(request, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            throw new TwitchException("Failed to get response from Twitch.");
        }
    }

    // Convert JSON format data returned from Twitch to an Arraylist of Game objects
    private List<Game> getGameList(String data) throws TwitchException {
        JsonMapper mapper = new JsonMapper();
        try {
            List<Game> gameList = Arrays.asList(mapper.readValue(data, Game[].class)); // List<T> is interface
            return gameList;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new TwitchException("Failed to get response from Twitch.");
        }
    }

    // Integrate search() and getGameList() together, returns the top x popular games from Twitch.
    public List<Game> topGames(Integer limit) throws TwitchException {
        if (limit <= 0) {
            limit = gameLimit;
        } else if (limit > 100) {
            limit = gameLimitMax;
        }
        return getGameList(searchTwitch(buildGameURL(null, limit, TOP_GAME_URL_PATTERN)));
    }

    // Integrate search() and getGameList() together, returns the dedicated game based on the game name.
    public Game searchGame(String gameName) throws TwitchException {
        List<Game> gameList = getGameList(searchTwitch(buildGameURL(gameName, -1, GAME_SEARCH_URL_PATTERN)));
        return gameList.size() > 0 ? gameList.get(0) : null;
    }

    // Similar to buildGameURL, build Search URL that will be used when calling Twitch API.
    // e.g. https://api.twitch.tv/helix/clips?game_id=12924.
    private String buildSearchURL(String gameId, String url, int limit) {
        try {
            gameId = URLEncoder.encode(gameId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new TwitchException("Failed to get response from Twitch.");
        }
        return String.format(url, gameId, limit);
    }

    // Similar to getGameList, convert the json data returned from Twitch to a list of Item objects.
    private List<Item> getItemList(String data) throws TwitchException {
        ObjectMapper mapper = new JsonMapper();
        try {
            return Arrays.asList(mapper.readValue(data, Item[].class));
        } catch (JsonProcessingException e) {
            throw new TwitchException("Failed to get response from Twitch.");
        }
    }

    // Returns the top x streams based on game ID.
    private List<Item> searchStreams (String gameId, int limit) {
        List<Item> itemList = getItemList(searchTwitch(buildSearchURL(gameId, STREAM_SEARCH_URL_PATTERN, limit)));
        for(Item item: itemList) {
            item.setType(ItemType.STREAM);
        }
        return itemList;
    }

    // Returns the top x clips based on game ID.
    private List<Item> searchVideos (String gameId, int limit) {
        List<Item> itemList = getItemList(searchTwitch(buildSearchURL(gameId, VIDEO_SEARCH_URL_PATTERN, limit)));
        for(Item item: itemList) {
            item.setType(ItemType.VIDEO);
        }
        return itemList;    }

    // Returns the top x videos based on game ID.
    private List<Item> searchClips (String gameId, int limit) {
        List<Item> itemList = getItemList(searchTwitch(buildSearchURL(gameId, CLIP_SEARCH_URL_PATTERN, limit)));
        for(Item item: itemList) {
            item.setType(ItemType.CLIP);
        }
        return itemList;
    }

    public List<Item> searchByType(String gameId, ItemType type, int limit) {
        List<Item> itemList = Collections.emptyList();
        switch (type) {
            case STREAM:
                itemList = searchStreams(gameId, limit);
                break;
            case VIDEO:
                itemList = searchVideos(gameId, limit);
                break;
            case CLIP:
                itemList = searchClips(gameId, limit);
                break;
        }

        // Update gameId for all items. GameId is used by recommendation function
        for (Item item: itemList) {
            item.setGameId(gameId);
        }
        return itemList;
    }

    public Map<String, List<Item>> searchItems(String gameId, int limit) throws TwitchException {
        if (limit == 0) {
            limit = searchLimit;
        }
        Map<String, List<Item>> map = new HashMap<>();
        for (ItemType type: ItemType.values()) {
            map.put(type.toString(), searchByType(gameId, type, limit));
        }
        return map;
    }
}