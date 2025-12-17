package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DaySchedule {
    private String open;  // Format: "HH:mm" e.g., "09:00"
    private String close; // Format: "HH:mm" e.g., "18:00"
    private boolean closed; // true if the shop is closed that day

    public DaySchedule() {
        this.closed = false;
    }

    public DaySchedule(String open, String close) {
        this.open = open;
        this.close = close;
        this.closed = false;
    }

    public DaySchedule(boolean closed) {
        this.closed = closed;
        if (closed) {
            this.open = null;
            this.close = null;
        }
    }

    // Getters and Setters
    public String getOpen() { return open; }
    public void setOpen(String open) { this.open = open; }

    public String getClose() { return close; }
    public void setClose(String close) { this.close = close; }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { 
        this.closed = closed;
        if (closed) {
            this.open = null;
            this.close = null;
        }
    }

    @Override
    public String toString() {
        if (closed) {
            return "Closed";
        }
        return open + " - " + close;
    }
}
