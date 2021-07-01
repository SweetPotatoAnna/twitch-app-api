package com.laioffer.jupiter.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class ServletUtil {
    public static <T> void writeItemMap(HttpServletResponse response, T contentObj) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().println(new JsonMapper().writeValueAsString(contentObj));
    }

    // Help encrypt the user password before save to the database
    public static String encrytPassword(String userId, String password) {
        return DigestUtils.md5Hex(userId + DigestUtils.md5Hex(password)).toLowerCase();
    }

    // Check if the session is still valid, which means the user has been logged in successfully.
    public static String validateSession(HttpServletRequest request) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null) return null;
        return (String) httpSession.getAttribute("user_id");
    }

    // Check if the session is still valid, which means the user has been logged in successfully. Forbidden access when
    // the session is invalid.
    public static String validateSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return (String) httpSession.getAttribute("user_id");
    }

    // Read user data from the request body
    public static <T> T readRequestBody(HttpServletRequest request, Class<T> valueType) throws IOException {
        try {
            T res = new ObjectMapper().readValue(request.getReader(), valueType);
            return res;
        } catch (JsonParseException | JsonMappingException e) {
            return null;
        }
    }
}