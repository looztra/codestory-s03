package codestory.resources;

import codestory.core.Direction;
import codestory.core.engine.ElevatorEngine;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User: cfurmaniak
 * Date: 31/10/13
 * Time: 19:03
 */
@Produces(MediaType.TEXT_PLAIN)
@Path("/")
public class ElevatorResource {

    private final ElevatorEngine elevatorEngine;

    @Inject
    public ElevatorResource(ElevatorEngine engine) {
        this.elevatorEngine = engine;
    }

    @GET
    public String getState(@QueryParam("includeFullUserList")Optional<Boolean> includeFullUserList,
                           @QueryParam("includeLastRequests") Optional<Boolean> includeLastRequests) {
        return elevatorEngine.getState(includeFullUserList,includeLastRequests);
    }

    @Path("call")
    @GET
    public Response call(@QueryParam("atFloor") int atFloor, @QueryParam("to") String to) {
        synchronized (elevatorEngine) {
            elevatorEngine.call(atFloor, Direction.valueOf(to));
        }
        return Response.ok().build();
    }

    @Path("go")
    @GET
    public Response go(@QueryParam("floorToGo") int floorToGo) {
        synchronized (elevatorEngine) {
            elevatorEngine.go(floorToGo);
        }
        return Response.ok().build();
    }

    @Path("userHasEntered")
    @GET
    public Response userHasEntered() {
        synchronized (elevatorEngine) {
            elevatorEngine.userHasEntered(null);
        }
        return Response.ok().build();
    }

    @Path("userHasExited")
    @GET
    public Response userHasExited() {
        synchronized (elevatorEngine) {
            elevatorEngine.userHasExited(null);
        }
        return Response.ok().build();
    }

    @Path("reset")
    @GET
    public Response reset(@QueryParam("cause") String cause, @QueryParam("lowerFloor") @DefaultValue("0") int lowerFloor,
                          @QueryParam("higherFloor") @DefaultValue("5") int higherFloor,
                          @QueryParam("cabinSize") @DefaultValue("30") int cabinSize) {
        synchronized (elevatorEngine) {
            elevatorEngine.reset(cause, lowerFloor, higherFloor, cabinSize);
        }
        return Response.ok().build();
    }

    @Path("nextCommand")
    @GET
    public String nextCommand() {
        synchronized (elevatorEngine) {
            return elevatorEngine.nextCommand().toString();
        }
    }
}
