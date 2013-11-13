package codestory.core.engine;

import codestory.core.Command;
import codestory.core.Direction;
import codestory.core.Door;
import codestory.core.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import codestory.core.engine.ElevatorEngine;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: cfurmaniak
 * Date: 31/10/13
 * Time: 20:05
 */
@Slf4j
@Setter(AccessLevel.PROTECTED)
@Getter(AccessLevel.PROTECTED)
public class S03E01W1Elevator implements ElevatorEngine {

    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new GuavaModule());
    public static final int LAST_COMMANDS_QUEUE_SIZE = 10;
    public static final int DEFAULT_LOWER_FLOOR = 0;
    public static final int DEFAULT_HIGHER_FLOOR = 5;
    public static final int DEFAULT_CABIN_SIZE = 30;
    protected Map<Integer, String> lastCommands = initLastCommandQueue();
    private AtomicInteger ticks = new AtomicInteger(0);
    @Getter
    private Integer lowerFloor = new Integer(0);
    @Getter
    private Integer higherFloor = new Integer(0);
    private AtomicInteger currentNbOfUsersInsideTheElevator = new AtomicInteger();
    private AtomicInteger currentFloor = new AtomicInteger();
    private AtomicInteger previousFloor = new AtomicInteger();
    private Multimap<Integer, Direction> userWaitingByFloor = ArrayListMultimap.create();
    private Multimap<Integer, Direction> stopRequestedByFloor = ArrayListMultimap.create();
    private Direction currentDirection;
    private Command previousCommand;
    private Door currentDoorStatus;
    private int middleFloor;
    private int maxCapacity;

    public S03E01W1Elevator() {
        reset("self initializing", DEFAULT_LOWER_FLOOR, DEFAULT_HIGHER_FLOOR, DEFAULT_CABIN_SIZE);
    }

    public static Map<Integer, String> initLastCommandQueue() {
        return new LinkedHashMap<Integer, String>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                return this.size() > LAST_COMMANDS_QUEUE_SIZE;
            }
        };
    }

    public ElevatorEngine reset(String cause, int lowerFloor, int higherFloor, int cabinSize) {
        if (ticks.get() > 0) {
            S03E01W1Elevator.log.warn("RESET, cause: <{]>", cause);
            logCurrentState("reset, cause:" + cause);
        }
        lastCommands = initLastCommandQueue();
        this.lowerFloor = lowerFloor;
        this.higherFloor = higherFloor;
        this.middleFloor = evaluateMiddleFloor();
        // pour le moment, c'est plutôt le nb max de personne qu'on veut transporter pour éviter de s'arrêter trop
        // souvent et tenter de marquer plus de points (PAS IMPLEMENTE!!!)
        maxCapacity = higherFloor - lowerFloor;
        //
        currentNbOfUsersInsideTheElevator.set(0);
        currentFloor.set(0);
        previousFloor.set(0);
        currentDirection = Direction.UP;
        previousCommand = Command.NOTHING;
        currentDoorStatus = Door.CLOSE;
        userWaitingByFloor = ArrayListMultimap.create();
        stopRequestedByFloor = ArrayListMultimap.create();
        return this;
    }

    @Override
    public Command nextCommand() {
        ticks.incrementAndGet();

        logCurrentState("nextCommand(before processing), previousCommand: <" + previousCommand + ">");
        Command nextCommand;
        if (shouldDoNothing()) {
            nextCommand = Command.NOTHING;
        } else if (shouldCloseTheDoor()) {
            currentDoorStatus = Door.CLOSE;
            nextCommand = Command.CLOSE;
        } else if (shouldOpenTheDoor()) {
            currentDoorStatus = Door.OPEN;
            nextCommand = Command.OPEN;
        } else {
            Direction nextDirection = getNextDirection();
            if (nextDirection == Direction.UP) {
                previousFloor.set(currentFloor.getAndIncrement());
                currentDirection = Direction.UP;
                nextCommand = Command.UP;
            } else {
                previousFloor.set(currentFloor.getAndDecrement());
                currentDirection = Direction.DOWN;
                nextCommand = Command.DOWN;
            }
        }
        lastCommands.put(ticks.get(), currentFloor + ":" + nextCommand);
        logCurrentState("nextCommand (after processing) <" + nextCommand + ">");
        previousCommand = nextCommand;

        return nextCommand;
    }

    public ElevatorEngine call(Integer atFloor, Direction to) {
        checkFloorValue(atFloor);
        checkNotNull(to, "'to' cannot be null");
        S03E01W1Elevator.log.info("call(atFloor:{}, to:{})", atFloor, to);
        synchronized (userWaitingByFloor) {
            userWaitingByFloor.put(atFloor, to);
        }
        return this;
    }

    public ElevatorEngine go(Integer floorToGo) {
        S03E01W1Elevator.log.info("go(floorToGo:{})", floorToGo);
        checkFloorValue(floorToGo);
        synchronized (stopRequestedByFloor) {
            stopRequestedByFloor.put(floorToGo, getDirectionFromGoInfo(floorToGo));
        }
        return this;
    }

    public ElevatorEngine userHasEntered(User user) {
        logCurrentState("userHasEntered(" + user + ")-before");
        currentNbOfUsersInsideTheElevator.incrementAndGet();
/*        synchronized (userWaitingByFloor) {
            if (someoneIsWaitingAt(currentFloor.get())) {
                userWaitingByFloor.removeAll(currentFloor.get());
            } else if (someoneIsWaitingAt(previousFloor.get())) {
                log.info("userHasEntered({}) => could not deregister a wait at currentFloor <{}>, did it at previousFloor <{}>", user, currentFloor,
                        previousFloor);
                userWaitingByFloor.removeAll(previousFloor.get());
            } else {
                log.error("userHasEntered(): we are doomed (WTF?), no user should have entered neither at currentFloor <{}> nor at previousFloor <{}>",
                        currentFloor, previousFloor);
            }

        }*/
//        logCurrentState("userHasEntered(" + user + ")-after");
        return this;
    }

    public ElevatorEngine userHasExited(User user) {
        logCurrentState("userHasExited(" + user + ")-before");
        currentNbOfUsersInsideTheElevator.decrementAndGet();
/*        synchronized (stopRequestedByFloor) {
            if (stopRequestedAt(currentFloor.get())) {
                stopRequestedByFloor.removeAll(currentFloor.get());
            } else if (stopRequestedAt(previousFloor.get())) {
                log.info("userHasExited({}) => could not deregister stop at currentFloor <{}>, did it at previousFloor <{}>", user, currentFloor, previousFloor);
                stopRequestedByFloor.removeAll(previousFloor.get());
            } else {
                log.error("userHasExited(): we are doomed (WTF?), no user should have exited neither at currentFloor <{}> nor at previousFloor <{}>",
                        currentFloor, previousFloor);
            }
        }*/
//        logCurrentState("userHasExited(" + user + ")-after");
        return this;
    }

    @VisibleForTesting
    protected boolean shouldDoNothing() {
        // retourner au milieu si rien à faire
        return nobodyHasCalled() && nobodyHasRequestedAStop() && currentFloor.get() == middleFloor;
    }

    @VisibleForTesting
    protected boolean shouldCloseTheDoor() {
        return currentDoorStatus == Door.OPEN;
    }

    @VisibleForTesting
    protected boolean shouldOpenTheDoor() {
        boolean openTheDoor = false;
        if (currentDoorStatus == Door.CLOSE) {
//            log.info("shouldOpenTheDoor (maybe)=> Door.CLOSE");
            // case A
            if (userWaitingAtCurrentFloorForCurrentDirection() && !justClosedTheDoor()) {
                // case A
//                log.info("shouldOpenTheDoor (yes)=> userWaitingAtCurrentFloor() && previousCommand != Command.CLOSE");
                clearWaitingListForCurrentFloor();
                if (userInsideElevatorNeedToGetOut()) {
                    clearStopListForCurrentFloor();
                }

                openTheDoor = true;
            } else if (userInsideElevatorNeedToGetOut()) {
                // case B
//                log.info("shouldOpenTheDoor (yes)=> userInsideElevatorNeedToGetOut()");
                clearStopListForCurrentFloor();
                if (userWaitingAtCurrentFloor()) {
                    clearWaitingListForCurrentFloor();
                }
                openTheDoor = true;
            } else if (!justClosedTheDoor() && userWaitingAtCurrentFloor()) {
                if (currentDirection == Direction.DOWN) {
                    // case C
                    if (!someoneIsWaitingAtLowerLevels(currentFloor.get())) {
                        clearWaitingListForCurrentFloor();
                        if (userInsideElevatorNeedToGetOut()) {
                            clearStopListForCurrentFloor();
                        }
                        openTheDoor = true;
                    }
                } else if (currentDirection == Direction.UP) {
                    // case D
                    if (!someoneIsWaitingAtUpperLevels(currentFloor.get())) {
                        clearWaitingListForCurrentFloor();
                        if (userInsideElevatorNeedToGetOut()) {
                            clearStopListForCurrentFloor();
                        }
                        openTheDoor = true;
                    }
                }

            } else {
//                log.info("shouldOpenTheDoor (finally no)=> else Door.CLOSE");
                openTheDoor = false;
            }
        }
        return openTheDoor;
    }

    @VisibleForTesting
    protected void clearStopListForCurrentFloor() {
        synchronized (stopRequestedByFloor) {

            Collection removed = stopRequestedByFloor.removeAll(currentFloor.get());
            if (removed.isEmpty()) {
                S03E01W1Elevator.log.error("clearStopListForCurrentFloor(): asked to remove stops for floor <{}> but did not remove anything, WTF?", currentFloor);
            } else {
                S03E01W1Elevator.log.info("clearStopListForCurrentFloor({}) => <{}>", currentFloor, removed.size());
            }
        }
    }

    @VisibleForTesting
    protected void clearWaitingListForCurrentFloor() {
        synchronized (userWaitingByFloor) {
            Collection removed = userWaitingByFloor.removeAll(currentFloor.get());
            if (removed.isEmpty()) {
                S03E01W1Elevator.log.error("clearWaitingListForCurrentFloor(): asked to remove waiting users for floor <{}> but did not remove anything, WTF?", currentFloor);
            } else {
                S03E01W1Elevator.log.info("clearWaitingListForCurrentFloor({}) => <{}>", currentFloor, removed.size());
            }
        }
    }

    @VisibleForTesting
    protected boolean justClosedTheDoor() {
        return previousCommand == Command.CLOSE;
    }

    @VisibleForTesting
    protected Direction getDirectionFromGoInfo(Integer floorToGo) {
        if (floorToGo - currentFloor.get() > 0) {
            return Direction.UP;
        } else if (floorToGo - currentFloor.get() < 0) {
            return Direction.DOWN;
        } else {
            return currentDirection;
        }
    }

    @VisibleForTesting
    protected boolean userWaitingAtCurrentFloor() {
        synchronized (userWaitingByFloor) {
            return userWaitingByFloor.get(currentFloor.get()).size() > 0;
        }
    }

    @VisibleForTesting
    protected boolean userWaitingAtCurrentFloorForCurrentDirection() {
        boolean v = false;
        synchronized (userWaitingByFloor) {
            // si on est au premier ou au dernier étage, on ne tient pas compte de la direction
            if (currentFloor.get() == higherFloor || currentFloor.get() == lowerFloor) {
                if (userWaitingAtCurrentFloor()) {
                    v = true;
                }
            } else if (userWaitingByFloor.get(currentFloor.get()).contains(currentDirection)) {
                v = true;
            }
        }
        return v;
    }

    @VisibleForTesting
    protected boolean userInsideElevatorNeedToGetOut() {
        return stopRequestedAt(currentFloor.get());
    }

    @VisibleForTesting
    protected boolean elevatorIsAtTop() {
        return currentFloor.get() == higherFloor;
    }

    @VisibleForTesting
    protected boolean elevatorIsAtBottom() {
        return currentFloor.get() == lowerFloor;
    }

    @VisibleForTesting
    protected Direction getNextDirection() {
        Direction direction = currentDirection;
        if (elevatorIsAtBottom()) {
            //log.info("elevatorIsAtBottom");
            direction = Direction.UP;
        } else if (elevatorIsAtTop()) {
            //log.info("elevatorIsAtTop");
            direction = Direction.DOWN;
        } else if (nobodyHasCalled() && nobodyHasRequestedAStop()) {
            //log.info("nobodyHasCalled():{} && nobodyHasRequestedAStop(): {}",nobodyHasCalled(),nobodyHasRequestedAStop());
            if (currentFloor.get() > middleFloor) {
                //log.info("currentFloor.get() > middleFloor");
                direction = Direction.DOWN;
            } else {
                //log.info("ELSE(currentFloor.get() > middleFloor)");
                direction = Direction.UP;
            }
        } else if (currentDirection == Direction.UP) {
            //log.info("currentDirection == Direction.UP");
            if (someoneIsWaitingAtUpperLevels(currentFloor.get()) || someoneRequestedAStopAtUpperLevels(currentFloor
                    .get())) {
                direction = Direction.UP;
            } else {
                direction = Direction.DOWN;
            }
        } else if (currentDirection == Direction.DOWN) {
            //log.info("currentDirection == Direction.DOWN");
            if (someoneIsWaitingAtLowerLevels(currentFloor.get()) || someoneRequestedAStopAtLowerLevels(currentFloor
                    .get())) {
                //log.info("something to do downstairs");
                direction = Direction.DOWN;
            } else {
                //log.info("NOthing to do downstairs");
                direction = Direction.UP;
            }
        } else {
            S03E01W1Elevator.log.warn("getNextDirection(): using safe DEFAULT <{}>", direction);
        }

        return direction;
    }

    @VisibleForTesting
    protected boolean someoneIsWaitingAtLowerLevels(Integer currentFloor) {
        boolean someoneIsWaitingAtALowerLevel = false;
        synchronized (userWaitingByFloor) {
            someoneIsWaitingAtALowerLevel = false;
            for (int l = lowerFloor; l < currentFloor; l++) {
                if (userWaitingByFloor.containsKey(l)) {
                    someoneIsWaitingAtALowerLevel = true;
                    break;
                }
            }
        }
        return someoneIsWaitingAtALowerLevel;
    }

    @VisibleForTesting
    protected boolean someoneRequestedAStopAtLowerLevels(Integer currentFloor) {
        boolean someoneRequestedAStopAtALowerLevel = false;
        synchronized (stopRequestedByFloor) {
            someoneRequestedAStopAtALowerLevel = false;
            for (int l = lowerFloor; l < currentFloor; l++) {
                if (stopRequestedByFloor.containsKey(l)) {
                    someoneRequestedAStopAtALowerLevel = true;
                    break;
                }
            }
        }
        return someoneRequestedAStopAtALowerLevel;
    }

    @VisibleForTesting
    protected boolean someoneIsWaitingAtUpperLevels(Integer currentFloor) {
        boolean someoneIsWaitingAtUpperLevels = false;
        if (currentFloor != higherFloor) {
            synchronized (userWaitingByFloor) {
                someoneIsWaitingAtUpperLevels = false;
                for (int l = currentFloor + 1; l <= higherFloor; l++) {
                    if (userWaitingByFloor.containsKey(l)) {
                        someoneIsWaitingAtUpperLevels = true;
                        break;
                    }
                }
            }
        }
        return someoneIsWaitingAtUpperLevels;
    }

    @VisibleForTesting
    protected boolean someoneRequestedAStopAtUpperLevels(Integer currentFloor) {
        boolean someoneRequestedAStopAtUpperLevels = false;
        if (currentFloor != higherFloor) {
            synchronized (stopRequestedByFloor) {
                someoneRequestedAStopAtUpperLevels = false;
                for (int l = currentFloor + 1; l <= higherFloor; l++) {
                    if (stopRequestedByFloor.containsKey(l)) {
                        someoneRequestedAStopAtUpperLevels = true;
                        break;
                    }
                }
            }
        }
        return someoneRequestedAStopAtUpperLevels;
    }

    @VisibleForTesting
    protected boolean nobodyHasCalled() {
        boolean empty = false;
        synchronized (userWaitingByFloor) {
            empty = userWaitingByFloor.isEmpty();
        }
        return empty;
    }

    @VisibleForTesting
    protected boolean nobodyHasRequestedAStop() {
        boolean answer = false;
        synchronized (stopRequestedByFloor) {
            answer = stopRequestedByFloor.isEmpty();
        }
        return answer;
    }

    @VisibleForTesting
    protected boolean stopRequestedAt(int floor) {
        return stopRequestedByFloor.containsKey(floor);
    }

    @VisibleForTesting
    protected boolean someoneIsWaitingAt(int floor) {
        return userWaitingByFloor.containsKey(floor);
    }

    @VisibleForTesting
    protected void checkFloorValue(int floorValue) {
        checkArgument(floorValue >= lowerFloor, "'atFloor' cannot be negative");
        checkArgument(floorValue <= higherFloor, "'atFloor' cannot be more than the total nb of floors <" +
                higherFloor + ">");
    }

    @VisibleForTesting
    protected String logWaitingListContent() {
        String value;
        synchronized (userWaitingByFloor) {
            StringBuilder stringBuilder = new StringBuilder();
            Set<Integer> keySet = userWaitingByFloor.keySet();
            if (keySet.size() > 0) {
                for (int floor : keySet) {
                    int[] dCount = getByFloorByDirection(userWaitingByFloor, floor);
                    stringBuilder.append("f=").append(floor).append(",w=").append(Direction.DOWN).append(":").append
                            (dCount[0]).append('_').append(Direction.UP).append(":").append(dCount[1]).append("#");
                }
                value = stringBuilder.toString();
            } else {
                value = "w=empty";
            }
        }
        return value;
    }

    @VisibleForTesting
    protected String logRequestedStops() {
        String value;
        synchronized (stopRequestedByFloor) {
            StringBuilder stringBuilder = new StringBuilder();
            Set<Integer> keySet = stopRequestedByFloor.keySet();
            if (keySet.size() > 0) {
                for (int floor : keySet) {
                    int[] dCount = getByFloorByDirection(stopRequestedByFloor, floor);
                    stringBuilder.append("f=").append(floor).append(",s=").append(Direction.DOWN).append(":").append
                            (dCount[0]).append('_').append(Direction.UP).append(":").append(dCount[1]).append("#");
                }
                value = stringBuilder.toString();
            } else {
                value = "s=empty";
            }
        }
        return value;
    }

    protected int[] getByFloorByDirection(Multimap<Integer, Direction> multimap, int floor) {
        int nbDown = 0;
        int nbUp = 0;
        Collection<Direction> directions = multimap.get(floor);
        for (Direction direction : directions) {
            if (direction == Direction.DOWN) {
                nbDown++;
            } else {
                nbUp++;
            }
        }
        return new int[]{nbDown, nbUp};
    }

    @VisibleForTesting
    protected void logCurrentState(String from) {
//        ElevatorContext context = ElevatorContext.builder()
//                .source(from)
//                .tick(ticks.get())
//                .currentFloor(currentFloor.get())
//                .previousFloor(previousFloor.get())
//                .middleFloor(middleFloor)
//                .currentNbOfUsersInsideTheElevator(currentNbOfUsersInsideTheElevator.get())
//                .previousCommand(previousCommand)
//                .currentDirection(currentDirection)
//                .someoneIsWaitingAtLowerLevels(someoneIsWaitingAtLowerLevels(currentFloor.get()))
//                .someoneIsWaitingAtUpperLevels(someoneIsWaitingAtUpperLevels(currentFloor.get()))
//                .someoneRequestedAStopAtLowerLevels(someoneRequestedAStopAtLowerLevels(currentFloor.get()))
//                .someoneRequestedAStopAtUpperLevels(someoneRequestedAStopAtUpperLevels(currentFloor.get()))
//                .userWaitingAtCurrentFloor(userWaitingAtCurrentFloor())
//                .userInsideElevatorNeedToGetOut(userInsideElevatorNeedToGetOut())
//                .currentDoorStatus(currentDoorStatus)
//                .waitingList(userWaitingByFloor)
//                .stopList(stopRequestedByFloor)
//                .lastCommands(lastCommands)
//                .build();
//
//        try {
//            log.info(MAPPER.writeValueAsString(context));
//        } catch (JsonProcessingException e) {
//            log.error("could not process context to json for tick <{}>", ticks.get(), e);
//        }
//

        S03E01W1Elevator.log.info("logCurrentState(from:{}): tick <{}>, floor: <{}>, previousFloor: <{}>, middleFloor: <{}>, nbOfPassengers <{}>, " +
                "previousCommand <{}>, " +
                "currentDirection <{}>, someoneIsWaitingAtLowerLevels <{}>, someoneIsWaitingAtUpperLevels<{}>, userWaitingAtCurrentFloor<{}>, " +
                "userInsideElevatorNeedToGetOut<{}>, currentDoorStatus <{}>, waitingList <{}>, stopList <{}>, lastCommands <{}>",
                from, ticks.get(), currentFloor, previousFloor, middleFloor,
                currentNbOfUsersInsideTheElevator,
                previousCommand,
                currentDirection, someoneIsWaitingAtLowerLevels(currentFloor.get()), someoneIsWaitingAtUpperLevels(currentFloor.get()), userWaitingAtCurrentFloor(),
                userInsideElevatorNeedToGetOut(), currentDoorStatus, logWaitingListContent(), logRequestedStops(), lastCommandsAsString());
    }

    protected String lastCommandsAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        synchronized (lastCommands) {
            for (Map.Entry<Integer, String> entry : lastCommands.entrySet()) {
                stringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("#");
            }
        }
        return stringBuilder.toString();
    }

    protected int evaluateMiddleFloor() {
        return (higherFloor - lowerFloor) / 2 + lowerFloor;
    }
}
