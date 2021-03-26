package com.xl.redisaux.common.utils.lock;



import java.util.Objects;

/**
 * @author tanjl11
 * @date 2020/10/14 10:18
 */

public enum UnLockStatus {
    LOCK_NOT_EXISTS(-1L, "该锁不存在"),
    NOT_HAVE_LOCK(0L, "该锁非调用线程所占用"),
    UNLOCK_SUCCESS(1L, "解锁成功"),
    RETAIN_LOCK(2L, "解锁但仍持有");
    private Long status;
    private String mean;

    UnLockStatus(Long status, String mean) {
        this.status = status;
        this.mean = mean;
    }

    public Long getStatus() {
        return status;
    }

    public String getMean() {
        return mean;
    }

    public static UnLockStatus getByStatus(Long status){
        for (UnLockStatus value : values()) {
            if(Objects.equals(value.getStatus(),status)){
                return value;
            }
        }
        return null;
    }
}
