package org.ignas.frauddetection.transactionstatistics.domain;

public class SquaredIncrement {

    private float sumIncrement;
    private float quantityIncrement;

    public SquaredIncrement(float sumIncrement, float quantityIncrement) {
        this.sumIncrement = sumIncrement;
        this.quantityIncrement = quantityIncrement;
    }

    public float getSumIncrement() {
        return sumIncrement;
    }

    public float getQuantityIncrement() {
        return quantityIncrement;
    }
}
