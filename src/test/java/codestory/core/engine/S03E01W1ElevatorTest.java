package codestory.core.engine;

import codestory.core.Command;
import codestory.core.Direction;
import codestory.core.Door;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fest.assertions.Assertions.assertThat;

/**
 * User: cfurmaniak
 * Date: 02/11/13
 * Time: 19:10
 */
public class S03E01W1ElevatorTest {

    private S03E01W1Elevator elevator;

    @BeforeMethod
    public void setUp() {
        elevator = new S03E01W1Elevator();
    }

    @Test
    public void check_initial_state() {
        assertThat(elevator.getCurrentDirection()).isEqualTo(Direction.UP);
        assertThat(elevator.getCurrentDoorStatus()).isEqualTo(Door.CLOSE);
        assertThat(elevator.getCurrentNbOfUsersInsideTheElevator().get()).isEqualTo(0);
        assertThat(elevator.getCurrentFloor().get()).isEqualTo(0);
        assertThat(elevator.getPreviousCommand()).isEqualTo(Command.NOTHING);
        assertThat(elevator.getUserWaitingByFloor().isEmpty()).isTrue();
        assertThat(elevator.getStopRequestedByFloor().isEmpty()).isTrue();
    }

    @Test
    public void shouldCloseTheDoor_should_return_true_when_door_is_open() {
        elevator.setCurrentDoorStatus(Door.OPEN);
        assertThat(elevator.shouldCloseTheDoor()).isTrue();
    }

    @Test
    public void shouldCloseTheDoor_should_return_false_when_door_is_close() {
        elevator.setCurrentDoorStatus(Door.CLOSE);
        assertThat(elevator.shouldCloseTheDoor()).isFalse();
    }

    @Test
    public void userWaitingAtCurrentFloor_should_return_true() {
        elevator.setCurrentFloor(new AtomicInteger(2));
        elevator.call(2, Direction.DOWN);
        assertThat(elevator.userWaitingAtCurrentFloor()).isTrue();
    }

    @Test
    public void userWaitingAtCurrentFloor_should_return_false() {
        elevator.setCurrentFloor(new AtomicInteger(2));
        elevator.call(3, Direction.DOWN);
        assertThat(elevator.userWaitingAtCurrentFloor()).isFalse();
    }

    @Test
    public void shouldOpenTheDoor_should_return_true_when_someone_is_waiting_at_current_floor_and_previous_command_is_not_CLOSE() {
        elevator.setCurrentDoorStatus(Door.CLOSE);
        elevator.setPreviousCommand(Command.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(3));
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.call(3, Direction.DOWN);
        elevator.logCurrentState("007");
        assertThat(elevator.userWaitingAtCurrentFloor()).isTrue();
        assertThat(elevator.userInsideElevatorNeedToGetOut()).isFalse();
        assertThat(elevator.shouldOpenTheDoor()).isTrue();
    }

    @Test
    public void shouldOpenTheDoor_should_return_true_when_someone_requested_a_stop_at_current_floor_and_previous_command_is_CLOSE() {
        elevator.userHasEntered(null);
        elevator.go(3);
        elevator.setCurrentDoorStatus(Door.CLOSE);
        elevator.setPreviousCommand(Command.CLOSE);
        elevator.setCurrentFloor(new AtomicInteger(3));
        assertThat(elevator.userInsideElevatorNeedToGetOut()).isTrue();
        assertThat(elevator.shouldOpenTheDoor()).isTrue();
    }

    @Test
    public void shouldOpenTheDoor_should_return_true_when_someone_requested_a_stop_at_current_floor_and_previous_command_is_not_CLOSE() {
        elevator.userHasEntered(null);
        elevator.go(3);
        elevator.setCurrentDoorStatus(Door.CLOSE);
        elevator.setPreviousCommand(Command.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(3));
        assertThat(elevator.userInsideElevatorNeedToGetOut()).isTrue();
        assertThat(elevator.shouldOpenTheDoor()).isTrue();
    }

    @Test
    public void
    shouldOpenTheDoor_should_return_true_when_someone_is_waiting_at_current_floor_and_someone_requested_a_stop_at_current_floor_and_previous_command_is_CLOSE() {
        elevator.userHasEntered(null);
        elevator.go(3);
        elevator.call(3, Direction.DOWN);
        elevator.setCurrentDoorStatus(Door.CLOSE);
        elevator.setPreviousCommand(Command.CLOSE);
        elevator.setCurrentFloor(new AtomicInteger(3));
        assertThat(elevator.userInsideElevatorNeedToGetOut()).isTrue();
        assertThat(elevator.userWaitingAtCurrentFloor()).isTrue();
        assertThat(elevator.shouldOpenTheDoor()).isTrue();
    }

    @Test
    public void
    shouldOpenTheDoor_should_return_true_when_someone_is_waiting_at_current_floor_and_someone_requested_a_stop_at_current_floor_and_previous_command_is_not_CLOSE() {
        elevator.userHasEntered(null);
        elevator.go(3);
        elevator.call(3, Direction.DOWN);
        elevator.setCurrentDoorStatus(Door.CLOSE);
        elevator.setPreviousCommand(Command.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(3));
        assertThat(elevator.userInsideElevatorNeedToGetOut()).isTrue();
        assertThat(elevator.userWaitingAtCurrentFloor()).isTrue();
        assertThat(elevator.shouldOpenTheDoor()).isTrue();
    }

    @Test
    public void shouldOpenTheDoor_should_return_false_when_door_is_already_open_and_previous_door_state_is_CLOSE() {
        elevator.setPreviousCommand(Command.CLOSE);
        elevator.setCurrentDoorStatus(Door.OPEN);
        assertThat(elevator.shouldOpenTheDoor()).isFalse();
    }

    @Test
    public void shouldOpenTheDoor_should_return_false_when_door_is_already_open_and_previous_door_state_is_OPEN() {
        elevator.setPreviousCommand(Command.OPEN);
        elevator.setCurrentDoorStatus(Door.OPEN);
        assertThat(elevator.shouldOpenTheDoor()).isFalse();
    }

    @Test
    public void nobodyHasCalled_should_return_true_when_nobody_has_made_a_call() {
        elevator.setUserWaitingByFloor(ArrayListMultimap.<Integer, Direction>create());
        assertThat(elevator.nobodyHasCalled()).isTrue();
    }

    @Test
    public void nobodyHasCalled_should_return_false_when_someone_has_made_a_call() {
        Multimap<Integer, Direction> waitingList = ArrayListMultimap.create();
        waitingList.put(3, Direction.DOWN);
        elevator.setUserWaitingByFloor(waitingList);
        assertThat(elevator.nobodyHasCalled()).isFalse();
    }

    @Test
    public void nobodyHasRequestedAStop_should_return_true_when_nobody_requested_a_stop() {
        elevator.setStopRequestedByFloor(ArrayListMultimap.<Integer, Direction>create());
        assertThat(elevator.nobodyHasRequestedAStop()).isTrue();
    }

    @Test
    public void nobodyHasRequestedAStop_should_return_false_when_someone_requested_a_stop() {
        Multimap<Integer, Direction> stopList = ArrayListMultimap.create();
        stopList.put(3, Direction.DOWN);
        elevator.setStopRequestedByFloor(stopList);
        assertThat(elevator.nobodyHasRequestedAStop()).isFalse();
    }

    @Test
    public void shouldDoNothing_works_as_expected_when_nobody_did_nothing_and_we_are_at_middle_floor() {
        elevator.setStopRequestedByFloor(ArrayListMultimap.<Integer, Direction>create());
        elevator.setUserWaitingByFloor(ArrayListMultimap.<Integer, Direction>create());
        elevator.setCurrentFloor(new AtomicInteger(elevator.getMiddleFloor()));
        assertThat(elevator.shouldDoNothing()).isTrue();
    }

    @Test
    public void shouldDoNothing_works_as_expected_when_nobody_did_nothing_and_we_are_not_at_middle_floor() {
        elevator.setStopRequestedByFloor(ArrayListMultimap.<Integer, Direction>create());
        elevator.setUserWaitingByFloor(ArrayListMultimap.<Integer, Direction>create());
        elevator.setCurrentFloor(new AtomicInteger(0));
        assertThat(elevator.shouldDoNothing()).isFalse();
    }

    @Test
    public void shouldDoNothing_works_as_expected_when_someone_is_waiting() {
        Multimap<Integer, Direction> waitingList = ArrayListMultimap.create();
        waitingList.put(3, Direction.DOWN);
        elevator.setStopRequestedByFloor(ArrayListMultimap.<Integer, Direction>create());
        elevator.setUserWaitingByFloor(waitingList);
        assertThat(elevator.shouldDoNothing()).isFalse();
    }

    @Test
    public void shouldDoNothing_works_as_expected_when_someone_requested_a_stop() {
        Multimap<Integer, Direction> stopList = ArrayListMultimap.create();
        stopList.put(3, Direction.DOWN);
        elevator.setStopRequestedByFloor(stopList);
        elevator.setUserWaitingByFloor(ArrayListMultimap.<Integer, Direction>create());
        assertThat(elevator.shouldDoNothing()).isFalse();
    }

    @Test
    public void elevatorIsAtTop_is_at_top() {
        elevator.setCurrentFloor(new AtomicInteger(elevator.getHigherFloor()));
        assertThat(elevator.elevatorIsAtTop()).isTrue();
    }

    @Test
    public void elevatorIsAtTop_is_not_at_top() {
        elevator.setCurrentFloor(new AtomicInteger(elevator.getHigherFloor() - 1));
        assertThat(elevator.elevatorIsAtTop()).isFalse();
    }

    @Test
    public void elevatorIsAtBottom_is_at_bottom() {
        elevator.setCurrentFloor(new AtomicInteger(elevator.getLowerFloor()));
        assertThat(elevator.elevatorIsAtBottom()).isTrue();
    }

    @Test
    public void elevatorIsAtBottom_is_not_at_bottom() {
        elevator.setCurrentFloor(new AtomicInteger(elevator.getLowerFloor() + 1));
        assertThat(elevator.elevatorIsAtBottom()).isFalse();
    }

    @Test
    public void test_call() {
        elevator.setCurrentFloor(new AtomicInteger(0));
        elevator.call(3, Direction.DOWN);
        assertThat(elevator.getUserWaitingByFloor().get(3)).isNotEmpty();
    }

/*
    @Test
    public void test_userHasEntered() {
        elevator.setCurrentFloor(new AtomicInteger(0));
        elevator.call(3, Direction.DOWN);
        assertThat(elevator.getUserWaitingByFloor().get(3)).isNotEmpty();
        elevator.setCurrentFloor(new AtomicInteger(3));
        elevator.userHasEntered(null);
        assertThat(elevator.getUserWaitingByFloor().get(3)).isEmpty();
    }
*/

/*    @Test
    public void test_userHasEntered_when_request_is_not_in_synch_with_context() {
        elevator.setCurrentFloor(new AtomicInteger(0));
        elevator.call(3, Direction.DOWN);
        assertThat(elevator.getUserWaitingByFloor().get(3)).isNotEmpty();
        elevator.nextCommand(); // UP
        elevator.nextCommand(); // UP
        elevator.nextCommand(); // UP
        elevator.nextCommand(); // OPEN
        elevator.nextCommand(); // CLOSE
        elevator.nextCommand(); // UP
        assertThat(elevator.getCurrentFloor().get()).isEqualTo(3);
        elevator.userHasEntered(null);
    }*/

    @Test
    public void evaluateMiddleFloor_works_as_expected_with_default_elevator() {
        assertThat(elevator.getMiddleFloor()).isEqualTo(2);
    }

    @Test
    public void evaluateMiddleFloor_works_as_expected_with_customized_elevator() {
        elevator.reset("tests", 0, 19, ElevatorEngine.DEFAULT_CABIN_SIZE);
        assertThat(elevator.getMiddleFloor()).isEqualTo(9);
    }

    @Test
    public void evaluateMiddleFloor_works_as_expected_with_customized_elevator_and_lower_floor_not_at_0() {
        elevator.reset("tests", 5, 10, ElevatorEngine.DEFAULT_CABIN_SIZE);
        assertThat(elevator.getMiddleFloor()).isEqualTo(7);
    }

    @Test
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_true_when_single_direction_for_requested_floor
            () {
        Multimap<Integer, Direction> userWaitingByFloor = ArrayListMultimap.create();
        userWaitingByFloor.put(1, Direction.DOWN);
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(1));
        elevator.setUserWaitingByFloor(userWaitingByFloor);
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isTrue();
    }

    @Test
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_true_when_both_directions_for_requested_floor
            () {
        Multimap<Integer, Direction> userWaitingByFloor = ArrayListMultimap.create();
        userWaitingByFloor.put(1, Direction.DOWN);
        userWaitingByFloor.put(1, Direction.UP);
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(1));
        elevator.setUserWaitingByFloor(userWaitingByFloor);
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isTrue();
    }

    @Test
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_false_when_nothing_for_current_direction
            () {
        Multimap<Integer, Direction> userWaitingByFloor = ArrayListMultimap.create();
        userWaitingByFloor.put(1, Direction.UP);
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(1));
        elevator.setUserWaitingByFloor(userWaitingByFloor);
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isFalse();
    }

    @Test
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_false_when_nobody_is_waiting
            () {
        Multimap<Integer, Direction> userWaitingByFloor = ArrayListMultimap.create();
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(1));
        elevator.setUserWaitingByFloor(userWaitingByFloor);
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isFalse();
    }

    @Test
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_true_at_top_even_if_not_the_same_direction
            () {
        Multimap<Integer, Direction> userWaitingByFloor = ArrayListMultimap.create();
        userWaitingByFloor.put(elevator.getHigherFloor(), Direction.DOWN);
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(elevator.getHigherFloor()));
        elevator.setUserWaitingByFloor(userWaitingByFloor);
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isTrue();
    }

    @Test
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_true_at_bottom_even_if_not_the_same_direction
            () {
        Multimap<Integer, Direction> userWaitingByFloor = ArrayListMultimap.create();
        userWaitingByFloor.put(elevator.getLowerFloor(), Direction.UP);
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(elevator.getLowerFloor()));
        elevator.setUserWaitingByFloor(userWaitingByFloor);
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isTrue();
    }

    @Test
    public void someoneIsWaitingAtLowerLevels_should_return_true() {
        elevator.call(1, Direction.DOWN);
        assertThat(elevator.someoneIsWaitingAtLowerLevels(3)).isTrue();
    }

    @Test
    public void someoneIsWaitingAtLowerLevels_should_return_false() {
        elevator.call(4, Direction.DOWN);
        assertThat(elevator.someoneIsWaitingAtLowerLevels(3)).isFalse();
    }

    @Test
    public void someoneRequestedAStopAtLowerLevels_should_return_true() {
        elevator.go(1);
        assertThat(elevator.someoneRequestedAStopAtLowerLevels(3)).isTrue();
    }

    @Test
    public void someoneRequestedAStopAtLowerLevels_should_return_false() {
        elevator.go(4);
        assertThat(elevator.someoneRequestedAStopAtLowerLevels(3)).isFalse();
    }

    @Test
    public void getNextDirection_should_be_UP_when_at_bottom() {
        elevator.setCurrentFloor(new AtomicInteger(elevator.getLowerFloor()));
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);
    }

    @Test
    public void getNextDirection_should_be_DOWN_when_at_top() {
        elevator.setCurrentFloor(new AtomicInteger(elevator.getHigherFloor()));
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);
    }

    @Test
    public void getNextDirection_should_be_DOWN_if_nothing_to_do_and_currentFloor_is_higher_than_middle_floor() {
        int currentFloor = 4;
        elevator.setCurrentFloor(new AtomicInteger(currentFloor));
        assertThat(currentFloor > elevator.getMiddleFloor()).isTrue();
        assertThat(elevator.nobodyHasCalled()).isTrue();
        assertThat(elevator.nobodyHasRequestedAStop()).isTrue();
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);
    }

    @Test
    public void getNextDirection_should_be_UP_if_nothing_to_do_and_currentFloor_is_lower_than_middle_floor() {
        int currentFloor = 1;
        elevator.setCurrentFloor(new AtomicInteger(currentFloor));
        assertThat(currentFloor < elevator.getMiddleFloor()).isTrue();
        assertThat(elevator.nobodyHasCalled()).isTrue();
        assertThat(elevator.nobodyHasRequestedAStop()).isTrue();
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);
    }

    // getNextDirection, currentDir=UP
    @Test
    public void getNextDirection_should_be_UP_when_current_dir_is_UP_and_someone_waiting_upstairs() {
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(0));
        elevator.call(3, Direction.UP);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);

    }

    @Test
    public void getNextDirection_should_be_UP_when_current_dir_is_UP_and_someone_requested_a_stop_at_higher_floor() {
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(0));
        elevator.userHasEntered(null);
        elevator.go(3);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);

    }

    @Test
    public void getNextDirection_should_be_UP_when_current_dir_is_UP_and_someone_requested_a_stop_at_higher_floor_and_someone_waiting_upstairs() {
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(0));
        elevator.userHasEntered(null);
        elevator.call(3, Direction.UP);
        elevator.go(3);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);
    }

    @Test
    public void getNextDirection_should_be_DOWN_when_current_dir_is_UP_and_someone_waiting_downstairs_and_nobody_waiting_upstairs() {
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(3));
        elevator.call(2, Direction.UP);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);

    }

    @Test
    public void getNextDirection_should_be_DOWN_when_current_dir_is_UP_and_someone_requested_a_stop_lower_and_nobody_higher() {
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(3));
        elevator.userHasEntered(null);
        elevator.go(1);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);
    }

    @Test
    public void
    getNextDirection_should_be_DOWN_when_current_dir_is_UP_and_someone_requested_a_stop_lower_and_someone_is_waiting_downstairs_and_nobody_requested_something_higher
            () {
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(3));
        elevator.userHasEntered(null);
        elevator.call(2, Direction.UP);
        elevator.go(1);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);
    }

    @Test
    public void getNextDirection_should_be_UP_when_current_dir_is_UP_and_someone_waiting_upstairs_and_lower_actions_also_needed() {
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(1));
        elevator.call(3, Direction.UP);
        elevator.call(1, Direction.UP);
        elevator.go(0);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);

    }

    // getNextDirection, currentDir=DOWN
    @Test
    public void getNextDirection_should_be_DOWN_when_current_dir_is_DOWN_and_someone_waiting_downstairs() {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(elevator.getHigherFloor()));
        elevator.call(3, Direction.DOWN);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);

    }

    @Test
    public void getNextDirection_should_be_DOWN_when_current_dir_is_DOWN_and_someone_requested_a_stop_at_lower_floor() {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(elevator.getHigherFloor()));
        elevator.userHasEntered(null);
        elevator.go(3);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);

    }

    @Test
    public void getNextDirection_should_be_DOWN_when_current_dir_is_DOWN_and_someone_requested_a_stop_at_lower_floor_and_someone_waiting_downstairs() {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(elevator.getHigherFloor()));
        elevator.userHasEntered(null);
        elevator.call(3, Direction.UP);
        elevator.go(3);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);

    }


    @Test
    public void getNextDirection_should_be_UP_when_current_dir_is_DOWN_and_someone_waiting_upstairs_and_nobody_waiting_downstairs() {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(3));
        elevator.call(4, Direction.UP);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);

    }

    @Test
    public void getNextDirection_should_be_UP_when_current_dir_is_DOWN_and_someone_requested_a_higher_floor_and_nobody_at_lower() {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(3));
        elevator.userHasEntered(null);
        elevator.go(4);
        assertThat(elevator.nobodyHasCalled()).isTrue();
        assertThat(elevator.nobodyHasRequestedAStop()).isFalse();
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);
    }

    @Test
    public void
    getNextDirection_should_be_UP_when_current_dir_is_DOWN_and_someone_requested_a_stop_higher_and_someone_is_waiting_upstairs_and_nobody_requested_something_lower
            () {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(3));
        elevator.userHasEntered(null);
        elevator.call(4, Direction.UP);
        elevator.go(5);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);
    }


    @Test
    public void getNextDirection_should_be_DOWN_when_current_dir_is_DOWN_and_someone_waiting_downstairs_and_upper_actions_also_needed() {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(2));
        elevator.call(3, Direction.UP);
        elevator.go(5);
        elevator.call(1, Direction.UP);
        elevator.go(0);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);

    }

    @Test
    public void justClosedTheDoor_should_be_true() {
        elevator.setPreviousCommand(Command.CLOSE);
        assertThat(elevator.justClosedTheDoor()).isTrue();
    }

    @DataProvider(name = "providerFor_previousCommand_is_not_CLOSE")
    public Object[][] providerFor_justClosedTheDoor_should_be_false() {
        return new Object[][]{
                {Command.DOWN},
                {Command.NOTHING},
                {Command.OPEN},
                {Command.UP}
        };
    }

    @Test(dataProvider = "providerFor_previousCommand_is_not_CLOSE")
    public void justClosedTheDoor_should_be_false(Command command) {
        elevator.setPreviousCommand(command);
        assertThat(elevator.justClosedTheDoor()).isFalse();
    }

    @Test
    public void clearWaitingListForCurrentFloor_should_clear_the_waiting_list_for_current_floor() {
        int floor = 3;
        elevator.call(floor, Direction.DOWN);
        elevator.call(floor, Direction.UP);
        assertThat(elevator.getUserWaitingByFloor().isEmpty()).isFalse();
        elevator.setCurrentFloor(new AtomicInteger(floor));
        elevator.clearWaitingListForCurrentFloor();
        assertThat(elevator.getUserWaitingByFloor().isEmpty()).isTrue();
    }

    @Test
    public void clearStopListForCurrentFloor_should_clear_the_waiting_list_for_current_floor() {
        int floor = 3;
        elevator.setCurrentFloor(new AtomicInteger(0));
        elevator.userHasEntered(null);
        elevator.userHasEntered(null);
        elevator.go(floor);
        elevator.go(floor);
        elevator.setCurrentFloor(new AtomicInteger(floor));
        assertThat(elevator.getStopRequestedByFloor().isEmpty()).isFalse();

        elevator.clearStopListForCurrentFloor();
        assertThat(elevator.getStopRequestedByFloor().isEmpty()).isTrue();
    }

    @DataProvider(name = "providerFor_test_getByFloorByDirection")
    public Object[][] providerFor_test_getByFloorByDirection() {
        Multimap<Integer, Direction> multimap1 = ArrayListMultimap.create();
        int floor1 = 2;
        multimap1.putAll(floor1, Arrays.asList(Direction.DOWN, Direction.UP, Direction.DOWN, Direction.DOWN));

        Multimap<Integer, Direction> multimap2 = ArrayListMultimap.create();
        int floor2 = 0;
        multimap2.putAll(floor2, Arrays.asList(Direction.DOWN, Direction.DOWN, Direction.DOWN,
                Direction.DOWN));


        Multimap<Integer, Direction> multimap3 = ArrayListMultimap.create();
        int floor3 = 0;
        multimap3.putAll(floor3, Arrays.asList(Direction.UP, Direction.UP));


        Multimap<Integer, Direction> multimap4 = ArrayListMultimap.create();
        return new Object[][]{
                {multimap1, floor1, 3, 1},
                {multimap2, floor2, 4, 0},
                {multimap3, floor3, 0, 2},
                {multimap4, 5, 0, 0}

        };
    }

    @Test(dataProvider = "providerFor_test_getByFloorByDirection")
    public void test_getByFloorByDirection(Multimap<Integer, Direction> multimap, int floor, int expectedNbDown,
                                           int expectedNbUp) {
        int[] expected = elevator.getByFloorByDirection(multimap, floor);
        assertThat(expected[0]).isEqualTo(expectedNbDown);
        assertThat(expected[1]).isEqualTo(expectedNbUp);
    }
}

