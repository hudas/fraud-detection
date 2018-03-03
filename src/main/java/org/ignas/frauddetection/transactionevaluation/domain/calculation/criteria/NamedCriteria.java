package org.ignas.frauddetection.transactionevaluation.domain.calculation.criteria;

import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.Evaluator;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.MappableToOperation;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.PrintableResult;
import org.ignas.frauddetection.transactionevaluation.domain.stats.DebtorStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.EnvironmentStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.GlobalStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;

public class NamedCriteria<T> implements Criteria {

    String name;
    private String group;

    private Evaluator<T> calculator;
    private MappableToOperation<T> mapper;

    NamedCriteria(String name, String group, Evaluator<T> calculator, MappableToOperation<T> mapper) {
        this.group = group;
        this.name = name;
        this.calculator = calculator;
        this.mapper = mapper;
    }

    public String name() {
        return name;
    }

    public String group() {
        return group;
    }

    public PrintableResult evaluate(Transaction transaction, HistoricalData stats) {
        return calculator.evaluate(mapper.transform(transaction, stats));
    }

    // TODO: Could be refactored into common factory
    public static class Builder<O> {

        private String name;
        private String group;

        private Evaluator<O> evaluator;
        private MappableToOperation<O> mapper;

        public Builder(String name, String group) {
            this.name = name;
            this.group = group;
        }

        public Builder<O> calculator(Evaluator<O> calculator) {
            this.evaluator = calculator;
            return this;
        }

        public Builder<O> mapper(MappableToOperation<O> mapper) {
            this.mapper = mapper;
            return this;
        }

        public NamedCriteria<O> build() {
            return new NamedCriteria<O>(name, group, evaluator, mapper);
        }
    }
}
