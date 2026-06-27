package cl.dsoto.onboarding.webservice.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingStartRequestResource {

    @NotBlank
    @Email
    private String email;
}
