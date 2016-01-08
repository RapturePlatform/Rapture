/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.server.web.websocket;

import java.io.IOException;

import javax.websocket.Session;

import rapture.exchange.TopicMessageHandler;

public class SessionState implements TopicMessageHandler {
	private Session session;
	private boolean subscribed = false;
	private String domain;
	private String exchange;
	private String topic;
	private long subscriptionId;
	
	public SessionState(Session session) {
		this.session = session;
	}
	
	public void setSubscribed(String domain, String exchange, String topic) {
		this.domain = domain;
		this.exchange = exchange;
		this.topic = topic;
		this.subscribed = true;
	}

	public boolean isSubscribed() {
		return subscribed;
	}

	public void setSubscriptionId(long id) {
		subscriptionId = id;
	}

	@Override
	public void deliverMessage(String exchange, String topic, String sentTopic,
			String message) {
		try {
			session.getBasicRemote().sendText(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getExchange() {
		return exchange;
	}

	public long getHandle() {
		return subscriptionId;
	}

	public String getDomain() {
		return domain;
	}

}
