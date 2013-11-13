package codestory.core.engine;

import codestory.core.Command;
import codestory.core.Direction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

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

//    127.0.0.1 - - [13/Nov/2013:21:06:45 +0000] "GET /reset?lowerFloor=0&higherFloor=19&cabinSize=11&cause=player+has+requested+a+reset HTTP/1.1" 200 0 2 2
//    127.0.0.1 - - [13/Nov/2013:21:06:49 +0000] "GET /nextCommand HTTP/1.1" 200 2 3 3
//    127.0.0.1 - - [13/Nov/2013:21:06:49 +0000] "GET /call?atFloor=3&to=UP HTTP/1.1" 200 0 2 2
//    127.0.0.1 - - [13/Nov/2013:21:06:50 +0000] "GET /call?atFloor=4&to=DOWN HTTP/1.1" 200 0 3 3
//    127.0.0.1 - - [13/Nov/2013:21:06:50 +0000] "GET /nextCommand HTTP/1.1" 200 2 3 3
//    127.0.0.1 - - [13/Nov/2013:21:06:51 +0000] "GET /call?atFloor=0&to=UP HTTP/1.1" 200 0 2 2
//    127.0.0.1 - - [13/Nov/2013:21:06:51 +0000] "GET /nextCommand HTTP/1.1" 200 2 2 2
//    127.0.0.1 - - [13/Nov/2013:21:06:52 +0000] "GET /call?atFloor=16&to=UP HTTP/1.1" 200 0 2 2
//    127.0.0.1 - - [13/Nov/2013:21:06:52 +0000] "GET /nextCommand HTTP/1.1" 200 4 4 4
//    127.0.0.1 - - [13/Nov/2013:21:06:52 +0000] "GET /userHasEntered HTTP/1.1" 200 0 2 2
//    127.0.0.1 - - [13/Nov/2013:21:06:52 +0000] "GET /go?floorToGo=19 HTTP/1.1" 200 0 2 2
//    127.0.0.1 - - [13/Nov/2013:21:06:53 +0000] "GET /call?atFloor=0&to=UP HTTP/1.1" 200 0 3 3
//    127.0.0.1 - - [13/Nov/2013:21:06:53 +0000] "GET /nextCommand HTTP/1.1" 200 5 3 3
    @Test
    public void complexe_scenario_1() {
        ///reset?lowerFloor=0&higherFloor=19&cabinSize=11&cause=player+has+requested+a+reset
        elevator.reset("complexe_scenario_1", 0, 19, 11);
        //nextCommand
        org.fest.assertions.Assertions.assertThat(elevator.nextCommand()).isEqualTo(Command.UP);
        org.fest.assertions.Assertions.assertThat(elevator.getCurrentFloor().get()).isEqualTo(1);
        elevator.call(3, Direction.UP);
        org.fest.assertions.Assertions.assertThat(elevator.someoneIsWaitingAtUpperLevels()).isTrue();
        org.fest.assertions.Assertions.assertThat(elevator.getUsers()).hasSize(1);
        elevator.call(4, Direction.DOWN);
        org.fest.assertions.Assertions.assertThat(elevator.someoneIsWaitingAtUpperLevels()).isTrue();
        org.fest.assertions.Assertions.assertThat(elevator.getUsers()).hasSize(2);
        org.fest.assertions.Assertions.assertThat(elevator.getCurrentNbOfUsersInsideTheElevator().get()).isEqualTo(0);
        //nextCommand
        org.fest.assertions.Assertions.assertThat(elevator.nextCommand()).isEqualTo(Command.UP);
        org.fest.assertions.Assertions.assertThat(elevator.getCurrentFloor().get()).isEqualTo(2);
        elevator.call(0, Direction.UP);
        org.fest.assertions.Assertions.assertThat(elevator.someoneIsWaitingAtUpperLevels()).isTrue();
        org.fest.assertions.Assertions.assertThat(elevator.someoneIsWaitingAtLowerLevels()).isTrue();
        org.fest.assertions.Assertions.assertThat(elevator.getUsers()).hasSize(3);
        org.fest.assertions.Assertions.assertThat(elevator.getCurrentNbOfUsersInsideTheElevator().get()).isEqualTo(0);
        //nextCommand
        org.fest.assertions.Assertions.assertThat(elevator.nextCommand()).isEqualTo(Command.UP);
        org.fest.assertions.Assertions.assertThat(elevator.getCurrentFloor().get()).isEqualTo(3);
        elevator.call(16, Direction.UP);
        org.fest.assertions.Assertions.assertThat(elevator.someoneIsWaitingAtUpperLevels()).isTrue();
        org.fest.assertions.Assertions.assertThat(elevator.someoneIsWaitingAtLowerLevels()).isTrue();
        org.fest.assertions.Assertions.assertThat(elevator.getUsers()).hasSize(4);
        org.fest.assertions.Assertions.assertThat(elevator.getCurrentNbOfUsersInsideTheElevator().get()).isEqualTo(0);
        //nextCommand
        org.fest.assertions.Assertions.assertThat(elevator.nextCommand()).isEqualTo(Command.OPEN);
        org.fest.assertions.Assertions.assertThat(elevator.getCurrentFloor().get()).isEqualTo(3);
        elevator.userHasEntered(null);
        elevator.go(19);
        org.fest.assertions.Assertions.assertThat(elevator.someoneIsWaitingAtUpperLevels()).isTrue();
        org.fest.assertions.Assertions.assertThat(elevator.someoneIsWaitingAtLowerLevels()).isTrue();
        org.fest.assertions.Assertions.assertThat(elevator.getUsers()).hasSize(4);
        org.fest.assertions.Assertions.assertThat(elevator.getCurrentNbOfUsersInsideTheElevator().get()).isEqualTo(1);


    }
}

