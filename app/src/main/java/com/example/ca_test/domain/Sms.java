package com.example.ca_test.domain;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Sms {
    private String address;
    private String body;
    private long date;

    public Sms(String address, String body, long date) {
        this.address = address;
        this.body = body;
        this.date = date;
    }

    public String getAddress() {
        return address;
    }

    public String getBody() {
        return body;
    }

    public long getDate() {
        return date;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(date));
    }
}
