-- ============================================================
-- BigStock Daily Distinct Stock Quota
--
-- KEYS[1] = Daily Quota Redis Key
--
-- ARGV[1] = Stock Code
-- ARGV[2] = Maximum Distinct Stocks Per Day
-- ARGV[3] = TTL Seconds
--
-- Return:
-- 1 = ALLOWED
-- 0 = DAILY_QUOTA_EXCEEDED
-- ============================================================

local key = KEYS[1]

local stockCode = ARGV[1]

local maxDistinctStocks = tonumber(ARGV[2])

local ttlSeconds = tonumber(ARGV[3])

-- ============================================================
-- 1. Validate
-- ============================================================

if not stockCode or stockCode == "" then

    return redis.error_reply(
        "INVALID_STOCK_CODE"
    )

end

if not maxDistinctStocks or maxDistinctStocks < 1 then

    return redis.error_reply(
        "INVALID_DAILY_QUOTA_CONFIGURATION"
    )

end

if not ttlSeconds or ttlSeconds < 1 then

    return redis.error_reply(
        "INVALID_DAILY_QUOTA_TTL"
    )

end

-- ============================================================
-- 2. Check Existing Stock
--
-- 同一支股票重複查詢不增加每日不同股票配額。
-- ============================================================

local exists = redis.call(
    "SISMEMBER",
    key,
    stockCode
)

if exists == 1 then

    return 1

end

-- ============================================================
-- 3. Current Distinct Count
-- ============================================================

local currentCount = redis.call(
    "SCARD",
    key
)

-- ============================================================
-- 4. Check Quota
-- ============================================================

if currentCount >= maxDistinctStocks then

    return 0

end

-- ============================================================
-- 5. Record Stock
--
-- Lua 原子執行，避免並行請求超過配額。
-- ============================================================

redis.call(
    "SADD",
    key,
    stockCode
)

-- ============================================================
-- 6. Set TTL
-- ============================================================

redis.call(
    "EXPIRE",
    key,
    ttlSeconds
)

return 1