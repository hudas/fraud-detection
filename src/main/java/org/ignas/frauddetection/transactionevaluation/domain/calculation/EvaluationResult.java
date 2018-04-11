package org.ignas.frauddetection.transactionevaluation.domain.calculation;

public class EvaluationResult {

    private PrintableResult result;
    private Float rawResult;

    public EvaluationResult(PrintableResult result, float rawResult) {
        this.result = result;
        this.rawResult = rawResult;
    }

    public PrintableResult getResult() {
        return result;
    }

    public float getRawResult() {
        return rawResult;
    }
}
