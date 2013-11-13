package codestory.core.engine;

import codestory.core.Direction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static codestory.core.engine.assertions.Assertions.assertThat;

public class S03E01W2FuncTest {
    private S03E01W2Elevator elevator;

    @BeforeMethod
    public void setUp() {
        elevator = new S03E01W2Elevator();
    }

    @Test
    public void should_works_even_with_reset_at_start() {
        assertThat(elevator).is("CLOSE 0").reset("for test purposes").is("NOTHING 0").onTick("CLOSE 1");
    }

    @Test
    public void simple_scenario() {
        elevator.logCurrentState("simple_scenario");
        assertThat(elevator).is("CLOSE 0")
                .call(3, Direction.UP)
                .onTick("CLOSE  1")
                .onTick("  2")
                .onTick("  3")
                .onTick("OPEN   ").go(5)
                .onTick("CLOSE 3")
                .onTick("CLOSE 4")
                .onTick("CLOSE 5")
                .onTick("OPEN 5");
    }

    @Test
    public void should_not_go_to_top_and_get_to_middle_floor() {
        assertThat(elevator).is("CLOSE 0")
                .call(4, Direction.DOWN)
                .onTick("CLOSE   1")
                .onTick("CLOSE 2")
                .onTick("CLOSE 3")
                .onTick("CLOSE 4")
                .onTick("OPEN   ").userHasEntered(null).go(1)
                .onTick("CLOSE ")
                .onTick("CLOSE 3")
                .onTick("CLOSE 2")
                .onTick("CLOSE 1")
                .onTick("OPEN").userHasExited(null)
                .onTick("CLOSE 1")
                .onTick("CLOSE 2")
                .onTick("NOTHING")
        ;
    }

    @Test
    public void should_go_to_middle_floor_from_start() {
        assertThat(elevator).is("CLOSE 0")
                .onTick("CLOSE   1")
                .onTick("CLOSE 2")
                .onTick("NOTHING")
        ;
    }

    @Test
    public void should_go_to_middle_floor_then_go_down_for_first_user() {
        assertThat(elevator).is("CLOSE 0")
                .onTick("CLOSE   1")
                .onTick("CLOSE 2")
                .onTick("NOTHING")
                .onTick("NOTHING").call(1, Direction.UP)
                .onTick("CLOSE 1")
                .onTick("OPEN 1").userHasEntered(null).go(4)
                .onTick("CLOSE 1")
                .onTick("CLOSE 2")
                .onTick("CLOSE 3")
                .onTick("CLOSE 4")
                .onTick("OPEN 4").userHasExited(null)
        ;
    }

    @Test
    public void should_not_open_close_open_close_at_floor_where_there_are_stops_requested_and_waiting_users() {
        assertThat(elevator).is("CLOSE 0")
                .onTick("CLOSE   1")
                .onTick("CLOSE 2")
                .onTick("NOTHING") //wait at middleFloor
                .call(3, Direction.UP)
                .onTick("CLOSE 3")
                .onTick("OPEN 3").userHasEntered(null).go(4)
                .onTick("CLOSE 3").call(4, Direction.UP)
                .onTick("CLOSE 4")
                .onTick("OPEN 4").userHasExited(null).userHasEntered(null).go(5)
                .onTick("CLOSE 4")
                .onTick("CLOSE 5")
                .onTick("OPEN 5").userHasExited(null)
        ;
    }

}

