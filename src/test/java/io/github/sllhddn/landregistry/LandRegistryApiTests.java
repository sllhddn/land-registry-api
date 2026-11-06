package io.github.sllhddn.landregistry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack tests: HTTP -> controller -> service -> JPA -> Flyway-managed H2 schema.
 * Hibernate runs with ddl-auto=validate, so the context only starts if entities match the migration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LandRegistryApiTests {

    private static final AtomicInteger LOT = new AtomicInteger(1000);

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createParcel() throws Exception {
        String lot = "lot-" + LOT.incrementAndGet();
        String body = postJson("/api/parcels", """
                {"lotNo": "%s", "district": "District A", "areaSqm": 1250.5, "tenure": "FREEHOLD"}
                """.formatted(lot))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lotNo").value(lot.toUpperCase()))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private JsonNode submit(long parcelId, String type) throws Exception {
        String body = postJson("/api/applications", """
                {"parcelId": %d, "type": "%s", "applicantName": "Siti Aminah", "feeAmount": 150.00}
                """.formatted(parcelId, type))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private ResultActions move(long appId, String target) throws Exception {
        return postJson("/api/applications/" + appId + "/transitions", """
                {"status": "%s", "remarks": "checked"}
                """.formatted(target));
    }

    @Test
    void duplicateLotNumberIsRejected() throws Exception {
        long id = createParcel();
        String lot = json.readTree(mvc.perform(get("/api/parcels/" + id)).andReturn().getResponse()
                .getContentAsString()).get("lotNo").asText();

        postJson("/api/parcels", """
                {"lotNo": "%s", "district": "District B", "areaSqm": 10, "tenure": "LEASEHOLD"}
                """.formatted(lot.toLowerCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void invalidParcelReturnsFieldErrors() throws Exception {
        postJson("/api/parcels", """
                {"lotNo": "", "district": "X", "areaSqm": 12.123456, "tenure": "RENTED"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lotNo").exists())
                .andExpect(jsonPath("$.errors.areaSqm").exists())
                .andExpect(jsonPath("$.errors.tenure").exists());
    }

    @Test
    void unknownIdsReturn404() throws Exception {
        mvc.perform(get("/api/parcels/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/applications/999999")).andExpect(status().isNotFound());
    }

    @Test
    void applicationFollowsTheWorkflow() throws Exception {
        long appId = submit(createParcel(), "TRANSFER").get("id").asLong();

        move(appId, "APPROVED").andExpect(status().isConflict());           // must be reviewed first
        move(appId, "UNDER_REVIEW").andExpect(status().isOk());
        move(appId, "APPROVED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty());
        move(appId, "SUBMITTED").andExpect(status().isConflict());          // no going back
        move(appId, "REGISTERED").andExpect(status().isOk());
    }

    @Test
    void onlyOneOpenApplicationPerParcelAndType() throws Exception {
        long parcelId = createParcel();
        long first = submit(parcelId, "CHARGE").get("id").asLong();

        postJson("/api/applications", """
                {"parcelId": %d, "type": "CHARGE", "applicantName": "Another", "feeAmount": 1}
                """.formatted(parcelId))
                .andExpect(status().isConflict());

        submit(parcelId, "CAVEAT");                                          // different type is fine
        move(first, "WITHDRAWN").andExpect(status().isOk());
        submit(parcelId, "CHARGE");                                          // allowed once the first is closed
    }

    @Test
    void listFiltersByStatusAndSummaryCounts() throws Exception {
        long appId = submit(createParcel(), "LEASE").get("id").asLong();
        move(appId, "UNDER_REVIEW").andExpect(status().isOk());

        mvc.perform(get("/api/applications").param("status", "UNDER_REVIEW").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].status").value("UNDER_REVIEW"));

        mvc.perform(get("/api/applications/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.UNDER_REVIEW").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
