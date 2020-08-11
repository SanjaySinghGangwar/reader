package com.hgnis.reader.api;

import com.google.gson.JsonObject;
import com.hgnis.reader.visionModel.VisionModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface apiInterface {

    @POST("?key=AIzaSyA7hQ5A_MnRf2TM2yf0nIO61wdqNKPWgyQ")
    Call<VisionModel> visionApi(@Body JsonObject dataToSend);


}
