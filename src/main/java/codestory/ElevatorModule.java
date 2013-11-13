package codestory;

import codestory.core.engine.S03E01W2Elevator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import codestory.core.engine.ElevatorEngine;

/**
 * User: cfurmaniak
 * Date: 31/10/13
 * Time: 18:54
 */
public class ElevatorModule extends AbstractModule {
    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    protected ElevatorEngine providesElevatorEngine() {
        return new S03E01W2Elevator();
    }
}
