package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpeningHours {
    private DaySchedule monday;
    private DaySchedule tuesday;
    private DaySchedule wednesday;
    private DaySchedule thursday;
    private DaySchedule friday;
    private DaySchedule saturday;
    private DaySchedule sunday;

    public OpeningHours() {
        // Initialize with default closed schedule
        this.monday = new DaySchedule(true);
        this.tuesday = new DaySchedule(true);
        this.wednesday = new DaySchedule(true);
        this.thursday = new DaySchedule(true);
        this.friday = new DaySchedule(true);
        this.saturday = new DaySchedule(true);
        this.sunday = new DaySchedule(true);
    }

    // Static factory method for common schedules
    public static OpeningHours createDefaultWeekdaySchedule() {
        OpeningHours hours = new OpeningHours();
        // Monday to Friday: 9:00 - 18:00
        for (int i = 0; i < 5; i++) {
            DaySchedule schedule = new DaySchedule("09:00", "18:00");
            switch (i) {
                case 0: hours.setMonday(schedule); break;
                case 1: hours.setTuesday(schedule); break;
                case 2: hours.setWednesday(schedule); break;
                case 3: hours.setThursday(schedule); break;
                case 4: hours.setFriday(schedule); break;
            }
        }
        // Saturday: 10:00 - 16:00
        hours.setSaturday(new DaySchedule("10:00", "16:00"));
        // Sunday: Closed
        hours.setSunday(new DaySchedule(true));
        return hours;
    }

    // Getters and Setters
    public DaySchedule getMonday() { return monday; }
    public void setMonday(DaySchedule monday) { this.monday = monday; }

    public DaySchedule getTuesday() { return tuesday; }
    public void setTuesday(DaySchedule tuesday) { this.tuesday = tuesday; }

    public DaySchedule getWednesday() { return wednesday; }
    public void setWednesday(DaySchedule wednesday) { this.wednesday = wednesday; }

    public DaySchedule getThursday() { return thursday; }
    public void setThursday(DaySchedule thursday) { this.thursday = thursday; }

    public DaySchedule getFriday() { return friday; }
    public void setFriday(DaySchedule friday) { this.friday = friday; }

    public DaySchedule getSaturday() { return saturday; }
    public void setSaturday(DaySchedule saturday) { this.saturday = saturday; }

    public DaySchedule getSunday() { return sunday; }
    public void setSunday(DaySchedule sunday) { this.sunday = sunday; }
}
