package io.seata.benchmark.rpc.netty;

/**
 * @author ggndnn
 */
public class RpcPoolType {
    private RpcPoolType() {
    }

    /**
     * commons-pool and lock
     */
    public final static String POOL_TYPE_AP_1_LOCK = "ap_1_lock";

    /**
     * commons-pool and no lock
     */
    public final static String POOL_TYPE_AP_1_NO_LOCK = "ap_1_no_lock";

    /**
     * commons-pool, lock cas
     */
    public final static String POOL_TYPE_AP_1_LOCK_CAS = "ap_1_lock_cas";

    /**
     * commons-pool2 and lock
     */
    public final static String POOL_TYPE_AP_2_LOCK = "ap_2_lock";

    /**
     * commons-pool2 and no lock
     */
    public final static String POOL_TYPE_AP_2_NO_LOCK = "ap_2_no_lock";

    /**
     * commons-pool2, lock and cas
     */
    public final static String POOL_TYPE_AP_2_LOCK_CAS = "ap_2_lock_cas";
}
