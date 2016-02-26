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
package rapture.common;

import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

public class Messages {
    private static final String BUNDLE_NAME = "rapture.kernel.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private static Logger logger = Logger.getLogger(Messages.class);

    private static final HashMap<Locale, ResourceBundle> resourceBundles = new HashMap<Locale, ResourceBundle>();
    private static String Separator = "."; //$NON-NLS-1$

    private String module = null;
    private Locale locale = null;

    public Messages() {
        this(null, Locale.getDefault());
    }

    public Messages(String module) {
        this(module, Locale.getDefault());
    }

    public Messages(Locale locale) {
        this(null, locale);
    }

    public Messages(String module, Locale locale) {
        this.module = module;
        this.locale = locale;
    }

    public MessageFormat getMessage(String error) {
        return getMessage(module, error, (Object[]) null, locale);
    }

    public MessageFormat getMessage(String error, Object[] parameters) {
        return getMessage(module, error, parameters, locale);
    }

    public MessageFormat getMessage(String error, String parameter) {
        return getMessage(module, error, new Object[] {parameter}, locale);
    }

    public static MessageFormat getMessage(String module, String error, Object[] parameters, Locale locale) {
        ResourceBundle rb = findBundle(locale, BUNDLE_NAME);
        String messageRef = module + Separator + error;
        String messageTemplate;

        try {
            messageTemplate = rb.getString(messageRef);
        } catch (MissingResourceException mre) {
            try {
                messageTemplate = RESOURCE_BUNDLE.getString(messageRef);
            } catch (MissingResourceException mre2) {
                messageTemplate = mre2.getLocalizedMessage();
            // logger.trace(ExceptionToString.toString(mre));
            }
        }

        // handle nonexistent template
        if (messageTemplate == null) {
            messageTemplate = "Cannot find message {0} for {1} locale";
            parameters = new Object[] { messageRef, (locale == null) ? "default " : locale.getDisplayName() };
        }

        return new MessageFormat(messageTemplate, parameters, locale);
    }

    private static ResourceBundle findBundle(Locale locale, String catalog) throws MissingResourceException {
        ResourceBundle bundle = null;
        if (locale != null) {
            bundle = resourceBundles.get(locale);
            if (bundle == null) {
                synchronized (resourceBundles) {
                    try {
                        bundle = ResourceBundle.getBundle(catalog, locale);
                        resourceBundles.put(locale, bundle);
                    } catch (MissingResourceException e) {
                        logger.warn(e.getMessage() + " - ignoring locale");
                    }
                }
            }
        }
        return (bundle == null) ? RESOURCE_BUNDLE : bundle;
    }

    /**
     * @deprecated use getMessage 
     * @param key
     * @return
     */
    @Deprecated
    public static String getString(String key) {
        String[] keys = key.split("\\.");
        return getMessage(keys[0], keys[1], null, Locale.getDefault()).format();
    }
}
