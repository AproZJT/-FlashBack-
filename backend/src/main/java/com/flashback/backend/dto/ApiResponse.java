package com.flashback.backend.dto;

public class ApiResponse<T> {
    private boolean ok;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.ok = true;
        resp.data = data;
        return resp;
    }

    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.ok = false;
        resp.message = message;
        return resp;
    }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
