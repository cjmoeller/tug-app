package de.uni_oldenburg.tugtest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import de.uni_oldenburg.tugtest.model.Measurement;
import de.uni_oldenburg.tugtest.model.MeasurementType;
import de.uni_oldenburg.tugtest.model.RabbitMQManager;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private TextView main;
    private TextView state;
    private Button start;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private float[] mGravityReading = new float[3];
    private float[] mGyroReading = new float[3];
    private float[] lastGyro = new float[3];
    private float stepCount = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor linearAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor magneticFieldSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor stepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        main = findViewById(R.id.textView);
        state = findViewById(R.id.textView2);
        start = findViewById(R.id.button);

        start.setOnClickListener(v -> new CountDownTimer(10000, 1000) {

            public void onTick(long millisUntilFinished) {
                state.setText(String.valueOf(millisUntilFinished / 1000));
            }

            public void onFinish() {
                state.setText("test running");
                float[] m = {0, 0, 0};
                Measurement start = new Measurement(MeasurementType.MISC_START_MEASUREMENT, m);
                RabbitMQManager.getInstance().queueMeasurement(start);
                // Get instance of Vibrator from current Context
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                // Vibrate for 400 milliseconds
                v.vibrate(1000);
            }
        }.start());

        new Thread(() -> this.connect()).start();


        //Sensor listeners
        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_UI);

        mSensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_UI);

        mSensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI);

        mSensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_UI);

        mSensorManager.registerListener(this, linearAcc, SensorManager.SENSOR_DELAY_UI);

        mSensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_UI);
    }

    private Channel channel;


    public void connect() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String host = prefs.getString("rmq_host", "127.0.0.1");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(Constants.RABBIT_MQ_USER);
        factory.setPassword(Constants.RABBIT_MQ_PASSWORD);
        factory.setHost(host);
        factory.setPort(5672);
        try {

            Connection conn = factory.newConnection();
            channel = conn.createChannel();
            channel.exchangeDeclare(Constants.RABBIT_MQ_EXCHANGE, "direct", true);
            channel.queueDeclare(Constants.RABBIT_MQ_QUEUE_NAME, true, false, false, null);
            channel.queueBind(Constants.RABBIT_MQ_QUEUE_NAME, Constants.RABBIT_MQ_EXCHANGE, Constants.RABBIT_MQ_ROUTING_KEY);
            RabbitMQManager.getInstance().setRmqChannel(channel);
            RabbitMQManager.getInstance().startStream();

        } catch (IOException e) {
            e.printStackTrace();
            //Log.e("Log", e.getLocalizedMessage());
        } catch (TimeoutException e) {
            Log.e("Log", e.getLocalizedMessage());
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        RabbitMQManager.getInstance().stopStream();
    }

    private float[] lastAcc = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            main.setText(Arrays.toString(event.values));
            System.arraycopy(event.values, 0, mGravityReading,
                    0, mGravityReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            main.setText(Arrays.toString(event.values));
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            main.setText(Arrays.toString(event.values));
            System.arraycopy(event.values, 0, mGyroReading,
                    0, mGyroReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (stepCount == 0)
                stepCount = event.values[0];
            float tugSteps = event.values[0] - stepCount;
            float[] measSteps = {tugSteps, 0, 0};
            Measurement measurement = new Measurement(MeasurementType.STEPS, measSteps);
            RabbitMQManager.getInstance().queueMeasurement(measurement);
        }
        if (this.mAccelerometerReading != null && this.mMagnetometerReading != null) {
            float[] R = new float[16], I = new float[16], earthAcc = new float[16], earthRot = new float[16];
            SensorManager.getRotationMatrix(R, I,
                    mGravityReading, mMagnetometerReading);
            float[] inv = new float[16];
            float[] deviceRelativeAcc = {mAccelerometerReading[0], mAccelerometerReading[1], mAccelerometerReading[2], 0};

            android.opengl.Matrix.invertM(inv, 0, R, 0);
            android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcc, 0);
            float[] resultAcc = {earthAcc[0], earthAcc[1], earthAcc[2] - 9.81f}; //correct gravity

            float[] deviceGyro = {mGyroReading[0], mGyroReading[1], mGyroReading[2], 0};
            android.opengl.Matrix.multiplyMV(earthRot, 0, inv, 0, deviceGyro, 0);
            float[] resultGyro = {earthRot[0], earthRot[1], earthRot[2]};

            if (!Arrays.equals(this.lastAcc, resultAcc)) {
                RabbitMQManager.getInstance().queueMeasurement(new Measurement(MeasurementType.EARTH_FRAME_ALIGNED_ACC, resultAcc));
                this.lastAcc = resultAcc;

            }
            if (!Arrays.equals(this.lastGyro, resultGyro)) {
                RabbitMQManager.getInstance().queueMeasurement(new Measurement(MeasurementType.EARTH_FRAME_ALIGNED_GYRO, resultGyro));
                this.lastGyro = resultGyro;

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
