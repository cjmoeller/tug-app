package de.uni_oldenburg.tugtest;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import de.uni_oldenburg.tugtest.model.Measurement;
import de.uni_oldenburg.tugtest.model.MeasurementType;
import de.uni_oldenburg.tugtest.model.RabbitMQManager;

public class DataLayerListenerService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        // Loop through the events and send a message
        // to the node that created the data item.
        for (DataEvent event : dataEvents) {
            DataItem item = event.getDataItem();
            if (item.getUri().getPath().compareTo("/heartbeat") == 0) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                Integer heartbeat = dataMap.getInt("0");
                float[] data = {heartbeat, 0, 0};
                RabbitMQManager.getInstance().queueMeasurement(new Measurement(MeasurementType.HEARTBEAT, data)); //TODO: check if connected
                Log.i("def", "Pushed Heartbeat to rmq: "+ heartbeat + " bpm");
            }

        }
    }
}
