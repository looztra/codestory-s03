package codestory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import devlab722.dropwizard.bundle.LogstashLogbackEncoderConfiguration;
import lombok.Data;

/**
 * User: cfurmaniak
 * Date: 31/10/13
 * Time: 18:52
 */
@Data
public class ElevatorConfiguration extends Configuration {
    @JsonProperty
    LogstashLogbackEncoderConfiguration logstash;
}
