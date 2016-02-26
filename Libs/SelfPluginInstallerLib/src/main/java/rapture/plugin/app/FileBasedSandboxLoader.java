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
package rapture.plugin.app;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;

import org.apache.commons.lang3.tuple.Triple;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.plugin.install.PluginSandbox;
import rapture.plugin.install.PluginSandboxItem;

public class FileBasedSandboxLoader implements SandboxLoader {

	@Override
	public void loadSandboxFromEntries(String root, String part, PluginSandbox sandbox) throws Exception {
		String area = root + part;
		System.out.println("From " + area);

		String[] folders = getResourceFolders(area);
		String[] files = getResourceFiles(area);

		for (String f : files) {
			String path = root + part + f;
			Triple<String, String, Scheme> trip = PluginSandboxItem
					.extractScheme(part + f);
			RaptureURI uri = RaptureURI.createFromFullPathWithAttribute(
					trip.getLeft(), trip.getMiddle(), trip.getRight());
            String fileFullPath = SelfInstaller.class.getResource(path).getPath();
            System.out.println("File " + fileFullPath);
			sandbox.makeItemFromInternalEntry(uri, SelfInstaller.class.getResourceAsStream(path), fileFullPath, null);
		}

		for (String f : folders) {
			loadSandboxFromEntries(root, part + f + "/", sandbox);
		}
	}

	private String[] getResourceFolders(String path) throws Exception {
		URL dirURL = SelfInstaller.class.getClass().getResource(path);

		return new File(dirURL.toURI()).list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isDirectory();
			}

		});
	}

	private String[] getResourceFiles(String path) throws Exception {
		URL dirURL = SelfInstaller.class.getClass().getResource(path);
		return new File(dirURL.toURI()).list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isFile();
			}

		});
	}
}
