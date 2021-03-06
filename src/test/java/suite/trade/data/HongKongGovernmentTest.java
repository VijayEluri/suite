package suite.trade.data;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.trade.Time;

public class HongKongGovernmentTest {

	private HongKongGovernment hkg = new HongKongGovernment();

	@Test
	public void test() {
		var publicHolidays = hkg.queryPublicHolidays();
		System.out.println(publicHolidays);
		assertTrue(publicHolidays.contains(Time.of(2018, 12, 25)));
	}

}
