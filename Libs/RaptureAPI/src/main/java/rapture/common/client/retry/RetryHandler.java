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
package rapture.common.client.retry;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;

import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;

public class RetryHandler {

    private static final Logger log = Logger.getLogger(RetryHandler.class);
    
    public static <T> T execute(RetryFunction<T> function, int numAttempts) {
        int attemptsLeft = numAttempts;
        String errorMessage;
        Exception exception;
        do {
            try {
                errorMessage = null;
                return function.execute();
            } catch (URISyntaxException e) {
                exception = e;
                errorMessage = e.getMessage();
            } catch (ClientProtocolException e) {
                exception = e;
                errorMessage = "There was an error connecting to the server";
            } catch (IOException e) {
                exception = e;
                errorMessage = "There was an error connecting to the server";
            } catch (Exception e) {
                exception = e;
                errorMessage = "There was an error connecting to the server";
            }
            attemptsLeft--;
            if (attemptsLeft > 0) {
                //attempt recovery functions, like logging in
                function.recover();
            }
        } while (attemptsLeft > 0);

        RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, errorMessage);
        log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, exception));
        throw raptException;
    }

}
