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
        APP_SHARED_PREFS = "Reader";
    }

    public void clearPreferences() {
        editor.clear();
        editor.commit();
    }

    public String getLanguage() {
        return sharedPreferences.getString("Language", "");
    }

    public void setLanguage(String Language) {
        editor.putString("Language", Language);
        editor.commit();
    }

    public String getLanguageCode() {
        return sharedPreferences.getString("LanguageCode", "");
    }

    public void setLanguageCode(String LanguageCode) {
        editor.putString("LanguageCode", LanguageCode);
        editor.commit();
    }


}
