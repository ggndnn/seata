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
package io.seata.sqlparser.druid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * @author ggndnn
 */
class DefaultDruidLoader implements DruidLoader {
    /**
     * Default druid location in classpath
     */
    private final static String DRUID_LOCATION = "lib/sqlparser/druid.jar";

    private final URL druidUrl;

    /**
     * Mainly for unit test
     *
     * @param druidLocation druid location in classpath
     */
    DefaultDruidLoader(String druidLocation) {
        if (druidLocation == null) {
            druidLocation = DRUID_LOCATION;
        }
        URL url = DruidDelegatingSQLRecognizerFactory.class.getClassLoader().getResource(druidLocation);
        if (url != null) {
            try {
                // TODO  extract druid.jar to temp file now, use URLStreamHandler to handle nested jar loading in the future
                File tempFile = File.createTempFile("seata", "sqlparser");
                try (InputStream input = url.openStream()) {
                    byte[] buf = new byte[1024];
                    OutputStream output = new FileOutputStream(tempFile);
                    while (true) {
                        int readCnt = input.read(buf);
                        if (readCnt < 0) {
                            break;
                        }
                        output.write(buf, 0, readCnt);
                    }
                    output.flush();
                    output.close();
                }
                tempFile.deleteOnExit();
                druidUrl = tempFile.toURI().toURL();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            druidUrl = null;
        }
    }

    /**
     * Get default druid loader
     *
     * @return default druid loader
     */
    static DruidLoader get() {
        return new DefaultDruidLoader(DRUID_LOCATION);
    }

    @Override
    public URL getEmbeddedDruidLocation() {
        if (druidUrl == null) {
            throw new IllegalStateException("Can not find embedded druid");
        }
        return druidUrl;
    }
}
