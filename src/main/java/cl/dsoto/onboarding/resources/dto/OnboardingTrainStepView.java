package cl.dsoto.onboarding.resources.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingTrainStepView {

    private OnboardingTrainStep key;
    private String label;
    private OnboardingTrainStepStatus status;
}
