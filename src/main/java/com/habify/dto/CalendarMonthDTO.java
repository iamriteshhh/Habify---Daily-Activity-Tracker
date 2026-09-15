package com.habify.dto;

import java.util.List;

public class CalendarMonthDTO {

    private int year;
    private int month;
    private String monthName;
    private int totalActivitiesLogged;
    private int averageCompletion;
    private List<CalendarDayDTO> days;

    public CalendarMonthDTO() {
    }

    public CalendarMonthDTO(int year, int month, String monthName, int totalActivitiesLogged, int averageCompletion, List<CalendarDayDTO> days) {
        this.year = year;
        this.month = month;
        this.monthName = monthName;
        this.totalActivitiesLogged = totalActivitiesLogged;
        this.averageCompletion = averageCompletion;
        this.days = days;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public int getTotalActivitiesLogged() {
        return totalActivitiesLogged;
    }

    public void setTotalActivitiesLogged(int totalActivitiesLogged) {
        this.totalActivitiesLogged = totalActivitiesLogged;
    }

    public int getAverageCompletion() {
        return averageCompletion;
    }

    public void setAverageCompletion(int averageCompletion) {
        this.averageCompletion = averageCompletion;
    }

    public List<CalendarDayDTO> getDays() {
        return days;
    }

    public void setDays(List<CalendarDayDTO> days) {
        this.days = days;
    }
}
