package cl.dsoto.onboarding.webservice.resources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationStatusResource {

    private boolean confirmed;
    private OnboardingTrainResource train;

    public RegistrationStatusResource(OnboardingTrainResource train) {
        this(train.getCurrentStep() != OnboardingTrainStep.REGISTRATION, train);
    }
}
