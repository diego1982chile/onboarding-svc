package cl.dsoto.onboarding.webservice.resources;

import cl.dsoto.onboarding.model.OnboardingState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingTrainResource {

    private String username;
    private OnboardingState currentState;
    private OnboardingTrainStep currentStep;
    private List<OnboardingTrainStepResource> steps;
}
