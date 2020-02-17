--参数说明,key[1]为对应服务接口的信息，argv1为capacity,argv2为令牌生成速率,argv3为每次需要的令牌数,argv4为当前时间戳
local limitInfo = redis.call('hmget', KEYS[1], 'capacity', 'funnelRate', 'leftToken', 'lastTs')
local capacity = limitInfo[1]
local rate = limitInfo[2]
local leftToken = limitInfo[3]
local lastTs = limitInfo[4]

--初始化令牌桶
if capacity == false then
    capacity = tonumber(ARGV[1])
    rate = tonumber(ARGV[2])
    leftToken = tonumber(ARGV[5])
    lastTs = tonumber(ARGV[4])
    redis.call('hmset', KEYS[1], 'capacity', capacity, 'funnelRate', rate, 'leftToken', leftToken, 'lastTs', lastTs)
    return -1
else
    local nowTs = tonumber(ARGV[4])
    --计算距离上一次请求到现在生产令牌数
    local genTokenNum = tonumber((nowTs - lastTs) * rate)
    --计算该段时间的剩余令牌
    leftToken = genTokenNum + leftToken
    --设置剩余令牌
    leftToken = math.min(capacity, leftToken)
    --设置本次请求的时间
    lastTs = nowTs
    local need = tonumber(ARGV[3])
    --返回需要等待的毫秒数,-1则不用等待
    if leftToken >= need then
        --减去需要的令牌
        leftToken = leftToken - need
        --更新剩余空间和上一次的漏水时间戳
        redis.call('hmset', KEYS[1], 'leftToken', leftToken, 'lastTs', lastTs)
        return -1
    end
    return (need - leftToken) / rate
end