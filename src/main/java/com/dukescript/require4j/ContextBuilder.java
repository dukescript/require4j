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

import net.java.html.require.Factory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.java.html.js.JavaScriptBody;
import net.java.html.js.JavaScriptResource;

@JavaScriptResource("require.js")
public class ContextBuilder {
    private final List<Reg> arr = new ArrayList<>();
    
    @JavaScriptBody(args = {  }, body = "")
    public static native void initialize();
    
    private ContextBuilder() {
    }
    
    public static ContextBuilder newBuilder() {
        return new ContextBuilder();
    }
    
    public ContextBuilder define(String name, URL resource) {
        arr.add(new Reg(name, resource, null, null));
        return this;
    }
    
    public ContextBuilder define(String name, Factory f, String... deps) {
        arr.add(new Reg(name, null, f, deps));
        return this;
    }
    
    public Object require(String name) {
        initialize();
        StringBuilder config = new StringBuilder("require.config({\n");
        config.append("  paths: {\n");
        String sep = "";
        for (Reg r : arr) {
            if (r.resource == null) {
                continue;
            }
            config.append(sep);
            String res = r.resource.toString();
            if (res.endsWith(".js")) {
                res = res.substring(0, res.length() - 3);
            }
            config.append("    \"").append(r.name).append("\" : \"").append(res).append("\"\n");
            sep = ",\n";
        }
        config.append("  }\n");
        config.append("});\n");
        eval(config.toString());
        for (Reg r : arr) {
            if (r.factory == null) {
                continue;
            }
            defineFactory(r.name, r.deps, r.factory);
        }
        return requireWait(name);
    }
    
    @JavaScriptBody(args = { "script" }, body = "return eval(script);")
    static native Object eval(String script);
    
    @JavaScriptBody(args = {"name"}, body = "\n"
        + "if (name) {\n"
        + "  require([name]);\n"
        + "  return require(name);\n"
        + "} else {\n"
        + "  require([]);\n"
        + "}\n"
    )
    private static native Object requireWait(String name);
    
    @JavaScriptBody(args = { "name", "deps", "f" }, javacall = true, body = 
        "define(name, deps, function() {\n"
      + "  var args = Array.prototype.slice.call(arguments);\n"
      + "  return @com.dukescript.require4j.ContextBuilder::create(Ljava/lang/Object;Ljava/lang/Object;)(f, args);\n"
      + "});\n"
    )
    public static native void defineFactory(String name, String[] deps, Factory f);
    
    static Object create(Object factory, Object args) {
        Factory<?> f = (Factory) factory;
        Object[] arr = (Object[]) args;
        final Accessor acc = Accessor.getDefault();
        Class[] types = acc.parameterTypes(f);
        if (types != null) {
            int params = types.length;
            if (params != arr.length) {
                throw new IllegalArgumentException("Expecting array of length " + params + " but got " + Arrays.toString(arr));
            }
            for (int i = 0; i < arr.length; i++) {
                if (types[i].isInstance(arr[i])) {
                    continue;
                }
                if (types[i].isInterface()) {
                    arr[i] = convertToInterface(types[i], arr[i]);
                    continue;
                }
                throw new IllegalArgumentException("Expecting " + types[i] + " but found " + arr[i]);
            }
        }
        Object ret = acc.create(f, arr);
        try {
            PH ph = (PH) Proxy.getInvocationHandler(ret);
            return ph.jsObject;
        } catch (Exception ex) {
            return ret;
        }
    }
    
    private static <T> T convertToInterface(Class<T> type, final Object jsObject) {
        List<String> names = new ArrayList<>();
        for (Method m : type.getMethods()) {
            if (m.getDeclaringClass() == Object.class) {
                continue;
            }
            names.add(m.getName());
        }
        String[] required = names.toArray(new String[names.size()]);
        Object[] found = findProps(jsObject, required);
        if (required.length != found.length) {
            throw new IllegalArgumentException("Expecting " + type + " but found only " + Arrays.toString(found) + " functions");
        }
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, new PH(jsObject));
    }
    
    @JavaScriptBody(args = { "obj", "propNames" }, body = 
        "var found = new Array();\n" +
        "for (var i = 0; i < propNames.length; i++) {\n" +
        "  var p = obj[propNames[i]];\n" +
        "  if (typeof p !== 'undefined') {\n" +
        "    found.push(propNames[i]);\n" +
        "  }\n" +
        "\n" +
        "}\n" +
        "return found;\n" +
        "\n"
    )
    private static native Object[] findProps(Object obj, String[] propNames);

    @JavaScriptBody(args = { "jsObject", "name", "args" }, body = 
        "var p = jsObject[name];\n" +
        "var t = typeof p;\n" +
        "if (t === 'function') {\n" +
        "  return jsObject[name].apply(jsObject, args);\n" +
        "} else if (t !== 'undefined') {\n" +
        "  return p;\n" +
        "}\n" +
        "return null;\n" +
        "\n"
    )
    private static native Object callJS(Object jsObject, String name, Object[] args);
            
    
    private static final class Reg {
        private final String name;
        private final URL resource;
        private final Factory factory;
        private final String[] deps;

        public Reg(String name, URL resource, Factory factory, String[] deps) {
            this.name = name;
            this.resource = resource;
            this.factory = factory;
            this.deps = deps;
        }
    }

    private static class PH implements InvocationHandler {
        private final Object jsObject;

        public PH(Object jsObject) {
            this.jsObject = jsObject;
        }
            
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object ret = callJS(jsObject, method.getName(), args);
            Class<?> rt = method.getReturnType();
            if (!rt.isInstance(ret) && rt.isInterface()) {
                return convertToInterface(rt, ret);
            }
            return ret;
        }
    }
    
}
