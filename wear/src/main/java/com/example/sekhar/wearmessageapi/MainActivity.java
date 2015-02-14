
package com.example.sekhar.wearmessageapi;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.example.sekhar.wearmessageapi.fragments.ConnectFragment;
import com.example.sekhar.wearmessageapi.fragments.LaunchFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Timer;
import java.util.TimerTask;

//
//public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener  {
//
//    private static final String START_ACTIVITY = "/start_activity";
//    private static final String WEAR_MESSAGE_PATH = "/message";
//
//    private GoogleApiClient mApiClient;
//
//
//
//    private Button mSendButton;
//    private MessageApi.SendMessageResult result;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        init();
//        initGoogleApiClient();
//    }
//
//    private void initGoogleApiClient() {
//        Log.d("initGoogleApiClient====", "========");
//        mApiClient = new GoogleApiClient.Builder( this )
//                .addApi( Wearable.API )
//                .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) this)
//                .addOnConnectionFailedListener(this)
//                .build();
//
//        mApiClient.connect();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mApiClient.disconnect();
//    }
//
//    private void init() {
//        mSendButton = (Button) findViewById( R.id.btn_send );
//
//
//        mSendButton.setOnClickListener( new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.d("onClick========", "========");
//                    sendMessage(WEAR_MESSAGE_PATH, "HI");
//                }
//            }
//        );
//    }
//
//    private void sendMessage( final String path, final String text ) {
//        Log.d("sendMessage========", "========"+text);
//        new Thread( new Runnable() {
//            @Override
//            public void run() {
//                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
//
//                for(Node node : nodes.getNodes()) {
//
//                    result = Wearable.MessageApi.sendMessage(
//                            mApiClient, node.getId(), path, text.getBytes() ).await();
//                }
//            }
//        }).start();
//    }
//
//    @Override
//    public void onConnected(Bundle bundle) {
//        Log.d("onConnected====", "========");
//        sendMessage(START_ACTIVITY, "");
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//        Log.d("onConnectionSus====", "==");
//    }
//
//    @Override
//    public void onConnectionFailed(ConnectionResult connectionResult) {
//        Log.d("onConnectionFailed====", "========");
//    }
//}
//
/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The main activity for the Jumping Jack application. This activity registers itself to receive
 * sensor values. Since on wearable devices a full screen activity is very short-lived, we set the
 * FLAG_KEEP_SCREEN_ON to give user adequate time for taking actions but since we don't want to
 * keep screen on for an extended period of time, there is a SCREEN_ON_TIMEOUT_MS that is enforced
 * if no interaction is discovered.
 *
 * This activity includes a {@link android.support.v4.view.ViewPager} with two pages, one that
 * shows the current count and one that allows user to reset the counter. the current value of the
 * counter is persisted so that upon re-launch, the counter picks up from the last value. At any
 * stage, user can set this counter to 0.
 */
public class MainActivity extends Activity
        implements SensorEventListener,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {

    private static final String START_ACTIVITY = "/start_activity";
    public static final String WEAR_MESSAGE_PATH = "/message";

    private GoogleApiClient mApiClient;



    private Button mSendButton;
    private MessageApi.SendMessageResult result;


    private static final String TAG = "JJMainActivity";

    /** How long to keep the screen on when no activity is happening **/
    private static final long SCREEN_ON_TIMEOUT_MS = 20000; // in milliseconds

    /** an up-down movement that takes more than this will not be registered as such **/
    private static final long TIME_THRESHOLD_NS = 2000000000; // in nanoseconds (= 2sec)

    /**
     * Earth gravity is around 9.8 m/s^2 but user may not completely direct his/her hand vertical
     * during the exercise so we leave some room. Basically if the x-component of gravity, as
     * measured by the Gravity sensor, changes with a variation (delta) > GRAVITY_THRESHOLD,
     * we consider that a successful count.
     */
    private static final float GRAVITY_THRESHOLD = 7.0f;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private long mLastTime = 0;
    private boolean mUp = false;
    private int mJumpCounter = 0;
    private ViewPager mPager;
    //    private CounterFragment mCounterPage;
    private ConnectFragment mSettingPage;
    private LaunchFragment mLaunchPage;
    private ImageView mSecondIndicator;
    private ImageView mFirstIndicator;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jj_layout);
        setupViews();
        mHandler = new Handler();
        mJumpCounter = Utils.getCounterFromPreference(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        renewTimer();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        initGoogleApiClient();
    }

    private void setupViews() {
        mPager = (ViewPager) findViewById(R.id.pager);
        mFirstIndicator = (ImageView) findViewById(R.id.indicator_0);
//        mSecondIndicator = (ImageView) findViewById(R.id.indicator_1);
        final PagerAdapter adapter = new PagerAdapter(getFragmentManager());
//        mCounterPage = new CounterFragment();
        mLaunchPage = new LaunchFragment();
        mSettingPage = new ConnectFragment();
        adapter.addFragment(mSettingPage);
        adapter.addFragment(mLaunchPage);
        setIndicator(0);
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                setIndicator(i);
                renewTimer();
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        mPager.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Successfully registered for the sensor updates");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Unregistered for sensor events");
        }
    }

    float mLastX;
    float mLastY;
    float mLastZ;
    boolean mInitialized;
    float NOISE = 4.0f;

    @Override
    public void onSensorChanged(SensorEvent event) {

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        if (!mInitialized) {
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            mInitialized = true;
        } else {
            float deltaX = Math.abs(mLastX - x);
            float deltaY = Math.abs(mLastY - y);
            float deltaZ = Math.abs(mLastZ - z);
            if (deltaX < NOISE) deltaX = (float)0.0;
            if (deltaY < NOISE) deltaY = (float)0.0;
            if (deltaZ < NOISE) deltaZ = (float)0.0;
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            mFirstIndicator.setVisibility(View.VISIBLE);
            if (deltaX > deltaY) {
                mFirstIndicator.setImageResource(R.drawable.horizontal);
            } else if (deltaY > deltaX) {
                mFirstIndicator.setImageResource(R.drawable.vertical);
            } else {
                mFirstIndicator.setVisibility(View.INVISIBLE);
            }
        }
//        detectJump(event.values[0], event.timestamp);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    /**
     * A simple algorithm to detect a successful up-down movement of hand(s). The algorithm is
     * based on the assumption that when a person is wearing the watch, the x-component of gravity
     * as measured by the Gravity Sensor is +9.8 when the hand is downward and -9.8 when the hand
     * is upward (signs are reversed if the watch is worn on the right hand). Since the upward or
     * downward may not be completely accurate, we leave some room and instead of 9.8, we use
     * GRAVITY_THRESHOLD. We also consider the up <-> down movement successful if it takes less than
     * TIME_THRESHOLD_NS.
     */
    private void detectJump(float xValue, long timestamp) {
        if ((Math.abs(xValue) > GRAVITY_THRESHOLD)) {
            if(timestamp - mLastTime < TIME_THRESHOLD_NS && mUp != (xValue > 0)) {
                onJumpDetected(!mUp);
            }
            mUp = xValue > 0;
            mLastTime = timestamp;
        }
    }

    /**
     * Called on detection of a successful down -> up or up -> down movement of hand.
     */
    private void onJumpDetected(boolean up) {
        // we only count a pair of up and down as one successful movement
        if (up) {
            return;
        }
        mJumpCounter++;
        setCounter(mJumpCounter);
        renewTimer();
    }

    /**
     * Updates the counter on UI, saves it to preferences and vibrates the watch when counter
     * reaches a multiple of 10.
     */
    private void setCounter(int i) {
//        mCounterPage.setCounter(i);
        Utils.saveCounterToPreference(this, i);
        if (i > 0 && i % 10 == 0) {
            Utils.vibrate(this, 0);
        }
    }

    public void resetCounter() {
        setCounter(0);
        renewTimer();
    }

    /**
     * Starts a timer to clear the flag FLAG_KEEP_SCREEN_ON.
     */
    private void renewTimer() {
        if (null != mTimer) {
            mTimer.cancel();
        }
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG,
                            "Removing the FLAG_KEEP_SCREEN_ON flag to allow going to background");
                }
                resetFlag();
            }
        };
        mTimer = new Timer();
        mTimer.schedule(mTimerTask, SCREEN_ON_TIMEOUT_MS);
    }

    /**
     * Resets the FLAG_KEEP_SCREEN_ON flag so activity can go into background.
     */
    private void resetFlag() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Resetting FLAG_KEEP_SCREEN_ON flag to allow going to background");
                }
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                finish();
            }
        });
    }

    /**
     * Sets the page indicator for the ViewPager.
     */
    private void setIndicator(int i) {
        switch (i) {
            case 0:
//                mFirstIndicator.setImageResource(R.drawable.full_10);
//                mSecondIndicator.setImageResource(R.drawable.empty_10);
                break;
            case 1:
//                mFirstIndicator.setImageResource(R.drawable.empty_10);
//                mSecondIndicator.setImageResource(R.drawable.full_10);
                break;
        }
    }


    private void initGoogleApiClient() {
        Log.d("initGoogleApiClient====", "========");
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();
    }



        @Override
    public void onConnected(Bundle bundle) {
        Log.d("onConnected====", "========");
        sendMessage(START_ACTIVITY, "");
    }

    public void sendMessage( final String path, final String text ) {
        Log.d("sendMessage========", "========"+text);
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();

                for(Node node : nodes.getNodes()) {

                    result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("onConnectionSus====", "==");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("onConnectionFailed====", "========");
    }
}

