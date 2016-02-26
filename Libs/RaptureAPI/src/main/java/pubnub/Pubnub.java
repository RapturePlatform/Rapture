/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
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
package pubnub;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * PubNub 3.0 Real-time Push Cloud API
 *
 * @author Stephen Blum
 * @package pubnub
 */
public class Pubnub {
    private String ORIGIN = "pubsub.pubnub.com";
    private int LIMIT = 1800;
    private String PUBLISH_KEY = "";
    private String SUBSCRIBE_KEY = "";
    private String SECRET_KEY = "";
    private boolean SSL = false;

    /**
     * PubNub 2.0 Compatibility
     *
     * Prepare PubNub Class State.
     *
     * @param String Publish Key.
     * @param String Subscribe Key.
     */
    public Pubnub(String publishKey, String subscribeKey) {
        this.init(publishKey, subscribeKey, "", false);
    }

    /**
     * PubNub 3.0 without SSL
     *
     * Prepare PubNub Class State.
     *
     * @param String Publish Key.
     * @param String Subscribe Key.
     * @param String Secret Key.
     */
    public Pubnub(String pubishKey, String subscribeKey, String secretKey) {
        this.init(pubishKey, subscribeKey, secretKey, false);
    }

    /**
     * PubNub 3.0
     *
     * Prepare PubNub Class State.
     *
     * @param String  Publish Key.
     * @param String  Subscribe Key.
     * @param String  Secret Key.
     * @param boolean SSL Enabled.
     */
    public Pubnub(String publishKey, String subscribeKey, String secretKey, boolean sslOn) {
        this.init(publishKey, subscribeKey, secretKey, sslOn);
    }

    private String _encodeURIcomponent(String s) {
        StringBuilder o = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (isUnsafe(ch)) {
                o.append('%');
                o.append(toHex(ch / 16));
                o.append(toHex(ch % 16));
            } else
                o.append(ch);
        }
        return o.toString();
    }

    /**
     * Request URL
     *
     * @param List <String> request of url directories.
     * @return JSONArray from JSON response.
     */
    private ArrayNode _request(List<String> urlComponents) {
        String json = "";
        StringBuilder url = new StringBuilder();
        Iterator<String> urlIterator = urlComponents.iterator();

        url.append(this.ORIGIN);

        // Generate URL with UTF-8 Encoding
        while (urlIterator.hasNext()) {
            try {
                String url_bit = (String) urlIterator.next();
                url.append("/").append(_encodeURIcomponent(url_bit));
            } catch (Exception e) {
                e.printStackTrace();
                ArrayNode jsono = new ArrayNode(JsonNodeFactory.instance);
                try {
                    jsono.add("Failed UTF-8 Encoding URL.");
                } catch (Exception jsone) {
                }
                return jsono;
            }
        }

        // Fail if string too long
        if (url.length() > this.LIMIT) {
            ArrayNode jsono = new ArrayNode(JsonNodeFactory.instance);
            try {
                jsono.add(0);
                jsono.add("Message Too Long.");
            } catch (Exception jsone) {
            }
            return jsono;
        }

        try {
            URL request = new URL(url.toString());
            URLConnection conn = request.openConnection();
            String line = "";

            conn.setConnectTimeout(200000);
            conn.setReadTimeout(200000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            // Read JSON Message
            while ((line = reader.readLine()) != null) {
                json += line;
            }
            reader.close();

            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getJsonFactory();
            JsonParser jp = factory.createJsonParser(json);
            JsonNode actualObj = mapper.readTree(jp);
            return (ArrayNode) actualObj;

        } catch (Exception e) {

            ArrayNode jsono = new ArrayNode(JsonNodeFactory.instance);

            try {
                jsono.add("Failed JSONP HTTP Request.");
            } catch (Exception jsone) {
            }

            return jsono;
        }

    }

    /**
     * Subscribe - Private Interface
     *
     * Patch provided by petereddy on GitHub
     *
     * @param String   channel name.
     * @param Callback function callback.
     * @param String   timetoken.
     */
    private void _subscribe(String channel, Callback callback, String timetoken) {
        while (true) {
            try {
                // Build URL
                List<String> url = java.util.Arrays.asList("subscribe", this.SUBSCRIBE_KEY, channel, "0", timetoken);

                // Wait for Message
                ArrayNode messages = _request(url);

                // Update TimeToken
                if (messages.get(1).asText().length() > 0)
                    timetoken = messages.get(1).asText();

                messages = (ArrayNode) messages.get(0);
                // Run user Callback and Reconnect if user permits. If
                // there's a timeout then messages.length() == 0.
                for (int i = 0; messages.size() >= i; i++) {
                    JsonNode message = messages.get(i);
                    if (!callback.execute(message))
                        return;
                }
            } catch (Exception e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    /**
     * History
     *
     * Load history from a channel.
     *
     * @param String channel name.
     * @param int    limit history count response.
     * @return JSONArray of history.
     */
    public ArrayNode history(String channel, int limit) {
        List<String> url = new ArrayList<String>();

        url.add("history");
        url.add(this.SUBSCRIBE_KEY);
        url.add(channel);
        url.add("0");
        url.add(Integer.toString(limit));

        return _request(url);
    }

    /**
     * Init
     *
     * Prepare PubNub Class State.
     *
     * @param String  Publish Key.
     * @param String  Subscribe Key.
     * @param String  Secret Key.
     * @param boolean SSL Enabled.
     */
    public void init(String publishKey, String subscribeKey, String secretKey, boolean sslOn) {
        this.PUBLISH_KEY = publishKey;
        this.SUBSCRIBE_KEY = subscribeKey;
        this.SECRET_KEY = secretKey;
        this.SSL = sslOn;

        // SSL On?
        if (this.SSL) {
            this.ORIGIN = "https://" + this.ORIGIN;
        } else {
            this.ORIGIN = "http://" + this.ORIGIN;
        }
    }

    private boolean isUnsafe(char ch) {
        return " ~`!@#$%^&*()+=[]\\{}|;':\",./<>?".indexOf(ch) >= 0;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes("UTF-8"));
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);

            while (hashtext.length() < 32)
                hashtext = "0" + hashtext;

            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Publish
     *
     * Send a message to a channel.
     *
     * @param String channel name.
     * @return boolean false on fail.
     */
    public ArrayNode publish(String channel, String message) {
        // Generate String to Sign
        String signature = "0";
        if (this.SECRET_KEY.length() > 0) {
            StringBuilder stringToSign = new StringBuilder();
            stringToSign.append(this.PUBLISH_KEY).append('/').append(this.SUBSCRIBE_KEY).append('/').append(this.SECRET_KEY).append('/').append(channel)
                    .append('/').append(message.toString());

            // Sign Message
            signature = md5(stringToSign.toString());
        }

        // Build URL
        List<String> url = new ArrayList<String>();
        url.add("publish");
        url.add(this.PUBLISH_KEY);
        url.add(this.SUBSCRIBE_KEY);
        url.add(signature);
        url.add(channel);
        url.add("0");
        url.add(message);

        // Return JSONArray
        return _request(url);
    }

    /**
     * Subscribe
     *
     * This function is BLOCKING. Listen for a message on a channel.
     *
     * @param String   channel name.
     * @param Callback function callback.
     */
    public void subscribe(String channel, Callback callback) {
        this._subscribe(channel, callback, "0");
    }

    /**
     * Time
     *
     * Timestamp from PubNub Cloud.
     *
     * @return double timestamp.
     */
    public double time() {
        List<String> url = new ArrayList<String>();

        url.add("time");
        url.add("0");

        ArrayNode response = _request(url);
        return response.get(0).asDouble();
    }

    private char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }
}
