package cl.dsoto.onboarding.webservice.impl;

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

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
class DefaultOnboardingWebServiceTest {

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
                .when().get("/api/onboarding/train")
                .then()
                .statusCode(200)
                .body("username", nullValue())
                .body("currentState", nullValue())
                .body("currentStep", is("REGISTRATION"))
                .body("steps[0].key", is("REGISTRATION"))
                .body("steps[0].status", is("CURRENT"))
                .body("steps[1].key", is("EMAIL_VERIFICATION"))
                .body("steps[1].status", is("PENDING"))
                .body("steps[2].key", is("PROFILE_CREATION"))
                .body("steps[2].status", is("PENDING"));
    }

    @Test
    void shouldNotAdvancePublicTrainFromStageQueryParameter() {
        given()
                .queryParam("stage", "email-confirmed")
                .when().get("/api/onboarding/train")
                .then()
                .statusCode(200)
                .body("currentStep", is("REGISTRATION"));
    }

    @Test
    void shouldStartNewOnboardingWhenEmailIsUnknown() {
        given()
                .contentType("application/json")
                .body(Map.of("email", "new.user@example.com"))
                .when().post("/api/onboarding/start")
                .then()
                .statusCode(200)
                .body("email", is("new.user@example.com"))
                .body("registrationId", nullValue())
                .body("state", is("NEW"))
                .body("nextAction", is("COLLECT_PASSWORD"))
                .body("train.username", nullValue())
                .body("train.currentStep", is("REGISTRATION"));
    }

    @Test
    void shouldResumeEmailVerificationWhenEmailIsRegistered() {
        String registrationId = "resume-registration-123";
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(
                "resume.pending@example.com",
                registrationId
        ));

        given()
                .contentType("application/json")
                .body(Map.of("email", "resume.pending@example.com"))
                .when().post("/api/onboarding/start")
                .then()
                .statusCode(200)
                .body("email", is("resume.pending@example.com"))
                .body("registrationId", is(registrationId))
                .body("state", is("REGISTERED"))
                .body("nextAction", is("SHOW_EMAIL_VERIFICATION_PENDING"))
                .body("train.username", nullValue())
                .body("train.currentStep", is("EMAIL_VERIFICATION"));
    }

    @Test
    void shouldResumeVerifiedEmailAtProfileCreation() {
        String email = "resume.verified@example.com";
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(email, "resume-verified-123"));
        onboardingEngine.applyEvent(OnboardingEvent.emailVerified(email));

        given()
                .contentType("application/json")
                .body(Map.of("email", email))
                .when().post("/api/onboarding/start")
                .then()
                .statusCode(200)
                .body("email", is(email))
                .body("state", is("EMAIL_VERIFIED"))
                .body("nextAction", nullValue())
                .body("train.username", nullValue())
                .body("train.currentStep", is("PROFILE_CREATION"));
    }

    @Test
    void shouldReturnLoginActionWhenProfileIsCreated() {
        String email = "resume.complete@example.com";
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(email, "resume-complete-123"));
        onboardingEngine.applyEvent(OnboardingEvent.emailVerified(email));
        onboardingEngine.applyEvent(OnboardingEvent.profileCreated(email));

        given()
                .contentType("application/json")
                .body(Map.of("email", email))
                .when().post("/api/onboarding/start")
                .then()
                .statusCode(200)
                .body("email", is(email))
                .body("state", is("PROFILE_CREATED"))
                .body("nextAction", is("GO_TO_LOGIN"))
                .body("train.username", nullValue())
                .body("train.currentStep", is("PROFILE_CREATION"))
                .body("train.steps[2].status", is("COMPLETED"));
    }

    @Test
    void shouldReturnPendingRegistrationTrainFromIdentityStatus() {
        String registrationId = "registration-pending-123";
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(
                "registration.pending@example.com",
                registrationId
        ));

        given()
                .when().get("/api/onboarding/registrations/" + registrationId + "/status")
                .then()
                .statusCode(200)
                .body("confirmed", is(false))
                .body("train.username", nullValue())
                .body("train.currentState", is("REGISTERED"))
                .body("train.currentStep", is("EMAIL_VERIFICATION"));
    }

    @Test
    void shouldAdvanceTrainWhenIdentityConfirmsRegistration() {
        String registrationId = "registration-confirmed-123";
        String username = "registration.confirmed@example.com";
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username, registrationId));
        onboardingEngine.applyEvent(OnboardingEvent.emailVerified(username));

        given()
                .when().get("/api/onboarding/registrations/" + registrationId + "/status")
                .then()
                .statusCode(200)
                .body("confirmed", is(true))
                .body("train.username", nullValue())
                .body("train.currentState", is("EMAIL_VERIFIED"))
                .body("train.currentStep", is("PROFILE_CREATION"))
                .body("train.steps[0].status", is("COMPLETED"))
                .body("train.steps[1].status", is("COMPLETED"))
                .body("train.steps[2].status", is("CURRENT"));
    }

    @Test
    void shouldReturnNotFoundWhenIdentityDoesNotKnowRegistration() {
        String registrationId = "missing-registration";

        given()
                .when().get("/api/onboarding/registrations/" + registrationId + "/status")
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
                .body("currentStep", is("PROFILE_CREATION"));
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
