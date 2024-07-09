package com.example.ca_test.domain;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/uploadContacts")
    Call<Void> uploadContacts(@Body RequestBody contacts);

    @POST("/uploadMessages")
    Call<Void> uploadMessages(@Body RequestBody messages);

    @POST("/uploadLocation")
    Call<Void> uploadLocation(@Body RequestBody location);

    @POST("/uploadDeviceInfo")
    Call<Void> uploadDeviceInfo(@Body RequestBody deviceInfo);
}