package net.unicon.lti.model.ags;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgsModelTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    public void testLineItemAccessors() {
        LineItem lineItem = new LineItem();
        Results results = new Results();
        lineItem.setId("id");
        lineItem.setScoreMaximum("100");
        lineItem.setLabel("label");
        lineItem.setResourceId("res");
        lineItem.setTag("tag");
        lineItem.setResourceLinkId("rl");
        lineItem.setStartDateTime("start");
        lineItem.setEndDateTime("end");
        lineItem.setResults(results);

        assertEquals("id", lineItem.getId());
        assertEquals("100", lineItem.getScoreMaximum());
        assertEquals("label", lineItem.getLabel());
        assertEquals("res", lineItem.getResourceId());
        assertEquals("tag", lineItem.getTag());
        assertEquals("rl", lineItem.getResourceLinkId());
        assertEquals("start", lineItem.getStartDateTime());
        assertEquals("end", lineItem.getEndDateTime());
        assertSame(results, lineItem.getResults());
    }

    @Test
    public void testLineItemJsonOmitsNullOptionalFieldsAndIgnoresUnknown() {
        LineItem lineItem = new LineItem();
        lineItem.setLabel("Quiz");

        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(lineItem));

        assertEquals("Quiz", json.get("label").asString());
        assertTrue(json.has("scoreMaximum"));
        assertFalse(json.has("id"));
        assertFalse(json.has("tag"));
        assertFalse(json.has("results"));

        LineItem parsed = jsonMapper.readValue("{\"id\":\"x\",\"label\":\"L\",\"unknown\":1}", LineItem.class);
        assertEquals("x", parsed.getId());
        assertEquals("L", parsed.getLabel());
    }

    @Test
    public void testLineItemsAndResultsLists() {
        LineItems lineItems = new LineItems();
        assertNotNull(lineItems.getLineItemList());
        assertTrue(lineItems.getLineItemList().isEmpty());
        List<LineItem> lineItemList = List.of(new LineItem());
        lineItems.setLineItemList(lineItemList);
        assertSame(lineItemList, lineItems.getLineItemList());

        Results results = new Results();
        assertTrue(results.getResultList().isEmpty());
        List<Result> resultList = List.of(new Result());
        results.setResultList(resultList);
        assertSame(resultList, results.getResultList());
    }

    @Test
    public void testResultAccessorsAndJson() {
        Result result = jsonMapper.readValue(
                "{\"id\":\"r\",\"userId\":\"u\",\"resultMaximum\":\"10\",\"resultScore\":\"5\",\"comment\":\"c\",\"scoreOf\":\"li\",\"extra\":true}",
                Result.class);

        assertEquals("r", result.getId());
        assertEquals("u", result.getUserId());
        assertEquals("10", result.getResultMaximum());
        assertEquals("5", result.getResultScore());
        assertEquals("c", result.getComment());
        assertEquals("li", result.getScoreOf());

        result.setId("r2");
        result.setUserId("u2");
        result.setResultMaximum("20");
        result.setResultScore("15");
        result.setComment("c2");
        result.setScoreOf("li2");

        assertEquals("r2", result.getId());
        assertEquals("u2", result.getUserId());
        assertEquals("20", result.getResultMaximum());
        assertEquals("15", result.getResultScore());
        assertEquals("c2", result.getComment());
        assertEquals("li2", result.getScoreOf());
    }

    @Test
    public void testScoreAccessorsAndJson() {
        Score score = new Score();
        assertNull(score.getComment());
        score.setUserId("u");
        score.setScoreMaximum("10");
        score.setScoreGiven("7");
        score.setComment(" ");
        score.setActivityProgress("Completed");
        score.setGradingProgress("FullyGraded");
        score.setTimestamp("2026-01-01T00:00:00Z");

        assertEquals("u", score.getUserId());
        assertEquals("10", score.getScoreMaximum());
        assertEquals("7", score.getScoreGiven());
        assertEquals(" ", score.getComment());
        assertEquals("Completed", score.getActivityProgress());
        assertEquals("FullyGraded", score.getGradingProgress());
        assertEquals("2026-01-01T00:00:00Z", score.getTimestamp());

        Score parsed = jsonMapper.readValue(jsonMapper.writeValueAsString(score), Score.class);
        assertEquals("u", parsed.getUserId());
        assertEquals("7", parsed.getScoreGiven());
        assertEquals("FullyGraded", parsed.getGradingProgress());
        assertEquals("2026-01-01T00:00:00Z", parsed.getTimestamp());
    }
}
