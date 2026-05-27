package org.example.paperwise.Until;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@Component
public class DataUtile {
    private static final DateTimeFormatter DATE_TIME_FORMATTER=
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private  static final DateTimeFormatter DATE_FORMATTER=
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private  static final DateTimeFormatter TIME_FORMATTER=
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter YEAR=
            DateTimeFormatter.ofPattern("yyyy");
    //格式化为2026-04-21 14:30:00
    public static final String formatDateTime(LocalDateTime localDateTime) {
        if(localDateTime==null){
            return "";
        }
        return localDateTime.format(DATE_TIME_FORMATTER);
    }
    //格式化为2026-04-21
    public static final String formatDate(LocalDateTime localDateTime) {
        if(localDateTime==null){
            return "";
        }
        return localDateTime.format(DATE_FORMATTER);
    }
    public static final String formatTime(LocalDateTime localDateTime) {
        if(localDateTime==null){
            return "";
        }
        return localDateTime.format(TIME_FORMATTER);
    }
}
