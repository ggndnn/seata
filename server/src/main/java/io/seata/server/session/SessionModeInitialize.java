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
package io.seata.server.session;

import io.seata.common.executor.Initialize;

/**
 * @author ggndnn
 */
public interface SessionModeInitialize extends Initialize {
    /**
     * The constant DEFAULT.
     */
    String DEFAULT = "default";

    /**
     * The constant ROOT_SESSION_MANAGER_NAME.
     */
    String ROOT_SESSION_MANAGER_NAME = "root.data";

    /**
     * The constant ASYNC_COMMITTING_SESSION_MANAGER_NAME.
     */
    String ASYNC_COMMITTING_SESSION_MANAGER_NAME = "async.commit.data";

    /**
     * The constant RETRY_COMMITTING_SESSION_MANAGER_NAME.
     */
    String RETRY_COMMITTING_SESSION_MANAGER_NAME = "retry.commit.data";

    /**
     * The constant RETRY_ROLLBACKING_SESSION_MANAGER_NAME.
     */
    String RETRY_ROLLBACKING_SESSION_MANAGER_NAME = "retry.rollback.data";

    SessionManager getRootSessionManager();

    SessionManager getAsyncCommittingSessionManager();

    SessionManager getRetryCommittingSessionManager();

    SessionManager getRetryRollbackingSessionManager();

    void destory();
}
