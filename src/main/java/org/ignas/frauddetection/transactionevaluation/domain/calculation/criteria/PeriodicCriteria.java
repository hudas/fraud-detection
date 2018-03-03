package org.ignas.frauddetection.transactionevaluation.domain.calculation.criteria;

import com.google.common.collect.Lists;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.Evaluator;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.MappableToOperation;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.MappableToPeriodicOperation;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.PrintableResult;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.joda.time.Days;

import java.util.List;
import java.util.stream.Collectors;


public class PeriodicCriteria<O> extends NamedCriteria<O> {

    public static final String PERIOD_SEPARATOR = "/";

    private final Days period;

    private PeriodicCriteria(String name, String group, Days period, Evaluator<O> calculator, MappableToPeriodicOperation<O> mapper) {
        super(name, group, calculator, ((transaction, statistics) -> mapper.transform(period, transaction, statistics)));
        this.period = period;
    }

    @Override
    public String name() {
        return name + PERIOD_SEPARATOR + period;
    }


    // TODO: Could be refactored into common factory for all criteria types
    public static class Builder<O> {

        private String name;
        private String group;

        private Evaluator<O> evaluator;
        private List<Days> forPeriods;
        private MappableToPeriodicOperation<O> mapper;

        public Builder(String name, String group) {
            this.name = name;
            this.group = group;
        }

        public Builder<O> calculator(Evaluator<O> calculator) {
            this.evaluator = calculator;
            return this;
        }

        public Builder<O> mapper(MappableToPeriodicOperation<O> mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder<O> period(Days... periods) {
            this.forPeriods = Lists.newArrayList(periods);
            return this;
        }

        public List<PeriodicCriteria<O>> build() {
            return forPeriods.stream()
                    .map(it -> new PeriodicCriteria<O>(name, group, it, evaluator, mapper))
                    .collect(Collectors.toList());
        }
    }
}
