package org.springframework.integration.flow.config.xml;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FlowWithOptionalResponseTests {
	@Autowired
	@Qualifier("inputC")
	MessageChannel input;

	@Autowired
	@Qualifier("inputCO")
	MessageChannel inputForOptionalResponse;

	@Autowired
	@Qualifier("outputC")
	SubscribableChannel output;

	@Test
	public void testOneWay() {

		input.send(new GenericMessage<String>("hello"));
	}

	@Test
	public void testOptionResponse() {
		TestMessageHandler counter = new TestMessageHandler();

		output.subscribe(counter);

		for (int i = 0; i < 100; i++) {
			inputForOptionalResponse.send(new GenericMessage<String>("hello"));
		}

		assertTrue(String.valueOf(counter.count), counter.count > 1 && counter.count < 100);
	}

	static class TestMessageHandler implements MessageHandler {
		int count = 0;

		public void handleMessage(Message<?> message) throws MessagingException {
			count++;
		}
	}

}
