local kL=table.getn(KEYS)
local aL=table.getn(ARGV)
for i = 1, kL
do
    for k = 1,aL
    do redis.call('setbit', KEYS[i], tonumber(ARGV[k]), 1)
    end
end