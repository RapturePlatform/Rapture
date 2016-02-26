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
package rapture.script;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rapture.common.CallingContext;
import rapture.util.IDGenerator;

public class ScriptRunInfoCollector {
	private CallingContext callingContext;
	private Date startTime;
	private String scriptUri;
	private Boolean serverContext;
	private List<String> runOutput;
	private List<String> instrumentationOutput;
	
	private ScriptRunInfoCollector(CallingContext context, String scriptUri, Boolean serverContext) {
		this.callingContext = context;
		this.scriptUri = sanitize(scriptUri);		
		this.serverContext = serverContext;
		this.startTime = new Date();
		this.runOutput = new ArrayList<String>();
		this.instrumentationOutput = new ArrayList<String>();
	}
	

	private String sanitize(String uri) {
		if (uri.startsWith("script://")) {
			return uri.substring(9);
		}
		if (uri.startsWith("//")) {
			return uri.substring(2);
		}
		if (uri.startsWith("/")) {
			return uri.substring(1);
		}
		return uri;
	}


	public static ScriptRunInfoCollector createServerCollector(CallingContext context, String scriptUri) {
		ScriptRunInfoCollector ret = new ScriptRunInfoCollector(context, scriptUri, true);
		return ret;
	}
	
	public static ScriptRunInfoCollector createLocalCollector(CallingContext context, String fileName) {
		ScriptRunInfoCollector ret = new ScriptRunInfoCollector(context, fileName, false);
		return ret;
	}
	
	public void addOutput(List<String> list) {
		runOutput.addAll(list);
	}
	
	public void addInstrumentationLog(List<String> textLog) {
		instrumentationOutput.addAll(textLog);
	}

	public String getBlobUri() {
		// Different uris depending on whether local or remote
		if (serverContext) {
			return String.format("blob://sys.blob/script/server/%s/%s/%s/%s", scriptUri, getNiceDate(), getNiceTime(), getUniqueId());
		} else {
			return String.format("blob://sys.blob/script/local/%s/%s/%s/%s/%s", callingContext.getUser(), scriptUri, getNiceDate(), getNiceTime(), getUniqueId());			
		}
	}

	
	private String getUniqueId() {
		return IDGenerator.getUUID(10);
	}

	private String getNiceTime() {
		return stf.format(startTime);
	}

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat stf = new SimpleDateFormat("HHmmss");
	private String getNiceDate() {
		return sdf.format(startTime);
	}

	public String getBlobContent() {
		StringBuilder ret = new StringBuilder();
		ret.append("Script output");
		ret.append("\n");
		for(String l : runOutput) {
			ret.append(l);
			ret.append("\n");
		}
		return ret.toString();
	}
}
