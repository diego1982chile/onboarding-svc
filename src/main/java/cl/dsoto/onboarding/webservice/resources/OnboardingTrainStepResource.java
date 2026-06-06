package cl.dsoto.onboarding.webservice.resources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingTrainStepResource {

    private OnboardingTrainStep key;
    private String label;
    private OnboardingTrainStepStatus status;
}
