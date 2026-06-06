package cl.dsoto.onboarding.resources.dto;

import cl.dsoto.onboarding.model.OnboardingState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingTrainView {

    private String username;
    private OnboardingState currentState;
    private OnboardingTrainStep currentStep;
    private List<OnboardingTrainStepView> steps;
}
