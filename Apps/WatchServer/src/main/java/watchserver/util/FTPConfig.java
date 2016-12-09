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

import org.apache.commons.validator.routines.UrlValidator;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.net.URI;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "folder",
    "connection",
    "events"
})
public class FTPConfig {
    private UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
    
    private SourceType sourceType;
    @JsonProperty("folder")
    private String folder;
    @JsonProperty("connection")
    private Connection connection;
    @JsonProperty("events")
    private List<Event> events = null;

    /**
     * No args constructor for use in serialization
     * 
     */
    public FTPConfig() {
    }

    /**
     * 
     * @param folder
     * @param connection
     * @param events
     */
    public FTPConfig(String folder, Connection connection, List<Event> events) {
        super();
        this.folder = folder;
        this.connection = connection;
        this.events = events;
    }

    /**
     * 
     * @return
     *     The FTP folder string to be monitored 
     */
    public String getFullFTPUri(){
        URI uri = URI.create(getFolder());
        return uri.getScheme() + "://" + connection.getUsername() + ":" + connection.getPassword() + "@" 
                + uri.getAuthority() + ":" + connection.getPort() + connection.getPathtomonitor();
    }
    
    /**
     * 
     * @return
     *     The folder
     */
    @JsonProperty("folder")
    public String getFolder() {
        return folder;
    }

    /**
     * 
     * @param folder
     *     The folder
     * @throws Exception 
     */
    @JsonProperty("folder")
    public void setFolder(String folder) throws ConfigException {
        if (urlValidator.isValid(folder)) {
            this.folder = folder;
        } else {
            throw new ConfigException("FTP uri is not valid " + folder);
        }
        
    }

    public FTPConfig withFolder(String folder) {
        this.folder = folder;
        return this;
    }

    /**
     * 
     * @return
     *     The connection
     */
    @JsonProperty("connection")
    public Connection getConnection() {
        return connection;
    }

    /**
     * 
     * @param connection
     *     The connection
     */
    @JsonProperty("connection")
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public FTPConfig withConnection(Connection connection) {
        this.connection = connection;
        return this;
    }

    /**
     * 
     * @return
     *     The events
     */
    @JsonProperty("events")
    public List<Event> getEvents() {
        return events;
    }

    /**
     * 
     * @param events
     *     The events
     */
    @JsonProperty("events")
    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public FTPConfig withEvents(List<Event> events) {
        this.events = events;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

}
