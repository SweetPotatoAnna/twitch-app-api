package com.laioffer.jupiter.servlets;

import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.entities.FavoriteRequestBody;
import com.laioffer.jupiter.entities.Item;
import com.laioffer.jupiter.external.TwitchException;
import com.laioffer.jupiter.util.ServletUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "FavoriteServlet", value = "/favorite")
public class FavoriteServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = ServletUtil.validateSession(request, response);
        if (userId == null) return;

        // Get favorite item information from request body
        FavoriteRequestBody body = ServletUtil.readRequestBody(request, FavoriteRequestBody.class);
        if (body == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try(MySQLConnection conn = new MySQLConnection()) {
            // Save the favorite item to the database
            conn.setFavoriteItem(body.getFavoriteItem(), userId);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (TwitchException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = ServletUtil.validateSession(request, response);
        if (userId == null) return;

        try(MySQLConnection conn = new MySQLConnection()) {
            // Read the favorite items from the database
            Map<String, List<Item>> itemMap = conn.getFavoriteItems(userId);
            response.setStatus(HttpServletResponse.SC_OK);
            ServletUtil.writeItemMap(response, itemMap);
        } catch (TwitchException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = ServletUtil.validateSession(request, response);
        if (userId == null) return;

        FavoriteRequestBody body = ServletUtil.readRequestBody(request, FavoriteRequestBody.class);
        if (body == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try(MySQLConnection conn = new MySQLConnection()) {
            // Remove the favorite item to the database
            conn.unsetFavoriteItem(body.getFavoriteItem(), userId);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (TwitchException e) {
            throw new ServletException(e);
        }
    }
}
