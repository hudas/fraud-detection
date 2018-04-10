package org.ignas.frauddetection.probabilitystatistics.domain;

import org.ignas.frauddetection.transactionevaluation.api.request.TransactionData;
import org.joda.time.LocalDateTime;

import java.util.List;
import java.util.stream.Collectors;

public class PersonalPeriodStats {

    private int length;

    private List<PersonalPeriodTransaction> transactions;

    private int count = 0;
    private float sum = 0f;

    public PersonalPeriodStats(int length) {
        this.length = length;
    }

    public PersonalPeriodStats(int length, int count, float sum, List<PersonalPeriodTransaction> transactions) {
        this.length = length;
        this.transactions = transactions;
        this.count = count;
        this.sum = sum;
    }

    public void add(TransactionData requestTransaction) {
        this.transactions.add(
            new PersonalPeriodTransaction(requestTransaction.getTime(), requestTransaction.getAmount())
        );

        sum += requestTransaction.getAmount();
        count++;
    }

    public int getLength() {
        return length;
    }

    public List<PersonalPeriodTransaction> getTransactions() {
        return transactions;
    }

    public int getCount() {
        return count;
    }

    public float getSum() {
        return sum;
    }

    public void updateFrom(PersonalPeriodStats increment) {
        transactions.addAll(increment.getTransactions());
        count += increment.getCount();
        sum += increment.getSum();
    }

    public void removeOutdated(LocalDateTime at) {
        LocalDateTime minValidTime = at.minusDays(getLength());

        List<PersonalPeriodTransaction> outdatedTransactions = transactions.stream()
            .filter(it -> it.getTime().isBefore(minValidTime))
            .collect(Collectors.toList());

        for(PersonalPeriodTransaction outdated: outdatedTransactions) {
            transactions.remove(outdated);
            sum -= outdated.getAmount();
            count--;
        }
    }
}
