package cl.dsoto.onboarding.webservice.resources;

import cl.dsoto.onboarding.model.OnboardingState;
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
        this(train.getCurrentState() == OnboardingState.EMAIL_VERIFIED
                || train.getCurrentState() == OnboardingState.PROFILE_CREATED, train);
    }
}
