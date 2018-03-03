package org.ignas.frauddetection.transactionevaluation.domain.criteria;

import com.google.common.collect.Range;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.PrintableResult;

import static com.google.common.collect.Range.*;

public enum BooleanCriteria implements PrintableResult {

    TRUE {
        @Override
        public String representation() {
            return this.name();
        }
    },

    FALSE {
        @Override
        public String representation() {
            return name();
        }
    }
}

