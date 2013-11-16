package codestory.core.engine;

import codestory.core.Command;
import codestory.core.Direction;
import codestory.core.User;
import codestory.core.exception.ElevatorIsBrokenException;
import com.google.common.base.Optional;

public interface ElevatorEngine {

    public static final int DEFAULT_LOWER_FLOOR = 0;
    public static final int DEFAULT_HIGHER_FLOOR = 5;
    public static final int DEFAULT_CABIN_SIZE = 100;

    public ElevatorEngine call(Integer atFloor, Direction to) throws ElevatorIsBrokenException;

    public ElevatorEngine go(Integer floorToGo) throws ElevatorIsBrokenException;

    public Command nextCommand() throws ElevatorIsBrokenException;

    public ElevatorEngine userHasEntered(User user) throws ElevatorIsBrokenException;

    public ElevatorEngine userHasExited(User user) throws ElevatorIsBrokenException;

    public ElevatorEngine reset(String cause, int lowerFloor, int higherFloor,
                                int cabinSize) throws ElevatorIsBrokenException;

    public Integer getLowerFloor();

    public Integer getHigherFloor();

    public String getState(Optional<Boolean> oIncludeFullUserList, Optional<Boolean> oIncludeLastRequests);

}
