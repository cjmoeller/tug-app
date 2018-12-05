package de.uni_oldenburg.tugtest;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private TextView mTextViewHeart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("test", "Started MAIN");
        mTextViewHeart = (TextView) findViewById(R.id.text);
        SensorManager mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        Sensor mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d("test", "Started Listening");

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("test", "EVENT RECEIVED");
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            String msg = "" + (int) event.values[0];
            mTextViewHeart.setText(msg);


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
