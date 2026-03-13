package net;

import java.io.Serializable;

// client request sent to the server via RMI
// Contains action, target entity, and optional JSON payload
public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private String action;
    private String entity;
    private String userId;
    private String payload;

    public Request() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}