package org.oogle.calender;

import java.time.LocalDateTime;

@Deprecated
public class Event {
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    private int reminderMinutes;
    private boolean notified;
}

