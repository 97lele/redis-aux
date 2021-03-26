local isExists = redis.call('exists', KEYS[1])
if isExists == 0 then
    return false
else
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return true
end