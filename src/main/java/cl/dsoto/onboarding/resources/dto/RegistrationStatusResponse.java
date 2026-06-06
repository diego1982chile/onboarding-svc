package cl.dsoto.onboarding.resources.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationStatusResponse {

    private boolean confirmed;
    private OnboardingTrainView train;

    public RegistrationStatusResponse(OnboardingTrainView train) {
        this(train.getCurrentStep() != OnboardingTrainStep.REGISTRATION, train);
    }
}
