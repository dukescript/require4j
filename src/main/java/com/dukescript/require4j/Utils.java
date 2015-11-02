package com.dukescript.require4j;

/*
 * #%L
 * Require for Java - a library from the "DukeScript" project.
 * %%
 * Copyright (C) 2015 Dukehoff GmbH
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.InputStreamReader;
import java.net.URL;
import net.java.html.js.JavaScriptBody;
import org.netbeans.html.boot.spi.Fn;

public class Utils {
    private Utils() {
    }
    
    @JavaScriptBody(args = {}, javacall = true, body
        = "window.importScripts = function(url) {\n"
        + "  @com.dukescript.require4j.Utils::importScripts(Ljava/lang/String;)(url);\n"
        + "};\n"
    )
    public static native void registerImportScripts();

    static void importScripts(String url) throws Exception {
        URL u = new URL(url);
        try (InputStreamReader r = new InputStreamReader(u.openStream(), "UTF-8")) {
            Fn.activePresenter().loadScript(r);
        }
    }
}
