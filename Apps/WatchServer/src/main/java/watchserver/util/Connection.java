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
package watchserver.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "username",
    "password",
    "port",
    "pathtomonitor",
    "ftppassivemode"
})
public class Connection {

    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;
    @JsonProperty("port")
    private String port;
    @JsonProperty("pathtomonitor")
    private String pathtomonitor;
    @JsonProperty("ftppassivemode")
    private String ftppassivemode;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Connection() {
    }

    /**
     * 
     * @param port
     * @param ftppassivemode
     * @param username
     * @param pathtomonitor
     * @param password
     */
    public Connection(String username, String password, String port, String pathtomonitor, String ftppassivemode) {
        super();
        this.username = username;
        this.password = password;
        this.port = port;
        this.pathtomonitor = pathtomonitor;
        this.ftppassivemode = ftppassivemode;
    }

    /**
     * 
     * @return
     *     The username
     */
    @JsonProperty("username")
    public String getUsername() {
        return username;
    }

    /**
     * 
     * @param username
     *     The username
     */
    @JsonProperty("username")
    public void setUsername(String username) {
        this.username = username;
    }

    public Connection withUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * 
     * @return
     *     The password
     */
    @JsonProperty("password")
    public String getPassword() {
        return password;
    }

    /**
     * 
     * @param password
     *     The password
     */
    @JsonProperty("password")
    public void setPassword(String password) {
        this.password = password;
    }

    public Connection withPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * 
     * @return
     *     The port
     */
    @JsonProperty("port")
    public String getPort() {
        return port;
    }

    /**
     * 
     * @param port
     *     The port
     */
    @JsonProperty("port")
    public void setPort(String port) {
        this.port = port;
    }

    public Connection withPort(String port) {
        this.port = port;
        return this;
    }

    /**
     * 
     * @return
     *     The pathtomonitor
     */
    @JsonProperty("pathtomonitor")
    public String getPathtomonitor() {
        return pathtomonitor;
    }

    /**
     * 
     * @param pathtomonitor
     *     The pathtomonitor
     */
    @JsonProperty("pathtomonitor")
    public void setPathtomonitor(String pathtomonitor) {
        this.pathtomonitor = pathtomonitor;
    }

    public Connection withPathtomonitor(String pathtomonitor) {
        this.pathtomonitor = pathtomonitor;
        return this;
    }

    /**
     * 
     * @return
     *     The ftppassivemode
     */
    @JsonProperty("ftppassivemode")
    public String getFtppassivemode() {
        return ftppassivemode;
    }

    /**
     * 
     * @param ftppassivemode
     *     The ftppassivemode
     */
    @JsonProperty("ftppassivemode")
    public void setFtppassivemode(String ftppassivemode) {
        this.ftppassivemode = ftppassivemode;
    }

    public Connection withFtppassivemode(String ftppassivemode) {
        this.ftppassivemode = ftppassivemode;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
