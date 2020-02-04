--key[1]代表keyList的长度，key[2]代表字节位的长度，索要3--key[i]代表键
local length = 1
local array = {}
for i = 2, tonumber(KEYS[1])
do
    for k = 1, tonumber(KEYS[2])
    do array[length] = redis.call('getbit', KEYS[i+1], ARGV[k])
        length=length+1
    end
end
return array
