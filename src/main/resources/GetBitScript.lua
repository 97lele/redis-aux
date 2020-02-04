--ARGV[1]代表单个待判断元素对应的字节长度，
-- 索引 1-length 代表键，0为不存在，1为存在
local array = {}
local aL = table.getn(ARGV)-1
local kL = table.getn(KEYS)
local bitL = ARGV[1]
--把单个元素所对应的下标取出来
local elementSize = aL / bitL
--第一个元素2...ARGV[1]+1,第二个ARGV[1]+2...2ARGV[1]+1
for index = 1, elementSize
    --判断单个元素的情况
do local notAdd = true
    for i = (index - 1) * bitL + 2, index * bitL+1
        --判断对应的键下标是否都为true
    do
        local k = 1
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



