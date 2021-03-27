package com.defold.applovin;

import android.app.Activity;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;

import org.json.JSONObject;
import org.json.JSONException;

public class AppLovinMaxJNI {

    private static final String TAG = "AppLovinMaxJNI";

    public static native void applovinAddToQueue(int msg, String json);

    // CONSTANTS:
    // duplicate of enums from applovin_callback_private.h:
    private static final int MSG_INTERSTITIAL =         1;
    private static final int MSG_REWARDED =             2;
    private static final int MSG_BANNER =               3;
    private static final int MSG_INITIALIZATION =       4;

    private static final int EVENT_CLOSED =             1;
    private static final int EVENT_FAILED_TO_SHOW =     2;
    private static final int EVENT_OPENING =            3;
    private static final int EVENT_FAILED_TO_LOAD =     4;
    private static final int EVENT_LOADED =             5;
    private static final int EVENT_NOT_LOADED =         6;
    private static final int EVENT_EARNED_REWARD =      7;
    private static final int EVENT_COMPLETE =           8;
    private static final int EVENT_CLICKED =            9;
    private static final int EVENT_UNLOADED =           10;

    // END CONSTANTS


    private Activity activity;

    public AppLovinMaxJNI(Activity activity) {
        this.activity = activity;
    }

    public void initialize() {
        AppLovinSdk.getInstance(activity).getSettings().setVerboseLogging(true); // UNDONE Debug
        AppLovinSdk.getInstance(activity).setMediationProvider(AppLovinMediationProvider.MAX);
        AppLovinSdk.getInstance(activity).initializeSdk(new AppLovinSdk.SdkInitializationListener()
        {
            @Override
            public void onSdkInitialized(final AppLovinSdkConfiguration config)
            {
                sendSimpleMessage(MSG_INITIALIZATION, EVENT_COMPLETE);
            }
        } );
    }

    // https://www.baeldung.com/java-json-escaping
    private String getJsonConversionErrorMessage(String messageText) {
        String message = null;
        try {
            JSONObject obj = new JSONObject();
            obj.put("error", messageText);
            message = obj.toString();
        } catch (JSONException e) {
            message = "{ \"error\": \"Error while converting simple message to JSON.\" }";
        }
        return message;
    }

    private void sendSimpleMessage(int msg, int eventId) {
        String message = null;
        try {
            JSONObject obj = new JSONObject();
            obj.put("event", eventId);
            message = obj.toString();
        } catch (JSONException e) {
            message = getJsonConversionErrorMessage(e.getMessage());
        }
        applovinAddToQueue(msg, message);
    }

    private void sendSimpleMessage(int msg, int eventId, String key_2, String value_2) {
        String message = null;
        try {
            JSONObject obj = new JSONObject();
            obj.put("event", eventId);
            obj.put(key_2, value_2);
            message = obj.toString();
        } catch (JSONException e) {
            message = getJsonConversionErrorMessage(e.getMessage());
        }
        applovinAddToQueue(msg, message);
    }

    private void sendSimpleMessage(int msg, int eventId, String key_2, int value_2, String key_3, String value_3) {
        String message = null;
        try {
            JSONObject obj = new JSONObject();
            obj.put("event", eventId);
            obj.put(key_2, value_2);
            obj.put(key_3, value_3);
            message = obj.toString();
        } catch (JSONException e) {
            message = getJsonConversionErrorMessage(e.getMessage());
        }
        applovinAddToQueue(msg, message);
    }

//--------------------------------------------------
// Interstitial ADS

    private  MaxInterstitialAd interstitialAd;

    public void loadInterstitial(final String unitId) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                interstitialAd = new MaxInterstitialAd(unitId, activity);
                interstitialAd.setListener(new MaxAdListener() {
                    @Override
                    public void onAdLoaded(MaxAd ad) {
                        if (ad == interstitialAd) {
                            // to prevent reporting obsolete ad (if loadInterstitial was called multiple times)
                            return;
                        }

                        sendSimpleMessage(MSG_INTERSTITIAL, EVENT_LOADED);
                    }

                    @Override
                    public void onAdLoadFailed(String adUnitId, int errorCode) {
                        sendSimpleMessage(MSG_INTERSTITIAL, EVENT_FAILED_TO_LOAD,
                                "code", errorCode, "adUnitId", adUnitId);
                    }

                    @Override
                    public void onAdDisplayed(MaxAd ad) {
                        sendSimpleMessage(MSG_INTERSTITIAL, EVENT_OPENING,
                                "adUnitId", ad.getAdUnitId());
                    }

                    @Override
                    public void onAdHidden(MaxAd ad) {
                        sendSimpleMessage(MSG_INTERSTITIAL, EVENT_CLOSED,
                                "adUnitId", ad.getAdUnitId());
                    }

                    @Override
                    public void onAdClicked(MaxAd ad) {
                        sendSimpleMessage(MSG_INTERSTITIAL, EVENT_CLICKED,
                                "adUnitId", ad.getAdUnitId());
                    }

                    @Override
                    public void onAdDisplayFailed(MaxAd ad, int errorCode) {
                        sendSimpleMessage(MSG_INTERSTITIAL, EVENT_FAILED_TO_SHOW,
                                "code", errorCode, "adUnitId", ad.getAdUnitId());
                    }
                });

                interstitialAd.loadAd();
            }
        });
    }

    public void showInterstitial() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isInterstitialLoaded()) {
                    interstitialAd.showAd();
                } else {
                    // Log.d(TAG, "The interstitial ad wasn't ready yet.");
                    sendSimpleMessage(MSG_INTERSTITIAL, EVENT_NOT_LOADED,
                            "error", "Can't show Interstitial AD that wasn't loaded.");
                }
            }
        });
    }

    public boolean isInterstitialLoaded() {
        return interstitialAd != null && interstitialAd.isReady();
    }
}
