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

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.log4j.Logger;

@ServerEndpoint(value = "/topic")
public class TopicPublish {
	private static Logger log = Logger
			.getLogger(TopicPublish.class);
	private static SessionManager manager = new SessionManager();

	@OnOpen
	public void onOpen(Session session) {
		log.info("Connected ... " + session.getId());
		try {
			manager.registerSession(session);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@OnMessage
	public String onMessage(String message, Session session) {
		// The message here can be one of two messages. Quit will quit
		// subs (so check first 4 chars) will subscribe to the
		// "subs,[domain],[exchange],[topic] (with a callback which we use to
		// send messages back)

		log.info("Received message " + message);
		if (message.length() >= 4) {
			String code = message.substring(0, 4);
			log.info("Code is " + code);
			switch (code) {
			case "subs":
				String[] parts = message.split(",");
				if (parts.length == 4) {
					manager.subscribeSession(session, parts[1], parts[2], parts[3]);
				}
				break;
			case "quit":
				try {
					session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE,
							"Client asked to quit"));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				break;
			}
		}
		return message;
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		log.info(String.format("Session %s closed because of %s",
				session.getId(), closeReason));
		manager.deregisterSession(session);
	}
}
