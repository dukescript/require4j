package net.java.html.require;

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

import com.dukescript.require4j.ContextBuilder;
import com.dukescript.require4j.Utils;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import net.java.html.boot.script.Scripts;
import net.java.html.js.JavaScriptBody;
import org.netbeans.html.boot.spi.Fn;
import static org.testng.Assert.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RequireUtilsTest {
    private Fn.Presenter presenter;
    private Closeable toClose;
    
    public RequireUtilsTest() {
    }
    
    @BeforeMethod public void initPresenter() throws Exception {
        presenter = Scripts.createPresenter();
        toClose = Fn.activate(presenter);
        exec("var window = this;");
        Utils.registerImportScripts();
        ContextBuilder.initialize();
    }
    
    @AfterMethod public void closePresenter() throws IOException {
        toClose.close();
    }

    @Test
    public void testBasicRequireJSInteraction() throws Exception {
        exec(
            "define('one', function() { return 1; });",
            "define('two', function() { return 2; });",
            "define('plus', ['one', 'two'], \n function(one, two) {\n return one + two;\n });",
            "require([]);"
        );
        
        Object three = eval("require(['plus']); require('plus');");
        assertEquals(three, 3.0);
    }

    @JavaScriptBody(args = { "script" }, body = "return eval(script);")
    static native Object eval(String script);

    @Test
    public void testBasicRequireViaJava() throws Exception {
        ContextBuilder b = ContextBuilder.newBuilder();
        b.define("one", RequireUtilsTest.class.getResource("one.js"));
        b.define("two", RequireUtilsTest.class.getResource("two.js"));
        b.define("plus", RequireUtilsTest.class.getResource("plus.js"));
        
        Object three = b.require("plus");;
        assertEquals(three, 3.0);
    }

    @Test
    public void testBasicRequireComputedInJava() throws Exception {
        ContextBuilder b = ContextBuilder.newBuilder();
        b.define("one", RequireUtilsTest.class.getResource("one.js"));
        b.define("two", RequireUtilsTest.class.getResource("two.js"));
        class Plus extends Factory<Double> {
            @Override
            public Double create(Object... params) {
                assertEquals(params.length, 2, "Two args: " + Arrays.toString(params));
                return ((Number)params[0]).doubleValue() + ((Number)params[1]).doubleValue();
            }
        }
        b.define("plus", new Plus(), "one", "two");
        
        Object three = b.require("plus");;
        assertEquals(three, 3.0);
    }
    @Test
    public void testRequireConsumesValueFromJava() throws Exception {
        ContextBuilder b = ContextBuilder.newBuilder();
        b.define("one", RequireUtilsTest.class.getResource("one.js"));
        
        class Two extends Factory<Double> {
            @Override
            public Double create(Object... params) {
                assertEquals(params.length, 0, "No parameters");
                return 2.0;
            }
        }
        
        b.define("two", new Two());
        b.define("plus", RequireUtilsTest.class.getResource("plus.js"));
        
        Object three = b.require("plus");
        assertEquals(three, 3.0);
    }
    
    @Test
    public void testWorkingWithJSObject() throws Exception {
        ContextBuilder b = ContextBuilder.newBuilder();
        b.define("obj", RequireUtilsTest.class.getResource("hello.js"));
        b.define("msg", new Factory() {
            @Override
            protected Class[] parameterTypes() {
                return new Class[] { Hello.class };
            }
            
            @Override
            public Object create(Object... params) {
                return ((Hello)params[0]).hello();
            }
        }, "obj");
        
        
        Object hello = b.require("msg");
        assertEquals(hello, "Hello from Require.js!");
    }
    
    @Test
    public void testPassJSObjectBackToJS() throws Exception {
        ContextBuilder b = ContextBuilder.newBuilder();
        b.define("obj", RequireUtilsTest.class.getResource("hello.js"));
        b.define("compare", RequireUtilsTest.class.getResource("compare.js"));
        b.define("wrap", new Factory() {
            @Override
            protected Class[] parameterTypes() {
                return new Class[] { Hello.class };
            }
            
            @Override
            public Object create(Object... params) {
                return (Hello)params[0];
            }
        }, "obj");
        
        
        Object hello = b.require("compare");
        assertEquals(hello, Boolean.TRUE);
    }
    
    @Test
    public void testReturningJSObject() throws Exception {
        ContextBuilder b = ContextBuilder.newBuilder();
        b.define("factory", RequireUtilsTest.class.getResource("hellofactory.js"));
        b.define("obj", new Factory() {
            @Override
            protected Class[] parameterTypes() {
                return new Class[] { HelloFactory.class };
            }

            @Override
            protected Object create(Object... params) {
                HelloFactory hf = (HelloFactory) params[0];
                return hf.factory();
            }
        }, "factory");
        b.define("msg", new Factory() {
            @Override
            protected Class[] parameterTypes() {
                return new Class[] { Hello.class };
            }
            
            @Override
            public Object create(Object... params) {
                return ((Hello)params[0]).hello();
            }
        }, "obj");
        
        
        Object hello = b.require("msg");
        assertEquals(hello, "Hello from Require.js!");
    }
    
    
    private static void exec(String... lines) throws Exception {
        for (String l : lines) {
            if (l == null) {
                continue;
            }
            Fn.activePresenter().loadScript(new StringReader(l));
        }
    }
    
    public interface Hello {
        public String hello();
    }
    
    public interface HelloFactory {
        public Hello factory();
    }
}
