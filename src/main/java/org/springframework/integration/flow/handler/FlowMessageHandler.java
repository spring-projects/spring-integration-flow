/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.flow.handler;

import java.util.Collections;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.flow.FlowConstants;
import org.springframework.integration.flow.config.FlowUtils;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;

/**
 * A MessageHandler for Handling Flow input and output. Sends messages on its
 * input channel to the flow input channel and replies with the flow output (if
 * there is one) to its output channel.
 *
 * Internally creates a subscriber to a PublishSubscribeChannel automatically
 * created for the flow. Since all FlowMessageHandler instances subscribe to
 * this channel, a unique flow conversationId is used to correlate flow input
 * and output messages
 *
 * The output message contains a FLOW_OUTPUT_PORT_HEADER identifying which flow
 * output port produced the message
 * @see FlowUtils
 *
 * Error handling is done in a standard way. If the flow includes an error
 * channel which is bound to an output port, the handler will send the
 * ErrorMessage to the outputChannel. If the flow throws an exception, the
 * handler will create an ErrorMessage and send it to the outputChannel
 *
 * @author David Turanski
 *
 */
public class FlowMessageHandler extends AbstractReplyProducingMessageHandler {

	private static Log log = LogFactory.getLog(FlowMessageHandler.class);

	private final MessageChannel flowInputChannel;

	private final SubscribableChannel flowOutputChannel;

	private volatile MessageChannel errorChannel;

	private final long timeout;

	/**
	 *
	 * @param flowInputChannel the Flow input channel
	 * @param flowOutputChannel a PublishSubscribeChannel internally created and
	 * bridged to all flow output channels
	 * @param timeout the send timeout duration
	 */
	public FlowMessageHandler(MessageChannel flowInputChannel, SubscribableChannel flowOutputChannel, long timeout) {
		this.flowInputChannel = flowInputChannel;
		this.flowOutputChannel = flowOutputChannel;
		this.timeout = timeout;
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		UUID conversationId = requestMessage.getHeaders().getId();
		Message<?> message = MessageBuilder.fromMessage(requestMessage).pushSequenceDetails(conversationId, 0, 0)
				.build();
		try {

			ResponseMessageHandler responseMessageHandler = new ResponseMessageHandler(conversationId);
			flowOutputChannel.subscribe(responseMessageHandler);
			flowInputChannel.send(message, timeout);
			flowOutputChannel.unsubscribe(responseMessageHandler);

			return responseMessageHandler.getResponse();

		}
		catch (MessagingException me) {
			log.error(me.getMessage(), me);

			if (conversationId.equals(new IntegrationMessageHeaderAccessor(me.getFailedMessage()).getCorrelationId())) {
				if (errorChannel != null) {
					errorChannel.send(new ErrorMessage(me, Collections.singletonMap(
							FlowConstants.FLOW_OUTPUT_PORT_HEADER,
							(Object) FlowConstants.FLOW_HANDLER_EXCEPTION_HEADER_VALUE)));

				}
			}
			else {
				throw me;
			}
		}
		return null;
	}

	/*
	 * Internal MessageHandler for the flow response
	 */
	private static class ResponseMessageHandler implements MessageHandler {
		private final UUID conversationId;

		private volatile Message<?> response;

		public ResponseMessageHandler(UUID conversationId) {
			this.conversationId = conversationId;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.messaging.MessageHandler#handleMessage
		 * (org.springframework.messaging.Message)
		 */
		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			Object correlationId = new IntegrationMessageHeaderAccessor(message).getCorrelationId();
			if (log.isDebugEnabled()) {
				log.debug("handling flow response message with conversation Id "
						+ correlationId + ". Target conversation Id = " + this.conversationId
						+ " match = " + conversationId.equals(correlationId));
			}
			if (conversationId.equals(correlationId)) {
				this.response = MessageBuilder.fromMessage(message).popSequenceDetails().build();
				if (log.isDebugEnabled()) {
					log.debug("set flow response message " + this.response);
				}
			}
			else {

				/*
				 * Response from flow's ErrorChannel which is mapped to an
				 * output port.
				 */
				if (message instanceof ErrorMessage) {
					MessagingException me = (MessagingException) message.getPayload();
					if (conversationId.equals(new IntegrationMessageHeaderAccessor(me.getFailedMessage()).getCorrelationId())) {
						this.response = message;
					}
				}
			}
		}

		public Message<?> getResponse() {
			return this.response;
		}
	}

}
