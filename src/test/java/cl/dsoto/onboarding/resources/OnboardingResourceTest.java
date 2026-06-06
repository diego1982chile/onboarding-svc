package cl.dsoto.onboarding.resources;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.repositories.OnboardingProcessRepository;
import cl.dsoto.onboarding.services.OnboardingEngine;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
class OnboardingResourceTest {

    @Inject
    OnboardingEngine onboardingEngine;

    @Inject
    OnboardingProcessRepository repository;

    @BeforeEach
    void cleanUp() {
        QuarkusTransaction.requiringNew().run(repository::deleteAll);
    }

    @Test
    void shouldReturnStartingTrainForAnonymousVisitor() {
        given()
                .when().get("/api/onboarding/public/train")
                .then()
                .statusCode(200)
                .body("username", nullValue())
                .body("currentState", nullValue())
                .body("currentStep", is("REGISTRATION"))
                .body("steps[0].key", is("REGISTRATION"))
                .body("steps[0].status", is("CURRENT"))
                .body("steps[1].key", is("IDENTITY_CHECK"))
                .body("steps[1].status", is("PENDING"))
                .body("steps[2].key", is("PLAN_SELECTION"))
                .body("steps[2].status", is("PENDING"));
    }

    @Test
    void shouldNotAdvancePublicTrainFromStageQueryParameter() {
        given()
                .queryParam("stage", "email-confirmed")
                .when().get("/api/onboarding/public/train")
                .then()
                .statusCode(200)
                .body("currentStep", is("REGISTRATION"));
    }

    @Test
    void shouldReturnPendingRegistrationTrainFromIdentityStatus() {
        String registrationId = "registration-pending-123";
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(
                "registration.pending@example.com",
                registrationId
        ));

        given()
                .when().get("/api/onboarding/public/" + registrationId + "/status")
                .then()
                .statusCode(200)
                .body("confirmed", is(false))
                .body("train.username", nullValue())
                .body("train.currentState", is("REGISTERED"))
                .body("train.currentStep", is("REGISTRATION"));
    }

    @Test
    void shouldAdvanceTrainWhenIdentityConfirmsRegistration() {
        String registrationId = "registration-confirmed-123";
        String username = "registration.confirmed@example.com";
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username, registrationId));
        onboardingEngine.applyEvent(OnboardingEvent.emailVerified(username));

        given()
                .when().get("/api/onboarding/public/" + registrationId + "/status")
                .then()
                .statusCode(200)
                .body("confirmed", is(true))
                .body("train.username", nullValue())
                .body("train.currentState", is("EMAIL_VERIFIED"))
                .body("train.currentStep", is("IDENTITY_CHECK"))
                .body("train.steps[0].status", is("COMPLETED"))
                .body("train.steps[1].status", is("CURRENT"))
                .body("train.steps[2].status", is("PENDING"));
    }

    @Test
    void shouldReturnNotFoundWhenIdentityDoesNotKnowRegistration() {
        String registrationId = "missing-registration";

        given()
                .when().get("/api/onboarding/public/" + registrationId + "/status")
                .then()
                .statusCode(404);
    }

    @Test
    void shouldRejectMyTrainWhenAnonymous() {
        given()
                .when().get("/api/onboarding/me/train")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "user@example.com", roles = "USER")
    @JwtSecurity(claims = {
            @io.quarkus.test.security.jwt.Claim(key = "sub", value = "user@example.com")
    })
    void shouldReturnAuthenticatedTrainForUser() {
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered("user@example.com"));
        onboardingEngine.applyEvent(OnboardingEvent.emailVerified("user@example.com"));

        given()
                .when().get("/api/onboarding/me/train")
                .then()
                .statusCode(200)
                .body("username", is("user@example.com"))
                .body("currentState", is("EMAIL_VERIFIED"))
                .body("currentStep", is("IDENTITY_CHECK"));
    }

    @Test
    @TestSecurity(user = "missing@example.com", roles = "USER")
    @JwtSecurity(claims = {
            @io.quarkus.test.security.jwt.Claim(key = "sub", value = "missing@example.com")
    })
    void shouldReturnNotFoundWhenAuthenticatedUserHasNoOnboardingProcess() {
        given()
                .when().get("/api/onboarding/me/train")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "unprivileged@example.com", roles = "GUEST")
    @JwtSecurity(claims = {
            @io.quarkus.test.security.jwt.Claim(key = "sub", value = "unprivileged@example.com")
    })
    void shouldRejectMyTrainWithoutRequiredRole() {
        given()
                .when().get("/api/onboarding/me/train")
                .then()
                .statusCode(403);
    }
}
