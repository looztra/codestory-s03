package codestory.core;

import codestory.core.exception.ElevatorIsBrokenException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class User {

    public static final Integer UNSET = Integer.MIN_VALUE;
    private final Integer initialFloor;
    private final Direction direction;
    @Setter(AccessLevel.NONE)
    private Integer floorToGo = UNSET;
    private Integer currentFloor;
    private Integer tickToGo;
    private User.State state;
    private Integer tickToWait;

    public User(Integer initialFloor, Direction direction) throws ElevatorIsBrokenException {
        this.initialFloor = initialFloor;
        this.direction = direction;
        this.state = State.WAITING;
        this.tickToGo = 0;
        this.tickToWait = 0;
    }

    public void elevatorIsOpen(Integer floor) throws ElevatorIsBrokenException {
        if (waiting() && at(floor) && elevatorIsAtWaitingFloor(floor)) {
            state = State.TRAVELLING;
        } else if (traveling() && at(floorToGo)) {
            state = State.DONE;
        }
    }

    public boolean elevatorIsAtDestination(int floor) {
        return floorToGo == floor;
    }

    public boolean elevatorIsAtWaitingFloor(int floor) {
        return initialFloor == floor;
    }

    public boolean waiting() {
        return state == State.WAITING;
    }

    public Boolean traveling() {
        return state == State.TRAVELLING;
    }

    public Boolean done() {
        return state == State.DONE;
    }

    public void go(int floorToGo) {
        this.floorToGo = floorToGo;
    }

    public Boolean at(int floor) {
        return this.currentFloor == floor;
    }

    public Boolean didNotRequestedAStopYet() {
        return this.floorToGo == UNSET;
    }

    public Boolean requestedAStop() {
        return this.floorToGo != UNSET;
    }
    public void tick() {
        if (traveling()) {
            tickToGo++;
        }
        if (waiting()) {
            tickToWait++;
        }
    }

    public Integer bestTickToGo() {
        return Score.bestTickToGo(initialFloor, floorToGo);
    }
    public enum State {
        WAITING, TRAVELLING, DONE,;
    }

}
