package de.uni_oldenburg.tugtest;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import androidx.annotation.NonNull;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private TextView mTextViewHeart;
    private ImageView imageView;
    private FrameLayout rootPane;
    private SensorManager sensorManager;
    private DataClient dataClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("test", "Started MAIN");
        mTextViewHeart = findViewById(R.id.text);
        imageView = findViewById(R.id.imageView);
        rootPane = findViewById(R.id.rootPane);

        rootPane.setOnLongClickListener(v -> {
            this.finish();
            return false;
        });

        sensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        Sensor mHeartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d("test", "Started Listening");


        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                imageView,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f));
        scaleDown.setDuration(310);

        scaleDown.setRepeatCount(ObjectAnimator.INFINITE);
        scaleDown.setRepeatMode(ObjectAnimator.REVERSE);

        scaleDown.start();

        dataClient = Wearable.getDataClient(this);

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("test", "EVENT RECEIVED");
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            String msg = "" + (int) event.values[0];
            mTextViewHeart.setText(msg);

            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/heartbeat");
            putDataMapReq.getDataMap().putInt("0", (int) event.values[0]);
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            Task<DataItem> putDataTask = dataClient.putDataItem(putDataReq);

            putDataTask.addOnFailureListener(e -> Log.e("def", e.getLocalizedMessage()));
            putDataTask.addOnSuccessListener(dataItem -> Log.i("def", "Successfully sent data item"));

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onDestroy() {
        this.sensorManager.unregisterListener(this);
        super.onDestroy();
    }
}
