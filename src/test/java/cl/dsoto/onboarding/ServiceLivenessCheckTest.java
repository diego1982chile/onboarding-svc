package cl.dsoto.onboarding;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
class ServiceLivenessCheckTest {
    @Test
    void shouldReportServiceAsLive() {
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks.name", hasItem("onboarding-svc"));
    }
}
