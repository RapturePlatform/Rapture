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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.event.RaptureAlertEvent;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.alert.email.EmailAlerter;

public class AlerterFactory {
    private static final String RULES_FOLDER = "//sys.config/dp/alert/rule/";
    private static Logger log = Logger.getLogger(AlerterFactory.class);
            
    public static List<EventAlerter> getAlerters(RaptureAlertEvent event) {
        List<EventAlerter> alerters = new ArrayList<>();
        for(AlertRule rule : getAlertRules()) {
            if(rule.doesApply(event)) {
                alerters.add(getAlerter(rule));
            }
        }
        return alerters;
    }

    private static EventAlerter getAlerter(AlertRule rule) {
        switch(rule.getAlertType()) {
            case EmailAlerter.TYPE:
                return new EmailAlerter(rule.getDetails());
            default:
                return null;
        }
    }

    private static List<AlertRule> getAlertRules() {
        List<AlertRule> rules = new ArrayList<>();
        CallingContext context = ContextFactory.getKernelUser();
        try {
            Map<String, RaptureFolderInfo> allChildren = Kernel.getDoc().listDocsByUriPrefix(context, RULES_FOLDER, 0);
            for(String key : allChildren.keySet()) {
                RaptureFolderInfo info = allChildren.get(key);
                if(!info.isFolder()) {
                    String configString = Kernel.getDoc().getDoc(context, key);
                    rules.add(JacksonUtil.objectFromJson(configString, AlertRule.class));
                }
            }
        } catch (RaptureException e) {
            log.debug("No alert rules defined");
        }
        return rules;
    }
}
