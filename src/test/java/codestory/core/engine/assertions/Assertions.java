package codestory.core.engine.assertions;

import codestory.core.engine.ElevatorEngine;

public class Assertions {

    public static ElevatorEngineAssert assertThat(ElevatorEngine actual) {
        return new ElevatorEngineAssert(actual);
    }

}
