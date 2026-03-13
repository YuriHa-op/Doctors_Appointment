package net;

import java.io.Serializable;

//server response returned via RMI.
public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;
    private int code;
    private String message;
    private String data;

    public Response() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}