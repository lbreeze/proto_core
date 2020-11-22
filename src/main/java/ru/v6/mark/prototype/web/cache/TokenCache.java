package ru.v6.mark.prototype.web.cache;

import ru.v6.mark.prototype.domain.entity.Token;

public class TokenCache {

    private static ObjectCache itemCache = new ObjectCache();

    public static Token get(Object id) {

        Token token = (Token)itemCache.getObject(id);

        return token;
    }

    public static void add(Token obj) {
        itemCache.addObject(obj);
    }

    public static void clear() {
        itemCache.clear();
    }
}
