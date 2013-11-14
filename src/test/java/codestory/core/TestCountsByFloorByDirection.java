package codestory.core;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class TestCountsByFloorByDirection {
    @DataProvider(name="provider")
    public Object[][] provider() {
        return new Object[][] {
                {1, 2, 3, 2, 3},
                {722,2,null,2,0},
                {7,null,8,0,8},
                {-18,null,null,0,0}
        };
    }

    @Test(dataProvider = "provider")
    public void constructor_should_work(Integer floor, Integer actualNbDown, Integer actualNbUp, Integer expectedNbDown, Integer expectedNbUp) {
        CountsByFloorByDirection c = new CountsByFloorByDirection(floor,actualNbDown,actualNbUp);
        assertThat(c.getFloor()).isEqualTo(floor);
        assertThat(c.getCountByDirection().get(Direction.DOWN)).isEqualTo(expectedNbDown);
        assertThat(c.getCountByDirection().get(Direction.UP)).isEqualTo(expectedNbUp);
    }
}
