package org.ignas.frauddetection.httpapi;

public class APISchemas {

    private String evaluationSchema;
    private String markingSchema;

    public APISchemas(String evaluationSchema, String markingSchema) {
        this.evaluationSchema = evaluationSchema;
        this.markingSchema = markingSchema;
    }

    public String getEvaluationSchema() {
        return evaluationSchema;
    }

    public String getMarkingSchema() {
        return markingSchema;
    }
}
