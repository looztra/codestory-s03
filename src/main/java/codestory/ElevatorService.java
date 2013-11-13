package codestory;

import com.hubspot.dropwizard.guice.GuiceBundle;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import devlab722.dropwizard.bundle.LogstashLogbackEncoderBundle;
import devlab722.dropwizard.bundle.LogstashLogbackEncoderConfiguration;

/**
 * User: cfurmaniak
 * Date: 31/10/13
 * Time: 18:51
 */
public class ElevatorService extends Service<ElevatorConfiguration> {
    public static void main(String[] args) throws Exception {
        new ElevatorService().run(args);
    }

    @Override
    public void initialize(Bootstrap<ElevatorConfiguration> bootstrap) {
        bootstrap.setName("Elevator Service baby!");
        bootstrap.addBundle(GuiceBundle.<ElevatorConfiguration>newBuilder().addModule(new ElevatorModule()).enableAutoConfig(getClass()
                .getPackage().getName()).build()
        );
//        bootstrap.addBundle(new LogstashLogbackEncoderBundle<ElevatorConfiguration>() {
//            @Override
//            public LogstashLogbackEncoderConfiguration getConfiguration(ElevatorConfiguration configuration) {
//                return configuration.getLogstash();
//            }
//        });
    }

    @Override
    public void run(ElevatorConfiguration configuration, Environment environment) throws Exception {

    }


}
