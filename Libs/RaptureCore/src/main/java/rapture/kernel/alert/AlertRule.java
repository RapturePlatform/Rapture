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
package rapture.kernel.alert;

import rapture.event.EventLevel;
import rapture.event.RaptureAlertEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlertRule {
    private String eventType;
    private Set<EventLevel> eventLevel = new HashSet<>();
    private String alertType;
    private String details;

    public AlertRule(@JsonProperty("eventType") String eventType,
            @JsonProperty("eventLevel") String eventLevel,
            @JsonProperty("alertType") String alertType,
            @JsonProperty("details") String details) {
        this.eventType = eventType;
        this.alertType = alertType;
        this.details = details;
        if ("*".equals(eventLevel) || StringUtils.isBlank(eventLevel)) {
            this.eventLevel.addAll(Arrays.asList(EventLevel.values()));
        } else {
            for (String level : eventLevel.split(",")) {
                this.eventLevel.add(EventLevel.valueOf(level));
            }
        }
    }

    public boolean doesApply(RaptureAlertEvent event) {
        return event != null && eventType.equals(event.getType()) && eventLevel.contains(event.getEventLevel());
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Set<EventLevel> getEventLevel() {
        return eventLevel;
    }

    public void setEventLevel(Set<EventLevel> level) {
        this.eventLevel = level;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
