package com.laioffer.jupiter.servlets;

import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.db.MySQLException;
import com.laioffer.jupiter.entities.LoginRequestBody;
import com.laioffer.jupiter.entities.LoginResponseBody;
import com.laioffer.jupiter.util.ServletUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet(name = "LoginServlet", value = "/login")
public class LoginServlet extends HttpServlet {
    private static final Integer MAX_INACTIVE_INTERVAL = 600;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LoginRequestBody loginRequestBody = ServletUtil.readRequestBody(request, LoginRequestBody.class);

        if (loginRequestBody == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String name = "";
        try (MySQLConnection conn = new MySQLConnection()) {
            String userId = loginRequestBody.getUserId();
            String password = ServletUtil.encrytPassword(userId, loginRequestBody.getPassword());

            // Verify if the user ID and password are correct
            name = conn.verifyLogin(userId, password);
            if (name == "") {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                ServletUtil.writeItemMap(response, new LoginResponseBody(userId, name));
            }
        } catch (MySQLException e) {
            throw new ServletException(e);
        }

        // Create a new session for the user if user ID and password are correct, otherwise return Unauthorized error.
        if (!name.isEmpty()) {
            // Create a new session, put user ID as an attribute into the session object, and set the expiration time to
            // 600 seconds.
            HttpSession httpSession = request.getSession();
            httpSession.setAttribute("user_id", loginRequestBody.getUserId());
            httpSession.setMaxInactiveInterval(MAX_INACTIVE_INTERVAL);
        }
    }
}
