package com.laioffer.jupiter.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponseBody {
    @JsonProperty("user_id")
    private final String userId;

    @JsonProperty("name")
    private final String userName;

    public LoginResponseBody(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }
}
