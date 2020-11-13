package com.hgnis.reader.views.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hgnis.reader.R;
import com.hgnis.reader.services.Reader;
import com.hgnis.reader.utility.AppSharePreference;
import com.hgnis.reader.utility.NetworkUtils;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;

import static com.hgnis.reader.services.Reader.NOTIFICATION_ID;
import static com.hgnis.reader.utility.NetworkUtils.showAds;

public class MainActivity extends AppCompatActivity {

    private static final int CHATHEAD_OVERLAY_PERMISSION_REQUEST_CODE = 100;
    private static final int CUSTOM_OVERLAY_PERMISSION_REQUEST_CODE = 101;
    private static final int REQUEST_SCREENSHOT = 59706;
    RewardedAd rewardedAd;
    Intent intent;
    String languageSelected;
    String targetLanguage;
    NotificationChannel defaultChannel;
    NotificationManager manager;
    @BindView(R.id.OPEN)
    ImageView OPEN;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String APP_SHARED_PREFS;
    AppSharePreference appSharePreference;
    private MediaProjectionManager mgr;

    AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        sharedPref();
        addFreature();
        initilizeALL();
        if (NetworkUtils.isOnline(this)) {
            showAds(MainActivity.this);
        }

    }

    private void initilizeALL() {
        appSharePreference = new AppSharePreference(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String channelId = getString(R.string.default_floatingview_channel_id);
            final String channelName = getString(R.string.default_floatingview_channel_name);
            defaultChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(defaultChannel);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Reader.resultData == null) {
                startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_SCREENSHOT);
            }
        }

    }

    private void addFreature() {
        MobileAds.initialize(this, initializationStatus -> {

        });

        mAdView = findViewById(R.id.bannerOne);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void sharedPref() {
        sharedPreferences = this.getSharedPreferences(APP_SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        APP_SHARED_PREFS = "Tutor";
    }

    private void installVoiceData() {
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.google.android.tts"/*replace with the package name of the target TTS engine*/);
        String TAG = "TTS";
        try {
            Log.v(TAG, "Installing voice data: " + intent.toUri(0));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "" + ex.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to install TTS data, no acitivty found for " + intent + ")");
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHATHEAD_OVERLAY_PERMISSION_REQUEST_CODE) {
            showFloatingView(this, false, false);
        } else if (requestCode == CUSTOM_OVERLAY_PERMISSION_REQUEST_CODE) {
            showFloatingView(this, false, true);
        }

        if (requestCode == REQUEST_SCREENSHOT) {
            if (resultCode == RESULT_OK) {
                Reader.resultCode = resultCode;
                Reader.resultData = (Intent) data.clone();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(MainActivity.this, "Permission to take screenshot is required", Toast.LENGTH_SHORT).show();
                startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_SCREENSHOT);
            }
        }
    }

    @SuppressLint("NewApi")
    private void showFloatingView(Activity activity, boolean isShowOverlayPermission, boolean isCustomFloatingView) {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            startFloatingViewService(this, isCustomFloatingView);
            return;
        }
        if (Settings.canDrawOverlays(activity)) {
            startFloatingViewService(this, isCustomFloatingView);
            return;
        }

        if (isShowOverlayPermission) {
            final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
            startActivityForResult(intent, CHATHEAD_OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }


    private void startFloatingViewService(Activity activity, boolean isCustomFloatingView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (activity.getWindow().getAttributes().layoutInDisplayCutoutMode == WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER) {
                throw new RuntimeException("'windowLayoutInDisplayCutoutMode' do not be set to 'never'");
            }
            if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                throw new RuntimeException("Do not set Activity to landscape");
            }
        }

        intent = new Intent(activity, Reader.class);
        intent.putExtra(Reader.EXTRA_CUTOUT_SAFE_AREA, FloatingViewManager.findCutoutSafeArea(activity));
        ContextCompat.startForegroundService(activity, intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.e("onDestroyActivity", "onDestroyActivity");

        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
            Log.e("onDestroyActivityCancel", "onDestroyActivityCancel");
        }

        Intent serviceIntent = new Intent(MainActivity.this, Reader.class);
        stopService(serviceIntent);

        Log.e("onDestroyActivityStop", "onDestroyActivityStop");
        //showFloatingView(MainActivity.this, true, false);
        Log.e("onDestroyActivityShow", "onDestroyActivityShow");

    }

    @OnClick(R.id.OPEN)
    public void onViewClicked() {
        if (sharedPreferences.getString("targetLanguage", "").isEmpty()) {
            Toast.makeText(this, "Select a Language first", Toast.LENGTH_SHORT).show();
        } else {
            showFloatingView(MainActivity.this, true, false);
        }


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                minimizeApp();
                return true;
        }
        return super.onKeyDown(keyCode, event);

    }

    public void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    public void downloadLanguages(View view) {
        installVoiceData();
    }

    public void setLanguage(View view) {
        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this);
        View viewLanguage = getLayoutInflater().inflate(R.layout.popup_language, null);
        alert.setView(viewLanguage);
        final AlertDialog alertDialog = alert.create();
        alertDialog.show();
        SearchableSpinner searchableSpinner = viewLanguage.findViewById(R.id.languageSpinner);
        searchableSpinner.setTitle("Select language in which it has to converted");
        searchableSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        targetLanguage = "af";
                        languageSelected = "Afrikaans";
                        break;
                    case 1:
                        targetLanguage = "sq";
                        languageSelected = "Albanian";
                        break;
                    case 2:
                        targetLanguage = "am";
                        languageSelected = "Amharic";
                        break;
                    case 3:
                        targetLanguage = "ar";
                        languageSelected = "Arabic";
                        break;
                    case 4:
                        targetLanguage = "hy";
                        languageSelected = "Armenian";
                        break;
                    case 5:
                        targetLanguage = "az";
                        languageSelected = "Azerbaijani";
                        break;
                    case 6:
                        targetLanguage = "eu";
                        languageSelected = "Basque";
                        break;
                    case 7:
                        targetLanguage = "be";
                        languageSelected = "Belarusian";
                        break;
                    case 8:
                        targetLanguage = "bn";
                        languageSelected = "Bengali";
                        break;
                    case 9:
                        targetLanguage = "bs";
                        languageSelected = "Bosnian";
                        break;
                    case 10:
                        targetLanguage = "bg";
                        languageSelected = "Bulgarian";
                        break;
                    case 11:
                        targetLanguage = "ca";
                        languageSelected = "Catalan";
                        break;
                    case 12:
                        targetLanguage = "ceb";
                        languageSelected = "Cebuano";
                        break;
                    case 13:
                        targetLanguage = "zh-CN";
                        languageSelected = "Chinese (Simplified)";
                        break;
                    case 14:
                        targetLanguage = "zh-TW";
                        languageSelected = "Chinese (Traditional)";
                        break;
                    case 15:
                        targetLanguage = "co";
                        languageSelected = "Corsican";
                        break;
                    case 16:
                        targetLanguage = "hr";
                        languageSelected = "Croatian";
                        break;
                    case 17:
                        targetLanguage = "cs";
                        languageSelected = "Czech";
                        break;
                    case 18:
                        targetLanguage = "da";
                        languageSelected = "Danish";
                        break;
                    case 19:
                        targetLanguage = "nl";
                        languageSelected = "Dutch";
                        break;
                    case 20:
                        targetLanguage = "en";
                        languageSelected = "English";
                        break;
                    case 21:
                        targetLanguage = "eo";
                        languageSelected = "Esperanto";
                        break;
                    case 22:
                        targetLanguage = "et";
                        languageSelected = "Estonian";
                        break;
                    case 23:
                        targetLanguage = "fi";
                        languageSelected = "Finnish";
                        break;
                    case 24:
                        targetLanguage = "fr";
                        languageSelected = "French";
                        break;
                    case 25:
                        targetLanguage = "fy";
                        languageSelected = "Frisian";
                        break;
                    case 26:
                        targetLanguage = "gl";
                        languageSelected = "Galician";
                        break;
                    case 27:
                        targetLanguage = "ka";
                        languageSelected = "Georgian";
                        break;
                    case 28:
                        targetLanguage = "de";
                        languageSelected = "German";
                        break;
                    case 29:
                        targetLanguage = "el";
                        languageSelected = "Greek";
                        break;
                    case 30:
                        targetLanguage = "gu";
                        languageSelected = "Gujarati";
                        break;
                    case 31:
                        targetLanguage = "ht";
                        languageSelected = "Haitian Creole";
                        break;
                    case 32:
                        targetLanguage = "ha";
                        languageSelected = "Hausa";
                        break;
                    case 33:
                        targetLanguage = "haw";
                        languageSelected = "Hawaiian";
                        break;
                    case 34:
                        targetLanguage = "he";
                        languageSelected = "Hebrew";
                        break;
                    case 35:
                        targetLanguage = "hi";
                        languageSelected = "Hindi";
                        break;
                    case 36:
                        targetLanguage = "hmn";
                        languageSelected = "Hmong";
                        break;
                    case 37:
                        targetLanguage = "hu";
                        languageSelected = "Hungarian";
                        break;
                    case 38:
                        targetLanguage = "is";
                        languageSelected = "Icelandic";
                        break;
                    case 39:
                        targetLanguage = "ig";
                        languageSelected = "Igbo";
                        break;
                    case 40:
                        targetLanguage = "id";
                        languageSelected = "Indonesian";
                        break;
                    case 41:
                        targetLanguage = "ga";
                        languageSelected = "Irish";
                        break;
                    case 42:
                        targetLanguage = "it";
                        languageSelected = "Italian";
                        break;
                    case 43:
                        targetLanguage = "ja";
                        languageSelected = "Javanese";
                        break;
                    case 44:
                        targetLanguage = "jv";
                        languageSelected = "Javanese";
                        break;
                    case 45:
                        targetLanguage = "kn";
                        languageSelected = "Kannada";
                        break;
                    case 46:
                        targetLanguage = "kk";
                        languageSelected = "Kazakh";
                        break;
                    case 47:
                        targetLanguage = "km";
                        languageSelected = "Khmer";
                        break;
                    case 48:
                        targetLanguage = "rw";
                        languageSelected = "Kinyarwanda";
                        break;
                    case 49:
                        targetLanguage = "ko";
                        languageSelected = "Korean";
                        break;
                    case 50:
                        targetLanguage = "ku";
                        languageSelected = "Kurdish";
                        break;
                    case 51:
                        targetLanguage = "ky";
                        languageSelected = "Kyrgyz";
                        break;
                    case 52:
                        targetLanguage = "lo";
                        languageSelected = "Lao";
                        break;
                    case 53:
                        targetLanguage = "la";
                        languageSelected = "Latin";
                        break;
                    case 54:
                        targetLanguage = "lv";
                        languageSelected = "Latvian";
                        break;
                    case 55:
                        targetLanguage = "lt";
                        languageSelected = "Lithuanian";
                        break;
                    case 56:
                        targetLanguage = "lb";
                        languageSelected = "Luxembourgish";
                        break;
                    case 57:
                        targetLanguage = "mk";
                        languageSelected = "Macedonian";
                        break;
                    case 58:
                        targetLanguage = "mg";
                        languageSelected = "Malagasy";
                        break;
                    case 59:
                        targetLanguage = "ms";
                        languageSelected = "Malay";
                        break;
                    case 60:
                        targetLanguage = "ml";
                        languageSelected = "Malayalam";
                        break;
                    case 61:
                        targetLanguage = "mt";
                        languageSelected = "Maltese";
                        break;
                    case 62:
                        targetLanguage = "mi";
                        languageSelected = "Maori";
                        break;
                    case 63:
                        targetLanguage = "mr";
                        languageSelected = "Marathi";
                        break;
                    case 64:
                        targetLanguage = "mn";
                        languageSelected = "Mongolian";
                        break;
                    case 65:
                        targetLanguage = "my";
                        languageSelected = "Myanmar (Burmese)";
                        break;
                    case 66:
                        targetLanguage = "ne";
                        languageSelected = "Nepali";
                        break;
                    case 67:
                        targetLanguage = "no";
                        languageSelected = "Norwegian";
                        break;
                    case 68:
                        targetLanguage = "ny";
                        languageSelected = "Nyanja (Chichewa)";
                        break;
                    case 69:
                        targetLanguage = "or";
                        languageSelected = "Odia (Oriya)";
                        break;
                    case 70:
                        targetLanguage = "ps";
                        languageSelected = "Pashto";
                        break;
                    case 71:
                        targetLanguage = "fa";
                        languageSelected = "Persian";
                        break;
                    case 72:
                        targetLanguage = "pl";
                        languageSelected = "Polish";
                        break;
                    case 73:
                        targetLanguage = "pt";
                        languageSelected = "Portuguese (Portugal, Brazil)";
                        break;
                    case 74:
                        targetLanguage = "pa";
                        languageSelected = "Punjabi";
                        break;
                    case 75:
                        targetLanguage = "ro";
                        languageSelected = "Romanian";
                        break;
                    case 76:
                        targetLanguage = "ru";
                        languageSelected = "Russian";
                        break;
                    case 77:
                        targetLanguage = "sm";
                        languageSelected = "Samoan";
                        break;
                    case 78:
                        targetLanguage = "gd";
                        languageSelected = "Scots Gaelic";
                        break;
                    case 79:
                        targetLanguage = "sr";
                        languageSelected = "Serbian";
                        break;
                    case 80:
                        targetLanguage = "st";
                        languageSelected = "Sesotho";
                        break;
                    case 81:
                        targetLanguage = "sn";
                        languageSelected = "Shona";
                        break;
                    case 82:
                        targetLanguage = "sd";
                        languageSelected = "Sindhi";
                        break;
                    case 83:
                        targetLanguage = "si";
                        languageSelected = "Sinhala (Sinhalese)";
                        break;
                    case 84:
                        targetLanguage = "sk";
                        languageSelected = "Slovak";
                        break;
                    case 85:
                        targetLanguage = "sl";
                        languageSelected = "Slovenian";
                        break;
                    case 86:
                        targetLanguage = "so";
                        languageSelected = "Somali";
                        break;
                    case 87:
                        targetLanguage = "es";
                        languageSelected = "Spanish";
                        break;
                    case 88:
                        targetLanguage = "su";
                        languageSelected = "Sundanese";
                        break;
                    case 89:
                        targetLanguage = "sw";
                        languageSelected = "Swahili";
                        break;
                    case 90:
                        targetLanguage = "sv";
                        languageSelected = "Swedish";
                        break;
                    case 91:
                        targetLanguage = "tl";
                        languageSelected = "Tagalog (Filipino)";
                        break;
                    case 92:
                        targetLanguage = "tg";
                        languageSelected = "Tajik";
                        break;
                    case 93:
                        targetLanguage = "ta";
                        languageSelected = "Tamil";
                        break;
                    case 94:
                        targetLanguage = "tt";
                        languageSelected = "Tatar";
                        break;
                    case 95:
                        targetLanguage = "te";
                        languageSelected = "Telugu";
                        break;
                    case 96:
                        targetLanguage = "th";
                        languageSelected = "Thai";
                        break;
                    case 97:
                        targetLanguage = "tr";
                        languageSelected = "Turkish";
                        break;
                    case 98:
                        targetLanguage = "tk";
                        languageSelected = "Turkmen";
                        break;
                    case 99:
                        targetLanguage = "uk";
                        languageSelected = "Ukrainian";
                        break;
                    case 100:
                        targetLanguage = "ur";
                        languageSelected = "Urdu";
                        break;
                    case 101:
                        targetLanguage = "ug";
                        languageSelected = "Uyghur";
                        break;
                    case 102:
                        targetLanguage = "uz";
                        languageSelected = "Uzbek";
                        break;
                    case 103:
                        targetLanguage = "vi";
                        languageSelected = "Vietnamese";
                        break;
                    case 104:
                        targetLanguage = "cy";
                        languageSelected = "Welsh";
                        break;
                    case 105:
                        targetLanguage = "xh";
                        languageSelected = "Xhosa";
                        break;
                    case 106:
                        targetLanguage = "yi";
                        languageSelected = "Yiddish";
                        break;
                    case 107:
                        targetLanguage = "yo";
                        languageSelected = "Yoruba";
                        break;
                    case 108:
                        targetLanguage = "zu";
                        languageSelected = "Zulu";
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        Button setLanguage = viewLanguage.findViewById(R.id.setLanguage);
        setLanguage.setOnClickListener(view1 -> {
            if (targetLanguage.isEmpty() && languageSelected.isEmpty()) {
                Toast.makeText(MainActivity.this, "Select a language", Toast.LENGTH_SHORT).show();
            } else {
                editor.putString("targetLanguage", targetLanguage);
                editor.putString("SelectedLanguage", languageSelected);
                editor.commit();
                alertDialog.dismiss();
                showFloatingView(MainActivity.this, true, false);
            }

        });
    }

}