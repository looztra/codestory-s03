package codestory.core.engine.assertions;

import codestory.core.Command;
import codestory.core.Direction;
import codestory.core.Door;
import codestory.core.User;
import codestory.core.engine.ElevatorEngine;
import org.fest.assertions.GenericAssert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static codestory.core.Door.CLOSE;
import static codestory.core.Door.OPEN;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static org.fest.assertions.Assertions.assertThat;

public class ElevatorEngineAssert extends GenericAssert<ElevatorEngineAssert, ElevatorEngine> {

    private static final Pattern PATTERN = Pattern.compile("(OPEN|CLOSE|NOTHING)?(?: )*(\\d+)?");
    private Integer expectedFloor;
    private Door expectedDoor;
    private Integer actualFloor = 0;
    private Door actualDoor = CLOSE;

    ElevatorEngineAssert(ElevatorEngine actual) {
        super(ElevatorEngineAssert.class, actual);
    }

    public ElevatorEngineAssert is(String expectedState) {
        assertState(getMatcher(expectedState));
        return this;
    }

    public ElevatorEngineAssert call(Integer atFloor, Direction to) {
        actual.call(atFloor, to);
        return this;
    }

    public ElevatorEngineAssert go(Integer floorToGo) {
        actual.go(floorToGo);
        return this;
    }

    public ElevatorEngineAssert reset(String cause) {
        actual.reset(cause, ElevatorEngine.DEFAULT_LOWER_FLOOR, ElevatorEngine.DEFAULT_HIGHER_FLOOR, ElevatorEngine.DEFAULT_CABIN_SIZE);
        return this;
    }


    public ElevatorEngineAssert reset(String cause, int lowerFloor, int higherFloor) {
        actual.reset(cause, lowerFloor, higherFloor, ElevatorEngine.DEFAULT_CABIN_SIZE);
        return this;
    }

    public ElevatorEngineAssert reset(String cause, int lowerFloor, int higherFloor, int cabinSize) {
        actual.reset(cause, lowerFloor, higherFloor, cabinSize);
        return this;
    }


    public ElevatorEngineAssert userHasEntered(User user) {
        actual.userHasEntered(user);
        return this;
    }

    public ElevatorEngineAssert userHasExited(User user) {
        actual.userHasExited(user);
        return this;
    }

    private Matcher getMatcher(String expectedState) {
        Matcher matcher = PATTERN.matcher(expectedState);

        if (!matcher.matches()) {
            throw fail(format("\"%s\" is not recognized as a state expression", expectedState));
        }
        return matcher;
    }

    private void assertState(Matcher matcher) {
        String expectedElevatorState1 = matcher.group(1);
        String expectedElevatorFloor1 = matcher.group(2);

        if (expectedElevatorState1 != null) {
            if (!"NOTHING".equals(expectedElevatorState1)) {
                expectedDoor = Door.valueOf(expectedElevatorState1);
            }
        }
        if (expectedElevatorFloor1 != null) {
            expectedFloor = parseInt(expectedElevatorFloor1);
        }

        if (expectedDoor != null) {
            assertThat(actualDoor).isEqualTo(expectedDoor);
        }
        if (expectedFloor != null) {
            assertThat(actualFloor).isEqualTo(expectedFloor);
        }
    }

    public ElevatorEngineAssert onTick(String expectedState) {
        Matcher matcher = getMatcher(expectedState);
        tick();
        assertState(matcher);
        return this;
    }

    public ElevatorEngineAssert tick() {
        Command command = actual.nextCommand();

        switch (command) {
            case CLOSE:
                actualDoor = CLOSE;
                break;
            case OPEN:
                actualDoor = OPEN;
                break;
            case UP:
                actualDoor = CLOSE;
                actualFloor++;
                break;
            case DOWN:
                actualDoor = CLOSE;
                actualFloor--;
                break;
            case NOTHING:
                break;
        }

        return this;
    }

}
