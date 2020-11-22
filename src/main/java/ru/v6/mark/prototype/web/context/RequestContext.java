package ru.v6.mark.prototype.web.context;

public interface RequestContext {

    public String getUserName();
    public void setUserName(String userName);

    public void clear();
}
