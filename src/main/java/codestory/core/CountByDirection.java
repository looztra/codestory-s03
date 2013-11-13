package codestory.core;

import lombok.Getter;
import lombok.experimental.Builder;

/**
 * User: cfurmaniak
 * Date: 09/11/13
 * Time: 11:09
 */
@Builder
@Getter
public class CountByDirection {
    private final Direction direction;
    private final int count;

    public static CountByDirection incrementAndGet(CountByDirection source) {
        return CountByDirection.builder().direction(source.direction).count(source.count + 1).build();
    }
}
