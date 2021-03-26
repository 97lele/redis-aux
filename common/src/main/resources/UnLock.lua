--当为0时候释放锁
local isExists = redis.call('exists', KEYS[1])
--如果不等0，代表有key存在，如果当前count为0，删掉
if isExists == 1 then
    local lockInfo = redis.call('hmget', KEYS[1], 'currentThread', 'count')
    local currentThread = lockInfo[1]
    local count = tonumber(lockInfo[2])
    --不是自己的锁，不释放
    if currentThread ~= ARGV[1] then
        return 0
    end
    --返回成功解锁
    if count == 1 then
        redis.call('del', KEYS[1])
        return 1
    else
        --重入锁,仍持有
        redis.call('hincrby', KEYS[1], 'count', -1)
        return 2
    end
else
    --代表这个锁是不存在
    return -1
end


