-- Single Redis instance; all writes/read/expiry are one atomic operation.
-- ARGV: event millis, processing millis, tx id, customer hash, retention ms, ttl sec.
local t = tonumber(ARGV[1])
local now = tonumber(ARGV[2])
local floor = now - tonumber(ARGV[5])
for _, key in ipairs(KEYS) do
  redis.call('ZREMRANGEBYSCORE', key, '-inf', '(' .. floor)
end
-- NX makes DB rollback/retry and uncertain network outcomes idempotent.
redis.call('ZADD', KEYS[1], 'NX', t, ARGV[3])
redis.call('ZADD', KEYS[2], 'NX', t, ARGV[3] .. ':' .. ARGV[4])
for _, key in ipairs(KEYS) do redis.call('EXPIRE', key, tonumber(ARGV[6])) end
local function count(key, window)
  return redis.call('ZCOUNT', key, '(' .. (t-window), t)
end
local device = redis.call('ZRANGEBYSCORE', KEYS[2], '(' .. (t-3600000), t, 'LIMIT', 0, 10001)
if #device > 10000 then return {-1,-1,-1,-1,-1} end
local distinct = {}
for _, member in ipairs(device) do distinct[string.sub(member,38)] = true end
local n = 0
for _, _ in pairs(distinct) do n = n+1 end
return {count(KEYS[1],60000),count(KEYS[1],300000),count(KEYS[1],3600000),count(KEYS[2],300000),n}
