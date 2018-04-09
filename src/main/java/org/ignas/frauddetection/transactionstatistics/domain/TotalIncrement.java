package org.ignas.frauddetection.transactionstatistics.domain;

public class TotalIncrement {

    private float valueIncrease;
    private float valueIncreaseSquared;

    public TotalIncrement(float valueIncrease, float valueIncreaseSquared) {
        this.valueIncrease = valueIncrease;
        this.valueIncreaseSquared = valueIncreaseSquared;
    }

    public float getValueIncrease() {
        return valueIncrease;
    }

    public float getValueIncreaseSquared() {
        return valueIncreaseSquared;
    }
}
