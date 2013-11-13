package codestory.core;

import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.experimental.Builder;

import java.util.List;
import java.util.Map;

/**
 * User: cfurmaniak
 * Date: 04/11/13
 * Time: 20:59
 */
@Builder
@Getter
public class ElevatorContext {
    private final String source;
    private final int tick;
    private final int lowerFloor;
    private final int higherFloor;
    private final int currentFloor;
    private final int previousFloor;
    private final int middleFloor;
    private final int currentNbOfUsersInsideTheElevator;
    private final Command previousCommand;
    private final Direction previousDirection;
    private final Direction currentDirection;
    private final boolean someoneIsWaitingAtLowerLevels;
    private final boolean someoneIsWaitingAtUpperLevels;
    private final boolean someoneRequestedAStopAtLowerLevels;
    private final boolean someoneRequestedAStopAtUpperLevels;
    private final boolean userWaitingAtCurrentFloor;
    private final boolean userInsideElevatorNeedToGetOut;
    private final Door currentDoorStatus;
    private final Map<Integer, String> lastCommands;
    private final List<User> users;



}