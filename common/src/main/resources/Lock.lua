--一个map,有属性值和调用次数
local isExists = redis.call('exists', KEYS[1])
--代表初始提交
if isExists == 0 then
    redis.call('hmset', KEYS[1], 'currentThread', ARGV[1], 'count', 1);
    --设置超时时间
    redis.call('pexpire', KEYS[1], ARGV[2]);
    return true;
    --否则判断是否是同一个线程，如果是的话加1,返回true
else
    local currentThread = redis.call('hget', KEYS[1], 'currentThread')
    --如果当前线程相等，+1返回true
    if ARGV[1] == currentThread then
        redis.call('hincrby', KEYS[1], 'count', 1)
        return true
    end
    return false;
end



