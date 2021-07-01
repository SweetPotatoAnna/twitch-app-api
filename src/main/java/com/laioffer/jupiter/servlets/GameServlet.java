package com.laioffer.jupiter.servlets;

import com.laioffer.jupiter.external.TwitchClient;
import com.laioffer.jupiter.entities.Game;
import com.laioffer.jupiter.external.TwitchException;
import com.laioffer.jupiter.util.ServletUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "GameServlet", value = "/game")
public class GameServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Get gameName from request URL.
        String gameName = request.getParameter("game_name");
        String limit = request.getParameter("limit");
        TwitchClient twitchClient = new TwitchClient();

        // Let the client know the returned data is in JSON format.
        response.setContentType("application/json;charset=UTF-8");
        try {
            // Return the dedicated game information if gameName is provided in the request URL, otherwise return the
            // top x games.
            if (gameName != null) {
                Game game = twitchClient.searchGame(gameName);
                ServletUtil.writeItemMap(response, game);
            } else {
                List<Game> gameList = twitchClient.topGames(limit == null ? 0 : Integer.parseInt(limit));
                ServletUtil.writeItemMap(response, gameList);
            }
        } catch (TwitchException e){
            throw new ServletException(e);
        }
    }

}