/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.flow.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.flow.FlowConstants;
import org.springframework.integration.flow.Transaction.StubTransactionManager;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author David Turanski
 * @author Gary Russell
 *
 */
public class TransactionalFlowTests {

	@Test
	public void testFlowDirectCommit() {
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/META-INF/spring/integration/flows/transactional-flow/flow-context.xml",
				"/org/springframework/integration/flow/config/xml/txmanager-config.xml");
		MessageChannel inputChannel = applicationContext.getBean("inputChannel", MessageChannel.class);
		SubscribableChannel outputChannel = applicationContext.getBean("outputChannel", SubscribableChannel.class);
		StubTransactionManager transactionManager = applicationContext.getBean(StubTransactionManager.class);
		Handler handler = new Handler();
		outputChannel.subscribe(handler);
		inputChannel.send(new GenericMessage<String>("hello"));
		assertTrue(handler.messageReceived);
		assertTrue(transactionManager.committed);
		assertFalse(transactionManager.rolledback);
		applicationContext.close();
	}

	@Test
	public void testFlowDirectRollback() {
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/META-INF/spring/integration/flows/transactional-flow/flow-context.xml",
				"/org/springframework/integration/flow/config/xml/txmanager-config.xml");
		MessageChannel inputChannel = applicationContext.getBean("inputChannel", MessageChannel.class);
		SubscribableChannel outputChannel = applicationContext.getBean("outputChannel", SubscribableChannel.class);
		StubTransactionManager transactionManager = applicationContext.getBean(StubTransactionManager.class);
		Handler handler = new Handler();
		outputChannel.subscribe(handler);
		try {
			inputChannel.send(new GenericMessage<String>("rollback"));
			fail("should throw exception");
		}
		catch (Exception e) {
			assertFalse(handler.messageReceived);
			assertTrue(transactionManager.rolledback);
			assertFalse(transactionManager.committed);
		}
		applicationContext.close();
	}

	@Test
	public void testFlowCommit() {
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/org/springframework/integration/flow/config/xml/TransactionalFlowTests-context.xml",
				"/org/springframework/integration/flow/config/xml/txmanager-config.xml");
		MessageChannel inputChannel = applicationContext.getBean("inputC", MessageChannel.class);
		SubscribableChannel outputChannel = applicationContext.getBean("outputC", SubscribableChannel.class);
		StubTransactionManager transactionManager = applicationContext.getBean(StubTransactionManager.class);
		Handler handler = new Handler();
		outputChannel.subscribe(handler);
		inputChannel.send(new GenericMessage<String>("hello"));
		assertTrue(handler.messageReceived);
		assertTrue(transactionManager.committed);
		assertFalse(transactionManager.rolledback);
		applicationContext.close();
	}

	@Test
	public void testFlowRollbackWithGatewayErrorChannel() {
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/org/springframework/integration/flow/config/xml/TransactionalFlowTests-context.xml",
				"/org/springframework/integration/flow/config/xml/txmanager-config.xml");
		MessageChannel inputChannel = applicationContext.getBean("inputC", MessageChannel.class);
		SubscribableChannel errorChannel = applicationContext.getBean("errorChannel", SubscribableChannel.class);
		StubTransactionManager transactionManager = applicationContext.getBean(StubTransactionManager.class);
		Handler handler = new Handler();
		errorChannel.subscribe(handler);

		inputChannel.send(new GenericMessage<String>("rollback"));

		assertTrue(handler.messageReceived);
		assertTrue(handler.message instanceof ErrorMessage);
		assertEquals(FlowConstants.FLOW_HANDLER_EXCEPTION_HEADER_VALUE,
				handler.message.getHeaders().get(FlowConstants.FLOW_OUTPUT_PORT_HEADER));
		assertTrue(transactionManager.rolledback);
		assertFalse(transactionManager.committed);
		applicationContext.close();
	}

	private static class Handler implements MessageHandler {
		public boolean messageReceived;

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
			this.messageReceived = true;
			this.message = message;
		}

	}
}
