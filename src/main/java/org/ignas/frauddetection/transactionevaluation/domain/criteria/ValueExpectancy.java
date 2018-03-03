package org.ignas.frauddetection.transactionevaluation.domain.criteria;

import com.google.common.collect.Range;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.PrintableResult;

import static com.google.common.collect.Range.*;
import static com.google.common.collect.Range.greaterThan;

public enum ValueExpectancy implements PrintableResult {

    MUCH_LESS_THAN_EXPECTED(lessThan(-2f)) {
        @Override
        public String representation() {
            return this.name();
        }
    },

    LESS_THAN_EXPECTED(closedOpen(-2f, -1f)) {
        @Override
        public String representation() {
            return name();
        }
    },

    EXPECTED(closed(-1f, 1f)) {
        @Override
        public String representation() {
            return name();
        }
    },

    MORE_THAN_EXPECTED(openClosed(1f, 2f)) {
        @Override
        public String representation() {
            return name();
        }
    },

    MUCH_MORE_THAN_EXPECTED(greaterThan(2f)) {
        @Override
        public String representation() {
            return name();
        }
    };


    private Range<Float> range;

    ValueExpectancy(Range<Float> range) {
        this.range = range;
    }

    public boolean abstracts(Float deviationRate) {
        return this.range.contains(deviationRate);
    }

}

