package ru.v6.mark.prototype.web.cache;

import java.util.Calendar;
import java.util.Date;

public class CachedItem {
    private Identifiable object;
    private HasCode codeObject;
    private Date expireDate;

    private long count;

    public CachedItem(Identifiable t) {
        this(t, 10);
    }

    public CachedItem(HasCode t) {
        this(t, 10);
    }

    public CachedItem(Identifiable t, int minute) {
        this.object = t;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minute);
        this.expireDate = cal.getTime();
        this.count = 0;
    }

    public CachedItem(HasCode t, int minute) {
        this.codeObject = t;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minute);
        this.expireDate = cal.getTime();
        this.count = 0;
    }


    public Identifiable getObject() { return object; }
    public HasCode getCodeObject() { return codeObject; }

    public Date getExpireDate() { return expireDate; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
