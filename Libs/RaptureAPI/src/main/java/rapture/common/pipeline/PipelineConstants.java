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
package rapture.common.pipeline;

public class PipelineConstants {
    public static final String DEFAULT_EXCHANGE_DOMAIN = "main";
    public static final String ANONYMOUS_PREFIX = "$anonoymous";
    public static final String DEFAULT_EXCHANGE = "$defaultExchange";
    public static final String FANOUT_EXCHANGE = "raptureFanout";
    public static final String DIRECT_EXCHANGE = "raptureDirect";
    public static final String TOPIC_EXCHANGE = "raptureTopic";

    /**
     * The pipeline categories
     */
    // TODO is this the registry? maybe have a different file with a registry of
    // all possible categories? could be configurable on startup maybe?
    public static final String CATEGORY_ALPHA = "alpha";
    public static final String CATEGORY_RUNNER = "runners";
    public static final String CATEGORY_APPMANAGER = "appManagers";
    public static final String CATEGORY_DATACAPTURE = "dataCapture";
    public static final String CATEGORY_SEARCH = "search";

    // public enum PipelineConstants {
    //
    // DEFAULT_EXCHANGE_DOMAIN("main"), ANONYMOUS_PREFIX("$anonoymous"), DEFAULT_EXCHANGE("$defaultExchange"), FANOUT_EXCHANGE("raptureFanout"),
    // DIRECT_EXCHANGE(
    // "raptureDirect"), TOPIC_EXCHANGE("raptureTopic"),
    //
    // /**
    // * The pipeline categories
    // */
    // // TODO is this the registry? maybe have a different file with a registry of
    // // all possible categories? could be configurable on startup maybe?
    // CATEGORY_ALPHA("alpha"), CATEGORY_RUNNER("runners"), CATEGORY_APPMANAGER("appManagers"), CATEGORY_DATACAPTURE("dataCapture"), CATEGORY_SEARCH("search");
    //
    // private String name;
    //
    // PipelineConstants(String name) {
    // this.name = name;
    // }
    //
    // @Override
    // public String toString() {
    // return this.name;
    // }

}
