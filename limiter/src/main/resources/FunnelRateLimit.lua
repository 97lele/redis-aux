--参数说明,key[1]为对应服务接口的信息，argv1为capacity,argv2为漏水速率,argv3为一次所需流出的水量,argv4为时间戳
local limitInfo = redis.call('hmget', KEYS[1], 'capacity', 'funnelRate', 'requestNeed', 'water', 'lastTs')
local capacity = limitInfo[1]
local passRate = limitInfo[2]
local addWater = limitInfo[3]
local water = limitInfo[4]
local lastTs = limitInfo[5]

--初始化漏斗
if capacity == false then
    capacity = tonumber(ARGV[1])
    passRate = tonumber(ARGV[2])
    --请求一次所要加的水量
    addWater = tonumber(ARGV[3])
    --当前水量
    water = 0
    lastTs = tonumber(ARGV[4])
    redis.call('hmset', KEYS[1], 'capacity', capacity, 'funnelRate', passRate, 'requestNeed', addWater, 'water', water, 'lastTs', lastTs)
    return true
else
    local nowTs = tonumber(ARGV[4])
    --计算距离上一次请求到现在的漏水量
    local waterPass = tonumber((nowTs - lastTs) * passRate)
    --计算当前水量,即执行漏水
    water = math.max(0, water - waterPass)
    --设置本次请求的时间
    lastTs = nowTs
    --判断是否可以加水
    addWater = tonumber(addWater)
    if capacity - water >= addWater then
        --加水
        water = water + addWater
        --更新当前水量和时间戳
        redis.call('hmset', KEYS[1], 'water', water, 'lastTs', lastTs)
        return true
    end
    return false
end