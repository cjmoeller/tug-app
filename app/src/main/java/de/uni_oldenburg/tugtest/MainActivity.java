package de.uni_oldenburg.tugtest;

import androidx.appcompat.app.AppCompatActivity;
import de.uni_oldenburg.tugtest.model.Measurement;
import de.uni_oldenburg.tugtest.model.MeasurementType;
import de.uni_oldenburg.tugtest.model.RabbitMQManager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor gravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        final TextView main = findViewById(R.id.textView);

        new Thread(() -> this.connect()).start();


        /**
         *
         * SENSOR LISTENERS
         * 
         */
        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                main.setText(Arrays.toString(event.values));
                Measurement measurement = new Measurement(MeasurementType.ACCELERATION, event.values);
                RabbitMQManager.getInstance().queueMeasurement(measurement);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, accSensor, SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                main.setText(Arrays.toString(event.values));
                Measurement measurement = new Measurement(MeasurementType.GRAVITY, event.values);
                RabbitMQManager.getInstance().queueMeasurement(measurement);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                main.setText(Arrays.toString(event.values));
                Measurement measurement = new Measurement(MeasurementType.ROTATION, event.values);
                RabbitMQManager.getInstance().queueMeasurement(measurement);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private Channel channel;


    public void connect() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(Constants.RABBIT_MQ_USER);
        factory.setPassword(Constants.RABBIT_MQ_USER);
        factory.setHost(Constants.RABBIT_MQ_HOST);
        factory.setPort(5672);
        try {

            Connection conn = factory.newConnection();
            channel = conn.createChannel();
            channel.exchangeDeclare(Constants.RABBIT_MQ_EXCHANGE, "direct", true);
            channel.queueDeclare(Constants.RABBIT_MQ_QUEUE_NAME, true, false, true, null);
            channel.queueBind(Constants.RABBIT_MQ_QUEUE_NAME, Constants.RABBIT_MQ_EXCHANGE, Constants.RABBIT_MQ_ROUTING_KEY);
            RabbitMQManager.getInstance().setRmqChannel(channel);
            RabbitMQManager.getInstance().startStream();

        } catch (IOException e) {
            Log.e("Log", e.getLocalizedMessage());
        } catch (TimeoutException e) {
            Log.e("Log", e.getLocalizedMessage());
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        RabbitMQManager.getInstance().stopStream();
    }

}
