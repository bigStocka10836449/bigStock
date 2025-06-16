// rate_limit_token_bucket.lua
-- Token Bucket 限流
-- KEYS[1]: 限流 Key
-- ARGV[1]: 每秒補充速率（tokensPerSecond）
-- ARGV[2]: 最大桶容量（burst）
-- ARGV[3]: 當前時間戳（秒）

local rate = tonumber(ARGV[1])
local burst = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- 初始化 Token Bucket 結構
local bucket = redis.call("HMGET", KEYS[1], "tokens", "timestamp")
local tokens = tonumber(bucket[1]) or burst
local lastRefill = tonumber(bucket[2]) or now

-- 計算新的 token 數量
local delta = math.max(0, now - lastRefill)
local refill = delta * rate
local newTokens = math.min(burst, tokens + refill)

if newTokens < 1 then
    return 0
end

-- 減少一個 token 並更新狀態
redis.call("HMSET", KEYS[1], "tokens", newTokens - 1, "timestamp", now)
redis.call("EXPIRE", KEYS[1], 3600)
return 1