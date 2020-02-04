--key[1]代表单个待判断元素对应的字节长度，
-- 索引 2-length 代表键，0为不存在，1为存在
local array = {}
local aL = table.getn(ARGV)
local kL = table.getn(KEYS)
--把单个元素所对应的下标取出来
local elementSize = aL / tonumber(KEYS[1])
--第一个元素0...key[2],第二个key[2k]+1...2key[2]
for index = 1, elementSize
    --判断单个元素的情况
do local notAdd = true
    for i = (index - 1) * tonumber(KEYS[1]) + 1, index * tonumber(KEYS[1])
        --判断对应的键下标是否都为true
    do
        local k = 2
        while (notAdd and k <= kL)
        do
            if redis.call('getbit', KEYS[k], ARGV[i]) == 0 then
                array[index] = 0
                notAdd = false
            else
                k = k + 1
            end
        end
    end
    if notAdd then array[index] = 1 end
end
return array



