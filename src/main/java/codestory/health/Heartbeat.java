package codestory.health;

import com.yammer.metrics.core.HealthCheck;

/**
 * User: cfurmaniak
 * Date: 03/11/13
 * Time: 17:34
 */
public class Heartbeat extends HealthCheck {

    public Heartbeat() {
        super("heartbeat");
    }
    @Override
    protected Result check() throws Exception {
        return Result.healthy();
    }
}
