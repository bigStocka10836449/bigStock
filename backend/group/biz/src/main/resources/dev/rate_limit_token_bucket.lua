-- ============================================================
-- BigStock Redis Token Bucket
--
-- KEYS[1] = Redis Rate Limit Key
--
-- ARGV[1] = Tokens Per Second
-- ARGV[2] = Maximum Bucket Capacity
--
-- Return:
-- 1 = ALLOWED
-- 0 = REJECTED
-- ============================================================

local key = KEYS[1]

local rate = tonumber(ARGV[1])
local burst = tonumber(ARGV[2])

-- ============================================================
-- 1. Validate Configuration
-- ============================================================

if not rate or not burst or rate <= 0 or burst < 1 then

    return redis.error_reply(
        "INVALID_TOKEN_BUCKET_CONFIGURATION"
    )

end

-- ============================================================
-- 2. Redis Server Time
-- ============================================================

local redisTime = redis.call("TIME")

local now =
    tonumber(redisTime[1])
    + tonumber(redisTime[2]) / 1000000

-- ============================================================
-- 3. Load Token Bucket
-- ============================================================

local bucket = redis.call(
    "HMGET",
    key,
    "tokens",
    "timestamp"
)

local tokens = tonumber(bucket[1])

local lastRefill = tonumber(bucket[2])

if tokens == nil then
    tokens = burst
end

if lastRefill == nil then
    lastRefill = now
end

-- ============================================================
-- 4. Refill Tokens
-- ============================================================

local elapsed = math.max(
    0,
    now - lastRefill
)

local refill = elapsed * rate

local availableTokens = math.min(
    burst,
    tokens + refill
)

-- ============================================================
-- 5. Consume Token
-- ============================================================

local allowed = 0

if availableTokens >= 1 then

    availableTokens = availableTokens - 1

    allowed = 1

end

-- ============================================================
-- 6. Update Bucket
-- ============================================================

redis.call(
    "HSET",
    key,
    "tokens",
    availableTokens,
    "timestamp",
    now
)

-- ============================================================
-- 7. Expiration
--
-- 閒置後自動清除 Bucket。
-- ============================================================

local ttl = math.max(
    60,
    math.ceil(burst / rate) * 2
)

redis.call(
    "EXPIRE",
    key,
    ttl
)

return allowed