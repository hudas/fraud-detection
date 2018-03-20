package org.ignas.frauddetection.transactionevaluation.domain.criteria;

import com.google.common.collect.Range;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.PrintableResult;

import static com.google.common.collect.Range.*;

public enum AmountCategoryCriteria implements PrintableResult {

    VERY_SMALL_AMOUNT(atMost(25f)) {
        @Override
        public String representation() {
            return this.name();
        }
    },

    SMALL_AMOUNT(openClosed(25f, 100f)) {
        @Override
        public String representation() {
            return name();
        }
    },

    BIG_AMOUNT(openClosed(100f, 500f)) {
        @Override
        public String representation() {
            return name();
        }
    },

    VERY_BIG_AMOUNT(greaterThan(500f)) {
        @Override
        public String representation() {
            return name();
        }
    };


    private Range<Float> range;

    AmountCategoryCriteria(Range<Float> range) {
        this.range = range;
    }

    public boolean abstracts(Float deviationRate) {
        return this.range.contains(deviationRate);
    }
}

