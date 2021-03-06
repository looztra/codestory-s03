package codestory.core.engine;

import codestory.core.Command;
import codestory.core.CountsByFloorByDirection;
import codestory.core.Direction;
import codestory.core.Door;
import codestory.core.User;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static codestory.core.Direction.DOWN;
import static codestory.core.Direction.UP;
import static org.fest.assertions.Assertions.*;

/**
 * User: cfurmaniak
 * Date: 13/11/13
 * Time: 00:50
 */
public class S03E01W2ElevatorTest {

    private S03E01W2Elevator elevator;
    private List<User> users;

    @BeforeMethod
    public void setUp() {
        elevator = new S03E01W2Elevator();
        users = new ArrayList<>();
        elevator.setUsers(users);
    }

    @Test
    public void check_initial_state() {
        assertThat(elevator.getCurrentDirection()).isEqualTo(UP);
        assertThat(elevator.getCurrentDoorStatus()).isEqualTo(Door.CLOSE);
        assertThat(elevator.getCurrentNbOfUsersInsideTheElevator().get()).isEqualTo(0);
        assertThat(elevator.getCurrentFloor().get()).isEqualTo(0);
        assertThat(elevator.getPreviousCommand()).isEqualTo(Command.NOTHING);
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
        AtomicInteger currentFloor = new AtomicInteger(2);
        elevator.setCurrentFloor(currentFloor);
        User user1 = new User(2, DOWN);
        users.add(user1);
        assertThat(user1.getInitialFloor()).isEqualTo(currentFloor.get());
        assertThat(elevator.nbUserWaitingAtCurrentFloor()).isEqualTo(1);
        assertThat(elevator.userWaitingAtCurrentFloor()).isTrue();
    }

    @Test
    public void userWaitingAtCurrentFloor_should_return_false() {
        AtomicInteger currentFloor = new AtomicInteger(2);
        elevator.setCurrentFloor(currentFloor);
        User user1 = new User(3, DOWN);
        users.add(user1);
        assertThat(user1.getInitialFloor()).isNotEqualTo(currentFloor.get());
        assertThat(elevator.nbUserWaitingAtCurrentFloor()).isEqualTo(0);
        assertThat(elevator.userWaitingAtCurrentFloor()).isFalse();
    }

    @Test
    public void shouldOpenTheDoor_should_return_true_when_someone_is_waiting_at_current_floor_and_previous_command_is_not_CLOSE() {
        elevator.setCurrentDoorStatus(Door.CLOSE);
        elevator.setPreviousCommand(Command.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(3));
        elevator.setCurrentDirection(Direction.DOWN);
        User waitingUser = new User(3, Direction.DOWN);
        users.add(waitingUser);
        elevator.logCurrentState("007");
        elevator.updateUserState();
        assertThat(elevator.userWaitingAtCurrentFloor()).isTrue();
        assertThat(elevator.userInsideElevatorNeedToGetOut()).isFalse();
        assertThat(elevator.shouldOpenTheDoor()).isTrue();
    }

    @Test
    public void shouldOpenTheDoor_should_return_true_when_someone_requested_a_stop_at_current_floor_and_previous_command_is_CLOSE() {
        User userRequestingAStopAtFloor3 = new User(2, Direction.UP);
        AtomicInteger currentFloor = new AtomicInteger(3);
        userRequestingAStopAtFloor3.setCurrentFloor(currentFloor.get());
        userRequestingAStopAtFloor3.setState(User.State.TRAVELLING);
        userRequestingAStopAtFloor3.go(currentFloor.get());
        userRequestingAStopAtFloor3.setTickToWait(1);
        userRequestingAStopAtFloor3.setTickToGo(userRequestingAStopAtFloor3.bestTickToGo() + 1);
        users.add(userRequestingAStopAtFloor3);
        elevator.setCurrentFloor(currentFloor);
        assertThat(elevator.stopRequestedAt(currentFloor.get())).isTrue();
        assertThat(elevator.userInsideElevatorNeedToGetOut()).isTrue();
        assertThat(elevator.shouldOpenTheDoor()).isTrue();
    }

    @Test(dataProvider = "providerFor_previousCommand_is_not_CLOSE")
    public void
    shouldOpenTheDoor_should_return_true_when_someone_requested_a_stop_at_current_floor_and_previous_command_is_not_CLOSE(Command previousCommand) {
        User userRequestingAStopAtFloor3 = new User(2, Direction.UP);
        AtomicInteger currentFloor = new AtomicInteger(3);
        userRequestingAStopAtFloor3.setCurrentFloor(currentFloor.get());
        userRequestingAStopAtFloor3.setState(User.State.TRAVELLING);
        userRequestingAStopAtFloor3.go(currentFloor.get());
        users.add(userRequestingAStopAtFloor3);
        elevator.updateUserState();
        elevator.setCurrentFloor(currentFloor);
        elevator.setPreviousCommand(previousCommand);
        assertThat(elevator.stopRequestedAt(currentFloor.get())).isTrue();
        assertThat(elevator.userInsideElevatorNeedToGetOut()).isTrue();
        assertThat(elevator.shouldOpenTheDoor()).isTrue();
    }

    @Test
    public void
    shouldOpenTheDoor_should_return_true_when_someone_is_waiting_at_current_floor_and_someone_requested_a_stop_at_current_floor_and_previous_command_is_CLOSE() {
        User userRequestingAStopAtFloor3 = new User(2, Direction.UP);
        AtomicInteger currentFloor = new AtomicInteger(3);
        userRequestingAStopAtFloor3.setCurrentFloor(currentFloor.get());
        userRequestingAStopAtFloor3.setState(User.State.TRAVELLING);
        userRequestingAStopAtFloor3.go(currentFloor.get());
        userRequestingAStopAtFloor3.setTickToWait(1);
        userRequestingAStopAtFloor3.setTickToGo(userRequestingAStopAtFloor3.bestTickToGo() + 1);
        users.add(userRequestingAStopAtFloor3);
        User waitingUser = new User(3, Direction.DOWN);
        users.add(waitingUser);
        elevator.setCurrentFloor(currentFloor);
        elevator.setPreviousCommand(Command.CLOSE);
        elevator.updateUserState();
        assertThat(elevator.stopRequestedAt(currentFloor.get())).isTrue();
        assertThat(elevator.userInsideElevatorNeedToGetOut()).isTrue();
        assertThat(elevator.userWaitingAtCurrentFloor()).isTrue();
        assertThat(elevator.shouldOpenTheDoor()).isTrue();
    }

    @Test(dataProvider = "providerFor_previousCommand_is_not_CLOSE")
    public void
    shouldOpenTheDoor_should_return_true_when_someone_is_waiting_at_current_floor_and_someone_requested_a_stop_at_current_floor_and_previous_command_is_not_CLOSE(Command previousCommand) {

        User userRequestingAStopAtFloor3 = new User(2, Direction.UP);
        AtomicInteger currentFloor = new AtomicInteger(3);
        userRequestingAStopAtFloor3.setCurrentFloor(currentFloor.get());
        userRequestingAStopAtFloor3.setState(User.State.TRAVELLING);
        userRequestingAStopAtFloor3.go(currentFloor.get());
        userRequestingAStopAtFloor3.setTickToWait(1);
        userRequestingAStopAtFloor3.setTickToGo(userRequestingAStopAtFloor3.bestTickToGo() + 1);
        users.add(userRequestingAStopAtFloor3);
        User waitingUser = new User(3, Direction.DOWN);
        users.add(waitingUser);
        elevator.setCurrentFloor(currentFloor);
        elevator.setPreviousCommand(previousCommand);
        elevator.updateUserState();
        assertThat(elevator.stopRequestedAt(currentFloor.get())).isTrue();
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
    public void nobodyHasCalled_and_nobodyHasRequestedAStop_return_true_when_nobody_inside() {
        assertThat(elevator.nobodyHasCalled()).isTrue();
        assertThat(elevator.nobodyHasRequestedAStop()).isTrue();
    }

    @Test
    public void nobodyHasCalled_should_return_true_when_nobody_has_made_a_call() {
        User userRequestingAStopAtFloor3 = new User(2, Direction.UP);
        AtomicInteger currentFloor = new AtomicInteger(3);
        userRequestingAStopAtFloor3.setCurrentFloor(currentFloor.get());
        userRequestingAStopAtFloor3.setState(User.State.TRAVELLING);
        userRequestingAStopAtFloor3.go(currentFloor.get());
        users.add(userRequestingAStopAtFloor3);
        elevator.setCurrentFloor(currentFloor);
        assertThat(elevator.nobodyHasCalled()).isTrue();
    }

    @Test
    public void nobodyHasCalled_should_return_false_when_someone_has_made_a_call() {
        User userRequestingAStopAtFloor3 = new User(2, Direction.UP);
        AtomicInteger currentFloor = new AtomicInteger(3);
        userRequestingAStopAtFloor3.setCurrentFloor(currentFloor.get());
        userRequestingAStopAtFloor3.setState(User.State.TRAVELLING);
        userRequestingAStopAtFloor3.go(currentFloor.get());
        users.add(userRequestingAStopAtFloor3);
        User waitingUser = new User(4, Direction.DOWN);
        users.add(waitingUser);
        elevator.setCurrentFloor(currentFloor);
        assertThat(elevator.nobodyHasCalled()).isFalse();
    }

    @Test
    public void nobodyHasRequestedAStop_should_return_true_when_nobody_requested_a_stop() {
        AtomicInteger currentFloor = new AtomicInteger(3);
        User waitingUser = new User(3, Direction.DOWN);
        users.add(waitingUser);
        elevator.setCurrentFloor(currentFloor);
        assertThat(elevator.nobodyHasRequestedAStop()).isTrue();
    }

    @Test
    public void nobodyHasRequestedAStop_should_return_false_when_someone_requested_a_stop() {
        User userRequestingAStopAtFloor3 = new User(2, Direction.UP);
        AtomicInteger currentFloor = new AtomicInteger(3);
        userRequestingAStopAtFloor3.setCurrentFloor(currentFloor.get());
        userRequestingAStopAtFloor3.setState(User.State.TRAVELLING);
        userRequestingAStopAtFloor3.go(currentFloor.get());
        users.add(userRequestingAStopAtFloor3);
        User waitingUser = new User(4, Direction.DOWN);
        users.add(waitingUser);
        elevator.setCurrentFloor(currentFloor);
        assertThat(elevator.nobodyHasRequestedAStop()).isFalse();
    }

    @Test
    public void shouldDoNothing_works_as_expected_when_nobody_did_nothing_and_we_are_at_middle_floor() {
        elevator.setCurrentFloor(new AtomicInteger(elevator.getMiddleFloor()));
        assertThat(elevator.shouldDoNothing()).isTrue();
    }

    @Test
    public void shouldDoNothing_works_as_expected_when_nobody_did_nothing_and_we_are_not_at_middle_floor() {
        elevator.setCurrentFloor(new AtomicInteger(0));
        assertThat(elevator.shouldDoNothing()).isFalse();
    }

    @Test
    public void shouldDoNothing_works_as_expected_when_someone_is_waiting() {
        User waitingUser = new User(3, Direction.DOWN);
        users.add(waitingUser);
        assertThat(elevator.shouldDoNothing()).isFalse();
    }

    @Test
    public void shouldDoNothing_works_as_expected_when_someone_requested_a_stop() {
        User userRequestingAStopAtFloor3 = new User(2, Direction.UP);
        AtomicInteger currentFloor = new AtomicInteger(3);
        userRequestingAStopAtFloor3.setCurrentFloor(currentFloor.get());
        userRequestingAStopAtFloor3.setState(User.State.TRAVELLING);
        userRequestingAStopAtFloor3.go(currentFloor.get());
        users.add(userRequestingAStopAtFloor3);
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

    @Test(dataProvider = "directionProvider")
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_true_when_single_direction_for_requested_floor
            (Direction direction) {
        User waitingUser = new User(1, direction);
        users.add(waitingUser);
        elevator.setCurrentDirection(direction);
        elevator.setCurrentFloor(new AtomicInteger(1));
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isTrue();
    }

    @Test(dataProvider = "directionProvider")
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_true_when_both_directions_for_requested_floor
            (Direction direction) {
        User waitingUser1 = new User(1, Direction.DOWN);
        users.add(waitingUser1);
        User waitingUser2 = new User(1, Direction.UP);
        users.add(waitingUser2);
        elevator.setCurrentDirection(direction);
        elevator.setCurrentFloor(new AtomicInteger(1));
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isTrue();
    }

    @Test
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_false_when_nothing_for_current_direction
            () {
        User waitingUser = new User(1, Direction.UP);
        users.add(waitingUser);
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(1));
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isFalse();
    }

    @Test(dataProvider = "directionProvider")
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_false_when_nobody_is_waiting
            (Direction direction) {
        elevator.setCurrentDirection(direction);
        elevator.setCurrentFloor(new AtomicInteger(1));
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isFalse();
    }

    @Test(dataProvider = "providerFor_previousCommand_is_not_CLOSE")
    public void justClosedTheDoor_should_be_false(Command command) {
        elevator.setPreviousCommand(command);
        assertThat(elevator.justClosedTheDoor()).isFalse();
    }

    @Test
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_true_at_top_even_if_not_the_same_direction
            () {
        AtomicInteger currentFloor = new AtomicInteger(elevator.getHigherFloor());
        users.add(new User(currentFloor.get(), Direction.DOWN));
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(currentFloor);
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isTrue();
    }

    @Test
    public void
    userWaitingAtCurrentFloorForCurrentDirection_should_return_true_at_bottom_even_if_not_the_same_direction
            () {
        AtomicInteger currentFloor = new AtomicInteger(elevator.getLowerFloor());
        users.add(new User(currentFloor.get(), Direction.UP));
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(currentFloor);
        assertThat(elevator.userWaitingAtCurrentFloorForCurrentDirection()).isTrue();
    }

    @Test
    public void someoneIsWaitingAtLowerLevels_should_return_true() {
        users.add(new User(1, Direction.UP));
        elevator.setCurrentFloor(new AtomicInteger(3));
        assertThat(elevator.someoneIsWaitingAtLowerLevels()).isTrue();
    }

    @Test
    public void someoneIsWaitingAtLowerLevels_should_return_false() {
        users.add(new User(4, Direction.UP));
        elevator.setCurrentFloor(new AtomicInteger(3));
        assertThat(elevator.someoneIsWaitingAtLowerLevels()).isFalse();
    }

    @Test
    public void someoneRequestedAStopAtLowerLevels_should_return_true() {
        User stopRequestedUser = new User(1, Direction.UP);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(2);
        users.add(stopRequestedUser);
        elevator.setCurrentFloor(new AtomicInteger(3));
        assertThat(elevator.someoneRequestedAStopAtLowerLevels()).isTrue();
    }

    @Test
    public void someoneRequestedAStopAtLowerLevels_should_return_false() {
        User stopRequestedUser = new User(1, Direction.UP);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(4);
        users.add(stopRequestedUser);
        elevator.setCurrentFloor(new AtomicInteger(3));
        assertThat(elevator.someoneRequestedAStopAtLowerLevels()).isFalse();
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
        User stopRequestedUser = new User(1, Direction.UP);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(4);
        users.add(stopRequestedUser);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);

    }

    @Test
    public void getNextDirection_should_be_UP_when_current_dir_is_UP_and_someone_requested_a_stop_at_higher_floor_and_someone_waiting_upstairs() {
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(0));
        User stopRequestedUser = new User(1, Direction.UP);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(4);
        users.add(stopRequestedUser);
        elevator.call(4, Direction.UP);
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
        User stopRequestedUser = new User(1, Direction.UP);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(1);
        elevator.getUsers().add(stopRequestedUser);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);
    }

    @Test
    public void
    getNextDirection_should_be_DOWN_when_current_dir_is_UP_and_someone_requested_a_stop_lower_and_someone_is_waiting_downstairs_and_nobody_requested_something_higher
            () {
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(3));
        elevator.userHasEntered(null);
        User stopRequestedUser = new User(1, Direction.UP);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(1);
        elevator.getUsers().add(stopRequestedUser);
        elevator.call(2, Direction.UP);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);
    }

    @Test
    public void getNextDirection_should_be_UP_when_current_dir_is_UP_and_someone_waiting_upstairs_and_lower_actions_also_needed() {
        elevator.setCurrentDirection(Direction.UP);
        elevator.setCurrentFloor(new AtomicInteger(1));
        elevator.call(3, Direction.UP);
        elevator.call(1, Direction.UP);
        User stopRequestedUser = new User(1, Direction.DOWN);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(0);
        elevator.getUsers().add(stopRequestedUser);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);

    }

    // getNextDirection, currentDir=DOWN
    @Test
    public void getNextDirection_should_be_DOWN_when_current_dir_is_DOWN_and_someone_waiting_downstairs() {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(elevator.getHigherFloor() - 1));
        elevator.call(3, Direction.DOWN);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);
    }

    @Test
    public void getNextDirection_should_be_DOWN_when_current_dir_is_DOWN_and_someone_requested_a_stop_at_lower_floor() {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(elevator.getHigherFloor() - 1));
        elevator.userHasEntered(null);
        User stopRequestedUser = new User(1, Direction.DOWN);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(0);
        elevator.getUsers().add(stopRequestedUser);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);

    }

    @Test
    public void getNextDirection_should_be_DOWN_when_current_dir_is_DOWN_and_someone_requested_a_stop_at_lower_floor_and_someone_waiting_downstairs() {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(elevator.getHigherFloor() - 1));
        elevator.userHasEntered(null);
        elevator.call(3, Direction.UP);
        User stopRequestedUser = new User(1, Direction.DOWN);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(0);
        elevator.getUsers().add(stopRequestedUser);
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
        User stopRequestedUser = new User(1, Direction.DOWN);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(4);
        elevator.getUsers().add(stopRequestedUser);
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
        User stopRequestedUser = new User(1, Direction.DOWN);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(4);
        elevator.getUsers().add(stopRequestedUser);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.UP);
    }

    @Test
    public void getNextDirection_should_be_DOWN_when_current_dir_is_DOWN_and_someone_waiting_downstairs_and_upper_actions_also_needed() {
        elevator.setCurrentDirection(Direction.DOWN);
        elevator.setCurrentFloor(new AtomicInteger(2));
        elevator.call(3, Direction.UP);
        User stopRequestedUser = new User(1, Direction.DOWN);
        stopRequestedUser.setState(User.State.TRAVELLING);
        stopRequestedUser.go(5);
        elevator.getUsers().add(stopRequestedUser);
        elevator.call(1, Direction.UP);
        User anotherStopRequestedUser = new User(1, Direction.DOWN);
        anotherStopRequestedUser.setState(User.State.TRAVELLING);
        anotherStopRequestedUser.go(0);
        elevator.getUsers().add(anotherStopRequestedUser);
        assertThat(elevator.getNextDirection()).isEqualTo(Direction.DOWN);

    }

    @Test
    public void openTheDoor_should_clear_already_done_users() {
        User done1 = new User(3, Direction.DOWN);
        done1.setCurrentFloor(0);
        done1.go(2);
        done1.setState(User.State.DONE);
        done1.setTickToWait(1);
        done1.setTickToGo(done1.bestTickToGo() + 1);
        User w1 = new User(1, Direction.UP);
        w1.setCurrentFloor(0);
        User s1 = new User(5, Direction.DOWN);
        s1.go(1);
        s1.setCurrentFloor(0);
        users.add(done1);
        users.add(w1);
        users.add(s1);
        assertThat(users.size()).isEqualTo(3);
        elevator.openTheDoor();
        assertThat(users.size()).isEqualTo(2);
        assertThat(users.contains(done1)).isFalse();

    }

    @Test
    public void openTheDoor_should_clear_new_done_users() {

        User w1 = new User(1, Direction.UP);
        w1.setCurrentFloor(1);
        User s1 = new User(5, Direction.DOWN);
        s1.go(1);
        s1.setCurrentFloor(1);
        s1.setState(User.State.TRAVELLING);
        s1.setTickToWait(1);
        s1.setTickToGo(s1.bestTickToGo() + 2);
        users.add(w1);
        users.add(s1);
        assertThat(users.size()).isEqualTo(2);
        elevator.openTheDoor();
        assertThat(users.size()).isEqualTo(1);

    }

    @Test
    public void incrementValueForFloor_should_increment_as_expected() {
        Map<Integer, Integer> input = ImmutableMap.<Integer, Integer>builder().put(3, 3).put(1, 0).build();
        Map<Integer, Integer> output1 = S03E01W2Elevator.incrementValueForFloor(input, 3);
        Map<Integer, Integer> output2 = S03E01W2Elevator.incrementValueForFloor(input, 2);
        assertThat(output1.get(3)).isEqualTo(4);
        assertThat(output2.get(2)).isEqualTo(1);
    }

    @Test
    public void aggregateUserInfos_should_be_empty_when_no_user_in_the_system() {
        Map<String, List<CountsByFloorByDirection>> actual = elevator.aggregateUserInfos();
        assertThat(actual.get(S03E01W2Elevator.WAITING_LIST)).isEmpty();
        assertThat(actual.get(S03E01W2Elevator.STOP_LIST)).isEmpty();
    }

    @Test
    public void aggregateUserInfos_should_work() {
        User waitingUser1 = new User(1, Direction.DOWN);
        users.add(waitingUser1);
        User waitingUser2 = new User(3, Direction.UP);
        users.add(waitingUser2);
        users.add(new User(3, Direction.UP));
        User stop1 = new User(4, Direction.DOWN);
        stop1.go(3);
        stop1.setState(User.State.TRAVELLING);
        users.add(stop1);
        User stop2 = new User(2, Direction.DOWN);
        stop2.go(0);
        stop2.setState(User.State.TRAVELLING);
        users.add(stop2);

        Map<String, List<CountsByFloorByDirection>> actual = elevator.aggregateUserInfos();
        List<CountsByFloorByDirection> waitingList = actual.get(S03E01W2Elevator.WAITING_LIST);
        assertThat(waitingList).hasSize(2);
        for (CountsByFloorByDirection c : waitingList) {
            if (c.getFloor() == 1) {
                assertThat(c.getCountByDirection().get(Direction.DOWN)).isEqualTo(1);
                assertThat(c.getCountByDirection().get(Direction.UP)).isEqualTo(0);
            } else if (c.getFloor() == 3) {
                assertThat(c.getCountByDirection().get(Direction.DOWN)).isEqualTo(0);
                assertThat(c.getCountByDirection().get(Direction.UP)).isEqualTo(2);
            } else {
                throw new IllegalStateException("unexpected floor value <" + c.getFloor() + ">");
            }
        }

        List<CountsByFloorByDirection> stopList = actual.get(S03E01W2Elevator.STOP_LIST);
        assertThat(stopList).hasSize(2);
        for (CountsByFloorByDirection c : stopList) {
            if (c.getFloor() == 3) {
                assertThat(c.getCountByDirection().get(Direction.DOWN)).isEqualTo(1);
                assertThat(c.getCountByDirection().get(Direction.UP)).isEqualTo(0);
            } else if (c.getFloor() == 0) {
                assertThat(c.getCountByDirection().get(Direction.DOWN)).isEqualTo(1);
                assertThat(c.getCountByDirection().get(Direction.UP)).isEqualTo(0);
            } else {
                throw new IllegalStateException("unexpected floor value <" + c.getFloor() + ">");
            }
        }

    }

    @Test
    public void getDirectionFromGoInfo_should_return_UP() {
        elevator.setCurrentFloor(new AtomicInteger(3));
        assertThat(elevator.getDirectionFromGoInfo(4)).isEqualTo(Direction.UP);
    }

    @Test
    public void getDirectionFromGoInfo_should_return_DOWN() {
        elevator.setCurrentFloor(new AtomicInteger(3));
        assertThat(elevator.getDirectionFromGoInfo(1)).isEqualTo(Direction.DOWN);
    }

    @Test(dataProvider = "directionProvider")
    public void getDirectionFromGoInfo_should_return_currentDirection(Direction currentDirection) {
        AtomicInteger currentFloor = new AtomicInteger(3);
        elevator.setCurrentFloor(currentFloor);
        elevator.setCurrentDirection(currentDirection);
        assertThat(elevator.getDirectionFromGoInfo(currentFloor.get())).isEqualTo(currentDirection);
    }

    @Test(dataProvider = "providerFor_userRequestedAStopFor_should_work")
    public void userRequestedAStopFor_should_work(User user, Integer currentFloor, Direction elevatorDirection, Integer floorToGo, Integer expectedFloorToGo) {
        elevator.setCurrentDirection(elevatorDirection);
        elevator.setCurrentFloor(new AtomicInteger(currentFloor));
        users.add(user);
        elevator.userRequestedAStopFor(floorToGo);
        assertThat(user.getFloorToGo()).isEqualTo(expectedFloorToGo);

    }

    // DATAPROVIDERS

    @DataProvider(name = "providerFor_userRequestedAStopFor_should_work")
    public Object[][] providerFor_userRequestedAStopFor_should_work() {
        User u1 = new User(2, UP);
        u1.setState(User.State.TRAVELLING);
        User u2 = new User(2, UP);
        u2.setState(User.State.TRAVELLING);
        User u3 = new User(3, DOWN);
        u3.setState(User.State.TRAVELLING);
        u3.go(2);
        User w1 = new User(4, DOWN);
        return new Object[][]{
                {u1, 2, UP, 4, 4}, // traveling user, willing to go at 4th floor and elevator is at waiting floor
                {u2, 1, UP, 4, Integer.MIN_VALUE},// traveling user, willing to go at 4th floor and elevator is not at waiting floor
                {u3, 3, UP, 4, 2}, // traveling user,  with floorToGo already set and elevator is at waiting floor
                {w1, 3, UP, 4, Integer.MIN_VALUE}, // waiting user
        };
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

    @DataProvider(name = "directionProvider")
    public Object[][] directionProvider() {
        return new Object[][]{{DOWN}, {UP}};
    }


}
