package org.ignas.frauddetection.transactionstatistics.domain;

public class ConditionTotalIncrement {

    private long newInstances;
    private float valueIncrease;
    private float valueIncreaseSquared;

    public ConditionTotalIncrement(long newInstances, float valueIncrease, float valueIncreaseSquared) {
        this.newInstances = newInstances;
        this.valueIncrease = valueIncrease;
        this.valueIncreaseSquared = valueIncreaseSquared;
    }

    public long getNewInstances() {
        return newInstances;
    }

    public float getValueIncrease() {
        return valueIncrease;
    }

    public float getValueIncreaseSquared() {
        return valueIncreaseSquared;
    }
}
