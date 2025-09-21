package de.t0bx.sentiencefriends.lobby.utils;

public class PaginationUtil {

    public static int pageCount(int totalItems, int pageSize) {
        if (pageSize <= 0) return  1;
        if (totalItems <= 0) return  1;
        return ((totalItems - 1) / pageSize) + 1;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
