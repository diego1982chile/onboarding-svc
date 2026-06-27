package cl.dsoto.onboarding.events.identity.sqs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
public class SqsClientProducer {

    @ConfigProperty(name = "identity.events.aws.region")
    String region;

    @ConfigProperty(name = "identity.events.sqs.endpoint-override")
    Optional<String> endpointOverride;

    @Produces
    @ApplicationScoped
    SqsClient sqsClient() {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(region))
                .httpClientBuilder(UrlConnectionHttpClient.builder());

        if (endpointOverride.isPresent() && !endpointOverride.get().isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride.get()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("localstack", "localstack")
                    ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
