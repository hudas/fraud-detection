package org.ignas.frauddetection.httpapi.request;

import org.ignas.frauddetection.shared.Location;
import org.joda.time.DateTime;

import java.math.BigDecimal;

public class MarkRequest {

    /**
     * Any unique ID for transaction. Uniqueness must be ensured outside system scope.
     * Id is required only to ensure that transaction is not used twice by learning process
     */
    private String transactionId;

    public MarkRequest() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

}
