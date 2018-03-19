package org.ignas.frauddetection.transactionevaluation.integration.converters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaGroupProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.transactionevaluation.domain.CriteriaGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.jupiter.api.Assertions.*;

class GroupProbabilityAPIMapperTest {

    @Test
    void mapNullRequest() {
        CriteriaGroupProbabilityRequest request = GroupProbabilityAPIMapper.mapRequest(null);

        assertNotNull(request.getGroups());
        assertTrue(request.getGroups().isEmpty());
    }

    @Test
    void mapEmptyRequest() {
        CriteriaGroupProbabilityRequest request = GroupProbabilityAPIMapper.mapRequest(ImmutableList.of());

        assertNotNull(request.getGroups());
        assertTrue(request.getGroups().isEmpty());
    }

    @Test
    void mapRequest() {
        CriteriaGroupProbabilityRequest request = GroupProbabilityAPIMapper.mapRequest(ImmutableList.of("SUM", "COUNT"));

        assertNotNull(request.getGroups());
        assertTrue(request.getGroups().equals(ImmutableList.of("SUM", "COUNT")));
    }

    @Test
    void mapNullResponse() {
        Map<String, CriteriaGroup> table = GroupProbabilityAPIMapper.mapResponse(null);

        assertNotNull(table);
        assertTrue(table.isEmpty());
    }

    @Test
    void mapEmptyResponse() {
        Map<String, CriteriaGroup> table = GroupProbabilityAPIMapper.mapResponse(new BayesTable(null));

        assertNotNull(table);
        assertTrue(table.isEmpty());
    }

    @Test
    void mapEmptyTableResponse() {
        Map<String, CriteriaGroup> table = GroupProbabilityAPIMapper.mapResponse(new BayesTable(of()));

        assertNotNull(table);
        assertTrue(table.isEmpty());
    }

    @Test
    void mapResponse() {
        BayesTable dtoTable = new BayesTable(of(
            "SUM", of("LESS_THAN_EXPECTED", 0.1f, "EXPECTED", 0.2f, "MORE_THAN_EXPECTED", 0.3f),
            "COUNT", of("LESS_THAN_EXPECTED", 0.4f, "EXPECTED", 0.5f, "MORE_THAN_EXPECTED", 0.6f))
        );

        Map<String, CriteriaGroup> table = GroupProbabilityAPIMapper.mapResponse(dtoTable);

        assertEquals(0.1f, table.get("SUM").eventProbability("LESS_THAN_EXPECTED").floatValue());
        assertEquals(0.2f, table.get("SUM").eventProbability("EXPECTED").floatValue());
        assertEquals(0.3f, table.get("SUM").eventProbability("MORE_THAN_EXPECTED").floatValue());

        assertEquals(0.4f, table.get("COUNT").eventProbability("LESS_THAN_EXPECTED").floatValue());
        assertEquals(0.5f, table.get("COUNT").eventProbability("EXPECTED").floatValue());
        assertEquals(0.6f, table.get("COUNT").eventProbability("MORE_THAN_EXPECTED").floatValue());
    }
}
