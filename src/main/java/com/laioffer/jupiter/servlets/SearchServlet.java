package com.laioffer.jupiter.servlets;

import com.laioffer.jupiter.external.TwitchClient;
import com.laioffer.jupiter.entities.Item;
import com.laioffer.jupiter.external.TwitchException;
import com.laioffer.jupiter.util.ServletUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "SearchServlet", value = "/search")
public class SearchServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String gameId = request.getParameter("game_id");
        if (gameId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        TwitchClient twitchClient = new TwitchClient();
        try {
            Map<String, List<Item>> itemMap = twitchClient.searchItems(gameId, 0);
            ServletUtil.writeItemMap(response, itemMap);
        } catch (TwitchException e) {
            throw new ServletException(e);
        }
    }
}
