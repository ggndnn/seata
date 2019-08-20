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
package io.seata.server.session.file;

import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.StoreMode;
import io.seata.server.session.AbstractSessionModeInitialize;
import io.seata.server.session.SessionManager;

/**
 * FileBasedSessionModeInitialize
 *
 * @author ggndnn
 */
@LoadLevel(name = "file")
public class FileBasedSessionModeInitialize extends AbstractSessionModeInitialize {
    /**
     * The default session store dir
     */
    private static final String DEFAULT_SESSION_STORE_FILE_DIR = "sessionStore";

    /**
     * The constant CONFIG.
     */
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Override
    public void init() {
        //file store
        String sessionStorePath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR);
        if (sessionStorePath == null) {
            throw new StoreException("the {store.file.dir} is empty.");
        }
        rootSessionManager = EnhancedServiceLoader.load(SessionManager.class, StoreMode.FILE.name(),
                new Object[]{ROOT_SESSION_MANAGER_NAME, sessionStorePath});
        asyncCommittingSessionManager = EnhancedServiceLoader.load(SessionManager.class, DEFAULT,
                new Object[]{ASYNC_COMMITTING_SESSION_MANAGER_NAME});
        retryCommittingSessionManager = EnhancedServiceLoader.load(SessionManager.class, DEFAULT,
                new Object[]{RETRY_COMMITTING_SESSION_MANAGER_NAME});
        retryRollbackingSessionManager = EnhancedServiceLoader.load(SessionManager.class, DEFAULT,
                new Object[]{RETRY_ROLLBACKING_SESSION_MANAGER_NAME});
    }
}
