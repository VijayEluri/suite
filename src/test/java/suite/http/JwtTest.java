package suite.http;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JwtTest {

	private Jwt jwt = new Jwt();

	@Test
	public void test() {
		var payload0 = "{ \"userId\": \"b08f86af-35da-48f2-8fab-cef3904660bd\" }";
		var token = jwt.encode(payload0);
		System.out.println("JWT = " + token);
		var payload1 = jwt.decode(token);
		assertEquals(payload0, payload1);
	}

}
