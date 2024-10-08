package com.dhu.constants;

public class RedisConstants {
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final long LOGIN_USER_MIN_TTL = 30;
    public static final String REGISTER_CAPTCHA = "register:captcha:";
    public static final long REGISTER_CAPTCHA_MIN_TTL = 10;
    public static final String JOIN_TEAM_KEY = "join:code:";
    public static final long JOIN_TEAM_MIN_TTL = 30;
};
