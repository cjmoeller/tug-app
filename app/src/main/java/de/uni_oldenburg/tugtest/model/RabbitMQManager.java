package de.uni_oldenburg.tugtest.model;

import android.util.Log;

import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import de.uni_oldenburg.tugtest.Constants;

public class RabbitMQManager {
    private static RabbitMQManager instance;
    private boolean running;

    public Channel getRmqChannel() {
        return rmqChannel;
    }

    public void setRmqChannel(Channel rmqChannel) {
        this.rmqChannel = rmqChannel;
    }

    private Channel rmqChannel;

    public static synchronized RabbitMQManager getInstance() {
        if (instance == null)
            instance = new RabbitMQManager();
        return instance;
    }

    private RabbitMQManager() {
        queue = new LinkedBlockingQueue<>();
        running = false;
    }

    public void queueMeasurement(Measurement measurement) {
        this.queue.add(measurement);
        running = true;
    }

    private LinkedBlockingQueue<Measurement> queue;

    public void startStream() {
        new Thread(() -> {
            while (this.running) {

                try {
                    if (this.rmqChannel != null) {
                        Measurement nextMeasurement = queue.take();
                        rmqChannel.basicPublish(Constants.RABBIT_MQ_EXCHANGE, Constants.RABBIT_MQ_ROUTING_KEY, null, nextMeasurement.toByteArray());
                    } else {
                        Log.w("Log", "No rmq channel was set.");
                    }
                } catch (InterruptedException e) {
                    Log.e("Log", e.getLocalizedMessage());
                } catch (IOException e) {
                    Log.e("Log", e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void stopStream() {
        this.running = false;
    }

}
