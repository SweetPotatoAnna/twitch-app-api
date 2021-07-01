package com.laioffer.jupiter.servlets;

import com.laioffer.jupiter.recommendation.ItemRecommender;
import com.laioffer.jupiter.entities.Item;
import com.laioffer.jupiter.recommendation.RecommendationException;
import com.laioffer.jupiter.util.ServletUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "RecommendServlet", value = "/recommendation")
public class RecommendServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // If the user is successfully logged in, recommend by the favorite records, otherwise recommend by the top games.
        String userId = ServletUtil.validateSession(request);
        ItemRecommender itemRecommender = new ItemRecommender();
        Map<String, List<Item>> itemMap;
        try {
            if (userId == null) {
                itemMap = itemRecommender.recommendItemsByDefault();
            } else {
                itemMap = itemRecommender.recommendItemsByUser(userId);
            }
        } catch (RecommendationException e) {
            throw new ServletException(e);
        }
        ServletUtil.writeItemMap(response,itemMap);
    }
}
