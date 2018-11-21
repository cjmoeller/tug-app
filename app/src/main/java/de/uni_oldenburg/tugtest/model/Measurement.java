package de.uni_oldenburg.tugtest.model;

import java.nio.ByteBuffer;

/**
 * Measurement object, intended to be serialized and sent to rmq
 */
public class Measurement {

    private MeasurementType type;
    private float[] values;
    private long timeOfMeasurement;

    public MeasurementType getType() {
        return type;
    }

    public void setType(MeasurementType type) {
        this.type = type;
    }

    public float[] getValues() {
        return values;
    }

    public void setValues(float[] values) {
        this.values = values;
    }


    public Measurement(MeasurementType type, float[] values) {
        this.type = type;
        this.values = values;
        this.timeOfMeasurement = System.currentTimeMillis();
    }

    /**
     * Serialized Message. Format:
     *
     * | 1 byte: Type | 8 byte time in ms | 12 (3x4) byte measurement values (3D) |
     *
     * @return
     */
    public byte[] toByteArray() {
        byte[] result = new byte[21];
        byte type = (byte) this.type.ordinal();

        result[0] = type;

        byte[] time = ByteBuffer.allocate(8).putLong(this.timeOfMeasurement).array();
        byte[] val1 = ByteBuffer.allocate(4).putFloat(this.values[0]).array();
        byte[] val2 = ByteBuffer.allocate(4).putFloat(this.values[1]).array();
        byte[] val3 = ByteBuffer.allocate(4).putFloat(this.values[2]).array();

        System.arraycopy(time, 0, result, 1, 8);
        System.arraycopy(val1, 0, result, 9, 4);
        System.arraycopy(val2, 0, result, 13, 4);
        System.arraycopy(val3, 0, result, 17, 4);


        return result;
    }
}
