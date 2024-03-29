package com.hgnis.reader.services;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hgnis.reader.R;
import com.hgnis.reader.api.apiInterface;
import com.hgnis.reader.crop_slider.CropImageView;
import com.hgnis.reader.helper.ImageTransmogrifier;
import com.hgnis.reader.utility.AppSharePreference;
import com.hgnis.reader.utility.NetworkUtils;
import com.hgnis.reader.visionModel.VisionModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;
import es.dmoral.toasty.Toasty;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class Reader extends Service implements FloatingViewListener {
    public static final String EXTRA_CUTOUT_SAFE_AREA = "cutout_safe_area";
    public static final int NOTIFICATION_ID = 9083150;
    static final int VIRT_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    public static CircleImageView iconView = null;
    public static Intent resultData = null;
    public static int resultCode = 0;
    final private HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName(), android.os.Process.THREAD_PRIORITY_BACKGROUND);
    public LayoutInflater inflater;
    public WindowManager windowManager;
    public View view;
    /*Screenshot Variables */

    Context context = Reader.this;
    DisplayMetrics metrics;
    ImageView crossArrow, tickArrow;
    Bitmap icon;
    CropImageView cropImageView;
    NotificationManager mNotificationManager;
    String timeStamp;
    AppOpsManager appOps;
    UsageStatsManager usm;
    String defaultHomePackageName, currentForegroundPackageName;
    String extractText;
    TextToSpeech tss;
    int count;
    int Ccounter;
    AppSharePreference appSharePreference;
    private FloatingViewManager mFloatingViewManager;
    private MediaProjection projection;
    private VirtualDisplay vdisplay;
    private Handler handler;
    private MediaProjectionManager mgr;
    private ImageTransmogrifier it;
   /* private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String APP_SHARED_PREFS;*/

    public Reader() {
    }

    public int getStatusBarHeight() {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        int result = 0;
        int resourceId = cw.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = cw.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appSharePreference = new AppSharePreference(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mgr = (MediaProjectionManager) this.getSystemService(MEDIA_PROJECTION_SERVICE);
            usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);


        metrics = new DisplayMetrics();
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        initTextToSpeech();



    }

    private void initTextToSpeech() {
        tss = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int lang = tss.setLanguage(Locale.forLanguageTag(appSharePreference.getLanguageCode()));
                tss.setVoice(tss.getVoice());
                tss.setPitch(1);
                tss.setSpeechRate(0.9f);
                Log.i("TSS ", "Available Languages==========>>" + tss.getAvailableLanguages());
                if (lang == TextToSpeech.LANG_MISSING_DATA) {
                    Toasty.error(Reader.this, "Sorry, Language Data Missing").show();
                    Log.e("TTS", "Language not supported" + lang);
                } else if (lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    //Toasty.error(Reader.this, "Sorry, This Language Not Supported Currently").show();

                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mFloatingViewManager != null) {
            return START_REDELIVER_INTENT;
        }
        initTextToSpeech();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        inflater = LayoutInflater.from(this);
        iconView = (CircleImageView) inflater.inflate(R.layout.widget_chathead, null, false);

        iconView.setOnClickListener(v -> {
            extractText = "";
            if (tss.isSpeaking()) {
                tss.stop();
            }
            count = 0;
            Ccounter = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                if (usageAccessGranted(context)) {

                    getCurrentAppForegound();

                    if (defaultHomePackageName.equalsIgnoreCase(currentForegroundPackageName)) {
                        Toast.makeText(context, "Open app where I have to read", Toast.LENGTH_LONG).show();
                    } else {
                        createLayoutForServiceClass();
                    }
                } else {
                    Intent intent1 = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    intent1.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent1);
                }
            }
        });

        mFloatingViewManager = new FloatingViewManager(this, this);
        mFloatingViewManager.setFixedTrashIconImage(R.drawable.recycle_bin);
        mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_SHOW_ALWAYS);
        mFloatingViewManager.setSafeInsetRect(intent.getParcelableExtra(EXTRA_CUTOUT_SAFE_AREA));
        final FloatingViewManager.Options options = new FloatingViewManager.Options();
        options.overMargin = (int) (16 * metrics.density);
        mFloatingViewManager.addViewToWindow(iconView, options);
        startForeground(NOTIFICATION_ID, createNotification(this));

        return START_REDELIVER_INTENT;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean usageAccessGranted(Context context) {
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void createLayoutForServiceClass() {

        iconView.setVisibility(View.GONE);

        inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);

        params.gravity = Gravity.END | Gravity.TOP;
        view = inflater.inflate(R.layout.area_selection, null);

        crossArrow = view.findViewById(R.id.iv_cross);
        tickArrow = view.findViewById(R.id.iv_tick);
        cropImageView = view.findViewById(R.id.CropImageView);

        icon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.transparent_image);
        cropImageView.setImageBitmap(icon);

        crossArrow.setOnClickListener(v -> {
            Log.e("clicked", "Image is clicked");
            view.setVisibility(View.GONE);
            iconView.setVisibility(View.VISIBLE);
        });
        tickArrow.setOnClickListener(v -> {
            crossArrow.setVisibility(View.GONE);
            tickArrow.setVisibility(View.GONE);
            Log.e("clicked", "Image is clicked");
            startCapture();
        });
        windowManager.addView(view, params);

    }

    public void getCurrentAppForegound() {

        PackageManager localPackageManager = getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        defaultHomePackageName = localPackageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName;
        Log.e("hgnis", defaultHomePackageName);

        if (Build.VERSION.SDK_INT >= 21) {
            long time = System.currentTimeMillis();
            List<UsageStats> applist = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
            if (applist != null && applist.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                for (UsageStats usageStats : applist) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    currentForegroundPackageName = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
            Log.e("hgnis", "Current App in foreground is: " + currentForegroundPackageName);
        } else {

            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            currentForegroundPackageName = (manager.getRunningTasks(1).get(0)).topActivity.getPackageName();
            Log.e("hgnis", "Current App in foreground is: " + currentForegroundPackageName);
        }

    }

    @Override
    public void onDestroy() {

        destroy();
        stopCapture();
        Log.e("onDestroyService", "onDestroyService");
        super.onDestroy();
        tss.stop();
        tss.shutdown();

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void destroy() {

        if (mFloatingViewManager != null) {
            mFloatingViewManager.removeAllViewToWindow();
            mFloatingViewManager = null;
        }
    }

    private Notification createNotification(Context context) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getResources().getString(R.string.default_floatingview_channel_id));
        Notification notification = builder.setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(this.getResources().getString(R.string.app_name))
                .setContentText(this.getResources().getString(R.string.running))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        return notification;
    }

    @Override
    public void onFinishFloatingView() {
        stopSelf();
    }

    @Override
    public void onTouchFinished(boolean isFinishing, int x, int y) {

    }

    /*Screenshot Code*/
    public void processImage(final byte[] png) {
        File outputImage = new File(getExternalCacheDir()/*getExternalFilesDir(null)*/, "image" + ".png");
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                //File outputImage = new File(getCacheDir()/*getExternalFilesDir(null)*/, "image" + ".png");
                FileOutputStream fos = null;

                try {
                    fos = new FileOutputStream(outputImage);
                    fos.write(png);
                    fos.flush();
                    fos.getFD().sync();
                    fos.close();


                    MediaScannerConnection.scanFile(Reader.this,
                            new String[]{outputImage.getAbsolutePath()},
                            new String[]{"image/png"},
                            null);
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "Exception writing out screenshot", e);
                    Toast.makeText(context, "WRITE ERROR " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                hideViews();
            }
        });

        stopCapture();
        loadImageFromStorage(outputImage);
        //loadImageFromStorage(getCacheDir(), "image");
    }

    private void hideViews() {
        if (windowManager != null) {
            windowManager.removeView(view);
            windowManager = null;
        }

        iconView.setVisibility(View.VISIBLE);
    }

    private void loadImageFromStorage(File path) {

        try {
            File f = new File(String.valueOf(path));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            b.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            //vision Api
            JsonObject type = new JsonObject();
            JsonObject content = new JsonObject();
            JsonObject requests = new JsonObject();
            JsonArray jsonArray = new JsonArray();

            content.addProperty("content", imageString);
            type.addProperty("type", "TEXT_DETECTION");/*TEXT_DETECTION*//*DOCUMENT_TEXT_DETECTION*/
            requests.add("image", content);
            JsonArray array = new JsonArray();
            array.add(type);
            requests.add("features", array);
            jsonArray.add(requests);
            JsonObject request = new JsonObject();
            request.add("requests", jsonArray);
            if (count == 0) {
                count = count + 1;
                boolean stat = NetworkUtils.isOnline(this);
                if (stat) {
                    Retrofit.Builder builder = new Retrofit.Builder()
                            .baseUrl("https://vision.googleapis.com/v1/images:annotate/")
                            .addConverterFactory(GsonConverterFactory.create());
                    Retrofit retrofit = builder.build();
                    apiInterface apiInterface = retrofit.create(apiInterface.class);
                    Call<VisionModel> call = apiInterface.visionApi(request);
                    call.enqueue(new Callback<VisionModel>() {
                        @Override
                        public void onResponse(Call<VisionModel> call, Response<VisionModel> response) {
                            if (response.isSuccessful()) {
                                if (response.body() != null) {
                                    try {
                                        Log.i("RestAPI", "VISION " + response.body().getResponses().get(0).getFullTextAnnotation().getText());
                                        extractText = response.body().getResponses().get(0).getFullTextAnnotation().getText();
                                        tss();

                                    } catch (Exception e) {
                                        Toast.makeText(Reader.this, "Try Again ", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<VisionModel> call, Throwable t) {
                            Log.i("TAG", "onFailure: " + t.getLocalizedMessage());
                            Toast.makeText(Reader.this, "File to large to process !! \nTry Again", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "No internet Connection", Toast.LENGTH_LONG).show();
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    private void tss() {
        if (tss != null) {
            if (tss.isSpeaking()) {
                tss.stop();
            } else {
                tss.speak(extractText, TextToSpeech.QUEUE_FLUSH, null);

            }
        }
    }


    public void stopCapture() {
        if (projection != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                projection.stop();
            }
            vdisplay.release();
            projection = null;
        }
    }

    public void startCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            projection = mgr.getMediaProjection(resultCode, resultData);
        }

        it = new ImageTransmogrifier(this);

        MediaProjection.Callback cb = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cb = new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    vdisplay.release();
                }
            };
            vdisplay = projection.createVirtualDisplay("com.hgnis.reader",
                    it.getWidth(), it.getHeight(),
                    getResources().getDisplayMetrics().densityDpi
                    , VIRT_DISPLAY_FLAGS, it.getSurface(), null, handler);
            projection.registerCallback(cb, handler);

        }

    }

    public WindowManager getWindowManager() {
        return (windowManager);
    }

    public Handler getHandler() {
        return (handler);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("onTaskRemoved", "onTaskRemoved");

        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(Reader.this, Reader.class));
        } else {
            context.startService(new Intent(Reader.this, Reader.class));
        }
        super.onTaskRemoved(rootIntent);
    }


}
