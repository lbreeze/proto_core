package ru.v6.mark.prototype.web.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.Status;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {
    private Status result = null;
    private String message = null;
    private Serializable id = null;

    public Response() {}

    public Response(Status result) {
        this.result = result;
    }

    public Response(Status result, String message) {
        this.result = result;
        this.message = message;
    }

    public Response(Serializable id, Status result, String message) {
        this.result = result;
        this.message = message;
        this.id = id;
    }

    public Status getResult() {
        return result;
    }

    public void setResult(Status result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Serializable getId() {
        return id;
    }

    public void setId(Serializable id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Response{" +
                "result=" + result +
                ", message='" + message + '\'' +
                ", id=" + id +
                '}';
    }
}
