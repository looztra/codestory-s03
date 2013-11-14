package codestory.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import lombok.Getter;

import java.util.Map;

@Getter
public class CountsByFloorByDirection {
    private Integer floor;
    @JsonProperty("count")
    private Map<Direction, Integer> countByDirection = Maps.newHashMap();
    public CountsByFloorByDirection(Integer floor, Integer nbDown, Integer nbUp) {
        this.floor = floor;
        if( nbDown != null ) {
            countByDirection.put(Direction.DOWN, nbDown);
        } else {
            countByDirection.put(Direction.DOWN, 0);
        }
        if( nbUp != null ) {
            countByDirection.put(Direction.UP, nbUp);
        } else {
            countByDirection.put(Direction.UP, 0);
        }
    }


}
