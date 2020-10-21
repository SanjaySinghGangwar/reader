package com.hgnis.reader.utility;


import android.content.Context;
import android.content.SharedPreferences;

public class AppSharePreference {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String APP_SHARED_PREFS;

    public AppSharePreference(Context mContext) {
        sharedPreferences = mContext.getSharedPreferences(APP_SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        APP_SHARED_PREFS = "Tutor";
    }

    public void clearPreferences() {
        editor.clear();
        editor.commit();
    }

    public String getName() {
        return sharedPreferences.getString("Name", "");
    }

    public void setName(String MyName) {
        editor.putString("Name", MyName);
        editor.commit();
    }

    public String getCounter() {
        return sharedPreferences.getString("Counter", "");
    }

    public void setCounter(String Counter) {
        editor.putString("Counter", Counter);
        editor.commit();
    }


}
