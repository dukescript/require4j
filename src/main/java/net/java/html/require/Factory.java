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

import com.dukescript.require4j.Accessor;
import com.dukescript.require4j.ContextBuilder;


/** Factory that can register a Java object into the requirejs
 * infrastructure. Implement the factory,
 * {@link #Factory(java.lang.String, java.lang.String...) register the object}
 * into the require.js system. When the <code>id</code> is requested,
 * create the Java object to represent the <code>id</code> in the require.js
 * system.
 *
 * @param <R> the return value from the factory
 * @since 0.1
 */
public abstract class Factory<R> {
    /** Defines the factory.
     * 
     * @param id the id for the object that will be created by
     *   {@link #create(java.lang.Object...)} method
     * @param dependencies the names of dependencies - their values
     *   will be passed to {@link #create(java.lang.Object...)} as parameters
     */
    protected Factory(String id, String... dependencies) {
        if (id != null) {
            ContextBuilder.defineFactory(id, dependencies, this);
        }
    }

    /** Defines the factory without registering it anywhere.
     */
    Factory() {
    }

    Class[] parameterTypes() {
        return null;
    }

    /** Creates the object based on provided parameters.
     * The parameters are provided by the require.js infrastructure
     * and represents the values associated with dependencies specified
     * in the constructor.
     *
     * @param params values to use when creating the return value
     * @return the value to represent <code>id</code> specified when
     *   constructing the factory
     * @since 0.1
     */
    protected abstract R create(Object... params);

    static {
        new Accessor() {
            @Override
            protected Class[] parameterTypes(Factory f) {
                return f.parameterTypes();
            }

            @Override
            protected Object create(Factory f, Object... params) {
                return f.create(params);
            }
        };
    }
}
