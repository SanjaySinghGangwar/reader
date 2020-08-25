package com.hgnis.reader.views.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.hgnis.reader.R;
import com.hgnis.reader.services.Reader;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;

import static com.hgnis.reader.services.Reader.NOTIFICATION_ID;

public class MainActivity extends AppCompatActivity {

    private static final int CHATHEAD_OVERLAY_PERMISSION_REQUEST_CODE = 100;
    private static final int CUSTOM_OVERLAY_PERMISSION_REQUEST_CODE = 101;
    private static final int REQUEST_SCREENSHOT = 59706;
    public static List<Intent> sendingIntent = new ArrayList<>();

    Intent intent;
    NotificationChannel defaultChannel;
    NotificationManager manager;
    @BindView(R.id.OPEN)
    Button OPEN;

    /*@BindView(R.id.OPEN)
    Button OPEN;*/

    private MediaProjectionManager mgr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


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

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

                Toast.makeText(MainActivity.this, "Permission to take screenshot is reired", Toast.LENGTH_SHORT).show();
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
        showFloatingView(MainActivity.this, true, false);
        Log.e("onDestroyActivityShow", "onDestroyActivityShow");
    }

    @OnClick(R.id.OPEN)
    public void onViewClicked() {
        showFloatingView(MainActivity.this, true, false);
    }
}