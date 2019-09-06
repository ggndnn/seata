/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.sql.druid;

import io.seata.rm.datasource.util.JdbcConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ggndnn
 */
public class DruidIsolationTest {
    private final static String DRUID_CLASS_PREFIX = "com.alibaba.druid.";

    private final static String TEST_SQL = "insert into t_table_1 values(?, ?)";

    @Test
    public void testDruidIsolation() throws Exception {
        Class<?> druidSQLRecognizerFactoryClass = new DruidIsolationTestClassLoader().loadClass("io.seata.rm.datasource.sql.druid.DruidSQLRecognizerFactory");
        Method createMethod = druidSQLRecognizerFactoryClass.getMethod("create", String.class, String.class);
        Object druidSQLRecognizerFactory = druidSQLRecognizerFactoryClass.newInstance();
        createMethod.invoke(druidSQLRecognizerFactory, TEST_SQL, JdbcConstants.MYSQL);
    }

    private static class DruidIsolationTestClassLoader extends URLClassLoader {
        public DruidIsolationTestClassLoader() {
            super(getUrls(), ClassLoader.getSystemClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith(DRUID_CLASS_PREFIX)) {
                return loadInternalClass(name, resolve);
            } else {
                return super.loadClass(name, resolve);
            }
        }

        private Class<?> loadInternalClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c;
            synchronized (getClassLoadingLock(name)) {
                c = findLoadedClass(name);
                if (c == null) {
                    c = findClass(name);
                }
            }
            if (c == null) {
                throw new ClassNotFoundException(name);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }

        private static URL[] getUrls() {
            List<URL> urls = new ArrayList<>();
            urls.add(DruidIsolationTest.class.getClassLoader().getResource("druid-1.0.15.jar"));
            URLClassLoader urlClassLoader = (URLClassLoader) DruidIsolationTest.class.getClassLoader();
            urls.addAll(Arrays.asList(urlClassLoader.getURLs()));
            return urls.toArray(new URL[0]);
        }
    }
}
