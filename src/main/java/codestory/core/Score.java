package codestory.core;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class Score {

    final Integer nbFloors;
    Integer score;

    public Score(Integer lowerFloor, Integer higherFloor) {
        score = 0;
        this.nbFloors = higherFloor - lowerFloor + 1;
    }

    public static Integer bestTickToGo(Integer floor, Integer floorToGo) {
        // elevator is OPEN at floor
        final Integer elevatorHasToCloseDoorsWhenAtFloor = 1;
        final Integer elevatorGoesStraightFromFloorToFloorToGo = abs(floorToGo - floor);
        final Integer elevatorHasToOpenDoorsWhenAtFloorToGo = 1;

        return elevatorHasToCloseDoorsWhenAtFloor
                + elevatorGoesStraightFromFloorToFloorToGo
                + elevatorHasToOpenDoorsWhenAtFloorToGo;
    }

    public Integer score(User user) throws IllegalStateException {
        if (user.getTickToWait() < 1) {
            throw new IllegalStateException("when done, user have to wait at least one tick");
        }
        Integer bestTickToGo = bestTickToGo(user.getInitialFloor(), user.getFloorToGo());
        if (user.getTickToGo() < bestTickToGo) {
            throw new IllegalStateException("when done, user have to wait at least minimum amount of ticks");
        }
        Integer score = nbFloors
                - user.getTickToWait() / 2
                - user.getTickToGo()
                + bestTickToGo;
        return min(max(0, score), nbFloors);
    }

    public Score loose() {
        score -= nbFloors;
        return this;
    }

    public Score success(User user) throws IllegalStateException {
        score += score(user);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Score score1 = (Score) o;

        return score.equals(score1.score);
    }

    @Override
    public int hashCode() {
        return score;
    }

    public int getScore() {
        return score;
    }
}

