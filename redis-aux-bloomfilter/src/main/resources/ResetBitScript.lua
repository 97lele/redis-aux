local key = KEYS[1]
local start, last = 0, tonumber(ARGV[1])
local b = '\0'
-- 把多余的头和尾用setbit处理
local mstart, mlast = start % 8, (last + 1) % 8
--如果尾部有多出来的字节,采用单个字符处理并且把尾指针改变
if mlast > 0 then
    --获取尾指针的地址
    local t = math.max(last - mlast + 1, start)
    for i = t, last do
        redis.call('SETBIT', key, i, b)
    end
    last = t
end
-- 处理开头多余出来的部分
if mstart > 0 then
    local t = math.min(start - mstart + 7, last)
    for i = start, t do
        redis.call('SETBIT', key, i, b)
    end
    start = t + 1
end
-- 设置剩余范围
local rs, re = start / 8, (last + 1) / 8
local rl = re - rs
if rl > 0 then
    --string.rep拼接功能
    redis.call('SETRANGE', key, rs, string.rep(b, rl))
end
-- 删除其他的键
local others = redis.call('keys', string.format("%s-*", key))
for i = 1, table.getn(others)
do redis.call('del', others[i])
end