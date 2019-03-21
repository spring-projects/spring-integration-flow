/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.integration.flow.config.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 *
 * @author David Turanski
 * @author Gary Russell
 *
 */

public class FlowWithErrorTests {

	@Test
	public void testFlowThrowsExceptionWithGatewayErrorChannel() {
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/org/springframework/integration/flow/config/xml/FlowWithErrorTests-context.xml");
		MessageChannel inputChannel = applicationContext.getBean("inputC", MessageChannel.class);
		SubscribableChannel errorChannel = applicationContext.getBean("errorChannel", SubscribableChannel.class);
		Message<String> msg = new GenericMessage<String>("hello");
		Handler handler = new Handler();
		errorChannel.subscribe(handler);
		inputChannel.send(msg);
		assertTrue(handler.gotResponse);
		applicationContext.close();
	}

	@Test
	public void testDirectCallWithErrorChannel() {
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"classpath:/META-INF/spring/integration/flows/subflow5/subflow5-context.xml");
		MessageChannel inputChannel = applicationContext.getBean("subflow-input", MessageChannel.class);
		SubscribableChannel errorChannel = applicationContext.getBean("errorChannel", SubscribableChannel.class);

		errorChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof MessagingException);
			}
		});

		Message<String> msg = new GenericMessage<String>("hello");
		assertTrue(inputChannel.send(msg));
		applicationContext.close();
	}

	@Test
	public void testWithErrorChannel() {
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/org/springframework/integration/flow/config/xml/FlowWithErrorTests-context.xml");
		MessageChannel inputChannel = applicationContext.getBean("inputC1", MessageChannel.class);
		PollableChannel output = applicationContext.getBean("outputC1", PollableChannel.class);
		Message<String> msg = new GenericMessage<String>("hello");
		inputChannel.send(msg);

		Message<?> reply = output.receive(100);
		assertNotNull(reply);
		assertTrue(reply.getPayload() instanceof MessagingException);
		applicationContext.close();
	}

	private static class Handler implements MessageHandler {
		public boolean gotResponse;

		@SuppressWarnings("unused")
		public Message<?> message;

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.messaging.MessageHandler#handleMessage
		 * (org.springframework.messaging.Message)
		 */
		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			this.gotResponse = true;
			this.message = message;
		}

	}

}
