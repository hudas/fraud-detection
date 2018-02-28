package org.ignas.frauddetection.transactionevaluation.domain.criteria;

import com.google.common.collect.Range;

import static com.google.common.collect.Range.*;
import static com.google.common.collect.Range.greaterThan;

public enum ValueExpectancy {

    MUCH_LESS_THAN_EXPECTED(lessThan(-2f)),

    LESS_THAN_EXPECTED(closedOpen(-2f, -1f)),

    EXPECTED(closed(-1f, 1f)),

    MORE_THAN_EXPECTED(openClosed(1f, 2f)),

    MUCH_MORE_THAN_EXPECTED(greaterThan(2f));


    private Range<Float> range;

    ValueExpectancy(Range<Float> range) {
        this.range = range;
    }

    public boolean abstracts(Float deviationRate) {
        return this.range.contains(deviationRate);
    }

}

