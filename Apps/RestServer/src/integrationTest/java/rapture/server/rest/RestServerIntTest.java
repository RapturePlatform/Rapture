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
package rapture.server.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class RestServerIntTest {

    @Test
    public void testDocGet() throws UnirestException {
        String result = Unirest.get("http://localhost:4567/doc/matrix.config/mqSeriesConfig").asString().getBody();
        assertTrue(result.indexOf("connectionFactory") != -1);
    }

    @Test
    public void testDocPut() throws UnirestException {
        int status = Unirest.put("http://localhost:4567/doc/matrix.config/mqSeriesConfig")
                .body("{ \"environment\" : \"test\", \"connectionFactory\" : \"MATRIXCF\", \"outputQueue\" : \"RAP_TO_TB\", \"writeEnabled\" : false }")
                .asString().getStatus();
        assertEquals(200, status);
    }

    @Test
    public void testDocDelete() throws UnirestException {
        String result = Unirest.delete("http://localhost:4567/doc/matrix.config/mqSeriesConfig").asString().getBody();
        System.out.println(result);
        int status = Unirest.get("http://localhost:4567/doc/matrix.config/mqSeriesConfig").asString().getStatus();
        assertEquals(404, status);
    }

    @Test
    public void testBlobGet() throws UnirestException {
        String result = Unirest.get("http://localhost:4567/blob/matrix.archive/FIX/2016/05/18/8668_input").asString().getBody();
        System.out.println(result);
    }

    @Test
    public void testBlobPut() throws UnirestException {
        int status = Unirest.put("http://localhost:4567/blob/matrix.archive/FIX/2016/05/18/8668_input")
                .header("Content-Type", "text/plain")
                .body("a string value")
                .asString().getStatus();
        assertEquals(200, status);
    }

    @Test
    public void testBlobDelete() throws UnirestException {
        String result = Unirest.delete("http://localhost:4567/blob/matrix.archive/FIX/2016/05/18/8668_input").asString().getBody();
        System.out.println(result);
        int status = Unirest.get("http://localhost:4567/blob/matrix.archive/FIX/2016/05/18/8668_input").asString().getStatus();
        assertEquals(404, status);
    }

}
