--key[1]代表keyList的长度，key[2]代表字节位的长度，索要3--key[1]代表键
for i = 2, tonumber(KEYS[1])
do
    for k = 1, tonumber(KEYS[2])
    do redis.call('setbit', KEYS[i+1], tonumber(ARGV[k]), 1)
    end
end