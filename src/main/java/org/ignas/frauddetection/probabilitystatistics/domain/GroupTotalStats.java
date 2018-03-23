package org.ignas.frauddetection.probabilitystatistics.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupTotalStats {

    private float averageProbability;
    private float deviationProbability;

    private long sumOfOccurencesInFraud;

    private long sumOfSquaredFraudOccurences;

    public GroupTotalStats(
        float averageProbability,
        float deviationProbability,
        long sumOfOccurancesInFraud,
        long sumOfSquaredFraudOccurences) {

        this.averageProbability = averageProbability;
        this.deviationProbability = deviationProbability;
        this.sumOfOccurencesInFraud = sumOfOccurancesInFraud;
        this.sumOfSquaredFraudOccurences = sumOfSquaredFraudOccurences;
    }

    /**
     * Algorithm for Incremental Average probability amongs all possible combinations calculation was invented.
     *
     * sumOfOccurencesInFraud - precalculated sum of occurances in all possible combinations.
     *
     * The idea is that averageProbability = sumOfOccurencesInFraud/ totalFraudOccurences /numberOfCombinations
     *
     * Here: sumOfOccurencesInFraud/totalFraudOccurences in reality is sum of combination probabilities.
     *
     * @param additionalOccurrences
     * @param totalFraudTransactions
     * @param numberOfPossibleCombinations
     */
    public void updateStatistics(
        long additionalOccurrences,
        long totalFraudTransactions,
        int numberOfPossibleCombinations,
        List<CombinationStatistics> increments,
        List<CombinationStatistics> beforeUpdateValues) {

        if (totalFraudTransactions == 0) {
            System.out.println("There is no fraud occurences yet, will not update fraud stats!");
            return;
        }

//        Map is not really needed for functional purposes, however search by code in hashmap will be faster than iterating over the list.
        Map<String, CombinationUpdate> updates = new HashMap<>();

        for (CombinationStatistics before : beforeUpdateValues) {
            CombinationUpdate update = new CombinationUpdate(before.getGroup(), before.getCode());
            update.setBeforeState(before.getOccurences(), before.getFraudOccurences());

            updates.put(before.getCode(), update);
        }

        for (CombinationStatistics increment : increments) {
            CombinationUpdate update = updates.get(increment.getCode());

            update.addIncrements(increment.getOccurences(), increment.getFraudOccurences());
        }

        long squareDifference = 0;

        for (CombinationUpdate update : updates.values()) {
            long delta = update.getFraudOccurrencesDelta();
            long before = update.getFraudOccurrencesBeforeUpdate();

            // Trying to achieve incremental update of square sum: (before)^2 + squareDifference = (before+delta)^2
            // Idea based on that (before + delta) ^ 2 = (before ^ 2 + 2 * delta * before + delta ^ 2)
            squareDifference += delta * delta + 2 * delta * before;
        }

        sumOfOccurencesInFraud += additionalOccurrences;
        averageProbability = ((float) sumOfOccurencesInFraud) /(totalFraudTransactions * numberOfPossibleCombinations);

        float firstComposite = averageProbability * averageProbability * numberOfPossibleCombinations;
        float secondComposite = ((float) (sumOfSquaredFraudOccurences + squareDifference)) / (totalFraudTransactions * totalFraudTransactions);
        float thirdComposite = 2 * averageProbability * sumOfOccurencesInFraud / totalFraudTransactions;

        deviationProbability = (float) Math.sqrt((firstComposite + secondComposite + thirdComposite) / (numberOfPossibleCombinations - 1));
    }

    public float getAverageProbability() {
        return averageProbability;
    }

    public float getDeviationProbability() {
        return deviationProbability;
    }

    public long getSumOfOccurencesInFraud() {
        return sumOfOccurencesInFraud;
    }

    public long getSumOfSquaredFraudOccurences() {
        return sumOfSquaredFraudOccurences;
    }
}
