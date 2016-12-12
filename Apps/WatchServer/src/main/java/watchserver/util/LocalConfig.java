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

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jline.internal.Log;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.apache.commons.io.FileUtils;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "folder",
    "events"
})
public class LocalConfig {
    private SourceType sourceType;
    @JsonProperty("folder")
    private String folder;
    @JsonProperty("events")
    private List<Event> events = null;

    /**
     * No args constructor for use in serialization
     * 
     */
    public LocalConfig() {
    }

    /**
     * 
     * @param folder
     * @param events
     */
    public LocalConfig(String folder, List<Event> events) {
        super();
        this.folder = folder;
        this.events = events;
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
     * @throws ConfigException 
     */
    @JsonProperty("folder")
    public void setFolder(String folder) throws ConfigException {
        Path path = Paths.get(folder);
        if (Files.isDirectory(path)){
            this.folder = folder;
        } else {
            throw new ConfigException("Folder is not valid " + folder);
        } 
    }

    public LocalConfig withFolder(String folder) {
        this.folder = folder;
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

    public LocalConfig withEvents(List<Event> events) {
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
