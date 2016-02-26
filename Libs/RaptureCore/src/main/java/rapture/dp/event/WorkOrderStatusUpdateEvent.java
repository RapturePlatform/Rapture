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
package rapture.dp.event;

import rapture.event.EventLevel;

import org.stringtemplate.v4.ST;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by yanwang on 3/17/15.
 */
public class WorkOrderStatusUpdateEvent extends DecisionProcessEvent {
    public static final String TYPE = "STATUS_UPDATE";

    private String status;

    public WorkOrderStatusUpdateEvent(@JsonProperty("EventLevel") EventLevel eventLevel,
            @JsonProperty("workOrderUri") String workOrderUri,
            @JsonProperty("status") String status) {
        super(eventLevel, workOrderUri);
        this.status = status;
    }

    @JsonIgnore
    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String parseTemplate(String templateContent) {
        ST template = new ST(templateContent);
        template.add("status", status);
        template.add("level", getEventLevel());
        template.add("workOrderUri", getWorkOrderUri());
        return template.render();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
