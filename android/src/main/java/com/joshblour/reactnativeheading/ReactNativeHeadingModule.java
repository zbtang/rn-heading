package com.joshblour.reactnativeheading;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;


public class ReactNativeHeadingModule extends ReactContextBaseJavaModule implements SensorEventListener {

    private Context mApplicationContext;
    private int mAzimuth = 0; // degree
    private int newAzimuth = 0; // degree
    private float mFilter = 5;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float[] orientation = new float[3];
    private float[] rMat = new float[9];

    public ReactNativeHeadingModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mApplicationContext = reactContext.getApplicationContext();
    }

    @Override
    public String getName() {
        return "ReactNativeHeading";
    }


    @ReactMethod
    public void start(int filter, Promise promise) {

        if (mSensorManager == null) {
            mSensorManager = (SensorManager) mApplicationContext.getSystemService(Context.SENSOR_SERVICE);
        }

        if (mSensor == null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        mFilter = filter;
        boolean started = mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
        promise.resolve(started);
    }

    @ReactMethod
    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;

        // calculate th rotation matrix
        SensorManager.getRotationMatrixFromVector(rMat, event.values);

        // get the azimuth value (orientation[0]) in degree
        newAzimuth = (int) (((((Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360) -
                (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[2]))) + 360) % 360);

        //dont react to changes smaller than the filter value
        if (Math.abs(mAzimuth - newAzimuth) < mFilter) {
            return;
        }

        WritableMap heading = Arguments.createMap();
        heading.putInt("heading", newAzimuth);
        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("headingUpdated", heading);

        mAzimuth = newAzimuth;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
