package ru.v6.mark.prototype.web.cache;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ObjectCache {

    private ConcurrentMap<Object, CachedItem> items = new ConcurrentHashMap<>();

    public void clear() {
        items.clear();
    }

    public Identifiable getObject(Object key) {
        CachedItem item = items.get(key);
        if (item != null) {
            if (item.getExpireDate().before(new Date())) {
                //System.out.println(item.getClass() + "with  key '" + key + "' was used " + item.getCount() + " time(s)");
                return null;
            }

            item.setCount(item.getCount()+ 1);
            return item.getObject();
        }
        return null;
    }

    public HasCode getCodeObject(Object key) {
        CachedItem item = items.get(key);
        if (item != null) {
            if (item.getExpireDate().before(new Date())) {
                //System.out.println(item.getClass() + "with  key '" + key + "' was used " + item.getCount() + " time(s)");
                return null;
            }

            item.setCount(item.getCount()+ 1);
            return item.getCodeObject();
        }
        return null;
    }


    public void addObject(Identifiable t) {
        CachedItem item = new CachedItem(t);
        items.put(t.getId(), item);
    }

    public void addCodeObject(HasCode t) {
        CachedItem item = new CachedItem(t);
        items.put(t.getCode(), item);
    }

    public void addObject(Identifiable t, int minute) {
        CachedItem item = new CachedItem(t, minute);
        items.put(t.getId(), item);
    }

    public void addObject(Object key, Identifiable t) {
        CachedItem item = new CachedItem(t);
        items.put(key, item);
    }

    public void addObject(Object key, Identifiable t, int minute) {
        CachedItem item = new CachedItem(t, minute);
        items.put(key, item);
    }

    public Map<Object, CachedItem> getAllObject() {
        return items;
    }

    public void removeObject(Object key) {
        items.remove(key);
    }

    public int getSize() {
        return items.size();
    }

}
