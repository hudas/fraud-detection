package org.ignas.frauddetection.api.evaluation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EvaluationRequestDTO {

    private BigDecimal transactionAmount;
    private LocalDateTime transactionTime;
}
