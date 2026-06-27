package cl.dsoto.onboarding.webservice.resources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingStartResource {

    private String email;
    private String registrationId;
    private OnboardingStartState state;
    private OnboardingStartAction nextAction;
    private OnboardingTrainResource train;
}
