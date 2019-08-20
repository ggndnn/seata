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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.EnhancedServiceNotFoundException;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.StoreMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Session holder.
 *
 * @author sharajava
 */
public class SessionHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionHolder.class);

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static SessionModeInitialize sessionMode;

    /**
     * Init.
     *
     * @param mode the store mode: file、db
     * @throws IOException the io exception
     */
    public static void init(String mode) throws IOException {
        if (StringUtils.isBlank(mode)) {
            //use default
            mode = CONFIG.getConfig(ConfigurationKeys.STORE_MODE);
        }
        try {
            sessionMode = EnhancedServiceLoader.load(SessionModeInitialize.class, mode);
        } catch(EnhancedServiceNotFoundException e) {
            throw new IllegalArgumentException("unknown store mode:" + mode);
        }
        //relaod
        reload();
    }

    /**
     * Reload.
     */
    protected static void reload() {
        SessionManager rootSessionManager = sessionMode.getRootSessionManager();
        if (rootSessionManager instanceof Reloadable) {
            ((Reloadable)rootSessionManager).reload();

            Collection<GlobalSession> reloadedSessions = rootSessionManager.allSessions();
            if (reloadedSessions != null && !reloadedSessions.isEmpty()) {
                reloadedSessions.forEach(globalSession -> {
                    GlobalStatus globalStatus = globalSession.getStatus();
                    switch (globalStatus) {
                        case UnKnown:
                        case Committed:
                        case CommitFailed:
                        case Rollbacked:
                        case RollbackFailed:
                        case TimeoutRollbacked:
                        case TimeoutRollbackFailed:
                        case Finished:
                            throw new ShouldNeverHappenException("Reloaded Session should NOT be " + globalStatus);
                        case AsyncCommitting:
                            try {
                                globalSession.addSessionLifecycleListener(getAsyncCommittingSessionManager());
                                getAsyncCommittingSessionManager().addGlobalSession(globalSession);
                            } catch (TransactionException e) {
                                throw new ShouldNeverHappenException(e);
                            }
                            break;
                        default: {
                            ArrayList<BranchSession> branchSessions = globalSession.getSortedBranches();
                            // Lock
                            branchSessions.forEach(branchSession -> {
                                try {
                                    branchSession.lock();
                                } catch (TransactionException e) {
                                    throw new ShouldNeverHappenException(e);
                                }
                            });

                            switch (globalStatus) {
                                case Committing:
                                case CommitRetrying:
                                    try {
                                        globalSession.addSessionLifecycleListener(
                                            getRetryCommittingSessionManager());
                                        getRetryCommittingSessionManager().addGlobalSession(globalSession);
                                    } catch (TransactionException e) {
                                        throw new ShouldNeverHappenException(e);
                                    }
                                    break;
                                case Rollbacking:
                                case RollbackRetrying:
                                case TimeoutRollbacking:
                                case TimeoutRollbackRetrying:
                                    try {
                                        globalSession.addSessionLifecycleListener(
                                            getRetryRollbackingSessionManager());
                                        getRetryRollbackingSessionManager().addGlobalSession(globalSession);
                                    } catch (TransactionException e) {
                                        throw new ShouldNeverHappenException(e);
                                    }
                                    break;
                                case Begin:
                                    globalSession.setActive(true);
                                    break;
                                default:
                                    throw new ShouldNeverHappenException("NOT properly handled " + globalStatus);
                            }

                            break;

                        }
                    }

                });
            }
        }
    }

    /**
     * Gets root session manager.
     *
     * @return the root session manager
     */
    public static final SessionManager getRootSessionManager() {
        if (sessionMode.getRootSessionManager() == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return sessionMode.getRootSessionManager();
    }

    /**
     * Gets async committing session manager.
     *
     * @return the async committing session manager
     */
    public static final SessionManager getAsyncCommittingSessionManager() {
        if (sessionMode.getAsyncCommittingSessionManager() == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return sessionMode.getAsyncCommittingSessionManager();
    }

    /**
     * Gets retry committing session manager.
     *
     * @return the retry committing session manager
     */
    public static final SessionManager getRetryCommittingSessionManager() {
        if (sessionMode.getRetryCommittingSessionManager() == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return sessionMode.getRetryCommittingSessionManager();
    }

    /**
     * Gets retry rollbacking session manager.
     *
     * @return the retry rollbacking session manager
     */
    public static final SessionManager getRetryRollbackingSessionManager() {
        if (sessionMode.getRetryRollbackingSessionManager() == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return sessionMode.getRetryRollbackingSessionManager();
    }

    /**
     * Find global session.
     *
     * @param xid the xid
     * @return the global session
     */
    public static GlobalSession findGlobalSession(String xid) {
        return getRootSessionManager().findGlobalSession(xid);
    }

    public static void destory() {
        sessionMode.destory();
    }
}
