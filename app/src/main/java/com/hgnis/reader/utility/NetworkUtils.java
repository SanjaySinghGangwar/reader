package com.hgnis.reader.utility;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.hgnis.reader.views.activity.MainActivity;

public class NetworkUtils {
    static RewardedAd rewardedAd;

    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    public static void showAds(Activity activityName) {
        rewardedAd = new RewardedAd(activityName, "ca-app-pub-3940256099942544/5224354917"/*getString(R.string.RewardAdd)*/);
        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                if (rewardedAd.isLoaded()) {
                    RewardedAdCallback adCallback = new RewardedAdCallback() {
                        @Override
                        public void onRewardedAdOpened() {
                            Toast.makeText(activityName, "Watch this full video till we send messages", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onRewardedAdClosed() {
                        }

                        @Override
                        public void onUserEarnedReward(@NonNull RewardItem reward) {
                            Toast.makeText(activityName, "Thanks For Support", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onRewardedAdFailedToShow(AdError adError) {
                        }
                    };
                    rewardedAd.show(activityName, adCallback);
                } else {
                    Log.d("TAG", "The rewarded ad wasn't loaded yet.");
                }
            }

            @Override
            public void onRewardedAdFailedToLoad(LoadAdError adError) {
                Log.d("AddMOb", "onRewardedAdFailedToLoad: " + adError.getMessage());
            }

        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);
    }
}
