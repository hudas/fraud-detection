package org.ignas.frauddetection.httpapi;

import com.fasterxml.jackson.datatype.joda.JodaModule;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RoutingContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvaluationRequestHandlerTest {

    @Test
    void canHandleInternalErrors() {
        EvaluationRequestHandler handler = new EvaluationRequestHandler(
            testIntegration -> Future.failedFuture("Some error code"),
            logIntegration -> Future.succeededFuture());

        RoutingContext context = Mockito.mock(RoutingContext.class);
        JsonObject emptyRequest = new JsonObject();

        when(context.getBodyAsJson()).thenReturn(emptyRequest);

        handler.handle(context);

        verify(context, times(1)).fail(500);
    }

    @Test
    void canHandleExceptions() {
        EvaluationRequestHandler handler = new EvaluationRequestHandler(
            testIntegration -> Future.failedFuture(new IllegalArgumentException("Some error code")),
            logIntegration -> Future.succeededFuture()
        );

        RoutingContext context = Mockito.mock(RoutingContext.class);
        JsonObject emptyRequest = new JsonObject();

        when(context.getBodyAsJson()).thenReturn(emptyRequest);

        handler.handle(context);

        verify(context, times(1)).fail(500);
    }

    @Test
    void returnsResultAsJson() {
        Json.mapper.registerModule(new JodaModule());

        String testJson = "{\n" +
            "    \"transactionId\" : \"123\", " +
            "    \"debtorCreditCardId\" : \"1234\", " +
            "    \"debtorAccountId\": \"1235\", " +
            "    \"creditorAccountId\":\"1236\", " +
            "    \"amount\": 123.75, " +
            "    \"time\": \"2018-02-24T14:51:36.1234+03:00\", " +
            "    \"location\": { " +
            "        \"longtitude\" : \"54.312312\", " +
            "        \"latitude\" : \"25.312315\" " +
            "    } " +
            "}";
        EvaluationRequestHandler handler = new EvaluationRequestHandler(
            testIntegration -> Future.succeededFuture(1f),
            logIntegration -> Future.succeededFuture()
        );

        RoutingContext fakeContext = Mockito.mock(RoutingContext.class);
        HttpServerResponse fakeResponse = Mockito.mock(HttpServerResponse.class);

        JsonObject emptyRequest = new JsonObject(testJson);

        when(fakeContext.getBodyAsJson()).thenReturn(emptyRequest);
        when(fakeResponse.putHeader(anyString(), anyString())).thenReturn(fakeResponse);
        when(fakeContext.response()).thenReturn(fakeResponse);

        handler.handle(fakeContext);

        verify(fakeResponse, times(1)).end("{ \"fraud-probability\" : \"1.000000\" }");
    }
}
