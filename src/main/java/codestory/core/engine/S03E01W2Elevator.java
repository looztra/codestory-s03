package codestory.core.engine;

import codestory.core.Command;
import codestory.core.CountsByFloorByDirection;
import codestory.core.Direction;
import codestory.core.Door;
import codestory.core.ElevatorContext;
import codestory.core.Score;
import codestory.core.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.*;

/**
 * User: cfurmaniak
 * Date: 31/10/13
 * Time: 20:05
 */
@Slf4j
@Setter(AccessLevel.PROTECTED)
@Getter(AccessLevel.PROTECTED)
public class S03E01W2Elevator implements ElevatorEngine {

    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new GuavaModule());
    public static final int LAST_COMMANDS_QUEUE_SIZE = 10;
    public static final int DEFAULT_LOWER_FLOOR = 0;
    public static final int DEFAULT_HIGHER_FLOOR = 5;
    public static final int DEFAULT_CABIN_SIZE = 30;
    public static final String WAITING_LIST = "waitingList";
    public static final String STOP_LIST = "stopList";
    protected Map<Integer, String> lastCommands = initLastCommandQueue();
    private Score score;
    private AtomicInteger ticks = new AtomicInteger(0);
    @Getter
    private Integer lowerFloor = new Integer(0);
    @Getter
    private Integer higherFloor = new Integer(0);
    private AtomicInteger currentNbOfUsersInsideTheElevator = new AtomicInteger();
    private AtomicInteger currentFloor = new AtomicInteger();
    private AtomicInteger previousFloor = new AtomicInteger();
    private List<codestory.core.User> users;
    private Direction currentDirection;
    private Command previousCommand;
    private Door currentDoorStatus;
    private int middleFloor;
    private int cabinSize;
    private String lastResetCause;
    private ElevatorContext lastResetContext;


    public S03E01W2Elevator() {
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

    public static Map<Integer, Integer> incrementValueForFloor(Map<Integer, Integer> input, Integer floor) {
        Integer count;
        if (input.containsKey(floor)) {
            count = input.get(floor) + 1;
        } else {
            count = 1;
        }
        Map<Integer, Integer> tmp = Maps.newHashMap(input);
        tmp.put(floor, count);
        return ImmutableMap.<Integer, Integer>builder().putAll(tmp).build();
    }

    public ElevatorEngine reset(String cause, int lowerFloor, int higherFloor, int cabinSize) {
        if (ticks.get() > 0) {
            S03E01W2Elevator.log.warn("RESET, cause: <{}>, lowerFloor: <{}>, higherFloor: <{}>, cabinSize: <{}>",
                    cause, lowerFloor, higherFloor, cabinSize);
            logCurrentState("reset, cause:" + cause);
            lastResetCause = cause;
            lastResetContext = getCurrentElevatorContext();
        }
        score = new Score();
        lastCommands = initLastCommandQueue();
        this.lowerFloor = lowerFloor;
        this.higherFloor = higherFloor;
        this.cabinSize = cabinSize;
        this.middleFloor = evaluateMiddleFloor();
        users = new ArrayList<>();
        currentNbOfUsersInsideTheElevator.set(0);
        currentFloor.set(0);
        previousFloor.set(0);
        currentDirection = Direction.UP;
        previousCommand = Command.NOTHING;
        currentDoorStatus = Door.CLOSE;
        return this;
    }

    @Override
    public Command nextCommand() {
        ticks.incrementAndGet();
        updateUserState();
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
        tickForUsers();
        logCurrentState("nextCommand (after processing) <" + nextCommand + ">");
        previousCommand = nextCommand;

        return nextCommand;
    }

    public ElevatorEngine call(Integer atFloor, Direction to) {
        checkFloorValue(atFloor);
        checkNotNull(to, "'to' cannot be null");
        S03E01W2Elevator.log.info("call(atFloor:{}, to:{})", atFloor, to);
        registerNewUser(new User(atFloor, to));
        return this;
    }

    public ElevatorEngine go(Integer floorToGo) {
        S03E01W2Elevator.log.info("go(floorToGo:{})", floorToGo);
        checkFloorValue(floorToGo);
        userRequestedAStopFor(floorToGo);
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

    public String getState() {
        String jsonState = "UNDEF";
        ElevatorContext context = ElevatorContext.builder()
                .source("getState")
                .score(score)
                .tick(ticks.get())
                .lowerFloor(lowerFloor)
                .higherFloor(higherFloor)
                .currentFloor(currentFloor.get())
                .previousFloor(previousFloor.get())
                .middleFloor(middleFloor)
                .currentNbOfUsersInsideTheElevator(currentNbOfUsersInsideTheElevator.get())
                .previousCommand(previousCommand)
                .currentDirection(currentDirection)
                .someoneIsWaitingAtLowerLevels(someoneIsWaitingAtLowerLevels())
                .someoneIsWaitingAtUpperLevels(someoneIsWaitingAtUpperLevels())
                .someoneRequestedAStopAtLowerLevels(someoneRequestedAStopAtLowerLevels())
                .someoneRequestedAStopAtUpperLevels(someoneRequestedAStopAtUpperLevels())
                .userWaitingAtCurrentFloor(userWaitingAtCurrentFloor())
                .userInsideElevatorNeedToGetOut(userInsideElevatorNeedToGetOut())
                .currentDoorStatus(currentDoorStatus)
                .lastCommands(lastCommands)
                .users(users)
                .build();

        try {
            jsonState = MAPPER.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            log.error("could not process context to json for tick <{}>", ticks.get(), e);
        }
        return jsonState;
    }




    @VisibleForTesting
    protected void updateUserState() {
        synchronized (users) {
            for (User user : users) {
                user.setCurrentFloor(currentFloor.get());
            }
        }
    }

    @VisibleForTesting
    protected void tickForUsers() {
        synchronized (users) {
            for (User user : users) {
                user.tick();
            }
        }
    }

    @VisibleForTesting
    protected void registerNewUser(User user) {
        synchronized (users) {
            users.add(user);
        }
    }

    @VisibleForTesting
    protected void userRequestedAStopFor(int floorToGo) {
        synchronized (users) {
            for (User user : users) {
                if (user.traveling() && user.didNotRequestedAStopYet()) {
                    user.go(floorToGo);
                    break;
                }
            }
        }
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
                openTheDoor();
                openTheDoor = true;
            } else if (userInsideElevatorNeedToGetOut()) {
                // case B
//                log.info("shouldOpenTheDoor (yes)=> userInsideElevatorNeedToGetOut()");
                openTheDoor();
                openTheDoor = true;
            } else if (!justClosedTheDoor() && userWaitingAtCurrentFloor()) {
                if (currentDirection == Direction.DOWN) {
                    // case C
                    if (!someoneIsWaitingAtLowerLevels()) {
                        openTheDoor();
                        openTheDoor = true;
                    }
                } else if (currentDirection == Direction.UP) {
                    // case D
                    if (!someoneIsWaitingAtUpperLevels()) {
                        openTheDoor();
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
    protected void openTheDoor() {
        log.info("openTheDoor");
        List<User> doneUsers = new ArrayList<>();
        for (User user : users) {
            user.elevatorIsOpen(currentFloor.get());
            if (user.done()) {
                try {
                    score = score.success(user);
                    log.info("openTheDoor(): score for user <{}> is <{}>, totalScore is <{}>", user, Score.score(user),
                            score.getScore());
                } catch (IllegalStateException e) {
                    log.info("openTheDoor(): caught IllegalStateException <{}> while computing score for user <{}>", e.getMessage(), user.toString());
                }
                doneUsers.add(user);
            }
        }
        users.removeAll(doneUsers);
    }

    @VisibleForTesting
    protected boolean justClosedTheDoor() {
        return previousCommand == Command.CLOSE;
    }

    @VisibleForTesting
    protected boolean userWaitingAtCurrentFloor() {
        return nbUserWaitingAtCurrentFloor() > 0;
    }

    @VisibleForTesting
    protected int nbUserWaitingAtCurrentFloor() {
        int nb = 0;
        synchronized (users) {
            for (User user : users) {
                if (user.waiting() && user.getInitialFloor() == currentFloor.get()) {
                    nb++;
                }
            }
        }
        return nb;
    }

    @VisibleForTesting
    protected int nbUserWaitingAtCurrentFloorForCurrentDirection() {
        int nb = 0;
        synchronized (users) {
            for (User user : users) {
                if (user.waiting() && user.elevatorIsAtWaitingFloor(currentFloor.get()) && (user.getDirection() ==
                        currentDirection || currentFloor.get() == lowerFloor || currentFloor.get() == higherFloor)) {
                    nb++;
                }
            }
        }
        return nb;
    }

    @VisibleForTesting
    protected boolean userWaitingAtCurrentFloorForCurrentDirection() {
        return nbUserWaitingAtCurrentFloorForCurrentDirection() > 0;
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
            if (someoneIsWaitingAtUpperLevels() || someoneRequestedAStopAtUpperLevels()) {
                direction = Direction.UP;
            } else {
                direction = Direction.DOWN;
            }
        } else if (currentDirection == Direction.DOWN) {
            //log.info("currentDirection == Direction.DOWN");
            if (someoneIsWaitingAtLowerLevels() || someoneRequestedAStopAtLowerLevels()) {
                //log.info("something to do downstairs");
                direction = Direction.DOWN;
            } else {
                //log.info("NOthing to do downstairs");
                direction = Direction.UP;
            }
        } else {
            S03E01W2Elevator.log.warn("getNextDirection(): using safe DEFAULT <{}>", direction);
        }

        return direction;
    }

    @VisibleForTesting
    protected boolean someoneIsWaitingAtLowerLevels() {
        boolean someoneIsWaitingAtALowerLevel = false;
        synchronized (users) {
            for (User user : users) {
                if (user.waiting() && user.getInitialFloor() < currentFloor.get()) {
                    someoneIsWaitingAtALowerLevel = true;
                    break;
                }
            }
        }
        return someoneIsWaitingAtALowerLevel;
    }

    @VisibleForTesting
    protected boolean someoneRequestedAStopAtLowerLevels() {
        boolean someoneRequestedAStopAtALowerLevel = false;
        synchronized (users) {
            for (User user : users) {
                if (user.traveling() && user.requestedAStop() && user.getFloorToGo() < currentFloor.get()) {
                    someoneRequestedAStopAtALowerLevel = true;
                    break;
                }
            }
        }
        return someoneRequestedAStopAtALowerLevel;
    }

    @VisibleForTesting
    protected boolean someoneIsWaitingAtUpperLevels() {
        boolean someoneIsWaitingAtUpperLevels = false;
        if (currentFloor.get() != higherFloor) {
            synchronized (users) {
                for (User user : users) {
                    if (user.waiting() && user.getInitialFloor() > currentFloor.get()) {
                        someoneIsWaitingAtUpperLevels = true;
                        break;
                    }
                }
            }
        }
        return someoneIsWaitingAtUpperLevels;
    }

    @VisibleForTesting
    protected boolean someoneRequestedAStopAtUpperLevels() {
        boolean someoneRequestedAStopAtUpperLevels = false;
        if (currentFloor.get() != higherFloor) {
            synchronized (users) {
                for (User user : users) {
                    if (user.traveling() && user.requestedAStop() && user.getFloorToGo() > currentFloor.get()) {
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
        boolean empty = true;
        synchronized (users) {
            for (User user : users) {
                if (user.waiting()) {
                    empty = false;
                    break;
                }
            }
        }
        return empty;
    }

    @VisibleForTesting
    protected boolean nobodyHasRequestedAStop() {
        boolean answer = true;
        synchronized (users) {
            for (User user : users) {
                if (user.traveling() && user.requestedAStop()) {
                    answer = false;
                    break;
                }
            }
        }
        return answer;
    }

    @VisibleForTesting
    protected boolean stopRequestedAt(int floor) {
        boolean stopRequested = false;
        synchronized (users) {
            for (User user : users) {
                if (user.traveling() && user.requestedAStop() && user.getFloorToGo() == floor) {
                    stopRequested = true;
                }
            }
        }
        return stopRequested;
    }

    @VisibleForTesting
    protected void checkFloorValue(int floorValue) {
        checkArgument(floorValue >= lowerFloor, "'atFloor'=" + floorValue + " cannot be less than the lowerFloor <" +
                lowerFloor + ">");
        checkArgument(floorValue <= higherFloor, "'atFloor'=" + floorValue + " cannot be more than the higherFloor <" +
                higherFloor + ">");
    }

    protected Map<String, List<CountsByFloorByDirection>> aggregateUserInfos() {
        Map<Integer, Integer> waitCountByFloorForUp = new HashMap<>();
        Map<Integer, Integer> waitCountByFloorForDown = new HashMap<>();
        Map<Integer, Integer> stopCountByFloorForUp = new HashMap<>();
        Map<Integer, Integer> stopCountByFloorForDown = new HashMap<>();
        List<CountsByFloorByDirection> waitingList = new ArrayList<>();
        List<CountsByFloorByDirection> stopList = new ArrayList<>();
        synchronized (users) {
            for (User user : users) {
                if (user.waiting()) {
                    switch (user.getDirection()) {
                        case DOWN:
                            waitCountByFloorForDown = incrementValueForFloor(waitCountByFloorForDown, user.getInitialFloor());
                            break;
                        case UP:
                            waitCountByFloorForUp = incrementValueForFloor(waitCountByFloorForUp, user.getInitialFloor());
                            break;
                    }
                } else if (user.traveling()) {
                    switch (user.getDirection()) {
                        case DOWN:
                            stopCountByFloorForDown = incrementValueForFloor(stopCountByFloorForDown, user.getFloorToGo());
                            break;
                        case UP:
                            stopCountByFloorForUp = incrementValueForFloor(stopCountByFloorForUp, user.getFloorToGo());
                            break;
                    }
                }
            }
        }
        for (int f = lowerFloor; f <= higherFloor; f++) {
            if (waitCountByFloorForDown.containsKey(f) || waitCountByFloorForUp.containsKey(f)) {
                waitingList.add(new CountsByFloorByDirection(f, waitCountByFloorForDown.get(f), waitCountByFloorForUp.get(f)));
            }
            if (stopCountByFloorForDown.containsKey(f) || stopCountByFloorForUp.containsKey(f)) {
                stopList.add(new CountsByFloorByDirection(f, stopCountByFloorForDown.get(f), stopCountByFloorForUp.get(f)));
            }
        }
        return ImmutableMap.<String, List<CountsByFloorByDirection>>builder().put(WAITING_LIST, waitingList).put(STOP_LIST, stopList).build();
    }

    @VisibleForTesting
    protected String logWaitingListContent() {
        Multimap<Integer, User> waitByFloor = ArrayListMultimap.create();

        String value;
        if (waitByFloor.isEmpty()) {
            value = "{nobody is waiting}";
        } else {
            try {
                value = MAPPER.writeValueAsString(waitByFloor);
            } catch (JsonProcessingException e) {
                value = "{waits: exception " + e.getMessage() + "}";
                log.error("logRequestedStops(): JsonProcessingException", e);
            }
        }
        return value;
    }

    @VisibleForTesting
    protected String logRequestedStops() {
        Multimap<Integer, User> stopsByFloor = ArrayListMultimap.create();
        for (User user : users) {
            if (user.traveling() && user.requestedAStop()) {
                stopsByFloor.put(user.getFloorToGo(), user);
            }
        }
        String value;
        if (stopsByFloor.isEmpty()) {
            value = "{no stops}";
        } else {
            try {
                value = MAPPER.writeValueAsString(stopsByFloor);
            } catch (JsonProcessingException e) {
                value = "{stops: exception " + e.getMessage() + "}";
                log.error("logRequestedStops(): JsonProcessingException", e);
            }
        }
        return value;
    }

    @VisibleForTesting
    protected void logCurrentState(String from) {


        S03E01W2Elevator.log.info("logCurrentState(from:{}): tick <{}>, floor: <{}>, previousFloor: <{}>," +
                " middleFloor: <{}>, nbOfPassengers <{}>, totalUsers <{}>, previousCommand <{}>, " +
                "currentDirection <{}>, someoneIsWaitingAtLowerLevels <{}>, someoneIsWaitingAtUpperLevels<{}>," +
                " userWaitingAtCurrentFloor<{}>, userInsideElevatorNeedToGetOut<{}>, currentDoorStatus <{}>," +
                " waitingList <{}>, stopList <{}>, lastCommands <{}>, consistency <{}>",
                from, ticks.get(), currentFloor, previousFloor, middleFloor, currentNbOfUsersInsideTheElevator,
                users.size(), previousCommand, currentDirection, someoneIsWaitingAtLowerLevels(),
                someoneIsWaitingAtUpperLevels(), userWaitingAtCurrentFloor(), userInsideElevatorNeedToGetOut(),
                currentDoorStatus, logWaitingListContent(), logRequestedStops(), lastCommandsAsString(),
                stateIsInconsistent());
    }

    @VisibleForTesting
    protected boolean stateIsInconsistent() {
        boolean consistent = true;
        for (User user : users) {
            if (user.traveling() && user.didNotRequestedAStopYet()) {
                consistent = false;
            }
        }
        return consistent;
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

    private ElevatorContext getCurrentElevatorContext() {
        return ElevatorContext.builder()
                .source("getState")
                .score(score)
                .tick(ticks.get())
                .lowerFloor(lowerFloor)
                .higherFloor(higherFloor)
                .currentFloor(currentFloor.get())
                .previousFloor(previousFloor.get())
                .middleFloor(middleFloor)
                .currentNbOfUsersInsideTheElevator(currentNbOfUsersInsideTheElevator.get())
                .previousCommand(previousCommand)
                .currentDirection(currentDirection)
                .someoneIsWaitingAtLowerLevels(someoneIsWaitingAtLowerLevels())
                .someoneIsWaitingAtUpperLevels(someoneIsWaitingAtUpperLevels())
                .someoneRequestedAStopAtLowerLevels(someoneRequestedAStopAtLowerLevels())
                .someoneRequestedAStopAtUpperLevels(someoneRequestedAStopAtUpperLevels())
                .userWaitingAtCurrentFloor(userWaitingAtCurrentFloor())
                .userInsideElevatorNeedToGetOut(userInsideElevatorNeedToGetOut())
                .currentDoorStatus(currentDoorStatus)
                .lastCommands(lastCommands)
                .lastResetCause(lastResetCause)
                .lastResetContext(getLastResetContext())
                .users(users)
                .build();
    }

}
