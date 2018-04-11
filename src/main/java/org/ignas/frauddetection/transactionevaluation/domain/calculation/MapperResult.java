package org.ignas.frauddetection.transactionevaluation.domain.calculation;

public class MapperResult<T> {

    private T operationInput;
    private Float behaviourData;

    public MapperResult(T operationInput, Float behaviourData) {
        this.operationInput = operationInput;
        this.behaviourData = behaviourData;
    }

    public static <T> MapperResult withoutBehaviour(T operation) {
        return new MapperResult<T>(operation, null);
    }
    public T getOperationInput() {
        return operationInput;
    }

    public Float getBehaviourData() {
        return behaviourData;
    }
}
