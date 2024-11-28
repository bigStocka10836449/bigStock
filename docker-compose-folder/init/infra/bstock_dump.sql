--
-- PostgreSQL database dump
--

-- Dumped from database version 15.7
-- Dumped by pg_dump version 15.7

-- Started on 2024-11-05 01:27:11 UTC
\connect bstock bstockuser
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 6 (class 2615 OID 16386)
-- Name: bstock; Type: SCHEMA; Schema: -; Owner: bstockuser
--

CREATE SCHEMA bstock;


ALTER SCHEMA bstock OWNER TO bstockuser;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 215 (class 1259 OID 16387)
-- Name: role_info; Type: TABLE; Schema: bstock; Owner: bstockuser
--

CREATE TABLE bstock.role_info (
    id bigint NOT NULL,
    role_name character varying(6),
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    update_by character varying(50),
    role_id character varying(1)
);


ALTER TABLE bstock.role_info OWNER TO bstockuser;

--
-- TOC entry 3445 (class 0 OID 0)
-- Dependencies: 215
-- Name: COLUMN role_info.id; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.role_info.id IS '1: Admin 2: Member 3: User';


--
-- TOC entry 216 (class 1259 OID 16390)
-- Name: role_path; Type: TABLE; Schema: bstock; Owner: bstockuser
--

CREATE TABLE bstock.role_path (
    id bigint NOT NULL,
    role_id bigint NOT NULL,
    role_allowed_url_path character varying
);


ALTER TABLE bstock.role_path OWNER TO bstockuser;

--
-- TOC entry 217 (class 1259 OID 16395)
-- Name: securities_firms_day_operate; Type: TABLE; Schema: bstock; Owner: bstockuser
--

CREATE TABLE bstock.securities_firms_day_operate (
    stock_code text NOT NULL,
    trading_date date NOT NULL,
    seq bigint NOT NULL,
    price text,
    stock_buy_amount bigint,
    stock_sell_amount bigint,
    securities_firms character varying
);


ALTER TABLE bstock.securities_firms_day_operate OWNER TO bstockuser;

--
-- TOC entry 3446 (class 0 OID 0)
-- Dependencies: 217
-- Name: TABLE securities_firms_day_operate; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON TABLE bstock.securities_firms_day_operate IS '買賣日報表';


--
-- TOC entry 3447 (class 0 OID 0)
-- Dependencies: 217
-- Name: COLUMN securities_firms_day_operate.securities_firms; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.securities_firms_day_operate.securities_firms IS '券商';


--
-- TOC entry 218 (class 1259 OID 16400)
-- Name: shareholder_structure; Type: TABLE; Schema: bstock; Owner: bstockuser
--

CREATE TABLE bstock.shareholder_structure (
    week_of_year text,
    count_date text,
    closing_price text,
    price_change text,
    price_change_percent text,
    tdcc_stock text,
    less_1_board_lot text,
    between_1_and_5_board_lot text,
    between_5_and_10_board_lot text,
    between_10_and_15_board_lot text,
    between_15_and_20_board_lot text,
    between_20_and_30_board_lot text,
    between_30_and_40_board_lot text,
    between_40_and_50_board_lot text,
    between_50_and_100_board_lot text,
    between_100_and_200_board_lot text,
    between_200_and_400_board_lot text,
    between_400_and_600_board_lot text,
    between_600_and_800_board_lot text,
    between_800_and_1000_board_lot text,
    over_1000_board_lot text,
    id text NOT NULL,
    stock_code text,
    stock_name text,
    stock_total text,
    less_1_board_lot_people text,
    between_1_and_5_board_lot_people text,
    between_5_and_10_board_lot_people text,
    between_10_and_15_board_lot_people text,
    between_15_and_20_board_lot_people text,
    between_20_and_30_board_lot_people text,
    between_30_and_40_board_lot_people text,
    between_40_and_50_board_lot_people text,
    between_50_and_100_board_lot_people text,
    between_100_and_200_board_lot_people text,
    between_200_and_400_board_lot_people text,
    between_400_and_600_board_lot_people text,
    between_600_and_800_board_lot_people text,
    between_800_and_1000_board_lot_people text,
    over_1000_board_lot_people text,
    total_people text,
    opening_price text
);


ALTER TABLE bstock.shareholder_structure OWNER TO bstockuser;

--
-- TOC entry 3448 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.week_of_year; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.week_of_year IS '集保每周統計週期';


--
-- TOC entry 3449 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.count_date; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.count_date IS '統計日期';


--
-- TOC entry 3450 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.closing_price; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.closing_price IS '收盤價格';


--
-- TOC entry 3451 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.price_change; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.price_change IS '漲跌(元)';


--
-- TOC entry 3452 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.price_change_percent; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.price_change_percent IS '漲跌(%)';


--
-- TOC entry 3453 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.tdcc_stock; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.tdcc_stock IS '集保庫存(萬張)';


--
-- TOC entry 3454 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.less_1_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.less_1_board_lot IS '持股小於1張的人數的股數';


--
-- TOC entry 3455 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_1_and_5_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_1_and_5_board_lot IS '持股介於1至5張的人數的股數';


--
-- TOC entry 3456 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_5_and_10_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_5_and_10_board_lot IS '持股介於5至10張的人數的股數';


--
-- TOC entry 3457 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_10_and_15_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_10_and_15_board_lot IS '持股介於10至15張的人數的股數';


--
-- TOC entry 3458 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_15_and_20_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_15_and_20_board_lot IS '持股介於15至20張的人數的股數';


--
-- TOC entry 3459 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_20_and_30_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_20_and_30_board_lot IS '持股介於20至30張的人數的股數';


--
-- TOC entry 3460 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_30_and_40_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_30_and_40_board_lot IS '持股介於30至40張的人數的股數';


--
-- TOC entry 3461 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_40_and_50_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_40_and_50_board_lot IS '持股介於40至50張的人數的股數';


--
-- TOC entry 3462 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_50_and_100_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_50_and_100_board_lot IS '持股介於50至100張的人數的股數';


--
-- TOC entry 3463 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_100_and_200_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_100_and_200_board_lot IS '持股介於100至200張的人數的股數';


--
-- TOC entry 3464 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_200_and_400_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_200_and_400_board_lot IS '持股介於200至400張的人數的股數';


--
-- TOC entry 3465 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_400_and_600_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_400_and_600_board_lot IS '持股介於400至600張的人數的股數';


--
-- TOC entry 3466 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_600_and_800_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_600_and_800_board_lot IS '持股介於600至800張的人數的股數';


--
-- TOC entry 3467 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.between_800_and_1000_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.between_800_and_1000_board_lot IS '持股介於800至1000張的人數的股數';


--
-- TOC entry 3468 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.over_1000_board_lot; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.over_1000_board_lot IS '持股超過1000張的人數';


--
-- TOC entry 3469 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.id; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.id IS 'Id';


--
-- TOC entry 3470 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.stock_total; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.stock_total IS '總股數';


--
-- TOC entry 3471 (class 0 OID 0)
-- Dependencies: 218
-- Name: COLUMN shareholder_structure.opening_price; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.shareholder_structure.opening_price IS '開盤價(周)';




CREATE TABLE bstock.tmp_trade_volume_info (
	stock_code text ,
	trading_day date ,
	trade_volume text 
	
);



ALTER TABLE bstock.tmp_trade_volume_info OWNER TO bstockuser;

ALTER TABLE ONLY bstock.tmp_trade_volume_info
    ADD CONSTRAINT tmp_trade_volume_info_unique UNIQUE (stock_code, trading_day);




CREATE TABLE bstock.margin_trading_and_short_selling_info (
	trading_day date ,
	stock_code text ,
	margin_purchase_balance_previous_day text ,
	margin_purchase text ,
	margin_sales text ,
	cash_redemption text ,
	margin_purchase_balance text ,
	margin_purchase_quota text ,
	short_sale_balance_previous_day text ,
	short_sale text ,
	short_convering text ,
	stock_redemption text ,
	short_sale_balance text ,
	short_sale_quota text ,
	offsetting text 
	
);


COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.margin_purchase_balance_previous_day IS '融資前日餘額';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.margin_purchase IS '融資買進';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.margin_sales IS '融資賣出';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.cash_redemption IS '融資現金償還';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.margin_purchase_balance IS '融資今日餘額';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.margin_purchase_quota IS '融資限額';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.short_sale_balance_previous_day IS '融券前日餘額';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.short_sale IS '融券賣出';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.short_convering IS '融券現券償還';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.stock_redemption IS '融券股票償還';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.short_sale_balance IS '融券今日餘額';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.short_sale_quota IS '融券限額';
COMMENT ON COLUMN bstock.margin_trading_and_short_selling_info.offsetting IS '資券互抵(資券的單沖)';




ALTER TABLE bstock.margin_trading_and_short_selling_info OWNER TO bstockuser;

ALTER TABLE ONLY bstock.margin_trading_and_short_selling_info
    ADD CONSTRAINT margin_trading_and_short_selling_info_unique UNIQUE (trading_day, stock_code);


--
-- TOC entry 219 (class 1259 OID 16405)
-- Name: stock_day_price; Type: TABLE; Schema: bstock; Owner: bstockuser
--

CREATE TABLE bstock.stock_day_price (
    stock_code text,
    trading_day date,
    opening_price text,
    closing_price text,
    high_price text,
    low_price text,
    start_of_week_date date,
    end_of_week_date date,
    change text,
    week_of_year text,
    trading_volume text
);


ALTER TABLE bstock.stock_day_price OWNER TO bstockuser;

--
-- TOC entry 3472 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN stock_day_price.stock_code; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_day_price.stock_code IS '股票代號';


--
-- TOC entry 3473 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN stock_day_price.trading_date; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_day_price.trading_day IS '交易日';


--
-- TOC entry 3474 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN stock_day_price.opening_price; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_day_price.opening_price IS '開盤價';


--
-- TOC entry 3475 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN stock_day_price.closing_price; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_day_price.closing_price IS '收盤價';


--
-- TOC entry 3476 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN stock_day_price.high_price; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_day_price.high_price IS '最高價';


--
-- TOC entry 3477 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN stock_day_price.low_price; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_day_price.low_price IS '最低價';


COMMENT ON COLUMN bstock.stock_day_price.trading_volume IS '成交量(股,數量會有些微落差)';

--
-- TOC entry 3478 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN stock_day_price.start_of_week_date; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_day_price.start_of_week_date IS '這周的星期一日期';


--
-- TOC entry 3479 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN stock_day_price.end_of_week_date; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_day_price.end_of_week_date IS '這周的星期天日期';


--
-- TOC entry 3480 (class 0 OID 0)
-- Dependencies: 219
-- Name: COLUMN stock_day_price.change; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_day_price.change IS '漲跌幅';


--
-- TOC entry 222 (class 1259 OID 16449)
-- Name: stock_exchange_detail; Type: TABLE; Schema: bstock; Owner: bstockuser
--

CREATE TABLE bstock.stock_exchange_detail (
    stock_code text NOT NULL,
    exchange_time text NOT NULL,
    trading_date date NOT NULL,
    exchage_price text NOT NULL,
    exchage_quantity integer NOT NULL,
    seq integer
);


ALTER TABLE bstock.stock_exchange_detail OWNER TO bstockuser;

--
-- TOC entry 3481 (class 0 OID 0)
-- Dependencies: 222
-- Name: COLUMN stock_exchange_detail.stock_code; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_exchange_detail.stock_code IS '股票代號';


--
-- TOC entry 3482 (class 0 OID 0)
-- Dependencies: 222
-- Name: COLUMN stock_exchange_detail.exchange_time; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_exchange_detail.exchange_time IS '成交時間';


--
-- TOC entry 3483 (class 0 OID 0)
-- Dependencies: 222
-- Name: COLUMN stock_exchange_detail.trading_date; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_exchange_detail.trading_date IS '交易日期';


--
-- TOC entry 3484 (class 0 OID 0)
-- Dependencies: 222
-- Name: COLUMN stock_exchange_detail.exchage_price; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_exchange_detail.exchage_price IS '成交價格';


--
-- TOC entry 3485 (class 0 OID 0)
-- Dependencies: 222
-- Name: COLUMN stock_exchange_detail.exchage_quantity; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_exchange_detail.exchage_quantity IS '成交量';


--
-- TOC entry 3486 (class 0 OID 0)
-- Dependencies: 222
-- Name: COLUMN stock_exchange_detail.seq; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_exchange_detail.seq IS '流水號(區別如果是同時間同價格同數量的時候)';


--
-- TOC entry 220 (class 1259 OID 16410)
-- Name: stock_info; Type: TABLE; Schema: bstock; Owner: bstockuser
--

CREATE TABLE bstock.stock_info (
    stock_code text NOT NULL,
    stock_name text,
    stock_type character(1)
);


ALTER TABLE bstock.stock_info OWNER TO bstockuser;

--
-- TOC entry 3487 (class 0 OID 0)
-- Dependencies: 220
-- Name: COLUMN stock_info.stock_code; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_info.stock_code IS '股票代號';


--
-- TOC entry 3488 (class 0 OID 0)
-- Dependencies: 220
-- Name: COLUMN stock_info.stock_name; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_info.stock_name IS '股票名稱';


--
-- TOC entry 3489 (class 0 OID 0)
-- Dependencies: 220
-- Name: COLUMN stock_info.stock_type; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.stock_info.stock_type IS '股票類型 0: 上櫃 1:上市 2: 興櫃';


--
-- TOC entry 221 (class 1259 OID 16415)
-- Name: user_account; Type: TABLE; Schema: bstock; Owner: bstockuser
--

CREATE TABLE bstock.user_account (
    id character varying(25) NOT NULL,
    phone character varying(11),
    mail character varying(200) NOT NULL,
    gender character varying(1),
    birth_date date,
    role_id character varying(1),
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    update_by character varying(50),
    user_password character varying(64),
    status character varying(1),
    user_name character varying(200)
);


ALTER TABLE bstock.user_account OWNER TO bstockuser;

--
-- TOC entry 3490 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN user_account.phone; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.user_account.phone IS '電話';


--
-- TOC entry 3491 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN user_account.mail; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.user_account.mail IS '電子信箱';


--
-- TOC entry 3492 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN user_account.gender; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.user_account.gender IS '性別';


--
-- TOC entry 3493 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN user_account.birth_date; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.user_account.birth_date IS '生日';


--
-- TOC entry 3494 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN user_account.role_id; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.user_account.role_id IS '使用者的角色';


--
-- TOC entry 3495 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN user_account.user_password; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.user_account.user_password IS '使用者密碼';


--
-- TOC entry 3496 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN user_account.status; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.user_account.status IS '帳號目前狀態 1: 使用中 2:註冊中尚未驗證 3.刪除的帳號';


--
-- TOC entry 3497 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN user_account.user_name; Type: COMMENT; Schema: bstock; Owner: bstockuser
--

COMMENT ON COLUMN bstock.user_account.user_name IS '使用者姓名(看要不要顯示用)';


--
-- TOC entry 3432 (class 0 OID 16387)
-- Dependencies: 215
-- Data for Name: role_info; Type: TABLE DATA; Schema: bstock; Owner: bstockuser
--

COPY bstock.role_info (id, role_name, create_time, update_time, update_by, role_id) FROM stdin;
1	Admin	\N	\N	\N	1
2	Member	\N	\N	\N	2
3	User	\N	\N	\N	3
\.


--
-- TOC entry 3433 (class 0 OID 16390)
-- Dependencies: 216
-- Data for Name: role_path; Type: TABLE DATA; Schema: bstock; Owner: bstockuser
--

COPY bstock.role_path (id, role_id, role_allowed_url_path) FROM stdin;
1	1	{"POST":["/txn","/gateway/**","/biz/**"],"GET":["/gateway/**","/biz/**","/api/biz/**"]}
2	2	{"POST":["/txn","/gateway/**","/biz/**"],"GET":["/gateway/**","/biz/**","/api/biz/**"]}
3	3	{"POST":["/txn","/gateway/**"],"GET":["/gateway/**","/biz/**","/api/biz/**"]}
\.


--
-- TOC entry 3434 (class 0 OID 16395)
-- Dependencies: 217
-- Data for Name: securities_firms_day_operate; Type: TABLE DATA; Schema: bstock; Owner: bstockuser
--

COPY bstock.securities_firms_day_operate (stock_code, trading_date, seq, price, stock_buy_amount, stock_sell_amount, securities_firms) FROM stdin;
\.


--
-- TOC entry 3435 (class 0 OID 16400)
-- Dependencies: 218
-- Data for Name: shareholder_structure; Type: TABLE DATA; Schema: bstock; Owner: bstockuser
--

COPY bstock.shareholder_structure (week_of_year, count_date, closing_price, price_change, price_change_percent, tdcc_stock, less_1_board_lot, between_1_and_5_board_lot, between_5_and_10_board_lot, between_10_and_15_board_lot, between_15_and_20_board_lot, between_20_and_30_board_lot, between_30_and_40_board_lot, between_40_and_50_board_lot, between_50_and_100_board_lot, between_100_and_200_board_lot, between_200_and_400_board_lot, between_400_and_600_board_lot, between_600_and_800_board_lot, between_800_and_1000_board_lot, over_1000_board_lot, id, stock_code, stock_name, stock_total, less_1_board_lot_people, between_1_and_5_board_lot_people, between_5_and_10_board_lot_people, between_10_and_15_board_lot_people, between_15_and_20_board_lot_people, between_20_and_30_board_lot_people, between_30_and_40_board_lot_people, between_40_and_50_board_lot_people, between_50_and_100_board_lot_people, between_100_and_200_board_lot_people, between_200_and_400_board_lot_people, between_400_and_600_board_lot_people, between_600_and_800_board_lot_people, between_800_and_1000_board_lot_people, over_1000_board_lot_people, total_people, opening_price) FROM stdin;
\.


--
-- TOC entry 3436 (class 0 OID 16405)
-- Dependencies: 219
-- Data for Name: stock_day_price; Type: TABLE DATA; Schema: bstock; Owner: bstockuser
--

COPY bstock.stock_day_price (stock_code, trading_day, opening_price, closing_price, high_price, low_price, start_of_week_date, end_of_week_date, change, week_of_year) FROM stdin;
\.


--
-- TOC entry 3439 (class 0 OID 16449)
-- Dependencies: 222
-- Data for Name: stock_exchange_detail; Type: TABLE DATA; Schema: bstock; Owner: bstockuser
--

COPY bstock.stock_exchange_detail (stock_code, exchange_time, trading_date, exchage_price, exchage_quantity, seq) FROM stdin;
1101	14:30:00	2024-11-01	32.10	12	1
1101	13:30:00	2024-11-01	32.10	2103	2
1101	13:24:52	2024-11-01	31.95	2	3
1101	13:24:52	2024-11-01	32.05	1	4
1101	13:24:46	2024-11-01	31.95	1	5
1101	13:24:45	2024-11-01	32.00	1	6
1101	13:24:45	2024-11-01	32.00	1	7
1101	13:24:44	2024-11-01	32.00	3	8
1101	13:24:31	2024-11-01	32.05	6	9
1101	13:24:17	2024-11-01	32.00	1	10
1101	13:24:17	2024-11-01	32.00	3	11
1101	13:24:16	2024-11-01	32.05	1	12
1101	13:24:15	2024-11-01	32.00	3	13
1101	13:24:14	2024-11-01	32.00	1	14
1101	13:24:13	2024-11-01	31.95	2	15
1101	13:24:11	2024-11-01	31.95	1	16
1101	13:24:11	2024-11-01	31.95	6	17
1101	13:24:10	2024-11-01	32.00	2	18
1101	13:24:09	2024-11-01	32.00	1	19
1101	13:24:08	2024-11-01	32.00	2	20
1101	13:24:08	2024-11-01	32.05	1	21
1101	13:24:05	2024-11-01	32.00	1	22
1101	13:24:05	2024-11-01	32.00	1	23
1101	13:24:01	2024-11-01	32.05	6	24
1101	13:24:00	2024-11-01	32.00	1	25
1101	13:24:00	2024-11-01	32.05	1	26
1101	13:23:59	2024-11-01	32.00	2	27
1101	13:23:55	2024-11-01	32.00	1	28
1101	13:23:53	2024-11-01	32.00	1	29
1101	13:23:51	2024-11-01	32.00	1	30
1101	13:23:38	2024-11-01	32.00	1	31
1101	13:23:37	2024-11-01	32.00	1	32
1101	13:23:31	2024-11-01	32.05	6	33
1101	13:23:27	2024-11-01	32.00	2	34
1101	13:23:25	2024-11-01	32.05	2	35
1101	13:23:21	2024-11-01	32.00	1	36
1101	13:23:20	2024-11-01	32.00	1	37
1101	13:23:19	2024-11-01	32.00	1	38
1101	13:23:19	2024-11-01	32.00	1	39
1101	13:23:15	2024-11-01	32.00	2	40
1101	13:23:13	2024-11-01	32.00	5	41
1101	13:23:08	2024-11-01	32.00	3	42
1101	13:23:06	2024-11-01	32.00	2	43
1101	13:23:05	2024-11-01	32.05	1	44
1101	13:23:04	2024-11-01	32.05	1	45
1101	13:23:04	2024-11-01	32.05	9	46
1101	13:23:03	2024-11-01	32.05	1	47
1101	13:23:03	2024-11-01	32.05	2	48
1101	13:23:03	2024-11-01	32.05	1	49
1101	13:23:02	2024-11-01	32.05	1	50
1101	13:23:02	2024-11-01	32.05	2	51
1101	13:23:02	2024-11-01	32.05	1	52
1101	13:23:02	2024-11-01	32.05	2	53
1101	13:23:02	2024-11-01	32.10	2	54
1101	13:23:02	2024-11-01	32.10	4	55
1101	13:23:01	2024-11-01	32.10	1	56
1101	13:23:01	2024-11-01	32.10	2	57
1101	13:23:01	2024-11-01	32.10	25	58
1101	13:23:01	2024-11-01	32.15	6	59
1101	13:23:00	2024-11-01	32.10	2	60
1101	13:23:00	2024-11-01	32.10	6	61
1101	13:22:56	2024-11-01	32.10	5	62
1101	13:22:55	2024-11-01	32.10	2	63
1101	13:22:51	2024-11-01	32.10	2	64
1101	13:22:51	2024-11-01	32.10	3	65
1101	13:22:51	2024-11-01	32.10	2	66
1101	13:22:51	2024-11-01	32.10	2	67
1101	13:22:51	2024-11-01	32.10	3	68
1101	13:22:49	2024-11-01	32.10	3	69
1101	13:22:47	2024-11-01	32.10	2	70
1101	13:22:46	2024-11-01	32.15	1	71
1101	13:22:41	2024-11-01	32.10	3	72
1101	13:22:37	2024-11-01	32.15	5	73
1101	13:22:37	2024-11-01	32.10	3	74
1101	13:22:37	2024-11-01	32.15	6	75
1101	13:22:31	2024-11-01	32.15	6	76
1101	13:22:26	2024-11-01	32.15	3	77
1101	13:22:16	2024-11-01	32.15	5	78
1101	13:22:15	2024-11-01	32.10	1	79
1101	13:22:14	2024-11-01	32.10	1	80
1101	13:22:12	2024-11-01	32.15	4	81
1101	13:22:11	2024-11-01	32.10	1	82
1101	13:22:11	2024-11-01	32.10	1	83
1101	13:22:09	2024-11-01	32.10	1	84
1101	13:22:08	2024-11-01	32.10	1	85
1101	13:22:01	2024-11-01	32.15	6	86
1101	13:22:00	2024-11-01	32.15	2	87
1101	13:21:46	2024-11-01	32.10	2	88
1101	13:21:46	2024-11-01	32.15	2	89
1101	13:21:45	2024-11-01	32.10	10	90
1101	13:21:45	2024-11-01	32.15	4	91
1101	13:21:43	2024-11-01	32.10	3	92
1101	13:21:42	2024-11-01	32.15	1	93
1101	13:21:38	2024-11-01	32.10	1	94
1101	13:21:37	2024-11-01	32.15	2	95
1101	13:21:36	2024-11-01	32.10	1	96
1101	13:21:33	2024-11-01	32.10	1	97
1101	13:21:31	2024-11-01	32.15	6	98
1101	13:21:25	2024-11-01	32.15	2	99
1101	13:21:25	2024-11-01	32.15	2	100
1101	13:21:05	2024-11-01	32.15	2	101
1101	13:21:01	2024-11-01	32.15	2	102
1101	13:21:01	2024-11-01	32.15	6	103
1101	13:20:49	2024-11-01	32.10	11	104
1101	13:20:49	2024-11-01	32.15	1	105
1101	13:20:40	2024-11-01	32.10	5	106
1101	13:20:36	2024-11-01	32.10	1	107
1101	13:20:31	2024-11-01	32.10	3	108
1101	13:20:31	2024-11-01	32.15	1	109
1101	13:20:31	2024-11-01	32.15	3	110
1101	13:20:31	2024-11-01	32.15	3	111
1101	13:20:31	2024-11-01	32.15	1	112
1101	13:20:31	2024-11-01	32.10	1	113
1101	13:20:31	2024-11-01	32.10	4	114
1101	13:20:31	2024-11-01	32.10	2	115
1101	13:20:31	2024-11-01	32.10	2	116
1101	13:20:31	2024-11-01	32.10	109	117
1101	13:20:31	2024-11-01	32.10	6	118
1101	13:20:31	2024-11-01	32.10	2	119
1101	13:20:30	2024-11-01	32.10	1	120
1101	13:20:30	2024-11-01	32.10	1	121
1101	13:20:23	2024-11-01	32.10	1	122
1101	13:20:12	2024-11-01	32.10	1	123
1101	13:20:11	2024-11-01	32.10	2	124
1101	13:20:00	2024-11-01	32.10	6	125
1101	13:20:00	2024-11-01	32.10	2	126
1101	13:19:52	2024-11-01	32.05	4	127
1101	13:19:50	2024-11-01	32.10	1	128
1101	13:19:31	2024-11-01	32.10	6	129
1101	13:19:30	2024-11-01	32.10	2	130
1101	13:19:29	2024-11-01	32.05	2	131
1101	13:19:26	2024-11-01	32.10	3	132
1101	13:19:26	2024-11-01	32.10	5	133
1101	13:19:15	2024-11-01	32.10	3	134
1101	13:19:02	2024-11-01	32.05	2	135
1101	13:19:01	2024-11-01	32.10	6	136
1101	13:18:56	2024-11-01	32.05	11	137
1101	13:18:55	2024-11-01	32.10	2	138
1101	13:18:53	2024-11-01	32.10	1	139
1101	13:18:50	2024-11-01	32.10	1	140
1101	13:18:50	2024-11-01	32.05	1	141
1101	13:18:49	2024-11-01	32.10	2	142
1101	13:18:39	2024-11-01	32.10	1	143
1101	13:18:31	2024-11-01	32.10	6	144
1101	13:18:27	2024-11-01	32.10	4	145
1101	13:18:19	2024-11-01	32.10	3	146
1101	13:18:18	2024-11-01	32.05	2	147
1101	13:18:09	2024-11-01	32.10	1	148
1101	13:18:09	2024-11-01	32.05	2	149
1101	13:18:01	2024-11-01	32.10	6	150
1101	13:17:52	2024-11-01	32.10	3	151
1101	13:17:49	2024-11-01	32.10	2	152
1101	13:17:49	2024-11-01	32.10	1	153
1101	13:17:31	2024-11-01	32.10	2	154
1101	13:17:31	2024-11-01	32.10	1	155
1101	13:17:31	2024-11-01	32.10	6	156
1101	13:17:24	2024-11-01	32.05	4	157
1101	13:17:09	2024-11-01	32.05	2	158
1101	13:17:07	2024-11-01	32.10	1	159
1101	13:17:03	2024-11-01	32.05	1	160
1101	13:17:01	2024-11-01	32.10	6	161
1101	13:16:45	2024-11-01	32.10	1	162
1101	13:16:44	2024-11-01	32.10	2	163
1101	13:16:31	2024-11-01	32.10	6	164
1101	13:16:27	2024-11-01	32.10	2	165
1101	13:16:26	2024-11-01	32.10	3	166
1101	13:16:24	2024-11-01	32.10	2	167
1101	13:16:23	2024-11-01	32.10	1	168
1101	13:16:01	2024-11-01	32.10	1	169
1101	13:16:01	2024-11-01	32.10	1	170
1101	13:16:01	2024-11-01	32.10	6	171
1101	13:15:58	2024-11-01	32.10	2	172
1101	13:15:58	2024-11-01	32.10	1	173
1101	13:15:43	2024-11-01	32.10	2	174
1101	13:15:42	2024-11-01	32.10	1	175
1101	13:15:31	2024-11-01	32.10	1	176
1101	13:15:31	2024-11-01	32.10	1	177
1101	13:15:30	2024-11-01	32.10	6	178
1101	13:15:30	2024-11-01	32.10	1	179
1101	13:15:24	2024-11-01	32.05	1	180
1101	13:15:20	2024-11-01	32.10	2	181
1101	13:15:18	2024-11-01	32.05	5	182
1101	13:15:06	2024-11-01	32.10	1	183
1101	13:15:02	2024-11-01	32.05	2	184
1101	13:15:01	2024-11-01	32.10	6	185
1101	13:15:00	2024-11-01	32.10	1	186
1101	13:14:59	2024-11-01	32.05	3	187
1101	13:14:52	2024-11-01	32.10	2	188
1101	13:14:31	2024-11-01	32.10	6	189
1101	13:14:29	2024-11-01	32.05	3	190
1101	13:14:28	2024-11-01	32.05	1	191
1101	13:14:13	2024-11-01	32.10	2	192
1101	13:14:10	2024-11-01	32.05	5	193
1101	13:14:01	2024-11-01	32.10	6	194
1101	13:13:56	2024-11-01	32.10	2	195
1101	13:13:44	2024-11-01	32.05	1	196
1101	13:13:36	2024-11-01	32.05	1	197
1101	13:13:31	2024-11-01	32.10	2	198
1101	13:13:31	2024-11-01	32.10	6	199
1101	13:13:15	2024-11-01	32.10	2	200
1101	13:13:15	2024-11-01	32.10	2	201
1101	13:13:09	2024-11-01	32.10	2	202
1101	13:13:01	2024-11-01	32.05	4	203
1101	13:13:01	2024-11-01	32.10	6	204
1101	13:12:54	2024-11-01	32.10	2	205
1101	13:12:54	2024-11-01	32.10	4	206
1101	13:12:31	2024-11-01	32.05	2	207
1101	13:12:31	2024-11-01	32.10	2	208
1101	13:12:31	2024-11-01	32.10	6	209
1101	13:12:23	2024-11-01	32.05	6	210
1101	13:12:09	2024-11-01	32.10	2	211
1101	13:12:08	2024-11-01	32.10	2	212
1101	13:12:01	2024-11-01	32.10	6	213
1101	13:11:55	2024-11-01	32.05	1	214
1101	13:11:55	2024-11-01	32.05	1	215
1101	13:11:41	2024-11-01	32.10	2	216
1101	13:11:41	2024-11-01	32.10	1	217
1101	13:11:39	2024-11-01	32.10	1	218
1101	13:11:30	2024-11-01	32.10	6	219
1101	13:11:26	2024-11-01	32.10	2	220
1101	13:11:04	2024-11-01	32.05	1	221
1101	13:11:03	2024-11-01	32.10	5	222
1101	13:11:01	2024-11-01	32.10	6	223
1101	13:10:59	2024-11-01	32.10	2	224
1101	13:10:59	2024-11-01	32.10	2	225
1101	13:10:57	2024-11-01	32.10	1	226
1101	13:10:41	2024-11-01	32.10	2	227
1101	13:10:31	2024-11-01	32.10	6	228
1101	13:10:27	2024-11-01	32.05	1	229
1101	13:10:22	2024-11-01	32.05	1	230
1101	13:10:18	2024-11-01	32.05	1	231
1101	13:10:15	2024-11-01	32.05	2	232
1101	13:10:15	2024-11-01	32.05	1	233
1101	13:10:11	2024-11-01	32.10	3	234
1101	13:10:11	2024-11-01	32.05	1	235
1101	13:10:01	2024-11-01	32.05	1	236
1101	13:10:01	2024-11-01	32.10	6	237
1101	13:09:55	2024-11-01	32.05	2	238
1101	13:09:54	2024-11-01	32.05	1	239
1101	13:09:47	2024-11-01	32.05	9	240
1101	13:09:46	2024-11-01	32.05	1	241
1101	13:09:45	2024-11-01	32.10	2	242
1101	13:09:44	2024-11-01	32.05	1	243
1101	13:09:40	2024-11-01	32.10	2	244
1101	13:09:32	2024-11-01	32.05	1	245
1101	13:09:32	2024-11-01	32.05	1	246
1101	13:09:31	2024-11-01	32.10	6	247
1101	13:09:23	2024-11-01	32.10	10	248
1101	13:09:19	2024-11-01	32.10	2	249
1101	13:09:18	2024-11-01	32.05	1	250
1101	13:09:17	2024-11-01	32.10	2	251
1101	13:09:12	2024-11-01	32.05	6	252
1101	13:09:04	2024-11-01	32.05	1	253
1101	13:09:03	2024-11-01	32.05	1	254
1101	13:09:01	2024-11-01	32.10	2	255
1101	13:09:01	2024-11-01	32.10	6	256
1101	13:08:50	2024-11-01	32.05	1	257
1101	13:08:37	2024-11-01	32.10	2	258
1101	13:08:36	2024-11-01	32.05	1	259
1101	13:08:31	2024-11-01	32.10	6	260
1101	13:08:28	2024-11-01	32.05	1	261
1101	13:08:22	2024-11-01	32.05	1	262
1101	13:08:13	2024-11-01	32.10	2	263
1101	13:08:08	2024-11-01	32.05	1	264
1101	13:08:01	2024-11-01	32.10	6	265
1101	13:07:54	2024-11-01	32.05	1	266
1101	13:07:40	2024-11-01	32.05	1	267
1101	13:07:31	2024-11-01	32.10	6	268
1101	13:07:29	2024-11-01	32.05	1	269
1101	13:07:29	2024-11-01	32.05	10	270
1101	13:07:26	2024-11-01	32.05	1	271
1101	13:07:12	2024-11-01	32.05	1	272
1101	13:07:01	2024-11-01	32.10	6	273
1101	13:06:59	2024-11-01	32.05	5	274
1101	13:06:58	2024-11-01	32.05	1	275
1101	13:06:55	2024-11-01	32.05	1	276
1101	13:06:51	2024-11-01	32.05	1	277
1101	13:06:45	2024-11-01	32.10	2	278
1101	13:06:44	2024-11-01	32.05	1	279
1101	13:06:32	2024-11-01	32.05	1	280
1101	13:06:30	2024-11-01	32.10	6	281
1101	13:06:30	2024-11-01	32.05	1	282
1101	13:06:25	2024-11-01	32.10	5	283
1101	13:06:17	2024-11-01	32.10	3	284
1101	13:06:16	2024-11-01	32.05	5	285
1101	13:06:16	2024-11-01	32.05	1	286
1101	13:06:09	2024-11-01	32.10	1	287
1101	13:06:02	2024-11-01	32.05	1	288
1101	13:06:01	2024-11-01	32.10	6	289
1101	13:05:50	2024-11-01	32.05	1	290
1101	13:05:48	2024-11-01	32.05	1	291
1101	13:05:45	2024-11-01	32.10	3	292
1101	13:05:34	2024-11-01	32.05	1	293
1101	13:05:32	2024-11-01	32.05	1	294
1101	13:05:32	2024-11-01	32.10	1	295
1101	13:05:31	2024-11-01	32.10	6	296
1101	13:05:29	2024-11-01	32.10	1	297
1101	13:05:27	2024-11-01	32.10	1	298
1101	13:05:20	2024-11-01	32.05	1	299
1101	13:05:18	2024-11-01	32.10	2	300
1101	13:05:06	2024-11-01	32.05	1	301
1101	13:05:05	2024-11-01	32.05	1	302
1101	13:05:01	2024-11-01	32.05	2	303
1101	13:05:00	2024-11-01	32.10	6	304
1101	13:04:53	2024-11-01	32.10	2	305
1101	13:04:52	2024-11-01	32.05	1	306
1101	13:04:38	2024-11-01	32.05	1	307
1101	13:04:31	2024-11-01	32.10	2	308
1101	13:04:31	2024-11-01	32.10	6	309
1101	13:04:27	2024-11-01	32.05	2	310
1101	13:04:27	2024-11-01	32.05	1	311
1101	13:04:24	2024-11-01	32.05	1	312
1101	13:04:19	2024-11-01	32.05	1	313
1101	13:04:18	2024-11-01	32.10	1	314
1101	13:04:13	2024-11-01	32.05	1	315
1101	13:04:10	2024-11-01	32.05	1	316
1101	13:04:02	2024-11-01	32.05	1	317
1101	13:04:01	2024-11-01	32.10	6	318
1101	13:03:57	2024-11-01	32.05	1	319
1101	13:03:56	2024-11-01	32.05	1	320
1101	13:03:53	2024-11-01	32.05	2	321
1101	13:03:53	2024-11-01	32.05	2	322
1101	13:03:53	2024-11-01	32.05	1	323
1101	13:03:53	2024-11-01	32.05	1	324
1101	13:03:42	2024-11-01	32.05	1	325
1101	13:03:36	2024-11-01	32.05	3	326
1101	13:03:32	2024-11-01	32.05	1	327
1101	13:03:31	2024-11-01	32.10	6	328
1101	13:03:28	2024-11-01	32.05	1	329
1101	13:03:15	2024-11-01	32.05	1	330
1101	13:03:14	2024-11-01	32.05	1	331
1101	13:03:01	2024-11-01	32.10	6	332
1101	13:03:00	2024-11-01	32.05	1	333
1101	13:02:57	2024-11-01	32.10	2	334
1101	13:02:46	2024-11-01	32.05	1	335
1101	13:02:45	2024-11-01	32.05	1	336
1101	13:02:42	2024-11-01	32.10	2	337
1101	13:02:41	2024-11-01	32.05	1	338
1101	13:02:32	2024-11-01	32.05	1	339
1101	13:02:31	2024-11-01	32.10	6	340
1101	13:02:18	2024-11-01	32.05	1	341
1101	13:02:04	2024-11-01	32.05	1	342
1101	13:02:01	2024-11-01	32.10	6	343
1101	13:01:51	2024-11-01	32.05	2	344
1101	13:01:51	2024-11-01	32.05	2	345
1101	13:01:50	2024-11-01	32.05	1	346
1101	13:01:36	2024-11-01	32.05	1	347
1101	13:01:30	2024-11-01	32.10	6	348
1101	13:01:30	2024-11-01	32.05	1	349
1101	13:01:29	2024-11-01	32.05	3	350
1101	13:01:29	2024-11-01	32.05	29	351
1101	13:01:22	2024-11-01	32.00	1	352
1101	13:01:08	2024-11-01	32.00	1	353
1101	13:01:07	2024-11-01	32.00	2	354
1101	13:01:07	2024-11-01	32.05	1	355
1101	13:01:01	2024-11-01	32.05	6	356
1101	13:00:59	2024-11-01	32.00	9	357
1101	13:00:54	2024-11-01	32.00	1	358
1101	13:00:40	2024-11-01	32.00	1	359
1101	13:00:33	2024-11-01	32.00	2	360
1101	13:00:31	2024-11-01	32.05	4	361
1101	13:00:31	2024-11-01	32.05	16	362
1101	13:00:31	2024-11-01	32.05	6	363
1101	13:00:26	2024-11-01	32.00	1	364
1101	13:00:12	2024-11-01	32.00	1	365
1101	13:00:01	2024-11-01	32.05	6	366
1101	13:00:00	2024-11-01	32.00	1	367
1101	12:59:58	2024-11-01	32.00	1	368
1101	12:59:53	2024-11-01	31.95	2	369
1101	12:59:52	2024-11-01	32.00	6	370
1101	12:59:52	2024-11-01	32.00	69	371
1101	12:59:52	2024-11-01	32.00	3	372
1101	12:59:52	2024-11-01	32.05	11	373
1101	12:59:52	2024-11-01	32.05	4	374
1101	12:59:52	2024-11-01	32.00	3	375
1101	12:59:52	2024-11-01	32.00	52	376
1101	12:59:52	2024-11-01	32.05	135	377
1101	12:59:44	2024-11-01	32.05	1	378
1101	12:59:31	2024-11-01	32.10	6	379
1101	12:59:30	2024-11-01	32.05	1	380
1101	12:59:30	2024-11-01	32.10	2	381
1101	12:59:19	2024-11-01	32.10	1	382
1101	12:59:16	2024-11-01	32.05	1	383
1101	12:59:06	2024-11-01	32.05	2	384
1101	12:59:02	2024-11-01	32.05	1	385
1101	12:59:00	2024-11-01	32.10	6	386
1101	12:58:48	2024-11-01	32.10	1	387
1101	12:58:48	2024-11-01	32.05	1	388
1101	12:58:35	2024-11-01	32.10	2	389
1101	12:58:34	2024-11-01	32.05	1	390
1101	12:58:30	2024-11-01	32.10	6	391
1101	12:58:20	2024-11-01	32.05	1	392
1101	12:58:20	2024-11-01	32.10	3	393
1101	12:58:06	2024-11-01	32.05	1	394
1101	12:58:01	2024-11-01	32.10	6	395
1101	12:57:53	2024-11-01	32.10	2	396
1101	12:57:52	2024-11-01	32.05	1	397
1101	12:57:38	2024-11-01	32.05	1	398
1101	12:57:31	2024-11-01	32.10	2	399
1101	12:57:31	2024-11-01	32.10	6	400
1101	12:57:24	2024-11-01	32.05	1	401
1101	12:57:10	2024-11-01	32.10	2	402
1101	12:57:10	2024-11-01	32.05	1	403
1101	12:57:01	2024-11-01	32.10	6	404
1101	12:56:56	2024-11-01	32.05	1	405
1101	12:56:48	2024-11-01	32.10	2	406
1101	12:56:48	2024-11-01	32.05	2	407
1101	12:56:42	2024-11-01	32.05	1	408
1101	12:56:31	2024-11-01	32.10	6	409
1101	12:56:28	2024-11-01	32.05	1	410
1101	12:56:27	2024-11-01	32.05	2	411
1101	12:56:26	2024-11-01	32.10	2	412
1101	12:56:14	2024-11-01	32.05	1	413
1101	12:56:05	2024-11-01	32.10	1	414
1101	12:56:00	2024-11-01	32.10	7	415
1101	12:56:00	2024-11-01	32.05	1	416
1101	12:55:58	2024-11-01	32.10	3	417
1101	12:55:46	2024-11-01	32.05	1	418
1101	12:55:46	2024-11-01	32.05	1	419
1101	12:55:33	2024-11-01	32.05	1	420
1101	12:55:32	2024-11-01	32.05	1	421
1101	12:55:31	2024-11-01	32.10	2	422
1101	12:55:31	2024-11-01	32.10	7	423
1101	12:55:18	2024-11-01	32.05	1	424
1101	12:55:18	2024-11-01	32.05	1	425
1101	12:55:09	2024-11-01	32.05	1	426
1101	12:55:05	2024-11-01	32.10	1	427
1101	12:55:05	2024-11-01	32.10	2	428
1101	12:55:04	2024-11-01	32.05	1	429
1101	12:55:01	2024-11-01	32.05	2	430
1101	12:55:01	2024-11-01	32.10	7	431
1101	12:54:50	2024-11-01	32.05	1	432
1101	12:54:47	2024-11-01	32.10	2	433
1101	12:54:36	2024-11-01	32.05	1	434
1101	12:54:31	2024-11-01	32.10	7	435
1101	12:54:22	2024-11-01	32.05	1	436
1101	12:54:20	2024-11-01	32.10	2	437
1101	12:54:20	2024-11-01	32.10	2	438
1101	12:54:08	2024-11-01	32.05	1	439
1101	12:54:04	2024-11-01	32.05	2	440
1101	12:54:01	2024-11-01	32.10	7	441
1101	12:53:54	2024-11-01	32.10	3	442
1101	12:53:54	2024-11-01	32.05	1	443
1101	12:53:40	2024-11-01	32.05	1	444
1101	12:53:38	2024-11-01	32.05	3	445
1101	12:53:30	2024-11-01	32.10	7	446
1101	12:53:26	2024-11-01	32.05	1	447
1101	12:53:23	2024-11-01	32.10	2	448
1101	12:53:12	2024-11-01	32.05	1	449
1101	12:53:09	2024-11-01	32.10	1	450
1101	12:53:06	2024-11-01	32.10	2	451
1101	12:53:06	2024-11-01	32.10	2	452
1101	12:53:01	2024-11-01	32.10	7	453
1101	12:52:58	2024-11-01	32.05	1	454
1101	12:52:49	2024-11-01	32.10	1	455
1101	12:52:44	2024-11-01	32.05	1	456
1101	12:52:44	2024-11-01	32.10	2	457
1101	12:52:31	2024-11-01	32.10	7	458
1101	12:52:30	2024-11-01	32.05	1	459
1101	12:52:22	2024-11-01	32.05	1	460
1101	12:52:21	2024-11-01	32.10	2	461
1101	12:52:21	2024-11-01	32.10	1	462
1101	12:52:16	2024-11-01	32.05	1	463
1101	12:52:02	2024-11-01	32.05	1	464
1101	12:52:01	2024-11-01	32.10	7	465
1101	12:51:56	2024-11-01	32.05	4	466
1101	12:51:52	2024-11-01	32.10	2	467
1101	12:51:52	2024-11-01	32.10	2	468
1101	12:51:48	2024-11-01	32.05	1	469
1101	12:51:34	2024-11-01	32.05	1	470
1101	12:51:31	2024-11-01	32.10	7	471
1101	12:51:30	2024-11-01	32.10	3	472
1101	12:51:20	2024-11-01	32.05	1	473
1101	12:51:09	2024-11-01	32.10	1	474
1101	12:51:06	2024-11-01	32.05	1	475
1101	12:51:01	2024-11-01	32.10	7	476
1101	12:51:00	2024-11-01	32.10	2	477
1101	12:51:00	2024-11-01	32.05	1	478
1101	12:50:58	2024-11-01	32.05	1	479
1101	12:50:58	2024-11-01	32.05	1	480
1101	12:50:55	2024-11-01	32.10	7	481
1101	12:50:52	2024-11-01	32.05	1	482
1101	12:50:38	2024-11-01	32.10	2	483
1101	12:50:38	2024-11-01	32.05	1	484
1101	12:50:38	2024-11-01	32.10	2	485
1101	12:50:31	2024-11-01	32.10	7	486
1101	12:50:24	2024-11-01	32.05	1	487
1101	12:50:17	2024-11-01	32.10	2	488
1101	12:50:17	2024-11-01	32.10	1	489
1101	12:50:10	2024-11-01	32.05	1	490
1101	12:50:01	2024-11-01	32.10	7	491
1101	12:50:00	2024-11-01	32.10	1	492
1101	12:49:56	2024-11-01	32.05	1	493
1101	12:49:56	2024-11-01	32.05	1	494
1101	12:49:55	2024-11-01	32.10	2	495
1101	12:49:54	2024-11-01	32.05	1	496
1101	12:49:42	2024-11-01	32.05	1	497
1101	12:49:31	2024-11-01	32.10	2	498
1101	12:49:31	2024-11-01	32.10	7	499
1101	12:49:28	2024-11-01	32.05	1	500
1101	12:49:20	2024-11-01	32.05	1	501
1101	12:49:15	2024-11-01	32.05	1	502
1101	12:49:14	2024-11-01	32.05	1	503
1101	12:49:01	2024-11-01	32.10	7	504
1101	12:49:00	2024-11-01	32.05	1	505
1101	12:48:56	2024-11-01	32.05	1	506
1101	12:48:46	2024-11-01	32.05	1	507
1101	12:48:32	2024-11-01	32.05	1	508
1101	12:48:31	2024-11-01	32.10	7	509
1101	12:48:23	2024-11-01	32.05	1	510
1101	12:48:20	2024-11-01	32.05	10	511
1101	12:48:18	2024-11-01	32.05	1	512
1101	12:48:08	2024-11-01	32.05	1	513
1101	12:48:04	2024-11-01	32.05	1	514
1101	12:48:01	2024-11-01	32.10	7	515
1101	12:47:50	2024-11-01	32.05	1	516
1101	12:47:36	2024-11-01	32.05	1	517
1101	12:47:35	2024-11-01	32.05	4	518
1101	12:47:35	2024-11-01	32.05	3	519
1101	12:47:33	2024-11-01	32.05	1	520
1101	12:47:31	2024-11-01	32.10	2	521
1101	12:47:31	2024-11-01	32.10	7	522
1101	12:47:22	2024-11-01	32.05	1	523
1101	12:47:14	2024-11-01	32.10	1	524
1101	12:47:08	2024-11-01	32.10	2	525
1101	12:47:08	2024-11-01	32.05	1	526
1101	12:47:01	2024-11-01	32.10	7	527
1101	12:46:59	2024-11-01	32.10	1	528
1101	12:46:56	2024-11-01	32.10	1	529
1101	12:46:54	2024-11-01	32.05	1	530
1101	12:46:48	2024-11-01	32.10	2	531
1101	12:46:47	2024-11-01	32.10	1	532
1101	12:46:40	2024-11-01	32.05	1	533
1101	12:46:30	2024-11-01	32.10	7	534
1101	12:46:28	2024-11-01	32.05	1	535
1101	12:46:26	2024-11-01	32.05	1	536
1101	12:46:25	2024-11-01	32.10	2	537
1101	12:46:12	2024-11-01	32.05	1	538
1101	12:46:01	2024-11-01	32.10	7	539
1101	12:45:58	2024-11-01	32.05	1	540
1101	12:45:57	2024-11-01	32.10	3	541
1101	12:45:44	2024-11-01	32.05	1	542
1101	12:45:37	2024-11-01	32.05	1	543
1101	12:45:36	2024-11-01	32.10	24	544
1101	12:45:31	2024-11-01	32.10	7	545
1101	12:45:30	2024-11-01	32.05	1	546
1101	12:45:27	2024-11-01	32.10	2	547
1101	12:45:17	2024-11-01	32.10	1	548
1101	12:45:16	2024-11-01	32.05	1	549
1101	12:45:02	2024-11-01	32.10	2	550
1101	12:45:02	2024-11-01	32.05	1	551
1101	12:45:01	2024-11-01	32.10	7	552
1101	12:44:58	2024-11-01	32.05	1	553
1101	12:44:53	2024-11-01	32.05	4	554
1101	12:44:48	2024-11-01	32.05	1	555
1101	12:44:44	2024-11-01	32.10	2	556
1101	12:44:34	2024-11-01	32.05	1	557
1101	12:44:31	2024-11-01	32.10	7	558
1101	12:44:20	2024-11-01	32.10	2	559
1101	12:44:20	2024-11-01	32.05	1	560
1101	12:44:06	2024-11-01	32.05	1	561
1101	12:44:03	2024-11-01	32.10	1	562
1101	12:44:01	2024-11-01	32.10	2	563
1101	12:44:00	2024-11-01	32.10	7	564
1101	12:43:52	2024-11-01	32.05	1	565
1101	12:43:51	2024-11-01	32.05	1	566
1101	12:43:50	2024-11-01	32.10	10	567
1101	12:43:46	2024-11-01	32.05	1	568
1101	12:43:38	2024-11-01	32.10	2	569
1101	12:43:38	2024-11-01	32.05	1	570
1101	12:43:32	2024-11-01	32.05	1	571
1101	12:43:31	2024-11-01	32.05	1	572
1101	12:43:31	2024-11-01	32.10	7	573
1101	12:43:24	2024-11-01	32.05	1	574
1101	12:43:22	2024-11-01	32.05	1	575
1101	12:43:20	2024-11-01	32.05	2	576
1101	12:43:12	2024-11-01	32.05	1	577
1101	12:43:10	2024-11-01	32.05	1	578
1101	12:43:08	2024-11-01	32.10	2	579
1101	12:43:08	2024-11-01	32.10	1	580
1101	12:43:01	2024-11-01	32.05	1	581
1101	12:43:01	2024-11-01	32.10	7	582
1101	12:43:00	2024-11-01	32.05	1	583
1101	12:42:56	2024-11-01	32.05	1	584
1101	12:42:43	2024-11-01	32.10	2	585
1101	12:42:42	2024-11-01	32.05	1	586
1101	12:42:31	2024-11-01	32.05	1	587
1101	12:42:31	2024-11-01	32.10	7	588
1101	12:42:28	2024-11-01	32.05	1	589
1101	12:42:25	2024-11-01	32.10	3	590
1101	12:42:14	2024-11-01	32.05	1	591
1101	12:42:12	2024-11-01	32.05	1	592
1101	12:42:01	2024-11-01	32.10	2	593
1101	12:42:00	2024-11-01	32.10	7	594
1101	12:42:00	2024-11-01	32.05	1	595
1101	12:42:00	2024-11-01	32.05	1	596
1101	12:41:57	2024-11-01	32.05	1	597
1101	12:41:57	2024-11-01	32.10	2	598
1101	12:41:46	2024-11-01	32.05	1	599
1101	12:41:41	2024-11-01	32.05	1	600
1101	12:41:35	2024-11-01	32.05	1	601
1101	12:41:32	2024-11-01	32.05	2	602
1101	12:41:32	2024-11-01	32.05	1	603
1101	12:41:30	2024-11-01	32.10	7	604
1101	12:41:18	2024-11-01	32.05	1	605
1101	12:41:15	2024-11-01	32.10	2	606
1101	12:41:15	2024-11-01	32.05	1	607
1101	12:41:04	2024-11-01	32.05	1	608
1101	12:41:01	2024-11-01	32.10	7	609
1101	12:40:50	2024-11-01	32.05	1	610
1101	12:40:49	2024-11-01	32.10	1	611
1101	12:40:36	2024-11-01	32.05	1	612
1101	12:40:35	2024-11-01	32.05	3	613
1101	12:40:34	2024-11-01	32.05	1	614
1101	12:40:31	2024-11-01	32.10	2	615
1101	12:40:31	2024-11-01	32.10	7	616
1101	12:40:22	2024-11-01	32.05	1	617
1101	12:40:16	2024-11-01	32.05	1	618
1101	12:40:08	2024-11-01	32.05	1	619
1101	12:40:03	2024-11-01	32.05	1	620
1101	12:40:00	2024-11-01	32.10	7	621
1101	12:39:58	2024-11-01	32.05	1	622
1101	12:39:54	2024-11-01	32.05	1	623
1101	12:39:40	2024-11-01	32.05	1	624
1101	12:39:33	2024-11-01	32.05	1	625
1101	12:39:33	2024-11-01	32.05	4	626
1101	12:39:33	2024-11-01	32.05	12	627
1101	12:39:31	2024-11-01	32.10	7	628
1101	12:39:26	2024-11-01	32.05	1	629
1101	12:39:12	2024-11-01	32.05	1	630
1101	12:39:00	2024-11-01	32.10	7	631
1101	12:38:58	2024-11-01	32.05	1	632
1101	12:38:54	2024-11-01	32.05	3	633
1101	12:38:44	2024-11-01	32.05	1	634
1101	12:38:33	2024-11-01	32.05	1	635
1101	12:38:32	2024-11-01	32.05	1	636
1101	12:38:31	2024-11-01	32.05	1	637
1101	12:38:31	2024-11-01	32.10	7	638
1101	12:38:16	2024-11-01	32.05	1	639
1101	12:38:02	2024-11-01	32.05	1	640
1101	12:38:01	2024-11-01	32.10	7	641
1101	12:37:53	2024-11-01	32.05	3	642
1101	12:37:52	2024-11-01	32.10	3	643
1101	12:37:52	2024-11-01	32.10	3	644
1101	12:37:52	2024-11-01	32.10	85	645
1101	12:37:48	2024-11-01	32.10	1	646
1101	12:37:43	2024-11-01	32.10	1	647
1101	12:37:34	2024-11-01	32.10	1	648
1101	12:37:32	2024-11-01	32.10	1	649
1101	12:37:30	2024-11-01	32.15	7	650
1101	12:37:24	2024-11-01	32.10	1	651
1101	12:37:22	2024-11-01	32.10	1	652
1101	12:37:21	2024-11-01	32.15	2	653
1101	12:37:21	2024-11-01	32.10	2	654
1101	12:37:20	2024-11-01	32.10	1	655
1101	12:37:18	2024-11-01	32.10	5	656
1101	12:37:06	2024-11-01	32.10	1	657
1101	12:37:05	2024-11-01	32.10	1	658
1101	12:37:01	2024-11-01	32.15	3	659
1101	12:37:01	2024-11-01	32.15	7	660
1101	12:36:52	2024-11-01	32.10	1	661
1101	12:36:38	2024-11-01	32.10	1	662
1101	12:36:30	2024-11-01	32.15	7	663
1101	12:36:24	2024-11-01	32.10	1	664
1101	12:36:10	2024-11-01	32.10	1	665
1101	12:36:01	2024-11-01	32.15	7	666
1101	12:35:56	2024-11-01	32.10	1	667
1101	12:35:45	2024-11-01	32.10	1	668
1101	12:35:45	2024-11-01	32.10	2	669
1101	12:35:45	2024-11-01	32.10	20	670
1101	12:35:42	2024-11-01	32.10	1	671
1101	12:35:38	2024-11-01	32.10	1	672
1101	12:35:31	2024-11-01	32.15	7	673
1101	12:35:28	2024-11-01	32.10	1	674
1101	12:35:15	2024-11-01	32.15	4	675
1101	12:35:14	2024-11-01	32.10	1	676
1101	12:35:02	2024-11-01	32.10	1	677
1101	12:35:01	2024-11-01	32.15	7	678
1101	12:35:00	2024-11-01	32.10	1	679
1101	12:34:59	2024-11-01	32.10	1	680
1101	12:34:50	2024-11-01	32.10	1	681
1101	12:34:50	2024-11-01	32.10	2	682
1101	12:34:46	2024-11-01	32.10	1	683
1101	12:34:44	2024-11-01	32.15	1	684
1101	12:34:32	2024-11-01	32.10	1	685
1101	12:34:31	2024-11-01	32.15	7	686
1101	12:34:20	2024-11-01	32.10	1	687
1101	12:34:19	2024-11-01	32.10	3	688
1101	12:34:19	2024-11-01	32.10	25	689
1101	12:34:18	2024-11-01	32.10	1	690
1101	12:34:15	2024-11-01	32.10	1	691
1101	12:34:13	2024-11-01	32.15	2	692
1101	12:34:09	2024-11-01	32.10	1	693
1101	12:34:07	2024-11-01	32.10	1	694
1101	12:34:04	2024-11-01	32.10	1	695
1101	12:34:04	2024-11-01	32.10	1	696
1101	12:34:03	2024-11-01	32.10	6	697
1101	12:34:01	2024-11-01	32.15	7	698
1101	12:33:57	2024-11-01	32.10	1	699
1101	12:33:56	2024-11-01	32.10	1	700
1101	12:33:50	2024-11-01	32.15	2	701
1101	12:33:50	2024-11-01	32.10	1	702
1101	12:33:43	2024-11-01	32.10	1	703
1101	12:33:36	2024-11-01	32.10	1	704
1101	12:33:36	2024-11-01	32.10	2	705
1101	12:33:35	2024-11-01	32.10	1	706
1101	12:33:30	2024-11-01	32.15	7	707
1101	12:33:25	2024-11-01	32.15	2	708
1101	12:33:24	2024-11-01	32.10	1	709
1101	12:33:22	2024-11-01	32.10	1	710
1101	12:33:11	2024-11-01	32.10	1	711
1101	12:33:08	2024-11-01	32.15	2	712
1101	12:33:08	2024-11-01	32.10	1	713
1101	12:33:03	2024-11-01	32.10	1	714
1101	12:33:03	2024-11-01	32.10	1	715
1101	12:33:01	2024-11-01	32.15	7	716
1101	12:32:54	2024-11-01	32.10	1	717
1101	12:32:54	2024-11-01	32.10	1	718
1101	12:32:50	2024-11-01	32.15	2	719
1101	12:32:49	2024-11-01	32.10	1	720
1101	12:32:40	2024-11-01	32.10	1	721
1101	12:32:39	2024-11-01	32.10	1	722
1101	12:32:35	2024-11-01	32.10	1	723
1101	12:32:31	2024-11-01	32.15	7	724
1101	12:32:27	2024-11-01	32.15	2	725
1101	12:32:27	2024-11-01	32.10	1	726
1101	12:32:26	2024-11-01	32.10	1	727
1101	12:32:23	2024-11-01	32.10	1	728
1101	12:32:12	2024-11-01	32.10	1	729
1101	12:32:09	2024-11-01	32.15	1	730
1101	12:32:05	2024-11-01	32.15	2	731
1101	12:32:05	2024-11-01	32.10	1	732
1101	12:32:04	2024-11-01	32.10	4	733
1101	12:32:01	2024-11-01	32.15	7	734
1101	12:31:59	2024-11-01	32.10	1	735
1101	12:31:55	2024-11-01	32.15	2	736
1101	12:31:48	2024-11-01	32.10	4	737
1101	12:31:44	2024-11-01	32.10	1	738
1101	12:31:42	2024-11-01	32.15	3	739
1101	12:31:31	2024-11-01	32.15	7	740
1101	12:31:30	2024-11-01	32.10	1	741
1101	12:31:17	2024-11-01	32.10	1	742
1101	12:31:03	2024-11-01	32.15	2	743
1101	12:31:03	2024-11-01	32.10	1	744
1101	12:31:01	2024-11-01	32.10	1	745
1101	12:31:01	2024-11-01	32.15	7	746
1101	12:30:49	2024-11-01	32.10	1	747
1101	12:30:48	2024-11-01	32.10	1	748
1101	12:30:47	2024-11-01	32.15	2	749
1101	12:30:35	2024-11-01	32.10	1	750
1101	12:30:30	2024-11-01	32.15	7	751
1101	12:30:21	2024-11-01	32.10	1	752
1101	12:30:19	2024-11-01	32.15	1	753
1101	12:30:08	2024-11-01	32.10	1	754
1101	12:30:06	2024-11-01	32.15	1	755
1101	12:30:01	2024-11-01	32.15	7	756
1101	12:29:55	2024-11-01	32.10	1	757
1101	12:29:53	2024-11-01	32.10	1	758
1101	12:29:53	2024-11-01	32.15	2	759
1101	12:29:52	2024-11-01	32.10	3	760
1101	12:29:39	2024-11-01	32.10	1	761
1101	12:29:31	2024-11-01	32.15	2	762
1101	12:29:31	2024-11-01	32.15	7	763
1101	12:29:25	2024-11-01	32.10	1	764
1101	12:29:11	2024-11-01	32.15	2	765
1101	12:29:11	2024-11-01	32.10	1	766
1101	12:29:01	2024-11-01	32.15	7	767
1101	12:28:57	2024-11-01	32.10	1	768
1101	12:28:43	2024-11-01	32.10	1	769
1101	12:28:35	2024-11-01	32.10	1	770
1101	12:28:34	2024-11-01	32.15	3	771
1101	12:28:31	2024-11-01	32.15	7	772
1101	12:28:30	2024-11-01	32.10	3	773
1101	12:28:29	2024-11-01	32.10	3	774
1101	12:28:29	2024-11-01	32.10	1	775
1101	12:28:27	2024-11-01	32.15	2	776
1101	12:28:15	2024-11-01	32.10	1	777
1101	12:28:15	2024-11-01	32.10	1	778
1101	12:28:01	2024-11-01	32.10	1	779
1101	12:28:01	2024-11-01	32.15	2	780
1101	12:28:01	2024-11-01	32.15	7	781
1101	12:27:47	2024-11-01	32.10	1	782
1101	12:27:43	2024-11-01	32.15	2	783
1101	12:27:33	2024-11-01	32.10	1	784
1101	12:27:33	2024-11-01	32.10	1	785
1101	12:27:30	2024-11-01	32.15	7	786
1101	12:27:19	2024-11-01	32.15	2	787
1101	12:27:19	2024-11-01	32.10	1	788
1101	12:27:05	2024-11-01	32.10	1	789
1101	12:27:00	2024-11-01	32.15	3	790
1101	12:27:00	2024-11-01	32.15	7	791
1101	12:26:51	2024-11-01	32.15	3	792
1101	12:26:51	2024-11-01	32.10	1	793
1101	12:26:41	2024-11-01	32.10	3	794
1101	12:26:37	2024-11-01	32.10	1	795
1101	12:26:31	2024-11-01	32.15	7	796
1101	12:26:30	2024-11-01	32.10	1	797
1101	12:26:27	2024-11-01	32.10	1	798
1101	12:26:23	2024-11-01	32.15	2	799
1101	12:26:23	2024-11-01	32.10	1	800
1101	12:26:09	2024-11-01	32.10	1	801
1101	12:26:01	2024-11-01	32.15	2	802
1101	12:26:01	2024-11-01	32.15	7	803
1101	12:25:55	2024-11-01	32.10	1	804
1101	12:25:41	2024-11-01	32.10	1	805
1101	12:25:31	2024-11-01	32.15	7	806
1101	12:25:27	2024-11-01	32.10	1	807
1101	12:25:21	2024-11-01	32.10	4	808
1101	12:25:13	2024-11-01	32.10	1	809
1101	12:25:02	2024-11-01	32.10	1	810
1101	12:25:01	2024-11-01	32.15	7	811
1101	12:25:00	2024-11-01	32.15	1	812
1101	12:24:59	2024-11-01	32.10	1	813
1101	12:24:55	2024-11-01	32.15	2	814
1101	12:24:55	2024-11-01	32.10	2	815
1101	12:24:45	2024-11-01	32.10	1	816
1101	12:24:31	2024-11-01	32.10	1	817
1101	12:24:31	2024-11-01	32.15	7	818
1101	12:24:18	2024-11-01	32.10	4	819
1101	12:24:17	2024-11-01	32.10	1	820
1101	12:24:15	2024-11-01	32.10	1	821
1101	12:24:15	2024-11-01	32.10	1	822
1101	12:24:15	2024-11-01	32.10	10	823
1101	12:24:03	2024-11-01	32.10	1	824
1101	12:24:01	2024-11-01	32.10	1	825
1101	12:24:01	2024-11-01	32.15	7	826
1101	12:23:56	2024-11-01	32.15	2	827
1101	12:23:56	2024-11-01	32.10	1	828
1101	12:23:54	2024-11-01	32.10	1	829
1101	12:23:49	2024-11-01	32.10	1	830
1101	12:23:47	2024-11-01	32.10	1	831
1101	12:23:35	2024-11-01	32.15	2	832
1101	12:23:35	2024-11-01	32.10	1	833
1101	12:23:31	2024-11-01	32.10	1	834
1101	12:23:31	2024-11-01	32.15	1	835
1101	12:23:31	2024-11-01	32.15	7	836
1101	12:23:23	2024-11-01	32.10	1	837
1101	12:23:22	2024-11-01	32.10	1	838
1101	12:23:21	2024-11-01	32.10	1	839
1101	12:23:17	2024-11-01	32.10	1	840
1101	12:23:17	2024-11-01	32.15	2	841
1101	12:23:07	2024-11-01	32.10	1	842
1101	12:23:05	2024-11-01	32.15	1	843
1101	12:23:04	2024-11-01	32.10	1	844
1101	12:23:01	2024-11-01	32.15	7	845
1101	12:22:55	2024-11-01	32.10	1	846
1101	12:22:55	2024-11-01	32.10	1	847
1101	12:22:53	2024-11-01	32.10	1	848
1101	12:22:52	2024-11-01	32.15	2	849
1101	12:22:39	2024-11-01	32.10	1	850
1101	12:22:31	2024-11-01	32.15	2	851
1101	12:22:31	2024-11-01	32.10	1	852
1101	12:22:30	2024-11-01	32.15	7	853
1101	12:22:25	2024-11-01	32.10	1	854
1101	12:22:11	2024-11-01	32.15	2	855
1101	12:22:11	2024-11-01	32.10	1	856
1101	12:22:03	2024-11-01	32.10	1	857
1101	12:22:03	2024-11-01	32.10	4	858
1101	12:22:01	2024-11-01	32.10	1	859
1101	12:22:00	2024-11-01	32.15	7	860
1101	12:21:57	2024-11-01	32.10	1	861
1101	12:21:44	2024-11-01	32.10	1	862
1101	12:21:43	2024-11-01	32.10	1	863
1101	12:21:41	2024-11-01	32.10	1	864
1101	12:21:41	2024-11-01	32.15	3	865
1101	12:21:31	2024-11-01	32.15	1	866
1101	12:21:31	2024-11-01	32.15	7	867
1101	12:21:29	2024-11-01	32.10	1	868
1101	12:21:15	2024-11-01	32.10	1	869
1101	12:21:03	2024-11-01	32.10	1	870
1101	12:21:03	2024-11-01	32.10	1	871
1101	12:21:01	2024-11-01	32.10	1	872
1101	12:21:01	2024-11-01	32.15	7	873
1101	12:20:58	2024-11-01	32.10	1	874
1101	12:20:47	2024-11-01	32.15	2	875
1101	12:20:47	2024-11-01	32.10	1	876
1101	12:20:47	2024-11-01	32.10	1	877
1101	12:20:33	2024-11-01	32.10	1	878
1101	12:20:31	2024-11-01	32.15	7	879
1101	12:20:19	2024-11-01	32.15	2	880
1101	12:20:19	2024-11-01	32.10	1	881
1101	12:20:19	2024-11-01	32.10	1	882
1101	12:20:05	2024-11-01	32.10	1	883
1101	12:20:01	2024-11-01	32.10	1	884
1101	12:20:01	2024-11-01	32.15	3	885
1101	12:20:00	2024-11-01	32.15	7	886
1101	12:19:51	2024-11-01	32.10	1	887
1101	12:19:39	2024-11-01	32.15	1	888
1101	12:19:37	2024-11-01	32.10	1	889
1101	12:19:31	2024-11-01	32.15	1	890
1101	12:19:31	2024-11-01	32.15	2	891
1101	12:19:30	2024-11-01	32.15	7	892
1101	12:19:23	2024-11-01	32.10	1	893
1101	12:19:09	2024-11-01	32.15	2	894
1101	12:19:09	2024-11-01	32.10	1	895
1101	12:19:00	2024-11-01	32.15	7	896
1101	12:18:55	2024-11-01	32.10	1	897
1101	12:18:49	2024-11-01	32.15	1	898
1101	12:18:48	2024-11-01	32.10	1	899
1101	12:18:46	2024-11-01	32.15	2	900
1101	12:18:46	2024-11-01	32.10	4	901
1101	12:18:46	2024-11-01	32.10	1	902
1101	12:18:41	2024-11-01	32.10	1	903
1101	12:18:31	2024-11-01	32.15	7	904
1101	12:18:27	2024-11-01	32.15	2	905
1101	12:18:27	2024-11-01	32.10	1	906
1101	12:18:21	2024-11-01	32.10	1	907
1101	12:18:20	2024-11-01	32.10	2	908
1101	12:18:13	2024-11-01	32.10	1	909
1101	12:18:11	2024-11-01	32.10	2	910
1101	12:18:01	2024-11-01	32.15	1	911
1101	12:18:01	2024-11-01	32.15	7	912
1101	12:17:59	2024-11-01	32.10	1	913
1101	12:17:51	2024-11-01	32.15	1	914
1101	12:17:45	2024-11-01	32.10	1	915
1101	12:17:31	2024-11-01	32.10	1	916
1101	12:17:31	2024-11-01	32.10	1	917
1101	12:17:31	2024-11-01	32.15	7	918
1101	12:17:19	2024-11-01	32.10	1	919
1101	12:17:17	2024-11-01	32.10	1	920
1101	12:17:03	2024-11-01	32.10	1	921
1101	12:17:01	2024-11-01	32.15	3	922
1101	12:17:00	2024-11-01	32.15	7	923
1101	12:16:50	2024-11-01	32.10	4	924
1101	12:16:49	2024-11-01	32.10	1	925
1101	12:16:35	2024-11-01	32.10	1	926
1101	12:16:32	2024-11-01	32.15	2	927
1101	12:16:32	2024-11-01	32.10	1	928
1101	12:16:31	2024-11-01	32.15	7	929
1101	12:16:21	2024-11-01	32.10	1	930
1101	12:16:08	2024-11-01	32.10	1	931
1101	12:16:07	2024-11-01	32.15	2	932
1101	12:16:07	2024-11-01	32.10	1	933
1101	12:16:01	2024-11-01	32.15	7	934
1101	12:15:53	2024-11-01	32.10	1	935
1101	12:15:52	2024-11-01	32.15	2	936
1101	12:15:39	2024-11-01	32.10	1	937
1101	12:15:31	2024-11-01	32.15	7	938
1101	12:15:25	2024-11-01	32.15	2	939
1101	12:15:25	2024-11-01	32.10	1	940
1101	12:15:11	2024-11-01	32.10	1	941
1101	12:15:03	2024-11-01	32.15	2	942
1101	12:15:02	2024-11-01	32.10	1	943
1101	12:15:01	2024-11-01	32.15	2	944
1101	12:15:00	2024-11-01	32.15	7	945
1101	12:14:57	2024-11-01	32.10	1	946
1101	12:14:56	2024-11-01	32.10	2	947
1101	12:14:43	2024-11-01	32.10	1	948
1101	12:14:32	2024-11-01	32.10	1	949
1101	12:14:31	2024-11-01	32.15	2	950
1101	12:14:30	2024-11-01	32.15	7	951
1101	12:14:29	2024-11-01	32.10	2	952
1101	12:14:29	2024-11-01	32.10	1	953
1101	12:14:22	2024-11-01	32.15	2	954
1101	12:14:22	2024-11-01	32.10	1	955
1101	12:14:15	2024-11-01	32.10	1	956
1101	12:14:07	2024-11-01	32.15	1	957
1101	12:14:01	2024-11-01	32.15	2	958
1101	12:14:01	2024-11-01	32.10	1	959
1101	12:14:01	2024-11-01	32.10	1	960
1101	12:14:01	2024-11-01	32.15	7	961
1101	12:13:50	2024-11-01	32.10	2	962
1101	12:13:47	2024-11-01	32.10	1	963
1101	12:13:43	2024-11-01	32.10	1	964
1101	12:13:33	2024-11-01	32.10	1	965
1101	12:13:31	2024-11-01	32.15	7	966
1101	12:13:19	2024-11-01	32.10	1	967
1101	12:13:05	2024-11-01	32.10	1	968
1101	12:13:00	2024-11-01	32.15	7	969
1101	12:12:56	2024-11-01	32.10	1	970
1101	12:12:51	2024-11-01	32.10	1	971
1101	12:12:47	2024-11-01	32.15	2	972
1101	12:12:46	2024-11-01	32.15	1	973
1101	12:12:39	2024-11-01	32.10	1	974
1101	12:12:37	2024-11-01	32.10	1	975
1101	12:12:34	2024-11-01	32.10	1	976
1101	12:12:30	2024-11-01	32.15	7	977
1101	12:12:23	2024-11-01	32.15	3	978
1101	12:12:23	2024-11-01	32.10	1	979
1101	12:12:09	2024-11-01	32.10	1	980
1101	12:12:01	2024-11-01	32.15	7	981
1101	12:11:57	2024-11-01	32.10	2	982
1101	12:11:55	2024-11-01	32.10	1	983
1101	12:11:53	2024-11-01	32.15	3	984
1101	12:11:41	2024-11-01	32.10	1	985
1101	12:11:33	2024-11-01	32.10	1	986
1101	12:11:31	2024-11-01	32.15	7	987
1101	12:11:27	2024-11-01	32.10	1	988
1101	12:11:26	2024-11-01	32.15	2	989
1101	12:11:13	2024-11-01	32.10	1	990
1101	12:11:02	2024-11-01	32.15	7	991
1101	12:11:01	2024-11-01	32.15	2	992
1101	12:11:00	2024-11-01	32.10	1	993
1101	12:10:45	2024-11-01	32.10	1	994
1101	12:10:39	2024-11-01	32.15	1	995
1101	12:10:32	2024-11-01	32.15	7	996
1101	12:10:30	2024-11-01	32.15	2	997
1101	12:10:30	2024-11-01	32.10	1	998
1101	12:10:23	2024-11-01	32.15	1	999
1101	12:10:17	2024-11-01	32.10	1	1000
1101	12:10:06	2024-11-01	32.10	1	1001
1101	12:10:03	2024-11-01	32.15	7	1002
1101	12:10:03	2024-11-01	32.10	1	1003
1101	12:09:59	2024-11-01	32.10	1	1004
1101	12:09:53	2024-11-01	32.10	1	1005
1101	12:09:49	2024-11-01	32.10	1	1006
1101	12:09:44	2024-11-01	32.10	1	1007
1101	12:09:36	2024-11-01	32.10	2	1008
1101	12:09:35	2024-11-01	32.10	1	1009
1101	12:09:34	2024-11-01	32.15	7	1010
1101	12:09:32	2024-11-01	32.15	2	1011
1101	12:09:32	2024-11-01	32.10	1	1012
1101	12:09:21	2024-11-01	32.10	1	1013
1101	12:09:08	2024-11-01	32.10	1	1014
1101	12:09:07	2024-11-01	32.10	1	1015
1101	12:09:06	2024-11-01	32.15	3	1016
1101	12:09:06	2024-11-01	32.15	7	1017
1101	12:08:55	2024-11-01	32.10	1	1018
1101	12:08:53	2024-11-01	32.10	1	1019
1101	12:08:39	2024-11-01	32.10	1	1020
1101	12:08:37	2024-11-01	32.15	7	1021
1101	12:08:36	2024-11-01	32.15	2	1022
1101	12:08:35	2024-11-01	32.10	1	1023
1101	12:08:33	2024-11-01	32.10	2	1024
1101	12:08:25	2024-11-01	32.10	1	1025
1101	12:08:13	2024-11-01	32.15	2	1026
1101	12:08:12	2024-11-01	32.15	2	1027
1101	12:08:11	2024-11-01	32.10	1	1028
1101	12:08:07	2024-11-01	32.15	7	1029
1101	12:08:05	2024-11-01	32.10	1	1030
1101	12:08:03	2024-11-01	32.10	3	1031
1101	12:07:57	2024-11-01	32.10	1	1032
1101	12:07:54	2024-11-01	32.15	2	1033
1101	12:07:43	2024-11-01	32.10	1	1034
1101	12:07:38	2024-11-01	32.15	7	1035
1101	12:07:37	2024-11-01	32.15	1	1036
1101	12:07:29	2024-11-01	32.15	2	1037
1101	12:07:29	2024-11-01	32.10	1	1038
1101	12:07:15	2024-11-01	32.10	1	1039
1101	12:07:12	2024-11-01	32.10	4	1040
1101	12:07:09	2024-11-01	32.15	7	1041
1101	12:07:05	2024-11-01	32.10	2	1042
1101	12:07:02	2024-11-01	32.10	1	1043
1101	12:07:01	2024-11-01	32.10	1	1044
1101	12:06:51	2024-11-01	32.10	1	1045
1101	12:06:50	2024-11-01	32.10	10	1046
1101	12:06:47	2024-11-01	32.10	1	1047
1101	12:06:43	2024-11-01	32.10	1	1048
1101	12:06:41	2024-11-01	32.15	7	1049
1101	12:06:33	2024-11-01	32.10	1	1050
1101	12:06:32	2024-11-01	32.15	2	1051
1101	12:06:19	2024-11-01	32.10	1	1052
1101	12:06:17	2024-11-01	32.10	2	1053
1101	12:06:12	2024-11-01	32.15	7	1054
1101	12:06:09	2024-11-01	32.10	1	1055
1101	12:06:05	2024-11-01	32.10	1	1056
1101	12:06:04	2024-11-01	32.15	2	1057
1101	12:05:51	2024-11-01	32.10	1	1058
1101	12:05:47	2024-11-01	32.10	1	1059
1101	12:05:43	2024-11-01	32.15	7	1060
1101	12:05:41	2024-11-01	32.10	1	1061
1101	12:05:38	2024-11-01	32.10	5	1062
1101	12:05:37	2024-11-01	32.15	2	1063
1101	12:05:37	2024-11-01	32.10	1	1064
1101	12:05:25	2024-11-01	32.10	1	1065
1101	12:05:14	2024-11-01	32.15	2	1066
1101	12:05:13	2024-11-01	32.15	7	1067
1101	12:05:09	2024-11-01	32.10	1	1068
1101	12:04:59	2024-11-01	32.10	1	1069
1101	12:04:57	2024-11-01	32.10	4	1070
1101	12:04:55	2024-11-01	32.15	2	1071
1101	12:04:55	2024-11-01	32.10	1	1072
1101	12:04:55	2024-11-01	32.10	1	1073
1101	12:04:44	2024-11-01	32.15	7	1074
1101	12:04:41	2024-11-01	32.10	1	1075
1101	12:04:33	2024-11-01	32.15	2	1076
1101	12:04:32	2024-11-01	32.15	2	1077
1101	12:04:27	2024-11-01	32.10	1	1078
1101	12:04:16	2024-11-01	32.15	7	1079
1101	12:04:13	2024-11-01	32.10	1	1080
1101	12:03:59	2024-11-01	32.10	1	1081
1101	12:03:47	2024-11-01	32.10	1	1082
1101	12:03:46	2024-11-01	32.15	7	1083
1101	12:03:45	2024-11-01	32.10	1	1084
1101	12:03:31	2024-11-01	32.10	1	1085
1101	12:03:17	2024-11-01	32.15	7	1086
1101	12:03:17	2024-11-01	32.10	1	1087
1101	12:03:13	2024-11-01	32.15	1	1088
1101	12:03:03	2024-11-01	32.10	1	1089
1101	12:03:03	2024-11-01	32.10	1	1090
1101	12:02:54	2024-11-01	32.10	5	1091
1101	12:02:51	2024-11-01	32.10	1	1092
1101	12:02:49	2024-11-01	32.10	1	1093
1101	12:02:48	2024-11-01	32.15	7	1094
1101	12:02:47	2024-11-01	32.15	3	1095
1101	12:02:47	2024-11-01	32.15	5	1096
1101	12:02:35	2024-11-01	32.10	1	1097
1101	12:02:21	2024-11-01	32.10	1	1098
1101	12:02:20	2024-11-01	32.15	7	1099
1101	12:02:17	2024-11-01	32.15	2	1100
1101	12:02:07	2024-11-01	32.10	1	1101
1101	12:01:58	2024-11-01	32.15	2	1102
1101	12:01:58	2024-11-01	32.10	3	1103
1101	12:01:53	2024-11-01	32.10	1	1104
1101	12:01:51	2024-11-01	32.15	7	1105
1101	12:01:39	2024-11-01	32.10	1	1106
1101	12:01:25	2024-11-01	32.10	1	1107
1101	12:01:25	2024-11-01	32.10	1	1108
1101	12:01:21	2024-11-01	32.15	7	1109
1101	12:01:11	2024-11-01	32.10	1	1110
1101	12:01:07	2024-11-01	32.15	3	1111
1101	12:00:57	2024-11-01	32.10	1	1112
1101	12:00:52	2024-11-01	32.15	7	1113
1101	12:00:43	2024-11-01	32.10	1	1114
1101	12:00:39	2024-11-01	32.15	2	1115
1101	12:00:39	2024-11-01	32.10	10	1116
1101	12:00:30	2024-11-01	32.10	1	1117
1101	12:00:29	2024-11-01	32.10	1	1118
1101	12:00:23	2024-11-01	32.15	7	1119
1101	12:00:15	2024-11-01	32.10	1	1120
1101	12:00:01	2024-11-01	32.10	1	1121
1101	12:00:00	2024-11-01	32.15	1	1122
1101	11:59:58	2024-11-01	32.10	1	1123
1101	11:59:54	2024-11-01	32.15	7	1124
1101	11:59:47	2024-11-01	32.10	1	1125
1101	11:59:33	2024-11-01	32.10	1	1126
1101	11:59:28	2024-11-01	32.10	2	1127
1101	11:59:25	2024-11-01	32.15	7	1128
1101	11:59:21	2024-11-01	32.10	1	1129
1101	11:59:19	2024-11-01	32.15	2	1130
1101	11:59:19	2024-11-01	32.10	1	1131
1101	11:59:05	2024-11-01	32.10	1	1132
1101	11:58:57	2024-11-01	32.15	2	1133
1101	11:58:56	2024-11-01	32.20	2	1134
1101	11:58:56	2024-11-01	32.15	5	1135
1101	11:58:51	2024-11-01	32.10	1	1136
1101	11:58:37	2024-11-01	32.15	1	1137
1101	11:58:37	2024-11-01	32.10	1	1138
1101	11:58:27	2024-11-01	32.15	7	1139
1101	11:58:23	2024-11-01	32.15	3	1140
1101	11:58:23	2024-11-01	32.10	1	1141
1101	11:58:09	2024-11-01	32.10	1	1142
1101	11:58:03	2024-11-01	32.10	1	1143
1101	11:58:03	2024-11-01	32.10	1	1144
1101	11:58:03	2024-11-01	32.15	1	1145
1101	11:58:03	2024-11-01	32.15	14	1146
1101	11:58:03	2024-11-01	32.15	29	1147
1101	11:57:59	2024-11-01	32.15	7	1148
1101	11:57:55	2024-11-01	32.10	1	1149
1101	11:57:52	2024-11-01	32.10	3	1150
1101	11:57:41	2024-11-01	32.05	1	1151
1101	11:57:29	2024-11-01	32.15	2	1152
1101	11:57:29	2024-11-01	32.10	5	1153
1101	11:57:27	2024-11-01	32.05	1	1154
1101	11:57:23	2024-11-01	32.10	3	1155
1101	11:57:13	2024-11-01	32.05	1	1156
1101	11:57:13	2024-11-01	32.10	1	1157
1101	11:57:01	2024-11-01	32.05	1	1158
1101	11:57:00	2024-11-01	32.10	7	1159
1101	11:57:00	2024-11-01	32.05	1	1160
1101	11:56:59	2024-11-01	32.05	1	1161
1101	11:56:53	2024-11-01	32.05	1	1162
1101	11:56:53	2024-11-01	32.10	2	1163
1101	11:56:53	2024-11-01	32.10	5	1164
1101	11:56:53	2024-11-01	32.10	19	1165
1101	11:56:45	2024-11-01	32.05	1	1166
1101	11:56:31	2024-11-01	32.05	1	1167
1101	11:56:26	2024-11-01	32.10	3	1168
1101	11:56:26	2024-11-01	32.10	1	1169
1101	11:56:17	2024-11-01	32.05	1	1170
1101	11:56:03	2024-11-01	32.05	1	1171
1101	11:55:49	2024-11-01	32.05	1	1172
1101	11:55:47	2024-11-01	32.10	2	1173
1101	11:55:35	2024-11-01	32.05	1	1174
1101	11:55:31	2024-11-01	32.10	3	1175
1101	11:55:21	2024-11-01	32.05	1	1176
1101	11:55:17	2024-11-01	32.05	1	1177
1101	11:55:07	2024-11-01	32.05	1	1178
1101	11:54:54	2024-11-01	32.05	4	1179
1101	11:54:53	2024-11-01	32.05	1	1180
1101	11:54:39	2024-11-01	32.05	1	1181
1101	11:54:32	2024-11-01	32.05	1	1182
1101	11:54:25	2024-11-01	32.05	1	1183
1101	11:54:11	2024-11-01	32.05	1	1184
1101	11:54:09	2024-11-01	32.05	4	1185
1101	11:54:00	2024-11-01	32.05	1	1186
1101	11:53:57	2024-11-01	32.10	2	1187
1101	11:53:57	2024-11-01	32.05	1	1188
1101	11:53:43	2024-11-01	32.05	1	1189
1101	11:53:29	2024-11-01	32.10	3	1190
1101	11:53:29	2024-11-01	32.05	1	1191
1101	11:53:15	2024-11-01	32.05	1	1192
1101	11:53:06	2024-11-01	32.05	2	1193
1101	11:53:01	2024-11-01	32.05	3	1194
1101	11:53:01	2024-11-01	32.10	2	1195
1101	11:53:01	2024-11-01	32.05	1	1196
1101	11:52:47	2024-11-01	32.05	1	1197
1101	11:52:47	2024-11-01	32.05	1	1198
1101	11:52:43	2024-11-01	32.10	2	1199
1101	11:52:33	2024-11-01	32.05	1	1200
1101	11:52:19	2024-11-01	32.05	1	1201
1101	11:52:05	2024-11-01	32.10	2	1202
1101	11:52:05	2024-11-01	32.05	1	1203
1101	11:52:02	2024-11-01	32.10	1	1204
1101	11:51:51	2024-11-01	32.10	3	1205
1101	11:51:51	2024-11-01	32.05	1	1206
1101	11:51:37	2024-11-01	32.05	1	1207
1101	11:51:23	2024-11-01	32.05	1	1208
1101	11:51:23	2024-11-01	32.10	2	1209
1101	11:51:23	2024-11-01	32.05	1	1210
1101	11:51:09	2024-11-01	32.05	1	1211
1101	11:50:55	2024-11-01	32.10	3	1212
1101	11:50:55	2024-11-01	32.05	1	1213
1101	11:50:41	2024-11-01	32.05	1	1214
1101	11:50:27	2024-11-01	32.05	1	1215
1101	11:50:26	2024-11-01	32.10	2	1216
1101	11:50:13	2024-11-01	32.05	1	1217
1101	11:50:01	2024-11-01	32.10	1	1218
1101	11:49:59	2024-11-01	32.05	1	1219
1101	11:49:52	2024-11-01	32.05	2	1220
1101	11:49:45	2024-11-01	32.05	1	1221
1101	11:49:36	2024-11-01	32.05	4	1222
1101	11:49:31	2024-11-01	32.05	1	1223
1101	11:49:26	2024-11-01	32.10	1	1224
1101	11:49:21	2024-11-01	32.05	1	1225
1101	11:49:20	2024-11-01	32.10	1	1226
1101	11:49:20	2024-11-01	32.10	1	1227
1101	11:49:20	2024-11-01	32.10	2	1228
1101	11:49:20	2024-11-01	32.10	9	1229
1101	11:49:20	2024-11-01	32.10	1	1230
1101	11:49:17	2024-11-01	32.05	1	1231
1101	11:49:16	2024-11-01	32.10	3	1232
1101	11:49:03	2024-11-01	32.05	1	1233
1101	11:48:49	2024-11-01	32.05	1	1234
1101	11:48:37	2024-11-01	32.05	2	1235
1101	11:48:35	2024-11-01	32.10	4	1236
1101	11:48:35	2024-11-01	32.10	1	1237
1101	11:48:35	2024-11-01	32.10	54	1238
1101	11:48:35	2024-11-01	32.10	1	1239
1101	11:48:23	2024-11-01	32.15	2	1240
1101	11:48:23	2024-11-01	32.15	1	1241
1101	11:48:23	2024-11-01	32.15	1	1242
1101	11:48:21	2024-11-01	32.15	2	1243
1101	11:48:21	2024-11-01	32.10	1	1244
1101	11:48:17	2024-11-01	32.15	5	1245
1101	11:48:17	2024-11-01	32.15	1	1246
1101	11:48:16	2024-11-01	32.10	5	1247
1101	11:48:08	2024-11-01	32.10	1	1248
1101	11:48:07	2024-11-01	32.15	2	1249
1101	11:48:07	2024-11-01	32.10	1	1250
1101	11:48:02	2024-11-01	32.10	1	1251
1101	11:48:01	2024-11-01	32.15	1	1252
1101	11:48:01	2024-11-01	32.15	2	1253
1101	11:48:01	2024-11-01	32.15	3	1254
1101	11:48:01	2024-11-01	32.15	2	1255
1101	11:47:53	2024-11-01	32.10	1	1256
1101	11:47:51	2024-11-01	32.10	3	1257
1101	11:47:51	2024-11-01	32.15	4	1258
1101	11:47:51	2024-11-01	32.10	2	1259
1101	11:47:51	2024-11-01	32.10	20	1260
1101	11:47:50	2024-11-01	32.10	44	1261
1101	11:47:50	2024-11-01	32.10	2	1262
1101	11:47:50	2024-11-01	32.10	3	1263
1101	11:47:50	2024-11-01	32.10	1	1264
1101	11:47:50	2024-11-01	32.10	2	1265
1101	11:47:50	2024-11-01	32.10	2	1266
1101	11:47:50	2024-11-01	32.10	1	1267
1101	11:47:50	2024-11-01	32.10	2	1268
1101	11:47:50	2024-11-01	32.10	2	1269
1101	11:47:50	2024-11-01	32.10	1	1270
1101	11:47:50	2024-11-01	32.05	4	1271
1101	11:47:50	2024-11-01	32.05	6	1272
1101	11:47:49	2024-11-01	32.05	1	1273
1101	11:47:49	2024-11-01	32.05	2	1274
1101	11:47:49	2024-11-01	32.05	22	1275
1101	11:47:49	2024-11-01	32.00	4	1276
1101	11:47:49	2024-11-01	32.00	88	1277
1101	11:47:49	2024-11-01	32.00	16	1278
1101	11:47:49	2024-11-01	32.00	140	1279
1101	11:47:49	2024-11-01	32.00	40	1280
1101	11:47:40	2024-11-01	32.00	7	1281
1101	11:47:39	2024-11-01	31.95	1	1282
1101	11:47:38	2024-11-01	32.00	7	1283
1101	11:47:26	2024-11-01	31.95	1	1284
1101	11:47:25	2024-11-01	31.95	1	1285
1101	11:47:24	2024-11-01	32.00	3	1286
1101	11:47:13	2024-11-01	31.95	1	1287
1101	11:47:07	2024-11-01	32.00	1	1288
1101	11:47:00	2024-11-01	31.95	1	1289
1101	11:46:58	2024-11-01	32.00	2	1290
1101	11:46:47	2024-11-01	31.95	1	1291
1101	11:46:34	2024-11-01	31.95	1	1292
1101	11:46:33	2024-11-01	32.00	2	1293
1101	11:46:21	2024-11-01	31.95	1	1294
1101	11:46:12	2024-11-01	31.95	2	1295
1101	11:46:11	2024-11-01	32.00	4	1296
1101	11:46:11	2024-11-01	32.00	59	1297
1101	11:46:10	2024-11-01	32.00	1	1298
1101	11:46:08	2024-11-01	32.00	3	1299
1101	11:46:08	2024-11-01	31.95	1	1300
1101	11:45:55	2024-11-01	31.95	1	1301
1101	11:45:47	2024-11-01	32.00	1	1302
1101	11:45:42	2024-11-01	31.95	1	1303
1101	11:45:39	2024-11-01	31.95	1	1304
1101	11:45:31	2024-11-01	31.95	1	1305
1101	11:45:31	2024-11-01	31.95	7	1306
1101	11:45:30	2024-11-01	32.00	10	1307
1101	11:45:30	2024-11-01	32.00	16	1308
1101	11:45:29	2024-11-01	31.95	6	1309
1101	11:45:29	2024-11-01	31.95	1	1310
1101	11:45:29	2024-11-01	31.95	2	1311
1101	11:45:29	2024-11-01	31.95	201	1312
1101	11:45:29	2024-11-01	31.90	3	1313
1101	11:45:29	2024-11-01	31.90	1	1314
1101	11:45:28	2024-11-01	31.95	2	1315
1101	11:45:15	2024-11-01	31.90	1	1316
1101	11:45:14	2024-11-01	31.95	1	1317
1101	11:45:10	2024-11-01	31.95	2	1318
1101	11:45:10	2024-11-01	31.90	3	1319
1101	11:45:03	2024-11-01	31.90	1	1320
1101	11:45:02	2024-11-01	31.90	2	1321
1101	11:44:50	2024-11-01	31.95	2	1322
1101	11:44:50	2024-11-01	31.90	1	1323
1101	11:44:37	2024-11-01	31.90	1	1324
1101	11:44:27	2024-11-01	31.95	2	1325
1101	11:44:26	2024-11-01	31.95	2	1326
1101	11:44:24	2024-11-01	31.90	1	1327
1101	11:44:11	2024-11-01	31.90	1	1328
1101	11:44:09	2024-11-01	31.90	3	1329
1101	11:44:08	2024-11-01	31.90	1	1330
1101	11:44:08	2024-11-01	31.95	2	1331
1101	11:44:08	2024-11-01	31.95	10	1332
1101	11:43:58	2024-11-01	31.90	1	1333
1101	11:43:45	2024-11-01	31.95	2	1334
1101	11:43:45	2024-11-01	31.90	1	1335
1101	11:43:36	2024-11-01	31.95	1	1336
1101	11:43:32	2024-11-01	31.90	1	1337
1101	11:43:19	2024-11-01	31.95	2	1338
1101	11:43:19	2024-11-01	31.90	1	1339
1101	11:43:16	2024-11-01	31.90	3	1340
1101	11:43:06	2024-11-01	31.90	1	1341
1101	11:42:53	2024-11-01	31.90	1	1342
1101	11:42:51	2024-11-01	31.95	2	1343
1101	11:42:40	2024-11-01	31.95	2	1344
1101	11:42:40	2024-11-01	31.90	1	1345
1101	11:42:32	2024-11-01	31.90	1	1346
1101	11:42:27	2024-11-01	31.90	1	1347
1101	11:42:14	2024-11-01	31.95	2	1348
1101	11:42:14	2024-11-01	31.90	1	1349
1101	11:42:14	2024-11-01	31.90	1	1350
1101	11:42:03	2024-11-01	31.90	1	1351
1101	11:42:01	2024-11-01	31.90	1	1352
1101	11:42:00	2024-11-01	31.90	1	1353
1101	11:41:52	2024-11-01	31.95	2	1354
1101	11:41:51	2024-11-01	31.90	3	1355
1101	11:41:48	2024-11-01	31.90	1	1356
1101	11:41:35	2024-11-01	31.90	1	1357
1101	11:41:29	2024-11-01	31.90	1	1358
1101	11:41:21	2024-11-01	31.90	1	1359
1101	11:41:09	2024-11-01	31.90	1	1360
1101	11:40:58	2024-11-01	31.90	3	1361
1101	11:40:56	2024-11-01	31.90	1	1362
1101	11:40:43	2024-11-01	31.90	1	1363
1101	11:40:29	2024-11-01	31.90	1	1364
1101	11:40:17	2024-11-01	31.90	1	1365
1101	11:40:17	2024-11-01	31.90	1	1366
1101	11:40:16	2024-11-01	31.90	1	1367
1101	11:40:15	2024-11-01	31.95	1	1368
1101	11:40:10	2024-11-01	31.95	1	1369
1101	11:40:04	2024-11-01	31.90	1	1370
1101	11:40:01	2024-11-01	31.95	2	1371
1101	11:40:01	2024-11-01	31.90	2	1372
1101	11:39:57	2024-11-01	31.90	1	1373
1101	11:39:51	2024-11-01	31.90	1	1374
1101	11:39:39	2024-11-01	31.90	1	1375
1101	11:39:38	2024-11-01	31.95	3	1376
1101	11:39:38	2024-11-01	31.90	1	1377
1101	11:39:25	2024-11-01	31.90	1	1378
1101	11:39:12	2024-11-01	31.90	1	1379
1101	11:39:07	2024-11-01	31.90	4	1380
1101	11:38:59	2024-11-01	31.95	2	1381
1101	11:38:59	2024-11-01	31.90	1	1382
1101	11:38:46	2024-11-01	31.90	1	1383
1101	11:38:44	2024-11-01	31.95	2	1384
1101	11:38:33	2024-11-01	31.90	1	1385
1101	11:38:19	2024-11-01	31.90	1	1386
1101	11:38:17	2024-11-01	31.95	3	1387
1101	11:38:07	2024-11-01	31.90	1	1388
1101	11:37:54	2024-11-01	31.90	1	1389
1101	11:37:51	2024-11-01	31.95	2	1390
1101	11:37:41	2024-11-01	31.90	1	1391
1101	11:37:28	2024-11-01	31.95	2	1392
1101	11:37:28	2024-11-01	31.90	1	1393
1101	11:37:15	2024-11-01	31.90	1	1394
1101	11:37:06	2024-11-01	31.90	1	1395
1101	11:37:05	2024-11-01	31.90	2	1396
1101	11:37:04	2024-11-01	31.90	2	1397
1101	11:37:03	2024-11-01	31.90	3	1398
1101	11:37:02	2024-11-01	31.95	2	1399
1101	11:37:02	2024-11-01	31.90	1	1400
1101	11:36:48	2024-11-01	31.90	1	1401
1101	11:36:46	2024-11-01	31.95	2	1402
1101	11:36:36	2024-11-01	31.90	1	1403
1101	11:36:28	2024-11-01	31.90	1	1404
1101	11:36:26	2024-11-01	31.90	1	1405
1101	11:36:23	2024-11-01	31.90	1	1406
1101	11:36:21	2024-11-01	31.95	2	1407
1101	11:36:10	2024-11-01	31.90	1	1408
1101	11:35:56	2024-11-01	31.90	1	1409
1101	11:35:44	2024-11-01	31.90	1	1410
1101	11:35:43	2024-11-01	31.90	1	1411
1101	11:35:39	2024-11-01	31.95	2	1412
1101	11:35:38	2024-11-01	31.90	1	1413
1101	11:35:31	2024-11-01	31.90	1	1414
1101	11:35:18	2024-11-01	31.90	1	1415
1101	11:35:05	2024-11-01	31.90	1	1416
1101	11:35:00	2024-11-01	31.95	2	1417
1101	11:35:00	2024-11-01	31.95	1	1418
1101	11:34:58	2024-11-01	31.90	2	1419
1101	11:34:52	2024-11-01	31.90	1	1420
1101	11:34:50	2024-11-01	31.95	2	1421
1101	11:34:39	2024-11-01	31.90	1	1422
1101	11:34:28	2024-11-01	31.90	1	1423
1101	11:34:26	2024-11-01	31.90	1	1424
1101	11:34:23	2024-11-01	31.95	3	1425
1101	11:34:13	2024-11-01	31.90	1	1426
1101	11:34:06	2024-11-01	31.90	4	1427
1101	11:34:02	2024-11-01	31.95	1	1428
1101	11:34:00	2024-11-01	31.90	1	1429
1101	11:33:59	2024-11-01	31.95	2	1430
1101	11:33:47	2024-11-01	31.90	1	1431
1101	11:33:34	2024-11-01	31.90	1	1432
1101	11:33:33	2024-11-01	31.95	2	1433
1101	11:33:21	2024-11-01	31.90	1	1434
1101	11:33:08	2024-11-01	31.90	1	1435
1101	11:33:06	2024-11-01	31.95	3	1436
1101	11:32:55	2024-11-01	31.90	1	1437
1101	11:32:42	2024-11-01	31.95	2	1438
1101	11:32:42	2024-11-01	31.90	1	1439
1101	11:32:28	2024-11-01	31.90	1	1440
1101	11:32:16	2024-11-01	31.95	2	1441
1101	11:32:16	2024-11-01	31.90	1	1442
1101	11:32:03	2024-11-01	31.90	1	1443
1101	11:32:02	2024-11-01	31.90	1	1444
1101	11:32:01	2024-11-01	31.90	4	1445
1101	11:31:57	2024-11-01	31.90	2	1446
1101	11:31:49	2024-11-01	31.90	1	1447
1101	11:31:43	2024-11-01	31.90	3	1448
1101	11:31:37	2024-11-01	31.90	1	1449
1101	11:31:24	2024-11-01	31.90	1	1450
1101	11:31:10	2024-11-01	31.90	1	1451
1101	11:31:04	2024-11-01	31.90	1	1452
1101	11:30:57	2024-11-01	31.90	1	1453
1101	11:30:45	2024-11-01	31.95	2	1454
1101	11:30:44	2024-11-01	31.90	1	1455
1101	11:30:32	2024-11-01	31.90	1	1456
1101	11:30:21	2024-11-01	31.90	1	1457
1101	11:30:19	2024-11-01	31.95	2	1458
1101	11:30:18	2024-11-01	31.90	1	1459
1101	11:30:17	2024-11-01	31.90	1	1460
1101	11:30:05	2024-11-01	31.90	1	1461
1101	11:30:03	2024-11-01	31.95	2	1462
1101	11:30:03	2024-11-01	31.95	5	1463
1101	11:29:57	2024-11-01	31.90	1	1464
1101	11:29:57	2024-11-01	31.90	3	1465
1101	11:29:53	2024-11-01	31.90	1	1466
1101	11:29:40	2024-11-01	31.95	2	1467
1101	11:29:40	2024-11-01	31.90	1	1468
1101	11:29:26	2024-11-01	31.90	1	1469
1101	11:29:21	2024-11-01	31.95	2	1470
1101	11:29:21	2024-11-01	31.90	4	1471
1101	11:29:13	2024-11-01	31.90	1	1472
1101	11:29:00	2024-11-01	31.90	1	1473
1101	11:29:00	2024-11-01	31.90	3	1474
1101	11:28:59	2024-11-01	31.95	2	1475
1101	11:28:47	2024-11-01	31.90	1	1476
1101	11:28:34	2024-11-01	31.90	1	1477
1101	11:28:21	2024-11-01	31.90	1	1478
1101	11:28:14	2024-11-01	31.90	3	1479
1101	11:28:09	2024-11-01	31.90	1	1480
1101	11:27:56	2024-11-01	31.90	1	1481
1101	11:27:43	2024-11-01	31.95	2	1482
1101	11:27:42	2024-11-01	31.90	1	1483
1101	11:27:33	2024-11-01	31.90	1	1484
1101	11:27:32	2024-11-01	31.95	10	1485
1101	11:27:29	2024-11-01	31.90	1	1486
1101	11:27:16	2024-11-01	31.90	1	1487
1101	11:27:14	2024-11-01	31.95	2	1488
1101	11:27:14	2024-11-01	31.95	1	1489
1101	11:27:08	2024-11-01	31.95	2	1490
1101	11:27:03	2024-11-01	31.90	1	1491
1101	11:27:03	2024-11-01	31.90	3	1492
1101	11:26:51	2024-11-01	31.95	2	1493
1101	11:26:51	2024-11-01	31.90	1	1494
1101	11:26:37	2024-11-01	31.90	1	1495
1101	11:26:25	2024-11-01	31.95	3	1496
1101	11:26:25	2024-11-01	31.90	1	1497
1101	11:26:15	2024-11-01	31.90	2	1498
1101	11:26:11	2024-11-01	31.90	1	1499
1101	11:26:02	2024-11-01	31.90	1	1500
1101	11:25:58	2024-11-01	31.90	2	1501
1101	11:25:58	2024-11-01	31.95	2	1502
1101	11:25:58	2024-11-01	31.90	1	1503
1101	11:25:46	2024-11-01	31.90	1	1504
1101	11:25:32	2024-11-01	31.90	1	1505
1101	11:25:31	2024-11-01	31.95	3	1506
1101	11:25:30	2024-11-01	31.95	10	1507
1101	11:25:19	2024-11-01	31.90	1	1508
1101	11:25:07	2024-11-01	31.90	1	1509
1101	11:24:58	2024-11-01	31.95	2	1510
1101	11:24:58	2024-11-01	31.90	3	1511
1101	11:24:55	2024-11-01	31.90	1	1512
1101	11:24:53	2024-11-01	31.90	1	1513
1101	11:24:44	2024-11-01	31.90	1	1514
1101	11:24:42	2024-11-01	31.90	2	1515
1101	11:24:41	2024-11-01	31.95	2	1516
1101	11:24:41	2024-11-01	31.90	3	1517
1101	11:24:41	2024-11-01	31.90	1	1518
1101	11:24:29	2024-11-01	31.95	1	1519
1101	11:24:28	2024-11-01	31.90	1	1520
1101	11:24:15	2024-11-01	31.90	1	1521
1101	11:24:11	2024-11-01	31.90	5	1522
1101	11:24:01	2024-11-01	31.90	1	1523
1101	11:23:48	2024-11-01	31.90	1	1524
1101	11:23:36	2024-11-01	31.95	2	1525
1101	11:23:35	2024-11-01	31.90	1	1526
1101	11:23:23	2024-11-01	31.90	1	1527
1101	11:23:10	2024-11-01	31.95	3	1528
1101	11:23:09	2024-11-01	31.90	1	1529
1101	11:22:56	2024-11-01	31.90	1	1530
1101	11:22:50	2024-11-01	31.90	3	1531
1101	11:22:44	2024-11-01	31.90	1	1532
1101	11:22:42	2024-11-01	31.95	1	1533
1101	11:22:42	2024-11-01	31.95	2	1534
1101	11:22:32	2024-11-01	31.90	3	1535
1101	11:22:30	2024-11-01	31.90	1	1536
1101	11:22:18	2024-11-01	31.90	1	1537
1101	11:22:17	2024-11-01	31.90	1	1538
1101	11:22:15	2024-11-01	31.95	2	1539
1101	11:22:04	2024-11-01	31.90	1	1540
1101	11:21:51	2024-11-01	31.90	1	1541
1101	11:21:51	2024-11-01	31.95	2	1542
1101	11:21:38	2024-11-01	31.90	1	1543
1101	11:21:26	2024-11-01	31.90	1	1544
1101	11:21:25	2024-11-01	31.95	3	1545
1101	11:21:12	2024-11-01	31.90	1	1546
1101	11:21:00	2024-11-01	31.90	1	1547
1101	11:20:47	2024-11-01	31.95	2	1548
1101	11:20:47	2024-11-01	31.90	1	1549
1101	11:20:34	2024-11-01	31.95	2	1550
1101	11:20:34	2024-11-01	31.90	1	1551
1101	11:20:20	2024-11-01	31.90	1	1552
1101	11:20:17	2024-11-01	31.90	9	1553
1101	11:20:07	2024-11-01	31.90	1	1554
1101	11:20:05	2024-11-01	31.90	1	1555
1101	11:20:01	2024-11-01	31.90	3	1556
1101	11:19:54	2024-11-01	31.90	1	1557
1101	11:19:41	2024-11-01	31.90	1	1558
1101	11:19:28	2024-11-01	31.90	1	1559
1101	11:19:15	2024-11-01	31.90	1	1560
1101	11:19:13	2024-11-01	31.95	3	1561
1101	11:19:02	2024-11-01	31.90	1	1562
1101	11:18:49	2024-11-01	31.90	1	1563
1101	11:18:47	2024-11-01	31.90	1	1564
1101	11:18:44	2024-11-01	31.90	1	1565
1101	11:18:43	2024-11-01	31.95	2	1566
1101	11:18:43	2024-11-01	31.90	20	1567
1101	11:18:41	2024-11-01	31.95	5	1568
1101	11:18:36	2024-11-01	31.90	1	1569
1101	11:18:24	2024-11-01	31.95	2	1570
1101	11:18:23	2024-11-01	31.90	1	1571
1101	11:18:11	2024-11-01	31.90	1	1572
1101	11:17:58	2024-11-01	31.95	2	1573
1101	11:17:57	2024-11-01	31.90	1	1574
1101	11:17:45	2024-11-01	31.90	2	1575
1101	11:17:45	2024-11-01	31.90	2	1576
1101	11:17:44	2024-11-01	31.90	1	1577
1101	11:17:42	2024-11-01	31.95	2	1578
1101	11:17:31	2024-11-01	31.90	1	1579
1101	11:17:18	2024-11-01	31.90	1	1580
1101	11:17:18	2024-11-01	31.95	2	1581
1101	11:17:05	2024-11-01	31.90	1	1582
1101	11:16:55	2024-11-01	31.90	1	1583
1101	11:16:52	2024-11-01	31.90	1	1584
1101	11:16:47	2024-11-01	31.90	1	1585
1101	11:16:46	2024-11-01	31.95	2	1586
1101	11:16:46	2024-11-01	31.90	3	1587
1101	11:16:39	2024-11-01	31.90	1	1588
1101	11:16:26	2024-11-01	31.90	1	1589
1101	11:16:26	2024-11-01	31.95	2	1590
1101	11:16:13	2024-11-01	31.90	1	1591
1101	11:16:12	2024-11-01	31.95	2	1592
1101	11:16:01	2024-11-01	31.90	1	1593
1101	11:15:47	2024-11-01	31.90	1	1594
1101	11:15:35	2024-11-01	31.95	2	1595
1101	11:15:34	2024-11-01	31.90	1	1596
1101	11:15:22	2024-11-01	31.95	2	1597
1101	11:15:21	2024-11-01	31.90	1	1598
1101	11:15:17	2024-11-01	31.90	2	1599
1101	11:15:08	2024-11-01	31.90	1	1600
1101	11:14:55	2024-11-01	31.90	1	1601
1101	11:14:53	2024-11-01	31.95	2	1602
1101	11:14:52	2024-11-01	31.90	1	1603
1101	11:14:52	2024-11-01	31.90	13	1604
1101	11:14:42	2024-11-01	31.90	1	1605
1101	11:14:40	2024-11-01	31.95	2	1606
1101	11:14:29	2024-11-01	31.90	1	1607
1101	11:14:16	2024-11-01	31.90	1	1608
1101	11:14:15	2024-11-01	31.95	3	1609
1101	11:14:03	2024-11-01	31.90	1	1610
1101	11:13:50	2024-11-01	31.90	1	1611
1101	11:13:49	2024-11-01	31.95	1	1612
1101	11:13:38	2024-11-01	31.95	2	1613
1101	11:13:37	2024-11-01	31.90	1	1614
1101	11:13:29	2024-11-01	31.90	2	1615
1101	11:13:25	2024-11-01	31.90	1	1616
1101	11:13:23	2024-11-01	31.90	1	1617
1101	11:13:11	2024-11-01	31.90	1	1618
1101	11:13:09	2024-11-01	31.90	2	1619
1101	11:13:09	2024-11-01	31.90	68	1620
1101	11:13:09	2024-11-01	31.90	2	1621
1101	11:12:58	2024-11-01	31.85	1	1622
1101	11:12:54	2024-11-01	31.85	2	1623
1101	11:12:45	2024-11-01	31.85	1	1624
1101	11:12:45	2024-11-01	31.85	1	1625
1101	11:12:43	2024-11-01	31.90	3	1626
1101	11:12:32	2024-11-01	31.85	1	1627
1101	11:12:20	2024-11-01	31.90	1	1628
1101	11:12:19	2024-11-01	31.85	1	1629
1101	11:12:19	2024-11-01	31.85	3	1630
1101	11:12:17	2024-11-01	31.90	2	1631
1101	11:12:06	2024-11-01	31.85	1	1632
1101	11:11:54	2024-11-01	31.90	2	1633
1101	11:11:53	2024-11-01	31.85	1	1634
1101	11:11:47	2024-11-01	31.85	4	1635
1101	11:11:41	2024-11-01	31.85	1	1636
1101	11:11:36	2024-11-01	31.90	3	1637
1101	11:11:31	2024-11-01	31.90	2	1638
1101	11:11:31	2024-11-01	31.90	1	1639
1101	11:11:27	2024-11-01	31.85	1	1640
1101	11:11:14	2024-11-01	31.85	1	1641
1101	11:11:02	2024-11-01	31.90	2	1642
1101	11:11:01	2024-11-01	31.85	1	1643
1101	11:10:49	2024-11-01	31.85	1	1644
1101	11:10:48	2024-11-01	31.85	1	1645
1101	11:10:47	2024-11-01	31.90	2	1646
1101	11:10:35	2024-11-01	31.85	1	1647
1101	11:10:23	2024-11-01	31.85	1	1648
1101	11:10:23	2024-11-01	31.90	2	1649
1101	11:10:22	2024-11-01	31.85	1	1650
1101	11:10:09	2024-11-01	31.85	1	1651
1101	11:10:00	2024-11-01	31.90	1	1652
1101	11:09:56	2024-11-01	31.85	1	1653
1101	11:09:56	2024-11-01	31.90	3	1654
1101	11:09:43	2024-11-01	31.85	1	1655
1101	11:09:31	2024-11-01	31.90	2	1656
1101	11:09:30	2024-11-01	31.85	1	1657
1101	11:09:17	2024-11-01	31.85	1	1658
1101	11:09:04	2024-11-01	31.85	1	1659
1101	11:09:02	2024-11-01	31.90	4	1660
1101	11:08:51	2024-11-01	31.85	1	1661
1101	11:08:38	2024-11-01	31.85	1	1662
1101	11:08:25	2024-11-01	31.80	1	1663
1101	11:08:19	2024-11-01	31.80	1	1664
1101	11:08:18	2024-11-01	31.85	22	1665
1101	11:08:14	2024-11-01	31.80	1	1666
1101	11:08:13	2024-11-01	31.80	13	1667
1101	11:08:13	2024-11-01	31.80	5	1668
1101	11:08:13	2024-11-01	31.85	1	1669
1101	11:08:13	2024-11-01	31.85	15	1670
1101	11:08:13	2024-11-01	31.80	2	1671
1101	11:08:13	2024-11-01	31.80	2	1672
1101	11:08:13	2024-11-01	31.80	163	1673
1101	11:08:13	2024-11-01	31.80	2	1674
1101	11:08:12	2024-11-01	31.75	1	1675
1101	11:08:11	2024-11-01	31.80	2	1676
1101	11:08:00	2024-11-01	31.80	1	1677
1101	11:07:59	2024-11-01	31.75	1	1678
1101	11:07:47	2024-11-01	31.80	2	1679
1101	11:07:46	2024-11-01	31.75	1	1680
1101	11:07:40	2024-11-01	31.80	1	1681
1101	11:07:33	2024-11-01	31.75	1	1682
1101	11:07:28	2024-11-01	31.75	1	1683
1101	11:07:21	2024-11-01	31.75	1	1684
1101	11:07:20	2024-11-01	31.75	1	1685
1101	11:07:20	2024-11-01	31.80	3	1686
1101	11:07:07	2024-11-01	31.75	1	1687
1101	11:07:05	2024-11-01	31.75	1	1688
1101	11:06:55	2024-11-01	31.80	2	1689
1101	11:06:54	2024-11-01	31.75	1	1690
1101	11:06:41	2024-11-01	31.75	1	1691
1101	11:06:34	2024-11-01	31.80	5	1692
1101	11:06:34	2024-11-01	31.75	1	1693
1101	11:06:33	2024-11-01	31.75	22	1694
1101	11:06:29	2024-11-01	31.80	3	1695
1101	11:06:28	2024-11-01	31.75	1	1696
1101	11:06:15	2024-11-01	31.75	1	1697
1101	11:06:12	2024-11-01	31.75	1	1698
1101	11:06:02	2024-11-01	31.80	1	1699
1101	11:06:02	2024-11-01	31.75	1	1700
1101	11:05:59	2024-11-01	31.75	3	1701
1101	11:05:49	2024-11-01	31.75	1	1702
1101	11:05:36	2024-11-01	31.75	1	1703
1101	11:05:26	2024-11-01	31.75	3	1704
1101	11:05:23	2024-11-01	31.75	1	1705
1101	11:05:21	2024-11-01	31.75	1	1706
1101	11:05:12	2024-11-01	31.75	1	1707
1101	11:05:07	2024-11-01	31.75	1	1708
1101	11:05:03	2024-11-01	31.75	1	1709
1101	11:05:02	2024-11-01	31.75	20	1710
1101	11:05:01	2024-11-01	31.80	8	1711
1101	11:05:01	2024-11-01	31.75	6	1712
1101	11:04:57	2024-11-01	31.75	1	1713
1101	11:04:57	2024-11-01	31.75	2	1714
1101	11:04:44	2024-11-01	31.75	1	1715
1101	11:04:43	2024-11-01	31.75	2	1716
1101	11:04:31	2024-11-01	31.75	1	1717
1101	11:04:18	2024-11-01	31.75	1	1718
1101	11:04:13	2024-11-01	31.75	1	1719
1101	11:04:12	2024-11-01	31.75	30	1720
1101	11:04:10	2024-11-01	31.80	1	1721
1101	11:04:06	2024-11-01	31.75	1	1722
1101	11:04:05	2024-11-01	31.75	4	1723
1101	11:04:05	2024-11-01	31.75	1	1724
1101	11:03:52	2024-11-01	31.75	1	1725
1101	11:03:46	2024-11-01	31.75	1	1726
1101	11:03:39	2024-11-01	31.75	1	1727
1101	11:03:26	2024-11-01	31.75	1	1728
1101	11:03:13	2024-11-01	31.75	1	1729
1101	11:03:12	2024-11-01	31.75	4	1730
1101	11:03:10	2024-11-01	31.80	1	1731
1101	11:03:00	2024-11-01	31.75	1	1732
1101	11:02:47	2024-11-01	31.75	1	1733
1101	11:02:45	2024-11-01	31.75	5	1734
1101	11:02:34	2024-11-01	31.75	1	1735
1101	11:02:34	2024-11-01	31.75	1	1736
1101	11:02:21	2024-11-01	31.75	1	1737
1101	11:02:15	2024-11-01	31.80	10	1738
1101	11:02:09	2024-11-01	31.75	1	1739
1101	11:02:08	2024-11-01	31.75	1	1740
1101	11:02:07	2024-11-01	31.75	4	1741
1101	11:01:55	2024-11-01	31.75	1	1742
1101	11:01:53	2024-11-01	31.75	3	1743
1101	11:01:42	2024-11-01	31.75	1	1744
1101	11:01:37	2024-11-01	31.80	1	1745
1101	11:01:29	2024-11-01	31.75	1	1746
1101	11:01:26	2024-11-01	31.75	3	1747
1101	11:01:16	2024-11-01	31.75	1	1748
1101	11:01:04	2024-11-01	31.75	1	1749
1101	11:01:03	2024-11-01	31.75	1	1750
1101	11:00:50	2024-11-01	31.75	1	1751
1101	11:00:49	2024-11-01	31.75	3	1752
1101	11:00:37	2024-11-01	31.75	1	1753
1101	11:00:35	2024-11-01	31.80	1	1754
1101	11:00:24	2024-11-01	31.75	1	1755
1101	11:00:22	2024-11-01	31.75	3	1756
1101	11:00:19	2024-11-01	31.80	1	1757
1101	11:00:11	2024-11-01	31.75	1	1758
1101	11:00:09	2024-11-01	31.80	1	1759
1101	11:00:03	2024-11-01	31.75	1	1760
1101	11:00:01	2024-11-01	31.75	2	1761
1101	10:59:58	2024-11-01	31.75	1	1762
1101	10:59:52	2024-11-01	31.80	2	1763
1101	10:59:45	2024-11-01	31.75	1	1764
1101	10:59:36	2024-11-01	31.80	5	1765
1101	10:59:32	2024-11-01	31.75	1	1766
1101	10:59:19	2024-11-01	31.75	1	1767
1101	10:59:19	2024-11-01	31.80	1	1768
1101	10:59:12	2024-11-01	31.75	4	1769
1101	10:59:06	2024-11-01	31.75	1	1770
1101	10:59:02	2024-11-01	31.75	2	1771
1101	10:58:55	2024-11-01	31.75	3	1772
1101	10:58:53	2024-11-01	31.75	1	1773
1101	10:58:40	2024-11-01	31.75	1	1774
1101	10:58:32	2024-11-01	31.80	1	1775
1101	10:58:27	2024-11-01	31.75	1	1776
1101	10:58:18	2024-11-01	31.80	1	1777
1101	10:58:14	2024-11-01	31.75	1	1778
1101	10:58:03	2024-11-01	31.75	1	1779
1101	10:58:01	2024-11-01	31.75	1	1780
1101	10:57:48	2024-11-01	31.75	1	1781
1101	10:57:35	2024-11-01	31.75	1	1782
1101	10:57:35	2024-11-01	31.75	1	1783
1101	10:57:22	2024-11-01	31.75	1	1784
1101	10:57:13	2024-11-01	31.80	1	1785
1101	10:57:09	2024-11-01	31.75	1	1786
1101	10:57:01	2024-11-01	31.75	4	1787
1101	10:57:01	2024-11-01	31.75	5	1788
1101	10:56:56	2024-11-01	31.75	1	1789
1101	10:56:49	2024-11-01	31.80	1	1790
1101	10:56:43	2024-11-01	31.75	1	1791
1101	10:56:36	2024-11-01	31.80	1	1792
1101	10:56:30	2024-11-01	31.75	1	1793
1101	10:56:17	2024-11-01	31.75	1	1794
1101	10:56:04	2024-11-01	31.75	1	1795
1101	10:56:00	2024-11-01	31.75	1	1796
1101	10:55:52	2024-11-01	31.80	2	1797
1101	10:55:51	2024-11-01	31.75	1	1798
1101	10:55:45	2024-11-01	31.75	1	1799
1101	10:55:38	2024-11-01	31.75	1	1800
1101	10:55:25	2024-11-01	31.75	1	1801
1101	10:55:12	2024-11-01	31.75	1	1802
1101	10:55:00	2024-11-01	31.75	2	1803
1101	10:54:59	2024-11-01	31.75	1	1804
1101	10:54:46	2024-11-01	31.75	1	1805
1101	10:54:33	2024-11-01	31.75	1	1806
1101	10:54:21	2024-11-01	31.75	1	1807
1101	10:54:20	2024-11-01	31.75	1	1808
1101	10:54:07	2024-11-01	31.75	1	1809
1101	10:53:54	2024-11-01	31.75	1	1810
1101	10:53:41	2024-11-01	31.75	1	1811
1101	10:53:33	2024-11-01	31.80	19	1812
1101	10:53:28	2024-11-01	31.75	1	1813
1101	10:53:15	2024-11-01	31.75	1	1814
1101	10:53:14	2024-11-01	31.80	1	1815
1101	10:53:02	2024-11-01	31.75	1	1816
1101	10:52:49	2024-11-01	31.75	1	1817
1101	10:52:36	2024-11-01	31.75	1	1818
1101	10:52:25	2024-11-01	31.75	1	1819
1101	10:52:23	2024-11-01	31.75	1	1820
1101	10:52:22	2024-11-01	31.75	6	1821
1101	10:52:16	2024-11-01	31.80	2	1822
1101	10:52:11	2024-11-01	31.75	1	1823
1101	10:52:10	2024-11-01	31.75	1	1824
1101	10:52:09	2024-11-01	31.80	1	1825
1101	10:51:57	2024-11-01	31.75	1	1826
1101	10:51:47	2024-11-01	31.75	4	1827
1101	10:51:44	2024-11-01	31.75	1	1828
1101	10:51:43	2024-11-01	31.80	10	1829
1101	10:51:31	2024-11-01	31.80	1	1830
1101	10:51:31	2024-11-01	31.75	1	1831
1101	10:51:23	2024-11-01	31.75	3	1832
1101	10:51:18	2024-11-01	31.75	1	1833
1101	10:51:05	2024-11-01	31.75	1	1834
1101	10:50:55	2024-11-01	31.75	1	1835
1101	10:50:52	2024-11-01	31.75	1	1836
1101	10:50:39	2024-11-01	31.75	1	1837
1101	10:50:26	2024-11-01	31.75	1	1838
1101	10:50:18	2024-11-01	31.75	1	1839
1101	10:50:13	2024-11-01	31.75	1	1840
1101	10:50:00	2024-11-01	31.75	1	1841
1101	10:49:54	2024-11-01	31.75	1	1842
1101	10:49:47	2024-11-01	31.75	1	1843
1101	10:49:34	2024-11-01	31.75	1	1844
1101	10:49:31	2024-11-01	31.75	10	1845
1101	10:49:24	2024-11-01	31.75	1	1846
1101	10:49:24	2024-11-01	31.75	7	1847
1101	10:49:24	2024-11-01	31.75	7	1848
1101	10:49:24	2024-11-01	31.75	7	1849
1101	10:49:24	2024-11-01	31.75	9	1850
1101	10:49:24	2024-11-01	31.75	3	1851
1101	10:49:24	2024-11-01	31.75	1	1852
1101	10:49:24	2024-11-01	31.75	284	1853
1101	10:49:24	2024-11-01	31.75	10	1854
1101	10:49:21	2024-11-01	31.75	1	1855
1101	10:49:08	2024-11-01	31.75	1	1856
1101	10:49:07	2024-11-01	31.80	1	1857
1101	10:49:03	2024-11-01	31.75	4	1858
1101	10:49:00	2024-11-01	31.75	3	1859
1101	10:48:58	2024-11-01	31.80	1	1860
1101	10:48:55	2024-11-01	31.75	1	1861
1101	10:48:42	2024-11-01	31.75	1	1862
1101	10:48:36	2024-11-01	31.80	1	1863
1101	10:48:29	2024-11-01	31.75	1	1864
1101	10:48:16	2024-11-01	31.75	1	1865
1101	10:48:03	2024-11-01	31.75	1	1866
1101	10:47:50	2024-11-01	31.75	1	1867
1101	10:47:42	2024-11-01	31.75	1	1868
1101	10:47:41	2024-11-01	31.75	3	1869
1101	10:47:37	2024-11-01	31.75	1	1870
1101	10:47:32	2024-11-01	31.80	1	1871
1101	10:47:32	2024-11-01	31.80	2	1872
1101	10:47:24	2024-11-01	31.75	1	1873
1101	10:47:11	2024-11-01	31.75	1	1874
1101	10:46:58	2024-11-01	31.75	1	1875
1101	10:46:48	2024-11-01	31.75	4	1876
1101	10:46:45	2024-11-01	31.75	1	1877
1101	10:46:32	2024-11-01	31.75	1	1878
1101	10:46:29	2024-11-01	31.80	5	1879
1101	10:46:26	2024-11-01	31.75	7	1880
1101	10:46:20	2024-11-01	31.75	1	1881
1101	10:46:19	2024-11-01	31.75	1	1882
1101	10:46:17	2024-11-01	31.75	1	1883
1101	10:46:06	2024-11-01	31.75	1	1884
1101	10:45:53	2024-11-01	31.75	1	1885
1101	10:45:53	2024-11-01	31.75	1	1886
1101	10:45:52	2024-11-01	31.75	1	1887
1101	10:45:40	2024-11-01	31.75	1	1888
1101	10:45:30	2024-11-01	31.75	3	1889
1101	10:45:30	2024-11-01	31.75	1	1890
1101	10:45:27	2024-11-01	31.75	1	1891
1101	10:45:14	2024-11-01	31.75	1	1892
1101	10:45:08	2024-11-01	31.75	1	1893
1101	10:45:01	2024-11-01	31.75	1	1894
1101	10:45:00	2024-11-01	31.80	1	1895
1101	10:44:57	2024-11-01	31.80	2	1896
1101	10:44:48	2024-11-01	31.75	1	1897
1101	10:44:47	2024-11-01	31.75	4	1898
1101	10:44:35	2024-11-01	31.75	1	1899
1101	10:44:23	2024-11-01	31.75	1	1900
1101	10:44:09	2024-11-01	31.75	1	1901
1101	10:43:56	2024-11-01	31.75	1	1902
1101	10:43:52	2024-11-01	31.75	3	1903
1101	10:43:51	2024-11-01	31.75	3	1904
1101	10:43:43	2024-11-01	31.75	1	1905
1101	10:43:37	2024-11-01	31.75	1	1906
1101	10:43:33	2024-11-01	31.75	8	1907
1101	10:43:30	2024-11-01	31.75	1	1908
1101	10:43:19	2024-11-01	31.75	1	1909
1101	10:43:04	2024-11-01	31.75	1	1910
1101	10:42:56	2024-11-01	31.75	3	1911
1101	10:42:51	2024-11-01	31.75	1	1912
1101	10:42:38	2024-11-01	31.75	1	1913
1101	10:42:37	2024-11-01	31.80	1	1914
1101	10:42:28	2024-11-01	31.80	2	1915
1101	10:42:25	2024-11-01	31.75	1	1916
1101	10:42:12	2024-11-01	31.75	1	1917
1101	10:42:10	2024-11-01	31.80	1	1918
1101	10:42:06	2024-11-01	31.80	1	1919
1101	10:41:59	2024-11-01	31.75	1	1920
1101	10:41:58	2024-11-01	31.80	1	1921
1101	10:41:49	2024-11-01	31.75	4	1922
1101	10:41:46	2024-11-01	31.80	1	1923
1101	10:41:46	2024-11-01	31.75	1	1924
1101	10:41:33	2024-11-01	31.75	1	1925
1101	10:41:25	2024-11-01	31.80	1	1926
1101	10:41:20	2024-11-01	31.75	1	1927
1101	10:41:07	2024-11-01	31.75	1	1928
1101	10:40:57	2024-11-01	31.75	1	1929
1101	10:40:54	2024-11-01	31.75	1	1930
1101	10:40:41	2024-11-01	31.75	1	1931
1101	10:40:28	2024-11-01	31.75	1	1932
1101	10:40:17	2024-11-01	31.80	1	1933
1101	10:40:17	2024-11-01	31.80	1	1934
1101	10:40:17	2024-11-01	31.75	12	1935
1101	10:40:15	2024-11-01	31.75	1	1936
1101	10:40:11	2024-11-01	31.80	1	1937
1101	10:40:05	2024-11-01	31.80	1	1938
1101	10:40:04	2024-11-01	31.80	2	1939
1101	10:40:02	2024-11-01	31.75	1	1940
1101	10:40:01	2024-11-01	31.80	3	1941
1101	10:39:49	2024-11-01	31.75	1	1942
1101	10:39:48	2024-11-01	31.80	1	1943
1101	10:39:36	2024-11-01	31.80	1	1944
1101	10:39:36	2024-11-01	31.75	1	1945
1101	10:39:33	2024-11-01	31.75	2	1946
1101	10:39:28	2024-11-01	31.75	1	1947
1101	10:39:23	2024-11-01	31.75	1	1948
1101	10:39:12	2024-11-01	31.80	1	1949
1101	10:39:10	2024-11-01	31.75	1	1950
1101	10:38:57	2024-11-01	31.75	1	1951
1101	10:38:54	2024-11-01	31.80	1	1952
1101	10:38:54	2024-11-01	31.75	3	1953
1101	10:38:48	2024-11-01	31.80	1	1954
1101	10:38:44	2024-11-01	31.80	1	1955
1101	10:38:44	2024-11-01	31.75	1	1956
1101	10:38:31	2024-11-01	31.80	1	1957
1101	10:38:31	2024-11-01	31.75	1	1958
1101	10:38:18	2024-11-01	31.75	1	1959
1101	10:38:15	2024-11-01	31.80	1	1960
1101	10:38:14	2024-11-01	31.80	1	1961
1101	10:38:14	2024-11-01	31.80	3	1962
1101	10:38:05	2024-11-01	31.80	1	1963
1101	10:38:05	2024-11-01	31.75	1	1964
1101	10:38:00	2024-11-01	31.80	1	1965
1101	10:38:00	2024-11-01	31.75	10	1966
1101	10:37:56	2024-11-01	31.80	1	1967
1101	10:37:55	2024-11-01	31.75	1	1968
1101	10:37:52	2024-11-01	31.75	1	1969
1101	10:37:39	2024-11-01	31.75	1	1970
1101	10:37:26	2024-11-01	31.75	1	1971
1101	10:37:13	2024-11-01	31.75	1	1972
1101	10:37:00	2024-11-01	31.75	1	1973
1101	10:37:00	2024-11-01	31.75	4	1974
1101	10:36:48	2024-11-01	31.75	1	1975
1101	10:36:34	2024-11-01	31.75	1	1976
1101	10:36:21	2024-11-01	31.75	1	1977
1101	10:36:08	2024-11-01	31.75	1	1978
1101	10:35:55	2024-11-01	31.75	1	1979
1101	10:35:42	2024-11-01	31.75	1	1980
1101	10:35:36	2024-11-01	31.80	5	1981
1101	10:35:29	2024-11-01	31.75	1	1982
1101	10:35:16	2024-11-01	31.75	1	1983
1101	10:35:13	2024-11-01	31.75	1	1984
1101	10:35:03	2024-11-01	31.75	1	1985
1101	10:35:01	2024-11-01	31.80	2	1986
1101	10:34:58	2024-11-01	31.80	2	1987
1101	10:34:57	2024-11-01	31.75	1	1988
1101	10:34:50	2024-11-01	31.75	1	1989
1101	10:34:37	2024-11-01	31.75	1	1990
1101	10:34:28	2024-11-01	31.75	1	1991
1101	10:34:26	2024-11-01	31.80	2	1992
1101	10:34:24	2024-11-01	31.75	1	1993
1101	10:34:13	2024-11-01	31.75	1	1994
1101	10:34:11	2024-11-01	31.75	1	1995
1101	10:34:11	2024-11-01	31.80	1	1996
1101	10:34:09	2024-11-01	31.75	1	1997
1101	10:34:08	2024-11-01	31.80	2	1998
1101	10:34:08	2024-11-01	31.80	31	1999
1101	10:33:58	2024-11-01	31.75	1	2000
1101	10:33:45	2024-11-01	31.75	1	2001
1101	10:33:37	2024-11-01	31.80	10	2002
1101	10:33:32	2024-11-01	31.75	1	2003
1101	10:33:19	2024-11-01	31.75	1	2004
1101	10:33:06	2024-11-01	31.75	1	2005
1101	10:32:54	2024-11-01	31.80	1	2006
1101	10:32:53	2024-11-01	31.75	1	2007
1101	10:32:43	2024-11-01	31.80	8	2008
1101	10:32:40	2024-11-01	31.75	1	2009
1101	10:32:34	2024-11-01	31.75	1	2010
1101	10:32:31	2024-11-01	31.75	2	2011
1101	10:32:27	2024-11-01	31.75	1	2012
1101	10:32:23	2024-11-01	31.75	3	2013
1101	10:32:14	2024-11-01	31.75	1	2014
1101	10:32:01	2024-11-01	31.75	1	2015
1101	10:31:58	2024-11-01	31.75	5	2016
1101	10:31:51	2024-11-01	31.80	5	2017
1101	10:31:48	2024-11-01	31.75	1	2018
1101	10:31:35	2024-11-01	31.75	1	2019
1101	10:31:22	2024-11-01	31.75	1	2020
1101	10:31:09	2024-11-01	31.75	1	2021
1101	10:31:06	2024-11-01	31.75	2	2022
1101	10:31:03	2024-11-01	31.80	1	2023
1101	10:31:03	2024-11-01	31.75	2	2024
1101	10:31:01	2024-11-01	31.75	2	2025
1101	10:30:56	2024-11-01	31.80	1	2026
1101	10:30:56	2024-11-01	31.75	1	2027
1101	10:30:49	2024-11-01	31.75	1	2028
1101	10:30:46	2024-11-01	31.80	1	2029
1101	10:30:46	2024-11-01	31.75	3	2030
1101	10:30:43	2024-11-01	31.75	1	2031
1101	10:30:30	2024-11-01	31.75	1	2032
1101	10:30:29	2024-11-01	31.80	1	2033
1101	10:30:29	2024-11-01	31.80	2	2034
1101	10:30:23	2024-11-01	31.80	3	2035
1101	10:30:17	2024-11-01	31.80	1	2036
1101	10:30:17	2024-11-01	31.75	1	2037
1101	10:30:04	2024-11-01	31.75	1	2038
1101	10:29:57	2024-11-01	31.75	2	2039
1101	10:29:51	2024-11-01	31.75	1	2040
1101	10:29:45	2024-11-01	31.75	7	2041
1101	10:29:38	2024-11-01	31.75	1	2042
1101	10:29:25	2024-11-01	31.75	1	2043
1101	10:29:24	2024-11-01	31.80	1	2044
1101	10:29:17	2024-11-01	31.80	1	2045
1101	10:29:14	2024-11-01	31.80	1	2046
1101	10:29:12	2024-11-01	31.75	1	2047
1101	10:29:10	2024-11-01	31.75	1	2048
1101	10:28:59	2024-11-01	31.80	1	2049
1101	10:28:59	2024-11-01	31.75	1	2050
1101	10:28:53	2024-11-01	31.75	1	2051
1101	10:28:51	2024-11-01	31.80	2	2052
1101	10:28:46	2024-11-01	31.80	1	2053
1101	10:28:46	2024-11-01	31.75	1	2054
1101	10:28:33	2024-11-01	31.75	1	2055
1101	10:28:20	2024-11-01	31.80	1	2056
1101	10:28:20	2024-11-01	31.75	1	2057
1101	10:28:20	2024-11-01	31.75	1	2058
1101	10:28:16	2024-11-01	31.80	1	2059
1101	10:28:10	2024-11-01	31.80	1	2060
1101	10:28:07	2024-11-01	31.80	1	2061
1101	10:28:07	2024-11-01	31.75	1	2062
1101	10:28:07	2024-11-01	31.75	1	2063
1101	10:28:00	2024-11-01	31.80	1	2064
1101	10:28:00	2024-11-01	31.75	1	2065
1101	10:28:00	2024-11-01	31.80	5	2066
1101	10:27:54	2024-11-01	31.75	1	2067
1101	10:27:44	2024-11-01	31.75	1	2068
1101	10:27:44	2024-11-01	31.80	1	2069
1101	10:27:44	2024-11-01	31.80	1	2070
1101	10:27:41	2024-11-01	31.75	1	2071
1101	10:27:28	2024-11-01	31.80	1	2072
1101	10:27:28	2024-11-01	31.75	1	2073
1101	10:27:28	2024-11-01	31.75	1	2074
1101	10:27:15	2024-11-01	31.75	1	2075
1101	10:27:03	2024-11-01	31.75	1	2076
1101	10:27:03	2024-11-01	31.80	1	2077
1101	10:27:03	2024-11-01	31.75	1	2078
1101	10:27:03	2024-11-01	31.80	2	2079
1101	10:27:02	2024-11-01	31.75	1	2080
1101	10:26:59	2024-11-01	31.80	1	2081
1101	10:26:59	2024-11-01	31.75	1	2082
1101	10:26:59	2024-11-01	31.80	2	2083
1101	10:26:49	2024-11-01	31.75	1	2084
1101	10:26:48	2024-11-01	31.80	1	2085
1101	10:26:48	2024-11-01	31.75	1	2086
1101	10:26:48	2024-11-01	31.75	1	2087
1101	10:26:48	2024-11-01	31.80	5	2088
1101	10:26:43	2024-11-01	31.80	1	2089
1101	10:26:36	2024-11-01	31.75	1	2090
1101	10:26:23	2024-11-01	31.80	1	2091
1101	10:26:23	2024-11-01	31.75	1	2092
1101	10:26:23	2024-11-01	31.75	1	2093
1101	10:26:20	2024-11-01	31.75	1	2094
1101	10:26:17	2024-11-01	31.75	2	2095
1101	10:26:10	2024-11-01	31.70	1	2096
1101	10:26:10	2024-11-01	31.70	1	2097
1101	10:26:09	2024-11-01	31.75	1	2098
1101	10:26:09	2024-11-01	31.75	1	2099
1101	10:26:09	2024-11-01	31.70	1	2100
1101	10:26:09	2024-11-01	31.75	5	2101
1101	10:26:06	2024-11-01	31.70	1	2102
1101	10:26:06	2024-11-01	31.75	1	2103
1101	10:26:03	2024-11-01	31.75	1	2104
1101	10:26:03	2024-11-01	31.75	1	2105
1101	10:26:03	2024-11-01	31.70	1	2106
1101	10:26:03	2024-11-01	31.70	1	2107
1101	10:26:03	2024-11-01	31.70	5	2108
1101	10:26:03	2024-11-01	31.75	2	2109
1101	10:26:03	2024-11-01	31.70	2	2110
1101	10:26:03	2024-11-01	31.75	1	2111
1101	10:26:03	2024-11-01	31.75	25	2112
1101	10:26:03	2024-11-01	31.75	6	2113
1101	10:26:02	2024-11-01	31.75	1	2114
1101	10:26:02	2024-11-01	31.75	1	2115
1101	10:26:02	2024-11-01	31.70	2	2116
1101	10:26:02	2024-11-01	31.75	9	2117
1101	10:25:59	2024-11-01	31.70	1	2118
1101	10:25:59	2024-11-01	31.70	1	2119
1101	10:25:57	2024-11-01	31.75	1	2120
1101	10:25:57	2024-11-01	31.75	1	2121
1101	10:25:57	2024-11-01	31.70	1	2122
1101	10:25:57	2024-11-01	31.70	1	2123
1101	10:25:57	2024-11-01	31.75	4	2124
1101	10:25:57	2024-11-01	31.70	2	2125
1101	10:25:57	2024-11-01	31.75	2	2126
1101	10:25:57	2024-11-01	31.70	1	2127
1101	10:25:57	2024-11-01	31.70	7	2128
1101	10:25:57	2024-11-01	31.75	36	2129
1101	10:25:49	2024-11-01	31.75	1	2130
1101	10:25:49	2024-11-01	31.70	1	2131
1101	10:25:49	2024-11-01	31.70	5	2132
1101	10:25:47	2024-11-01	31.70	1	2133
1101	10:25:47	2024-11-01	31.70	1	2134
1101	10:25:47	2024-11-01	31.75	5	2135
1101	10:25:44	2024-11-01	31.70	1	2136
1101	10:25:44	2024-11-01	31.75	1	2137
1101	10:25:44	2024-11-01	31.70	1	2138
1101	10:25:34	2024-11-01	31.70	1	2139
1101	10:25:34	2024-11-01	31.75	1	2140
1101	10:25:31	2024-11-01	31.70	1	2141
1101	10:25:30	2024-11-01	31.75	1	2142
1101	10:25:26	2024-11-01	31.70	1	2143
1101	10:25:26	2024-11-01	31.70	1	2144
1101	10:25:26	2024-11-01	31.75	1	2145
1101	10:25:26	2024-11-01	31.70	1	2146
1101	10:25:26	2024-11-01	31.70	1	2147
1101	10:25:26	2024-11-01	31.75	2	2148
1101	10:25:26	2024-11-01	31.70	5	2149
1101	10:25:26	2024-11-01	31.75	22	2150
1101	10:25:26	2024-11-01	31.75	1	2151
1101	10:25:26	2024-11-01	31.75	1	2152
1101	10:25:25	2024-11-01	31.70	1	2153
1101	10:25:25	2024-11-01	31.75	1	2154
1101	10:25:25	2024-11-01	31.70	2	2155
1101	10:25:25	2024-11-01	31.75	10	2156
1101	10:25:23	2024-11-01	31.70	1	2157
1101	10:25:22	2024-11-01	31.75	2	2158
1101	10:25:22	2024-11-01	31.75	1	2159
1101	10:25:22	2024-11-01	31.75	1	2160
1101	10:25:22	2024-11-01	31.75	1	2161
1101	10:25:20	2024-11-01	31.70	1	2162
1101	10:25:20	2024-11-01	31.75	2	2163
1101	10:25:18	2024-11-01	31.70	1	2164
1101	10:25:18	2024-11-01	31.70	1	2165
1101	10:25:15	2024-11-01	31.70	1	2166
1101	10:25:15	2024-11-01	31.75	1	2167
1101	10:25:15	2024-11-01	31.75	3	2168
1101	10:25:05	2024-11-01	31.70	1	2169
1101	10:25:05	2024-11-01	31.70	1	2170
1101	10:24:59	2024-11-01	31.70	1	2171
1101	10:24:52	2024-11-01	31.70	1	2172
1101	10:24:47	2024-11-01	31.75	1	2173
1101	10:24:45	2024-11-01	31.70	1	2174
1101	10:24:45	2024-11-01	31.75	1	2175
1101	10:24:43	2024-11-01	31.75	1	2176
1101	10:24:39	2024-11-01	31.75	1	2177
1101	10:24:39	2024-11-01	31.70	1	2178
1101	10:24:36	2024-11-01	31.70	1	2179
1101	10:24:36	2024-11-01	31.75	2	2180
1101	10:24:34	2024-11-01	31.75	1	2181
1101	10:24:34	2024-11-01	31.70	1	2182
1101	10:24:34	2024-11-01	31.75	5	2183
1101	10:24:26	2024-11-01	31.70	1	2184
1101	10:24:26	2024-11-01	31.70	1	2185
1101	10:24:24	2024-11-01	31.70	1	2186
1101	10:24:24	2024-11-01	31.75	2	2187
1101	10:24:23	2024-11-01	31.70	1	2188
1101	10:24:23	2024-11-01	31.75	1	2189
1101	10:24:23	2024-11-01	31.75	1	2190
1101	10:24:23	2024-11-01	31.70	2	2191
1101	10:24:23	2024-11-01	31.75	10	2192
1101	10:24:21	2024-11-01	31.70	1	2193
1101	10:24:21	2024-11-01	31.70	2	2194
1101	10:24:19	2024-11-01	31.75	1	2195
1101	10:24:16	2024-11-01	31.70	1	2196
1101	10:24:14	2024-11-01	31.70	1	2197
1101	10:24:14	2024-11-01	31.75	1	2198
1101	10:24:14	2024-11-01	31.70	1	2199
1101	10:24:14	2024-11-01	31.70	1	2200
1101	10:24:14	2024-11-01	31.75	2	2201
1101	10:24:14	2024-11-01	31.75	2	2202
1101	10:24:14	2024-11-01	31.70	4	2203
1101	10:24:14	2024-11-01	31.75	20	2204
1101	10:24:13	2024-11-01	31.70	1	2205
1101	10:24:11	2024-11-01	31.75	1	2206
1101	10:24:11	2024-11-01	31.70	1	2207
1101	10:24:11	2024-11-01	31.70	2	2208
1101	10:24:10	2024-11-01	31.75	1	2209
1101	10:24:10	2024-11-01	31.75	10	2210
1101	10:24:10	2024-11-01	31.75	1	2211
1101	10:24:09	2024-11-01	31.70	1	2212
1101	10:24:09	2024-11-01	31.70	1	2213
1101	10:24:09	2024-11-01	31.75	1	2214
1101	10:24:03	2024-11-01	31.75	1	2215
1101	10:24:01	2024-11-01	31.75	1	2216
1101	10:24:01	2024-11-01	31.75	1	2217
1101	10:24:01	2024-11-01	31.70	1	2218
1101	10:24:01	2024-11-01	31.70	3	2219
1101	10:24:01	2024-11-01	31.75	15	2220
1101	10:24:00	2024-11-01	31.70	1	2221
1101	10:23:53	2024-11-01	31.70	1	2222
1101	10:23:53	2024-11-01	31.75	1	2223
1101	10:23:47	2024-11-01	31.70	1	2224
1101	10:23:37	2024-11-01	31.75	2	2225
1101	10:23:36	2024-11-01	31.70	1	2226
1101	10:23:36	2024-11-01	31.75	1	2227
1101	10:23:36	2024-11-01	31.75	1	2228
1101	10:23:35	2024-11-01	31.70	1	2229
1101	10:23:34	2024-11-01	31.70	1	2230
1101	10:23:21	2024-11-01	31.70	1	2231
1101	10:23:21	2024-11-01	31.70	1	2232
1101	10:23:08	2024-11-01	31.70	1	2233
1101	10:22:55	2024-11-01	31.70	1	2234
1101	10:22:53	2024-11-01	31.75	1	2235
1101	10:22:47	2024-11-01	31.70	1	2236
1101	10:22:47	2024-11-01	31.75	1	2237
1101	10:22:43	2024-11-01	31.70	1	2238
1101	10:22:37	2024-11-01	31.70	1	2239
1101	10:22:32	2024-11-01	31.70	1	2240
1101	10:22:32	2024-11-01	31.70	4	2241
1101	10:22:32	2024-11-01	31.75	20	2242
1101	10:22:29	2024-11-01	31.70	1	2243
1101	10:22:16	2024-11-01	31.70	1	2244
1101	10:22:16	2024-11-01	31.75	1	2245
1101	10:22:16	2024-11-01	31.70	1	2246
1101	10:22:12	2024-11-01	31.75	1	2247
1101	10:22:10	2024-11-01	31.70	1	2248
1101	10:22:10	2024-11-01	31.70	2	2249
1101	10:22:10	2024-11-01	31.75	10	2250
1101	10:22:07	2024-11-01	31.75	1	2251
1101	10:22:03	2024-11-01	31.70	1	2252
1101	10:22:02	2024-11-01	31.70	1	2253
1101	10:22:02	2024-11-01	31.70	5	2254
1101	10:22:01	2024-11-01	31.70	1	2255
1101	10:22:01	2024-11-01	31.70	2	2256
1101	10:22:00	2024-11-01	31.75	10	2257
1101	10:21:58	2024-11-01	31.70	1	2258
1101	10:21:58	2024-11-01	31.70	4	2259
1101	10:21:58	2024-11-01	31.75	2	2260
1101	10:21:58	2024-11-01	31.70	20	2261
1101	10:21:57	2024-11-01	31.75	100	2262
1101	10:21:50	2024-11-01	31.70	1	2263
1101	10:21:50	2024-11-01	31.70	2	2264
1101	10:21:50	2024-11-01	31.70	1	2265
1101	10:21:49	2024-11-01	31.70	1	2266
1101	10:21:37	2024-11-01	31.70	1	2267
1101	10:21:37	2024-11-01	31.70	1	2268
1101	10:21:32	2024-11-01	31.75	1	2269
1101	10:21:25	2024-11-01	31.70	1	2270
1101	10:21:17	2024-11-01	31.70	1	2271
1101	10:21:17	2024-11-01	31.75	5	2272
1101	10:21:16	2024-11-01	31.70	1	2273
1101	10:21:16	2024-11-01	31.70	2	2274
1101	10:21:16	2024-11-01	31.75	2	2275
1101	10:21:11	2024-11-01	31.70	1	2276
1101	10:21:11	2024-11-01	31.70	1	2277
1101	10:21:10	2024-11-01	31.70	1	2278
1101	10:21:08	2024-11-01	31.75	1	2279
1101	10:21:07	2024-11-01	31.70	1	2280
1101	10:21:07	2024-11-01	31.75	3	2281
1101	10:20:58	2024-11-01	31.70	1	2282
1101	10:20:56	2024-11-01	31.75	1	2283
1101	10:20:53	2024-11-01	31.70	1	2284
1101	10:20:53	2024-11-01	31.70	1	2285
1101	10:20:46	2024-11-01	31.70	1	2286
1101	10:20:45	2024-11-01	31.70	1	2287
1101	10:20:38	2024-11-01	31.75	1	2288
1101	10:20:32	2024-11-01	31.70	1	2289
1101	10:20:32	2024-11-01	31.70	1	2290
1101	10:20:20	2024-11-01	31.70	1	2291
1101	10:20:17	2024-11-01	31.70	1	2292
1101	10:20:07	2024-11-01	31.70	1	2293
1101	10:20:01	2024-11-01	31.70	1	2294
1101	10:20:01	2024-11-01	31.70	2	2295
1101	10:20:00	2024-11-01	31.75	1	2296
1101	10:19:55	2024-11-01	31.70	1	2297
1101	10:19:55	2024-11-01	31.70	3	2298
1101	10:19:54	2024-11-01	31.70	1	2299
1101	10:19:41	2024-11-01	31.70	1	2300
1101	10:19:28	2024-11-01	31.70	1	2301
1101	10:19:28	2024-11-01	31.70	1	2302
1101	10:19:27	2024-11-01	31.75	1	2303
1101	10:19:18	2024-11-01	31.70	1	2304
1101	10:19:18	2024-11-01	31.70	3	2305
1101	10:19:15	2024-11-01	31.70	1	2306
1101	10:19:07	2024-11-01	31.70	1	2307
1101	10:19:02	2024-11-01	31.70	1	2308
1101	10:18:49	2024-11-01	31.70	1	2309
1101	10:18:49	2024-11-01	31.70	1	2310
1101	10:18:41	2024-11-01	31.70	1	2311
1101	10:18:40	2024-11-01	31.70	1	2312
1101	10:18:38	2024-11-01	31.70	1	2313
1101	10:18:38	2024-11-01	31.75	2	2314
1101	10:18:37	2024-11-01	31.70	1	2315
1101	10:18:37	2024-11-01	31.70	4	2316
1101	10:18:36	2024-11-01	31.70	1	2317
1101	10:18:23	2024-11-01	31.70	1	2318
1101	10:18:10	2024-11-01	31.70	1	2319
1101	10:17:57	2024-11-01	31.70	1	2320
1101	10:17:57	2024-11-01	31.70	1	2321
1101	10:17:54	2024-11-01	31.70	2	2322
1101	10:17:45	2024-11-01	31.70	1	2323
1101	10:17:44	2024-11-01	31.70	1	2324
1101	10:17:44	2024-11-01	31.70	1	2325
1101	10:17:31	2024-11-01	31.70	1	2326
1101	10:17:31	2024-11-01	31.70	1	2327
1101	10:17:18	2024-11-01	31.70	1	2328
1101	10:17:13	2024-11-01	31.70	1	2329
1101	10:17:13	2024-11-01	31.70	1	2330
1101	10:17:10	2024-11-01	31.70	1	2331
1101	10:17:08	2024-11-01	31.70	1	2332
1101	10:17:08	2024-11-01	31.75	5	2333
1101	10:17:05	2024-11-01	31.70	1	2334
1101	10:16:58	2024-11-01	31.70	1	2335
1101	10:16:58	2024-11-01	31.75	3	2336
1101	10:16:52	2024-11-01	31.70	1	2337
1101	10:16:49	2024-11-01	31.70	1	2338
1101	10:16:49	2024-11-01	31.75	1	2339
1101	10:16:47	2024-11-01	31.70	1	2340
1101	10:16:41	2024-11-01	31.70	1	2341
1101	10:16:39	2024-11-01	31.70	1	2342
1101	10:16:39	2024-11-01	31.70	3	2343
1101	10:16:39	2024-11-01	31.70	1	2344
1101	10:16:32	2024-11-01	31.70	1	2345
1101	10:16:32	2024-11-01	31.75	5	2346
1101	10:16:26	2024-11-01	31.70	1	2347
1101	10:16:26	2024-11-01	31.70	1	2348
1101	10:16:13	2024-11-01	31.70	1	2349
1101	10:16:12	2024-11-01	31.75	1	2350
1101	10:16:09	2024-11-01	31.70	1	2351
1101	10:16:09	2024-11-01	31.70	1	2352
1101	10:16:09	2024-11-01	31.70	1	2353
1101	10:16:07	2024-11-01	31.70	1	2354
1101	10:16:06	2024-11-01	31.70	1	2355
1101	10:16:06	2024-11-01	31.70	4	2356
1101	10:16:05	2024-11-01	31.70	20	2357
1101	10:16:02	2024-11-01	31.70	1	2358
1101	10:16:02	2024-11-01	31.75	2	2359
1101	10:16:02	2024-11-01	31.70	1	2360
1101	10:16:00	2024-11-01	31.70	2	2361
1101	10:16:00	2024-11-01	31.70	1	2362
1101	10:15:48	2024-11-01	31.70	1	2363
1101	10:15:48	2024-11-01	31.70	1	2364
1101	10:15:48	2024-11-01	31.75	5	2365
1101	10:15:47	2024-11-01	31.70	1	2366
1101	10:15:37	2024-11-01	31.70	1	2367
1101	10:15:34	2024-11-01	31.70	1	2368
1101	10:15:32	2024-11-01	31.70	1	2369
1101	10:15:32	2024-11-01	31.70	1	2370
1101	10:15:29	2024-11-01	31.75	1	2371
1101	10:15:21	2024-11-01	31.70	1	2372
1101	10:15:13	2024-11-01	31.70	1	2373
1101	10:15:13	2024-11-01	31.70	2	2374
1101	10:15:12	2024-11-01	31.70	1	2375
1101	10:15:11	2024-11-01	31.70	3	2376
1101	10:15:11	2024-11-01	31.70	11	2377
1101	10:15:08	2024-11-01	31.70	1	2378
1101	10:15:05	2024-11-01	31.70	1	2379
1101	10:15:04	2024-11-01	31.70	1	2380
1101	10:15:04	2024-11-01	31.70	2	2381
1101	10:15:03	2024-11-01	31.75	1	2382
1101	10:14:58	2024-11-01	31.70	1	2383
1101	10:14:58	2024-11-01	31.70	3	2384
1101	10:14:55	2024-11-01	31.70	1	2385
1101	10:14:42	2024-11-01	31.70	1	2386
1101	10:14:35	2024-11-01	31.70	1	2387
1101	10:14:29	2024-11-01	31.70	1	2388
1101	10:14:29	2024-11-01	31.70	1	2389
1101	10:14:26	2024-11-01	31.75	1	2390
1101	10:14:16	2024-11-01	31.70	1	2391
1101	10:14:11	2024-11-01	31.75	1	2392
1101	10:14:10	2024-11-01	31.70	1	2393
1101	10:14:10	2024-11-01	31.75	1	2394
1101	10:14:03	2024-11-01	31.70	1	2395
1101	10:13:50	2024-11-01	31.70	1	2396
1101	10:13:37	2024-11-01	31.70	1	2397
1101	10:13:24	2024-11-01	31.70	1	2398
1101	10:13:24	2024-11-01	31.70	1	2399
1101	10:13:22	2024-11-01	31.70	1	2400
1101	10:13:22	2024-11-01	31.70	5	2401
1101	10:13:12	2024-11-01	31.70	1	2402
1101	10:13:12	2024-11-01	31.75	5	2403
1101	10:13:12	2024-11-01	31.70	1	2404
1101	10:13:11	2024-11-01	31.70	1	2405
1101	10:13:11	2024-11-01	31.70	1	2406
1101	10:13:11	2024-11-01	31.70	1	2407
1101	10:12:58	2024-11-01	31.70	1	2408
1101	10:12:45	2024-11-01	31.70	1	2409
1101	10:12:32	2024-11-01	31.70	1	2410
1101	10:12:32	2024-11-01	31.70	1	2411
1101	10:12:19	2024-11-01	31.70	1	2412
1101	10:12:19	2024-11-01	31.70	1	2413
1101	10:12:13	2024-11-01	31.70	1	2414
1101	10:12:11	2024-11-01	31.70	1	2415
1101	10:12:11	2024-11-01	31.70	1	2416
1101	10:12:09	2024-11-01	31.70	1	2417
1101	10:12:07	2024-11-01	31.70	1	2418
1101	10:12:02	2024-11-01	31.70	1	2419
1101	10:12:00	2024-11-01	31.70	1	2420
1101	10:12:00	2024-11-01	31.70	1	2421
1101	10:12:00	2024-11-01	31.70	1	2422
1101	10:11:58	2024-11-01	31.70	2	2423
1101	10:11:58	2024-11-01	31.70	1	2424
1101	10:11:58	2024-11-01	31.70	1	2425
1101	10:11:58	2024-11-01	31.70	1	2426
1101	10:11:56	2024-11-01	31.70	2	2427
1101	10:11:56	2024-11-01	31.75	10	2428
1101	10:11:53	2024-11-01	31.70	1	2429
1101	10:11:53	2024-11-01	31.70	1	2430
1101	10:11:45	2024-11-01	31.75	1	2431
1101	10:11:41	2024-11-01	31.70	1	2432
1101	10:11:40	2024-11-01	31.70	1	2433
1101	10:11:39	2024-11-01	31.70	1	2434
1101	10:11:39	2024-11-01	31.70	2	2435
1101	10:11:38	2024-11-01	31.70	2	2436
1101	10:11:38	2024-11-01	31.70	1	2437
1101	10:11:38	2024-11-01	31.70	1	2438
1101	10:11:38	2024-11-01	31.70	2	2439
1101	10:11:38	2024-11-01	31.70	1	2440
1101	10:11:38	2024-11-01	31.70	2	2441
1101	10:11:38	2024-11-01	31.70	13	2442
1101	10:11:36	2024-11-01	31.70	1	2443
1101	10:11:36	2024-11-01	31.70	3	2444
1101	10:11:33	2024-11-01	31.70	1	2445
1101	10:11:32	2024-11-01	31.70	1	2446
1101	10:11:32	2024-11-01	31.70	1	2447
1101	10:11:31	2024-11-01	31.70	1	2448
1101	10:11:31	2024-11-01	31.70	1	2449
1101	10:11:30	2024-11-01	31.70	1	2450
1101	10:11:30	2024-11-01	31.70	1	2451
1101	10:11:29	2024-11-01	31.70	1	2452
1101	10:11:27	2024-11-01	31.70	1	2453
1101	10:11:18	2024-11-01	31.70	1	2454
1101	10:11:17	2024-11-01	31.70	1	2455
1101	10:11:14	2024-11-01	31.70	1	2456
1101	10:11:14	2024-11-01	31.70	1	2457
1101	10:11:08	2024-11-01	31.75	1	2458
1101	10:11:04	2024-11-01	31.70	1	2459
1101	10:11:04	2024-11-01	31.75	1	2460
1101	10:11:04	2024-11-01	31.70	1	2461
1101	10:11:04	2024-11-01	31.70	1	2462
1101	10:11:04	2024-11-01	31.70	1	2463
1101	10:11:03	2024-11-01	31.70	1	2464
1101	10:11:02	2024-11-01	31.75	1	2465
1101	10:11:02	2024-11-01	31.75	1	2466
1101	10:11:02	2024-11-01	31.75	1	2467
1101	10:11:01	2024-11-01	31.75	1	2468
1101	10:10:54	2024-11-01	31.75	1	2469
1101	10:10:54	2024-11-01	31.75	1	2470
1101	10:10:53	2024-11-01	31.75	1	2471
1101	10:10:53	2024-11-01	31.75	1	2472
1101	10:10:53	2024-11-01	31.75	5	2473
1101	10:10:52	2024-11-01	31.75	2	2474
1101	10:10:52	2024-11-01	31.75	10	2475
1101	10:10:50	2024-11-01	31.75	1	2476
1101	10:10:46	2024-11-01	31.75	1	2477
1101	10:10:46	2024-11-01	31.75	1	2478
1101	10:10:45	2024-11-01	31.75	2	2479
1101	10:10:43	2024-11-01	31.75	1	2480
1101	10:10:43	2024-11-01	31.75	3	2481
1101	10:10:39	2024-11-01	31.75	1	2482
1101	10:10:38	2024-11-01	31.75	1	2483
1101	10:10:38	2024-11-01	31.75	3	2484
1101	10:10:37	2024-11-01	31.75	1	2485
1101	10:10:23	2024-11-01	31.75	1	2486
1101	10:10:20	2024-11-01	31.75	1	2487
1101	10:10:20	2024-11-01	31.75	1	2488
1101	10:10:19	2024-11-01	31.75	2	2489
1101	10:10:17	2024-11-01	31.75	1	2490
1101	10:10:16	2024-11-01	31.75	1	2491
1101	10:10:16	2024-11-01	31.75	1	2492
1101	10:10:14	2024-11-01	31.75	1	2493
1101	10:10:12	2024-11-01	31.75	1	2494
1101	10:10:03	2024-11-01	31.75	1	2495
1101	10:10:02	2024-11-01	31.75	1	2496
1101	10:10:02	2024-11-01	31.75	1	2497
1101	10:10:02	2024-11-01	31.75	1	2498
1101	10:10:01	2024-11-01	31.75	2	2499
1101	10:10:00	2024-11-01	31.75	1	2500
1101	10:10:00	2024-11-01	31.75	1	2501
1101	10:10:00	2024-11-01	31.75	1	2502
1101	10:09:57	2024-11-01	31.75	1	2503
1101	10:09:57	2024-11-01	31.75	1	2504
1101	10:09:57	2024-11-01	31.75	2	2505
1101	10:09:57	2024-11-01	31.75	1	2506
1101	10:09:57	2024-11-01	31.75	1	2507
1101	10:09:55	2024-11-01	31.75	1	2508
1101	10:09:53	2024-11-01	31.75	1	2509
1101	10:09:53	2024-11-01	31.75	1	2510
1101	10:09:51	2024-11-01	31.75	1	2511
1101	10:09:50	2024-11-01	31.75	1	2512
1101	10:09:48	2024-11-01	31.75	1	2513
1101	10:09:45	2024-11-01	31.75	1	2514
1101	10:09:45	2024-11-01	31.75	1	2515
1101	10:09:43	2024-11-01	31.75	1	2516
1101	10:09:43	2024-11-01	31.75	1	2517
1101	10:09:31	2024-11-01	31.75	1	2518
1101	10:09:29	2024-11-01	31.75	1	2519
1101	10:09:29	2024-11-01	31.75	1	2520
1101	10:09:29	2024-11-01	31.75	1	2521
1101	10:09:28	2024-11-01	31.75	1	2522
1101	10:09:27	2024-11-01	31.75	1	2523
1101	10:09:27	2024-11-01	31.75	1	2524
1101	10:09:27	2024-11-01	31.75	1	2525
1101	10:09:27	2024-11-01	31.75	1	2526
1101	10:09:26	2024-11-01	31.75	1	2527
1101	10:09:24	2024-11-01	31.75	1	2528
1101	10:09:23	2024-11-01	31.75	1	2529
1101	10:09:23	2024-11-01	31.75	1	2530
1101	10:09:21	2024-11-01	31.75	1	2531
1101	10:09:20	2024-11-01	31.75	1	2532
1101	10:09:19	2024-11-01	31.75	1	2533
1101	10:09:19	2024-11-01	31.75	2	2534
1101	10:09:19	2024-11-01	31.75	2	2535
1101	10:09:18	2024-11-01	31.75	1	2536
1101	10:09:18	2024-11-01	31.75	1	2537
1101	10:09:18	2024-11-01	31.75	1	2538
1101	10:09:17	2024-11-01	31.75	1	2539
1101	10:09:16	2024-11-01	31.75	1	2540
1101	10:09:16	2024-11-01	31.75	1	2541
1101	10:09:13	2024-11-01	31.75	1	2542
1101	10:09:13	2024-11-01	31.75	1	2543
1101	10:09:12	2024-11-01	31.75	1	2544
1101	10:09:12	2024-11-01	31.75	1	2545
1101	10:09:11	2024-11-01	31.75	1	2546
1101	10:09:10	2024-11-01	31.75	1	2547
1101	10:09:10	2024-11-01	31.75	1	2548
1101	10:09:07	2024-11-01	31.75	1	2549
1101	10:09:06	2024-11-01	31.75	1	2550
1101	10:09:05	2024-11-01	31.75	1	2551
1101	10:09:05	2024-11-01	31.75	1	2552
1101	10:09:05	2024-11-01	31.75	1	2553
1101	10:09:04	2024-11-01	31.75	3	2554
1101	10:09:04	2024-11-01	31.75	1	2555
1101	10:09:04	2024-11-01	31.75	1	2556
1101	10:09:03	2024-11-01	31.75	2	2557
1101	10:09:01	2024-11-01	31.75	1	2558
1101	10:09:00	2024-11-01	31.75	1	2559
1101	10:09:00	2024-11-01	31.75	1	2560
1101	10:08:59	2024-11-01	31.75	2	2561
1101	10:08:59	2024-11-01	31.75	1	2562
1101	10:08:59	2024-11-01	31.75	3	2563
1101	10:08:55	2024-11-01	31.75	1	2564
1101	10:08:55	2024-11-01	31.75	1	2565
1101	10:08:53	2024-11-01	31.75	1	2566
1101	10:08:53	2024-11-01	31.75	3	2567
1101	10:08:53	2024-11-01	31.75	1	2568
1101	10:08:52	2024-11-01	31.75	1	2569
1101	10:08:52	2024-11-01	31.75	1	2570
1101	10:08:41	2024-11-01	31.75	1	2571
1101	10:08:41	2024-11-01	31.75	1	2572
1101	10:08:35	2024-11-01	31.75	1	2573
1101	10:08:34	2024-11-01	31.75	1	2574
1101	10:08:34	2024-11-01	31.75	1	2575
1101	10:08:33	2024-11-01	31.75	1	2576
1101	10:08:33	2024-11-01	31.75	2	2577
1101	10:08:33	2024-11-01	31.75	1	2578
1101	10:08:32	2024-11-01	31.75	1	2579
1101	10:08:32	2024-11-01	31.75	1	2580
1101	10:08:32	2024-11-01	31.75	1	2581
1101	10:08:32	2024-11-01	31.75	1	2582
1101	10:08:32	2024-11-01	31.75	2	2583
1101	10:08:32	2024-11-01	31.75	2	2584
1101	10:08:29	2024-11-01	31.75	1	2585
1101	10:08:29	2024-11-01	31.75	2	2586
1101	10:08:29	2024-11-01	31.75	1	2587
1101	10:08:29	2024-11-01	31.75	1	2588
1101	10:08:27	2024-11-01	31.75	1	2589
1101	10:08:27	2024-11-01	31.75	1	2590
1101	10:08:27	2024-11-01	31.75	1	2591
1101	10:08:25	2024-11-01	31.75	1	2592
1101	10:08:25	2024-11-01	31.75	1	2593
1101	10:08:24	2024-11-01	31.75	1	2594
1101	10:08:24	2024-11-01	31.75	3	2595
1101	10:08:22	2024-11-01	31.75	1	2596
1101	10:08:22	2024-11-01	31.75	4	2597
1101	10:08:17	2024-11-01	31.75	1	2598
1101	10:08:16	2024-11-01	31.75	1	2599
1101	10:08:14	2024-11-01	31.75	1	2600
1101	10:08:14	2024-11-01	31.75	1	2601
1101	10:08:11	2024-11-01	31.75	1	2602
1101	10:08:07	2024-11-01	31.75	2	2603
1101	10:08:04	2024-11-01	31.75	1	2604
1101	10:08:04	2024-11-01	31.75	1	2605
1101	10:08:02	2024-11-01	31.75	1	2606
1101	10:08:02	2024-11-01	31.75	1	2607
1101	10:07:59	2024-11-01	31.75	1	2608
1101	10:07:59	2024-11-01	31.75	1	2609
1101	10:07:59	2024-11-01	31.75	2	2610
1101	10:07:57	2024-11-01	31.75	1	2611
1101	10:07:57	2024-11-01	31.75	5	2612
1101	10:07:32	2024-11-01	31.75	1	2613
1101	10:07:28	2024-11-01	31.75	1	2614
1101	10:07:28	2024-11-01	31.75	1	2615
1101	10:07:27	2024-11-01	31.75	1	2616
1101	10:07:19	2024-11-01	31.75	2	2617
1101	10:07:17	2024-11-01	31.75	1	2618
1101	10:07:17	2024-11-01	31.75	3	2619
1101	10:07:06	2024-11-01	31.75	1	2620
1101	10:07:05	2024-11-01	31.75	1	2621
1101	10:07:05	2024-11-01	31.75	1	2622
1101	10:07:04	2024-11-01	31.75	2	2623
1101	10:07:01	2024-11-01	31.75	1	2624
1101	10:07:00	2024-11-01	31.75	1	2625
1101	10:07:00	2024-11-01	31.75	3	2626
1101	10:06:59	2024-11-01	31.75	1	2627
1101	10:06:58	2024-11-01	31.75	1	2628
1101	10:06:58	2024-11-01	31.75	1	2629
1101	10:06:58	2024-11-01	31.75	6	2630
1101	10:06:58	2024-11-01	31.75	1	2631
1101	10:06:58	2024-11-01	31.75	4	2632
1101	10:06:55	2024-11-01	31.75	1	2633
1101	10:06:53	2024-11-01	31.75	1	2634
1101	10:06:53	2024-11-01	31.75	2	2635
1101	10:06:53	2024-11-01	31.75	2	2636
1101	10:06:51	2024-11-01	31.75	1	2637
1101	10:06:51	2024-11-01	31.75	3	2638
1101	10:06:50	2024-11-01	31.75	1	2639
1101	10:06:50	2024-11-01	31.75	1	2640
1101	10:06:50	2024-11-01	31.75	1	2641
1101	10:06:50	2024-11-01	31.75	3	2642
1101	10:06:48	2024-11-01	31.75	1	2643
1101	10:06:48	2024-11-01	31.75	3	2644
1101	10:06:45	2024-11-01	31.75	2	2645
1101	10:06:45	2024-11-01	31.75	1	2646
1101	10:06:45	2024-11-01	31.75	1	2647
1101	10:06:45	2024-11-01	31.75	5	2648
1101	10:06:26	2024-11-01	31.75	1	2649
1101	10:06:23	2024-11-01	31.75	1	2650
1101	10:06:20	2024-11-01	31.80	1	2651
1101	10:06:20	2024-11-01	31.75	1	2652
1101	10:06:20	2024-11-01	31.75	2	2653
1101	10:06:15	2024-11-01	31.75	2	2654
1101	10:06:15	2024-11-01	31.75	1	2655
1101	10:06:15	2024-11-01	31.75	2	2656
1101	10:06:10	2024-11-01	31.75	1	2657
1101	10:06:10	2024-11-01	31.75	3	2658
1101	10:06:08	2024-11-01	31.80	1	2659
1101	10:06:05	2024-11-01	31.75	2	2660
1101	10:06:00	2024-11-01	31.75	1	2661
1101	10:06:00	2024-11-01	31.75	2	2662
1101	10:05:53	2024-11-01	31.75	2	2663
1101	10:05:49	2024-11-01	31.75	1	2664
1101	10:05:49	2024-11-01	31.75	2	2665
1101	10:05:49	2024-11-01	31.75	1	2666
1101	10:05:30	2024-11-01	31.75	1	2667
1101	10:05:26	2024-11-01	31.75	1	2668
1101	10:05:26	2024-11-01	31.75	1	2669
1101	10:05:24	2024-11-01	31.75	2	2670
1101	10:05:21	2024-11-01	31.75	1	2671
1101	10:05:21	2024-11-01	31.75	3	2672
1101	10:05:21	2024-11-01	31.75	1	2673
1101	10:05:15	2024-11-01	31.75	1	2674
1101	10:05:09	2024-11-01	31.75	1	2675
1101	10:05:08	2024-11-01	31.75	1	2676
1101	10:05:03	2024-11-01	31.75	1	2677
1101	10:04:56	2024-11-01	31.75	1	2678
1101	10:04:49	2024-11-01	31.75	1	2679
1101	10:04:46	2024-11-01	31.75	1	2680
1101	10:04:46	2024-11-01	31.75	3	2681
1101	10:04:45	2024-11-01	31.75	1	2682
1101	10:04:45	2024-11-01	31.75	1	2683
1101	10:04:45	2024-11-01	31.75	1	2684
1101	10:04:44	2024-11-01	31.75	2	2685
1101	10:04:43	2024-11-01	31.75	1	2686
1101	10:04:43	2024-11-01	31.75	2	2687
1101	10:04:40	2024-11-01	31.75	3	2688
1101	10:04:40	2024-11-01	31.75	13	2689
1101	10:04:39	2024-11-01	31.75	1	2690
1101	10:04:39	2024-11-01	31.75	4	2691
1101	10:04:39	2024-11-01	31.75	2	2692
1101	10:04:39	2024-11-01	31.75	17	2693
1101	10:04:39	2024-11-01	31.75	1	2694
1101	10:04:39	2024-11-01	31.75	1	2695
1101	10:04:39	2024-11-01	31.75	2	2696
1101	10:04:38	2024-11-01	31.75	6	2697
1101	10:04:38	2024-11-01	31.75	1	2698
1101	10:04:38	2024-11-01	31.75	1	2699
1101	10:04:38	2024-11-01	31.75	6	2700
1101	10:04:38	2024-11-01	31.75	6	2701
1101	10:04:38	2024-11-01	31.75	8	2702
1101	10:04:33	2024-11-01	31.75	1	2703
1101	10:04:32	2024-11-01	31.75	1	2704
1101	10:04:32	2024-11-01	31.75	1	2705
1101	10:04:32	2024-11-01	31.75	2	2706
1101	10:04:32	2024-11-01	31.75	8	2707
1101	10:04:32	2024-11-01	31.75	1	2708
1101	10:04:32	2024-11-01	31.75	3	2709
1101	10:04:32	2024-11-01	31.75	11	2710
1101	10:04:32	2024-11-01	31.75	57	2711
1101	10:04:30	2024-11-01	31.75	1	2712
1101	10:04:29	2024-11-01	31.75	3	2713
1101	10:04:29	2024-11-01	31.75	17	2714
1101	10:04:29	2024-11-01	31.75	84	2715
1101	10:04:01	2024-11-01	31.75	1	2716
1101	10:03:58	2024-11-01	31.75	1	2717
1101	10:03:58	2024-11-01	31.75	1	2718
1101	10:03:55	2024-11-01	31.75	1	2719
1101	10:03:54	2024-11-01	31.75	1	2720
1101	10:03:39	2024-11-01	31.75	1	2721
1101	10:03:39	2024-11-01	31.75	1	2722
1101	10:03:39	2024-11-01	31.75	2	2723
1101	10:03:37	2024-11-01	31.75	1	2724
1101	10:03:37	2024-11-01	31.75	3	2725
1101	10:03:23	2024-11-01	31.75	1	2726
1101	10:03:22	2024-11-01	31.75	1	2727
1101	10:03:22	2024-11-01	31.75	3	2728
1101	10:03:21	2024-11-01	31.75	1	2729
1101	10:03:21	2024-11-01	31.75	5	2730
1101	10:03:16	2024-11-01	31.75	1	2731
1101	10:03:14	2024-11-01	31.75	1	2732
1101	10:03:14	2024-11-01	31.75	1	2733
1101	10:03:14	2024-11-01	31.75	1	2734
1101	10:03:14	2024-11-01	31.80	5	2735
1101	10:03:12	2024-11-01	31.75	1	2736
1101	10:03:12	2024-11-01	31.75	1	2737
1101	10:03:12	2024-11-01	31.80	3	2738
1101	10:03:11	2024-11-01	31.80	3	2739
1101	10:03:11	2024-11-01	31.80	1	2740
1101	10:03:11	2024-11-01	31.80	1	2741
1101	10:03:11	2024-11-01	31.80	5	2742
1101	10:03:10	2024-11-01	31.80	1	2743
1101	10:03:09	2024-11-01	31.80	1	2744
1101	10:03:09	2024-11-01	31.80	1	2745
1101	10:03:09	2024-11-01	31.80	1	2746
1101	10:03:08	2024-11-01	31.80	1	2747
1101	10:03:08	2024-11-01	31.80	1	2748
1101	10:03:08	2024-11-01	31.80	1	2749
1101	10:03:08	2024-11-01	31.80	3	2750
1101	10:03:07	2024-11-01	31.80	1	2751
1101	10:03:07	2024-11-01	31.80	4	2752
1101	10:03:07	2024-11-01	31.80	1	2753
1101	10:03:07	2024-11-01	31.80	1	2754
1101	10:03:07	2024-11-01	31.80	1	2755
1101	10:03:07	2024-11-01	31.80	4	2756
1101	10:03:03	2024-11-01	31.80	1	2757
1101	10:03:03	2024-11-01	31.80	2	2758
1101	10:03:03	2024-11-01	31.80	1	2759
1101	10:02:59	2024-11-01	31.80	1	2760
1101	10:02:59	2024-11-01	31.80	1	2761
1101	10:02:59	2024-11-01	31.80	4	2762
1101	10:02:57	2024-11-01	31.80	1	2763
1101	10:02:57	2024-11-01	31.80	1	2764
1101	10:02:57	2024-11-01	31.80	3	2765
1101	10:02:56	2024-11-01	31.80	1	2766
1101	10:02:54	2024-11-01	31.80	1	2767
1101	10:02:54	2024-11-01	31.80	2	2768
1101	10:02:50	2024-11-01	31.80	1	2769
1101	10:02:49	2024-11-01	31.80	1	2770
1101	10:02:49	2024-11-01	31.80	2	2771
1101	10:02:46	2024-11-01	31.80	1	2772
1101	10:02:45	2024-11-01	31.80	1	2773
1101	10:02:44	2024-11-01	31.80	1	2774
1101	10:02:44	2024-11-01	31.80	1	2775
1101	10:02:43	2024-11-01	31.80	1	2776
1101	10:02:37	2024-11-01	31.80	1	2777
1101	10:02:36	2024-11-01	31.80	2	2778
1101	10:02:34	2024-11-01	31.80	1	2779
1101	10:02:34	2024-11-01	31.80	3	2780
1101	10:02:25	2024-11-01	31.80	1	2781
1101	10:02:25	2024-11-01	31.80	1	2782
1101	10:02:25	2024-11-01	31.80	2	2783
1101	10:02:25	2024-11-01	31.80	1	2784
1101	10:02:24	2024-11-01	31.80	1	2785
1101	10:02:24	2024-11-01	31.80	2	2786
1101	10:02:18	2024-11-01	31.80	2	2787
1101	10:02:18	2024-11-01	31.80	1	2788
1101	10:02:18	2024-11-01	31.80	2	2789
1101	10:02:18	2024-11-01	31.80	1	2790
1101	10:02:18	2024-11-01	31.80	1	2791
1101	10:02:18	2024-11-01	31.80	2	2792
1101	10:02:18	2024-11-01	31.80	7	2793
1101	10:02:17	2024-11-01	31.80	2	2794
1101	10:02:17	2024-11-01	31.80	10	2795
1101	10:02:13	2024-11-01	31.80	1	2796
1101	10:02:13	2024-11-01	31.80	1	2797
1101	10:02:12	2024-11-01	31.80	1	2798
1101	10:01:56	2024-11-01	31.80	1	2799
1101	10:01:56	2024-11-01	31.80	1	2800
1101	10:01:56	2024-11-01	31.80	2	2801
1101	10:01:55	2024-11-01	31.80	1	2802
1101	10:01:55	2024-11-01	31.80	2	2803
1101	10:01:55	2024-11-01	31.80	2	2804
1101	10:01:55	2024-11-01	31.80	13	2805
1101	10:01:55	2024-11-01	31.80	1	2806
1101	10:01:55	2024-11-01	31.80	2	2807
1101	10:01:51	2024-11-01	31.80	1	2808
1101	10:01:51	2024-11-01	31.80	3	2809
1101	10:01:49	2024-11-01	31.80	1	2810
1101	10:01:49	2024-11-01	31.80	4	2811
1101	10:01:48	2024-11-01	31.80	1	2812
1101	10:01:47	2024-11-01	31.80	1	2813
1101	10:01:44	2024-11-01	31.80	1	2814
1101	10:01:41	2024-11-01	31.80	1	2815
1101	10:01:40	2024-11-01	31.80	1	2816
1101	10:01:37	2024-11-01	31.80	1	2817
1101	10:01:36	2024-11-01	31.80	1	2818
1101	10:01:34	2024-11-01	31.80	1	2819
1101	10:01:32	2024-11-01	31.80	1	2820
1101	10:01:32	2024-11-01	31.80	1	2821
1101	10:01:32	2024-11-01	31.80	1	2822
1101	10:01:21	2024-11-01	31.80	1	2823
1101	10:01:17	2024-11-01	31.80	1	2824
1101	10:01:15	2024-11-01	31.80	2	2825
1101	10:01:07	2024-11-01	31.80	3	2826
1101	10:01:04	2024-11-01	31.80	1	2827
1101	10:01:04	2024-11-01	31.80	4	2828
1101	10:00:46	2024-11-01	31.80	3	2829
1101	10:00:43	2024-11-01	31.80	1	2830
1101	10:00:43	2024-11-01	31.80	5	2831
1101	10:00:23	2024-11-01	31.85	5	2832
1101	09:59:59	2024-11-01	31.80	1	2833
1101	09:59:58	2024-11-01	31.80	1	2834
1101	09:59:58	2024-11-01	31.80	2	2835
1101	09:59:57	2024-11-01	31.80	3	2836
1101	09:59:42	2024-11-01	31.80	2	2837
1101	09:59:42	2024-11-01	31.80	3	2838
1101	09:58:34	2024-11-01	31.85	1	2839
1101	09:58:23	2024-11-01	31.80	2	2840
1101	09:58:20	2024-11-01	31.80	4	2841
1101	09:57:23	2024-11-01	31.85	1	2842
1101	09:57:11	2024-11-01	31.85	1	2843
1101	09:56:30	2024-11-01	31.80	2	2844
1101	09:56:27	2024-11-01	31.80	3	2845
1101	09:56:27	2024-11-01	31.80	1	2846
1101	09:56:26	2024-11-01	31.80	2	2847
1101	09:56:24	2024-11-01	31.80	3	2848
1101	09:55:50	2024-11-01	31.80	1	2849
1101	09:55:48	2024-11-01	31.80	1	2850
1101	09:55:43	2024-11-01	31.80	1	2851
1101	09:55:43	2024-11-01	31.80	1	2852
1101	09:55:40	2024-11-01	31.80	2	2853
1101	09:55:25	2024-11-01	31.80	5	2854
1101	09:55:24	2024-11-01	31.80	10	2855
1101	09:55:21	2024-11-01	31.85	1	2856
1101	09:55:00	2024-11-01	31.85	1	2857
1101	09:54:56	2024-11-01	31.80	2	2858
1101	09:54:56	2024-11-01	31.80	3	2859
1101	09:54:16	2024-11-01	31.80	2	2860
1101	09:54:15	2024-11-01	31.80	3	2861
1101	09:54:11	2024-11-01	31.80	2	2862
1101	09:54:09	2024-11-01	31.80	3	2863
1101	09:53:20	2024-11-01	31.80	6	2864
1101	09:53:18	2024-11-01	31.80	1	2865
1101	09:53:18	2024-11-01	31.80	10	2866
1101	09:53:17	2024-11-01	31.80	1	2867
1101	09:53:15	2024-11-01	31.80	1	2868
1101	09:53:00	2024-11-01	31.80	1	2869
1101	09:52:59	2024-11-01	31.80	1	2870
1101	09:52:48	2024-11-01	31.80	1	2871
1101	09:52:48	2024-11-01	31.80	9	2872
1101	09:52:47	2024-11-01	31.80	1	2873
1101	09:52:47	2024-11-01	31.80	16	2874
1101	09:52:42	2024-11-01	31.80	1	2875
1101	09:52:41	2024-11-01	31.80	2	2876
1101	09:52:27	2024-11-01	31.80	6	2877
1101	09:52:27	2024-11-01	31.85	13	2878
1101	09:52:26	2024-11-01	31.85	12	2879
1101	09:52:26	2024-11-01	31.85	9	2880
1101	09:52:26	2024-11-01	31.85	5	2881
1101	09:52:26	2024-11-01	31.85	7	2882
1101	09:52:26	2024-11-01	31.85	2	2883
1101	09:52:25	2024-11-01	31.85	1	2884
1101	09:52:25	2024-11-01	31.85	69	2885
1101	09:52:25	2024-11-01	31.85	1	2886
1101	09:52:25	2024-11-01	31.90	69	2887
1101	09:52:25	2024-11-01	31.90	119	2888
1101	09:52:25	2024-11-01	31.90	50	2889
1101	09:52:00	2024-11-01	31.90	1	2890
1101	09:51:56	2024-11-01	31.90	1	2891
1101	09:51:33	2024-11-01	31.90	2	2892
1101	09:51:33	2024-11-01	31.90	3	2893
1101	09:50:48	2024-11-01	31.90	1	2894
1101	09:50:45	2024-11-01	31.90	1	2895
1101	09:50:28	2024-11-01	31.90	1	2896
1101	09:50:27	2024-11-01	31.90	2	2897
1101	09:50:26	2024-11-01	31.95	2	2898
1101	09:50:23	2024-11-01	31.90	3	2899
1101	09:50:23	2024-11-01	31.90	5	2900
1101	09:50:19	2024-11-01	31.90	10	2901
1101	09:49:46	2024-11-01	31.90	2	2902
1101	09:49:45	2024-11-01	31.90	3	2903
1101	09:49:27	2024-11-01	31.90	1	2904
1101	09:49:27	2024-11-01	31.90	1	2905
1101	09:48:50	2024-11-01	31.90	1	2906
1101	09:48:46	2024-11-01	31.90	1	2907
1101	09:48:13	2024-11-01	31.95	2	2908
1101	09:47:20	2024-11-01	31.90	1	2909
1101	09:47:15	2024-11-01	31.90	1	2910
1101	09:47:12	2024-11-01	31.90	2	2911
1101	09:47:06	2024-11-01	31.95	3	2912
1101	09:46:59	2024-11-01	31.95	1	2913
1101	09:46:54	2024-11-01	31.95	1	2914
1101	09:46:54	2024-11-01	31.95	1	2915
1101	09:46:54	2024-11-01	31.95	1	2916
1101	09:46:50	2024-11-01	31.95	8	2917
1101	09:46:45	2024-11-01	31.95	8	2918
1101	09:46:36	2024-11-01	31.90	1	2919
1101	09:46:34	2024-11-01	31.90	1	2920
1101	09:46:32	2024-11-01	31.90	2	2921
1101	09:46:30	2024-11-01	31.90	3	2922
1101	09:46:29	2024-11-01	31.90	1	2923
1101	09:46:25	2024-11-01	31.90	1	2924
1101	09:46:22	2024-11-01	31.90	1	2925
1101	09:46:18	2024-11-01	31.90	2	2926
1101	09:46:17	2024-11-01	31.90	3	2927
1101	09:46:14	2024-11-01	31.90	1	2928
1101	09:46:14	2024-11-01	31.90	12	2929
1101	09:46:14	2024-11-01	31.90	1	2930
1101	09:46:06	2024-11-01	31.90	1	2931
1101	09:46:01	2024-11-01	31.90	2	2932
1101	09:46:01	2024-11-01	31.90	1	2933
1101	09:46:01	2024-11-01	31.90	1	2934
1101	09:46:01	2024-11-01	31.90	2	2935
1101	09:46:01	2024-11-01	31.90	1	2936
1101	09:46:01	2024-11-01	31.90	31	2937
1101	09:45:56	2024-11-01	31.90	1	2938
1101	09:45:56	2024-11-01	31.90	1	2939
1101	09:45:56	2024-11-01	31.90	10	2940
1101	09:45:49	2024-11-01	31.90	1	2941
1101	09:45:36	2024-11-01	31.90	1	2942
1101	09:45:33	2024-11-01	31.90	1	2943
1101	09:45:33	2024-11-01	31.90	2	2944
1101	09:45:23	2024-11-01	31.85	2	2945
1101	09:45:20	2024-11-01	31.85	3	2946
1101	09:45:19	2024-11-01	31.90	1	2947
1101	09:45:19	2024-11-01	31.90	1	2948
1101	09:45:05	2024-11-01	31.90	1	2949
1101	09:45:03	2024-11-01	31.90	2	2950
1101	09:45:01	2024-11-01	31.90	1	2951
1101	09:45:01	2024-11-01	31.90	1	2952
1101	09:45:01	2024-11-01	31.90	6	2953
1101	09:44:41	2024-11-01	31.90	1	2954
1101	09:44:35	2024-11-01	31.90	1	2955
1101	09:44:35	2024-11-01	31.90	1	2956
1101	09:44:35	2024-11-01	31.90	5	2957
1101	09:44:32	2024-11-01	31.90	2	2958
1101	09:44:28	2024-11-01	31.90	5	2959
1101	09:44:27	2024-11-01	31.90	1	2960
1101	09:44:27	2024-11-01	31.85	6	2961
1101	09:44:26	2024-11-01	31.90	1	2962
1101	09:44:26	2024-11-01	31.85	4	2963
1101	09:44:26	2024-11-01	31.85	2	2964
1101	09:44:26	2024-11-01	31.85	6	2965
1101	09:44:02	2024-11-01	31.90	1	2966
1101	09:44:02	2024-11-01	31.90	2	2967
1101	09:43:44	2024-11-01	31.90	1	2968
1101	09:43:43	2024-11-01	31.90	1	2969
1101	09:43:33	2024-11-01	31.90	1	2970
1101	09:43:30	2024-11-01	31.90	1	2971
1101	09:43:30	2024-11-01	31.90	1	2972
1101	09:43:30	2024-11-01	31.90	3	2973
1101	09:43:04	2024-11-01	31.90	1	2974
1101	09:43:02	2024-11-01	31.85	1	2975
1101	09:43:01	2024-11-01	31.85	2	2976
1101	09:42:58	2024-11-01	31.90	1	2977
1101	09:42:53	2024-11-01	31.90	4	2978
1101	09:42:53	2024-11-01	31.90	1	2979
1101	09:42:53	2024-11-01	31.90	1	2980
1101	09:42:53	2024-11-01	31.90	5	2981
1101	09:42:47	2024-11-01	31.90	1	2982
1101	09:42:40	2024-11-01	31.90	1	2983
1101	09:42:40	2024-11-01	31.90	5	2984
1101	09:42:29	2024-11-01	31.90	1	2985
1101	09:42:29	2024-11-01	31.90	5	2986
1101	09:42:29	2024-11-01	31.90	1	2987
1101	09:42:24	2024-11-01	31.85	3	2988
1101	09:42:24	2024-11-01	31.90	2	2989
1101	09:42:24	2024-11-01	31.90	14	2990
1101	09:42:24	2024-11-01	31.90	4	2991
1101	09:42:24	2024-11-01	31.90	1	2992
1101	09:42:23	2024-11-01	31.90	8	2993
1101	09:42:22	2024-11-01	31.90	5	2994
1101	09:42:19	2024-11-01	31.90	6	2995
1101	09:42:18	2024-11-01	31.90	2	2996
1101	09:42:03	2024-11-01	31.85	5	2997
1101	09:41:59	2024-11-01	31.85	9	2998
1101	09:41:53	2024-11-01	31.90	19	2999
1101	09:41:52	2024-11-01	31.85	1	3000
1101	09:41:52	2024-11-01	31.85	1	3001
1101	09:41:51	2024-11-01	31.85	8	3002
1101	09:41:51	2024-11-01	31.85	39	3003
1101	09:41:51	2024-11-01	31.85	15	3004
1101	09:41:51	2024-11-01	31.80	2	3005
1101	09:41:51	2024-11-01	31.85	50	3006
1101	09:41:51	2024-11-01	31.85	2	3007
1101	09:41:51	2024-11-01	31.85	50	3008
1101	09:41:51	2024-11-01	31.85	5	3009
1101	09:41:43	2024-11-01	31.85	41	3010
1101	09:41:16	2024-11-01	31.85	2	3011
1101	09:41:12	2024-11-01	31.85	1	3012
1101	09:41:12	2024-11-01	31.85	5	3013
1101	09:41:06	2024-11-01	31.85	2	3014
1101	09:40:29	2024-11-01	31.85	1	3015
1101	09:40:19	2024-11-01	31.85	1	3016
1101	09:40:19	2024-11-01	31.85	2	3017
1101	09:39:32	2024-11-01	31.85	1	3018
1101	09:39:26	2024-11-01	31.85	2	3019
1101	09:39:17	2024-11-01	31.85	3	3020
1101	09:39:16	2024-11-01	31.85	23	3021
1101	09:39:12	2024-11-01	31.80	1	3022
1101	09:39:11	2024-11-01	31.80	1	3023
1101	09:39:11	2024-11-01	31.80	2	3024
1101	09:39:11	2024-11-01	31.85	1	3025
1101	09:39:09	2024-11-01	31.80	3	3026
1101	09:38:55	2024-11-01	31.80	2	3027
1101	09:38:53	2024-11-01	31.85	4	3028
1101	09:38:51	2024-11-01	31.80	3	3029
1101	09:38:47	2024-11-01	31.85	1	3030
1101	09:38:38	2024-11-01	31.85	2	3031
1101	09:38:38	2024-11-01	31.85	3	3032
1101	09:38:32	2024-11-01	31.85	8	3033
1101	09:38:26	2024-11-01	31.85	4	3034
1101	09:38:23	2024-11-01	31.85	1	3035
1101	09:38:20	2024-11-01	31.85	21	3036
1101	09:38:18	2024-11-01	31.85	2	3037
1101	09:38:16	2024-11-01	31.80	1	3038
1101	09:38:13	2024-11-01	31.80	1	3039
1101	09:38:05	2024-11-01	31.85	5	3040
1101	09:38:04	2024-11-01	31.85	8	3041
1101	09:38:02	2024-11-01	31.85	1	3042
1101	09:38:00	2024-11-01	31.85	9	3043
1101	09:37:11	2024-11-01	31.80	1	3044
1101	09:37:07	2024-11-01	31.80	2	3045
1101	09:37:04	2024-11-01	31.85	10	3046
1101	09:36:49	2024-11-01	31.85	1	3047
1101	09:36:22	2024-11-01	31.80	5	3048
1101	09:36:21	2024-11-01	31.80	10	3049
1101	09:35:53	2024-11-01	31.85	5	3050
1101	09:35:53	2024-11-01	31.85	5	3051
1101	09:35:37	2024-11-01	31.80	16	3052
1101	09:35:34	2024-11-01	31.80	30	3053
1101	09:35:33	2024-11-01	31.85	1	3054
1101	09:35:31	2024-11-01	31.85	10	3055
1101	09:34:59	2024-11-01	31.85	5	3056
1101	09:34:52	2024-11-01	31.85	1	3057
1101	09:34:47	2024-11-01	31.85	1	3058
1101	09:34:37	2024-11-01	31.85	1	3059
1101	09:34:34	2024-11-01	31.85	1	3060
1101	09:34:21	2024-11-01	31.85	6	3061
1101	09:34:20	2024-11-01	31.80	27	3062
1101	09:34:19	2024-11-01	31.80	50	3063
1101	09:33:56	2024-11-01	31.80	1	3064
1101	09:33:53	2024-11-01	31.80	2	3065
1101	09:33:52	2024-11-01	31.80	1	3066
1101	09:33:50	2024-11-01	31.80	1	3067
1101	09:33:46	2024-11-01	31.85	21	3068
1101	09:33:22	2024-11-01	31.85	1	3069
1101	09:33:00	2024-11-01	31.80	1	3070
1101	09:32:56	2024-11-01	31.80	1	3071
1101	09:32:33	2024-11-01	31.85	2	3072
1101	09:32:33	2024-11-01	31.85	10	3073
1101	09:32:28	2024-11-01	31.80	1	3074
1101	09:32:27	2024-11-01	31.80	1	3075
1101	09:32:18	2024-11-01	31.85	5	3076
1101	09:32:13	2024-11-01	31.80	1	3077
1101	09:32:12	2024-11-01	31.80	1	3078
1101	09:32:05	2024-11-01	31.80	1	3079
1101	09:32:02	2024-11-01	31.80	1	3080
1101	09:31:59	2024-11-01	31.85	10	3081
1101	09:31:40	2024-11-01	31.85	1	3082
1101	09:31:12	2024-11-01	31.85	1	3083
1101	09:31:11	2024-11-01	31.90	5	3084
1101	09:31:10	2024-11-01	31.85	9	3085
1101	09:31:10	2024-11-01	31.85	1	3086
1101	09:31:10	2024-11-01	31.85	4	3087
1101	09:31:08	2024-11-01	31.85	84	3088
1101	09:31:03	2024-11-01	31.80	15	3089
1101	09:31:03	2024-11-01	31.80	1	3090
1101	09:31:02	2024-11-01	31.85	4	3091
1101	09:31:02	2024-11-01	31.80	30	3092
1101	09:31:02	2024-11-01	31.85	9	3093
1101	09:31:02	2024-11-01	31.85	1	3094
1101	09:31:02	2024-11-01	31.80	75	3095
1101	09:31:02	2024-11-01	31.85	153	3096
1101	09:31:02	2024-11-01	31.85	15	3097
1101	09:30:49	2024-11-01	31.85	2	3098
1101	09:30:48	2024-11-01	31.85	3	3099
1101	09:30:40	2024-11-01	31.90	1	3100
1101	09:30:37	2024-11-01	31.85	4	3101
1101	09:30:37	2024-11-01	31.90	5	3102
1101	09:30:36	2024-11-01	31.85	18	3103
1101	09:30:35	2024-11-01	31.90	1	3104
1101	09:30:24	2024-11-01	31.85	2	3105
1101	09:30:24	2024-11-01	31.85	1	3106
1101	09:30:21	2024-11-01	31.85	2	3107
1101	09:30:21	2024-11-01	31.85	4	3108
1101	09:30:16	2024-11-01	31.90	1	3109
1101	09:30:15	2024-11-01	31.85	8	3110
1101	09:30:13	2024-11-01	31.90	27	3111
1101	09:30:02	2024-11-01	31.85	1	3112
1101	09:30:01	2024-11-01	31.90	1	3113
1101	09:29:58	2024-11-01	31.85	2	3114
1101	09:29:55	2024-11-01	31.85	3	3115
1101	09:29:54	2024-11-01	31.85	1	3116
1101	09:29:53	2024-11-01	31.85	1	3117
1101	09:29:26	2024-11-01	31.85	5	3118
1101	09:29:23	2024-11-01	31.85	10	3119
1101	09:29:21	2024-11-01	31.90	1	3120
1101	09:28:59	2024-11-01	31.85	3	3121
1101	09:28:59	2024-11-01	31.85	5	3122
1101	09:28:55	2024-11-01	31.90	11	3123
1101	09:28:55	2024-11-01	31.90	30	3124
1101	09:28:25	2024-11-01	31.85	2	3125
1101	09:28:22	2024-11-01	31.85	3	3126
1101	09:28:09	2024-11-01	31.90	2	3127
1101	09:28:01	2024-11-01	31.85	2	3128
1101	09:27:58	2024-11-01	31.85	3	3129
1101	09:27:57	2024-11-01	31.90	1	3130
1101	09:27:21	2024-11-01	31.90	1	3131
1101	09:27:20	2024-11-01	31.85	4	3132
1101	09:27:19	2024-11-01	31.85	4	3133
1101	09:27:17	2024-11-01	31.85	3	3134
1101	09:27:13	2024-11-01	31.90	1	3135
1101	09:26:38	2024-11-01	31.85	1	3136
1101	09:26:38	2024-11-01	31.85	1	3137
1101	09:26:35	2024-11-01	31.85	1	3138
1101	09:26:32	2024-11-01	31.90	1	3139
1101	09:26:31	2024-11-01	31.85	1	3140
1101	09:26:30	2024-11-01	31.85	1	3141
1101	09:26:23	2024-11-01	31.85	1	3142
1101	09:26:22	2024-11-01	31.85	1	3143
1101	09:26:21	2024-11-01	31.90	1	3144
1101	09:26:01	2024-11-01	31.85	1	3145
1101	09:26:00	2024-11-01	31.85	1	3146
1101	09:25:23	2024-11-01	31.85	9	3147
1101	09:25:23	2024-11-01	31.85	8	3148
1101	09:25:22	2024-11-01	31.85	4	3149
1101	09:25:22	2024-11-01	31.85	2	3150
1101	09:25:22	2024-11-01	31.85	2	3151
1101	09:25:20	2024-11-01	31.85	1	3152
1101	09:25:17	2024-11-01	31.85	2	3153
1101	09:25:15	2024-11-01	31.85	3	3154
1101	09:24:55	2024-11-01	31.85	1	3155
1101	09:24:51	2024-11-01	31.90	1	3156
1101	09:24:51	2024-11-01	31.90	3	3157
1101	09:24:51	2024-11-01	31.85	2	3158
1101	09:24:48	2024-11-01	31.85	4	3159
1101	09:24:19	2024-11-01	31.85	1	3160
1101	09:24:17	2024-11-01	31.85	1	3161
1101	09:24:13	2024-11-01	31.85	2	3162
1101	09:24:13	2024-11-01	31.85	4	3163
1101	09:24:07	2024-11-01	31.85	1	3164
1101	09:24:04	2024-11-01	31.85	1	3165
1101	09:24:02	2024-11-01	31.85	1	3166
1101	09:23:57	2024-11-01	31.85	1	3167
1101	09:23:57	2024-11-01	31.85	1	3168
1101	09:23:54	2024-11-01	31.85	1	3169
1101	09:23:53	2024-11-01	31.85	2	3170
1101	09:23:40	2024-11-01	31.85	1	3171
1101	09:23:37	2024-11-01	31.85	1	3172
1101	09:23:33	2024-11-01	31.85	1	3173
1101	09:23:33	2024-11-01	31.85	1	3174
1101	09:23:31	2024-11-01	31.90	1	3175
1101	09:22:50	2024-11-01	31.85	1	3176
1101	09:22:46	2024-11-01	31.85	2	3177
1101	09:22:43	2024-11-01	31.90	3	3178
1101	09:22:08	2024-11-01	31.85	1	3179
1101	09:22:08	2024-11-01	31.85	3	3180
1101	09:22:03	2024-11-01	31.85	1	3181
1101	09:22:00	2024-11-01	31.85	1	3182
1101	09:21:48	2024-11-01	31.85	2	3183
1101	09:21:45	2024-11-01	31.90	5	3184
1101	09:21:10	2024-11-01	31.85	18	3185
1101	09:21:10	2024-11-01	31.85	1	3186
1101	09:21:10	2024-11-01	31.85	14	3187
1101	09:21:10	2024-11-01	31.85	3	3188
1101	09:21:10	2024-11-01	31.85	1	3189
1101	09:21:10	2024-11-01	31.85	1	3190
1101	09:21:10	2024-11-01	31.85	49	3191
1101	09:21:10	2024-11-01	31.85	24	3192
1101	09:21:10	2024-11-01	31.85	31	3193
1101	09:21:10	2024-11-01	31.85	4	3194
1101	09:21:10	2024-11-01	31.85	21	3195
1101	09:21:05	2024-11-01	31.80	3	3196
1101	09:21:03	2024-11-01	31.80	5	3197
1101	09:19:59	2024-11-01	31.85	5	3198
1101	09:19:57	2024-11-01	31.80	3	3199
1101	09:19:54	2024-11-01	31.80	5	3200
1101	09:19:51	2024-11-01	31.80	10	3201
1101	09:19:50	2024-11-01	31.85	17	3202
1101	09:19:47	2024-11-01	31.85	5	3203
1101	09:19:46	2024-11-01	31.80	3	3204
1101	09:19:46	2024-11-01	31.85	1	3205
1101	09:19:45	2024-11-01	31.85	1	3206
1101	09:19:45	2024-11-01	31.85	1	3207
1101	09:19:45	2024-11-01	31.85	21	3208
1101	09:19:45	2024-11-01	31.85	20	3209
1101	09:19:45	2024-11-01	31.85	1	3210
1101	09:19:44	2024-11-01	31.85	1	3211
1101	09:19:43	2024-11-01	31.85	1	3212
1101	09:19:43	2024-11-01	31.80	1	3213
1101	09:19:43	2024-11-01	31.85	1	3214
1101	09:19:42	2024-11-01	31.85	1	3215
1101	09:19:39	2024-11-01	31.80	1	3216
1101	09:19:36	2024-11-01	31.80	1	3217
1101	09:19:27	2024-11-01	31.80	1	3218
1101	09:19:24	2024-11-01	31.80	1	3219
1101	09:19:23	2024-11-01	31.80	1	3220
1101	09:18:34	2024-11-01	31.80	3	3221
1101	09:18:31	2024-11-01	31.80	6	3222
1101	09:18:19	2024-11-01	31.80	1	3223
1101	09:18:17	2024-11-01	31.80	1	3224
1101	09:18:05	2024-11-01	31.80	1	3225
1101	09:17:56	2024-11-01	31.80	1	3226
1101	09:17:54	2024-11-01	31.80	1	3227
1101	09:17:46	2024-11-01	31.80	6	3228
1101	09:17:46	2024-11-01	31.80	11	3229
1101	09:17:45	2024-11-01	31.80	27	3230
1101	09:17:44	2024-11-01	31.85	81	3231
1101	09:17:39	2024-11-01	31.85	1	3232
1101	09:17:37	2024-11-01	31.80	1	3233
1101	09:17:32	2024-11-01	31.80	1	3234
1101	09:17:17	2024-11-01	31.80	1	3235
1101	09:16:56	2024-11-01	31.80	2	3236
1101	09:16:54	2024-11-01	31.80	1	3237
1101	09:16:54	2024-11-01	31.80	3	3238
1101	09:16:46	2024-11-01	31.80	1	3239
1101	09:16:45	2024-11-01	31.80	1	3240
1101	09:16:40	2024-11-01	31.80	4	3241
1101	09:16:39	2024-11-01	31.80	12	3242
1101	09:16:37	2024-11-01	31.80	22	3243
1101	09:16:31	2024-11-01	31.85	8	3244
1101	09:16:14	2024-11-01	31.80	3	3245
1101	09:16:13	2024-11-01	31.80	1	3246
1101	09:16:12	2024-11-01	31.80	5	3247
1101	09:16:07	2024-11-01	31.80	1	3248
1101	09:16:06	2024-11-01	31.80	1	3249
1101	09:15:53	2024-11-01	31.80	1	3250
1101	09:15:53	2024-11-01	31.80	2	3251
1101	09:15:46	2024-11-01	31.80	1	3252
1101	09:15:42	2024-11-01	31.80	1	3253
1101	09:15:40	2024-11-01	31.80	1	3254
1101	09:15:33	2024-11-01	31.80	1	3255
1101	09:15:33	2024-11-01	31.80	2	3256
1101	09:15:33	2024-11-01	31.80	9	3257
1101	09:15:32	2024-11-01	31.80	6	3258
1101	09:15:32	2024-11-01	31.80	3	3259
1101	09:15:32	2024-11-01	31.80	7	3260
1101	09:15:30	2024-11-01	31.80	3	3261
1101	09:15:29	2024-11-01	31.80	6	3262
1101	09:15:22	2024-11-01	31.80	1	3263
1101	09:15:19	2024-11-01	31.80	3	3264
1101	09:15:13	2024-11-01	31.80	2	3265
1101	09:15:11	2024-11-01	31.80	3	3266
1101	09:15:05	2024-11-01	31.80	1	3267
1101	09:15:01	2024-11-01	31.80	1	3268
1101	09:14:36	2024-11-01	31.85	1	3269
1101	09:14:23	2024-11-01	31.80	1	3270
1101	09:14:20	2024-11-01	31.80	2	3271
1101	09:14:15	2024-11-01	31.80	2	3272
1101	09:14:13	2024-11-01	31.80	4	3273
1101	09:14:11	2024-11-01	31.80	3	3274
1101	09:14:09	2024-11-01	31.80	5	3275
1101	09:14:03	2024-11-01	31.80	11	3276
1101	09:13:59	2024-11-01	31.80	1	3277
1101	09:13:59	2024-11-01	31.80	20	3278
1101	09:13:57	2024-11-01	31.80	3	3279
1101	09:13:57	2024-11-01	31.80	13	3280
1101	09:13:57	2024-11-01	31.85	30	3281
1101	09:13:36	2024-11-01	31.85	2	3282
1101	09:13:36	2024-11-01	31.85	11	3283
1101	09:13:36	2024-11-01	31.85	19	3284
1101	09:13:36	2024-11-01	31.85	30	3285
1101	09:13:36	2024-11-01	31.85	51	3286
1101	09:13:34	2024-11-01	31.85	3	3287
1101	09:13:32	2024-11-01	31.85	1	3288
1101	09:13:31	2024-11-01	31.85	3	3289
1101	09:13:26	2024-11-01	31.85	3	3290
1101	09:13:26	2024-11-01	31.85	1	3291
1101	09:13:17	2024-11-01	31.85	3	3292
1101	09:13:06	2024-11-01	31.85	1	3293
1101	09:12:47	2024-11-01	31.85	1	3294
1101	09:12:09	2024-11-01	31.85	1	3295
1101	09:11:40	2024-11-01	31.85	1	3296
1101	09:11:31	2024-11-01	31.85	1	3297
1101	09:11:19	2024-11-01	31.85	4	3298
1101	09:11:18	2024-11-01	31.85	1	3299
1101	09:10:53	2024-11-01	31.85	1	3300
1101	09:10:51	2024-11-01	31.85	1	3301
1101	09:10:40	2024-11-01	31.85	9	3302
1101	09:10:40	2024-11-01	31.85	7	3303
1101	09:10:39	2024-11-01	31.85	13	3304
1101	09:10:33	2024-11-01	31.85	5	3305
1101	09:10:16	2024-11-01	31.85	1	3306
1101	09:10:15	2024-11-01	31.85	1	3307
1101	09:10:05	2024-11-01	31.85	1	3308
1101	09:10:03	2024-11-01	31.85	6	3309
1101	09:10:02	2024-11-01	31.85	4	3310
1101	09:09:52	2024-11-01	31.85	2	3311
1101	09:09:43	2024-11-01	31.85	3	3312
1101	09:09:37	2024-11-01	31.85	1	3313
1101	09:09:35	2024-11-01	31.85	3	3314
1101	09:09:28	2024-11-01	31.85	1	3315
1101	09:09:17	2024-11-01	31.85	3	3316
1101	09:09:15	2024-11-01	31.85	1	3317
1101	09:09:14	2024-11-01	31.85	6	3318
1101	09:09:13	2024-11-01	31.85	13	3319
1101	09:09:10	2024-11-01	31.85	5	3320
1101	09:09:06	2024-11-01	31.85	5	3321
1101	09:09:04	2024-11-01	31.85	1	3322
1101	09:08:59	2024-11-01	31.85	1	3323
1101	09:08:57	2024-11-01	31.90	2	3324
1101	09:08:57	2024-11-01	31.90	9	3325
1101	09:08:57	2024-11-01	31.90	6	3326
1101	09:08:57	2024-11-01	31.90	11	3327
1101	09:08:30	2024-11-01	31.90	4	3328
1101	09:08:30	2024-11-01	31.85	7	3329
1101	09:08:30	2024-11-01	31.85	1	3330
1101	09:08:30	2024-11-01	31.85	13	3331
1101	09:08:27	2024-11-01	31.85	7	3332
1101	09:08:19	2024-11-01	31.85	5	3333
1101	09:08:18	2024-11-01	31.90	1	3334
1101	09:08:18	2024-11-01	31.90	1	3335
1101	09:08:18	2024-11-01	31.85	1	3336
1101	09:08:15	2024-11-01	31.90	9	3337
1101	09:07:53	2024-11-01	31.80	3	3338
1101	09:07:47	2024-11-01	31.80	6	3339
1101	09:07:45	2024-11-01	31.90	6	3340
1101	09:07:45	2024-11-01	31.90	1	3341
1101	09:07:43	2024-11-01	31.85	5	3342
1101	09:07:43	2024-11-01	31.85	2	3343
1101	09:07:43	2024-11-01	31.85	1	3344
1101	09:07:42	2024-11-01	31.85	1	3345
1101	09:07:41	2024-11-01	31.80	1	3346
1101	09:07:35	2024-11-01	31.80	2	3347
1101	09:07:34	2024-11-01	31.80	8	3348
1101	09:07:33	2024-11-01	31.80	23	3349
1101	09:07:29	2024-11-01	31.80	13	3350
1101	09:07:26	2024-11-01	31.85	2	3351
1101	09:07:26	2024-11-01	31.85	2	3352
1101	09:07:26	2024-11-01	31.85	50	3353
1101	09:07:24	2024-11-01	31.85	3	3354
1101	09:07:24	2024-11-01	31.85	1	3355
1101	09:07:24	2024-11-01	31.85	19	3356
1101	09:07:24	2024-11-01	31.85	7	3357
1101	09:07:24	2024-11-01	31.80	235	3358
1101	09:07:24	2024-11-01	31.75	1	3359
1101	09:07:20	2024-11-01	31.80	1	3360
1101	09:07:13	2024-11-01	31.75	1	3361
1101	09:07:11	2024-11-01	31.75	1	3362
1101	09:07:10	2024-11-01	31.75	4	3363
1101	09:07:05	2024-11-01	31.75	2	3364
1101	09:07:05	2024-11-01	31.75	1	3365
1101	09:06:52	2024-11-01	31.80	1	3366
1101	09:06:52	2024-11-01	31.80	10	3367
1101	09:06:49	2024-11-01	31.75	6	3368
1101	09:06:48	2024-11-01	31.75	5	3369
1101	09:06:39	2024-11-01	31.80	2	3370
1101	09:06:36	2024-11-01	31.75	2	3371
1101	09:06:33	2024-11-01	31.80	1	3372
1101	09:06:23	2024-11-01	31.80	1	3373
1101	09:05:46	2024-11-01	31.75	1	3374
1101	09:05:27	2024-11-01	31.80	2	3375
1101	09:05:26	2024-11-01	31.75	3	3376
1101	09:05:09	2024-11-01	31.75	2	3377
1101	09:05:04	2024-11-01	31.75	2	3378
1101	09:04:49	2024-11-01	31.75	10	3379
1101	09:04:28	2024-11-01	31.75	7	3380
1101	09:04:17	2024-11-01	31.80	24	3381
1101	09:04:17	2024-11-01	31.80	49	3382
1101	09:04:04	2024-11-01	31.75	1	3383
1101	09:03:59	2024-11-01	31.75	18	3384
1101	09:03:52	2024-11-01	31.75	1	3385
1101	09:03:45	2024-11-01	31.75	9	3386
1101	09:03:45	2024-11-01	31.75	8	3387
1101	09:03:29	2024-11-01	31.70	6	3388
1101	09:03:27	2024-11-01	31.70	6	3389
1101	09:03:27	2024-11-01	31.75	96	3390
1101	09:03:25	2024-11-01	31.70	3	3391
1101	09:03:24	2024-11-01	31.70	1	3392
1101	09:03:24	2024-11-01	31.70	3	3393
1101	09:03:23	2024-11-01	31.75	9	3394
1101	09:03:23	2024-11-01	31.75	36	3395
1101	09:03:23	2024-11-01	31.75	2	3396
1101	09:03:19	2024-11-01	31.80	15	3397
1101	09:03:18	2024-11-01	31.80	1	3398
1101	09:03:11	2024-11-01	31.80	1	3399
1101	09:03:11	2024-11-01	31.85	1	3400
1101	09:03:07	2024-11-01	31.80	8	3401
1101	09:03:05	2024-11-01	31.85	1	3402
1101	09:03:05	2024-11-01	31.85	2	3403
1101	09:03:05	2024-11-01	31.85	28	3404
1101	09:03:02	2024-11-01	31.80	29	3405
1101	09:03:02	2024-11-01	31.90	1	3406
1101	09:03:01	2024-11-01	31.80	1	3407
1101	09:03:01	2024-11-01	31.85	25	3408
1101	09:03:00	2024-11-01	31.85	1	3409
1101	09:03:00	2024-11-01	31.85	1	3410
1101	09:03:00	2024-11-01	31.85	1	3411
1101	09:03:00	2024-11-01	31.85	2	3412
1101	09:03:00	2024-11-01	31.85	7	3413
1101	09:03:00	2024-11-01	31.90	14	3414
1101	09:03:00	2024-11-01	31.90	20	3415
1101	09:03:00	2024-11-01	31.90	45	3416
1101	09:02:59	2024-11-01	31.90	7	3417
1101	09:02:58	2024-11-01	31.90	20	3418
1101	09:02:55	2024-11-01	31.90	1	3419
1101	09:02:55	2024-11-01	31.85	33	3420
1101	09:02:55	2024-11-01	31.85	15	3421
1101	09:02:55	2024-11-01	31.90	11	3422
1101	09:02:55	2024-11-01	31.85	133	3423
1101	09:02:55	2024-11-01	31.85	1	3424
1101	09:02:43	2024-11-01	31.80	6	3425
1101	09:02:41	2024-11-01	31.80	1	3426
1101	09:02:35	2024-11-01	31.80	17	3427
1101	09:02:34	2024-11-01	31.75	20	3428
1101	09:02:33	2024-11-01	31.80	1	3429
1101	09:02:32	2024-11-01	31.75	2	3430
1101	09:02:32	2024-11-01	31.75	56	3431
1101	09:02:29	2024-11-01	31.75	2	3432
1101	09:02:27	2024-11-01	31.75	1	3433
1101	09:02:21	2024-11-01	31.75	5	3434
1101	09:02:18	2024-11-01	31.70	2	3435
1101	09:02:17	2024-11-01	31.75	2	3436
1101	09:02:16	2024-11-01	31.75	1	3437
1101	09:02:15	2024-11-01	31.70	1	3438
1101	09:02:13	2024-11-01	31.75	1	3439
1101	09:02:06	2024-11-01	31.70	1	3440
1101	09:02:04	2024-11-01	31.75	2	3441
1101	09:01:53	2024-11-01	31.75	1	3442
1101	09:01:38	2024-11-01	31.75	34	3443
1101	09:01:37	2024-11-01	31.75	10	3444
1101	09:01:26	2024-11-01	31.75	21	3445
1101	09:01:26	2024-11-01	31.75	1	3446
1101	09:01:26	2024-11-01	31.75	4	3447
1101	09:01:21	2024-11-01	31.75	1	3448
1101	09:00:59	2024-11-01	31.75	13	3449
1101	09:00:58	2024-11-01	31.75	30	3450
1101	09:00:26	2024-11-01	31.75	2	3451
1101	09:00:15	2024-11-01	31.75	28	3452
1101	09:00:12	2024-11-01	31.75	29	3453
1101	09:00:08	2024-11-01	31.75	1	3454
1101	09:00:08	2024-11-01	31.70	8	3455
1101	09:00:08	2024-11-01	31.70	14	3456
1101	09:00:07	2024-11-01	31.70	8	3457
1101	09:00:07	2024-11-01	31.70	2	3458
1101	09:00:07	2024-11-01	31.70	23	3459
1101	09:00:07	2024-11-01	31.70	8	3460
1101	09:00:07	2024-11-01	31.75	1	3461
1101	09:00:07	2024-11-01	31.75	1	3462
1101	09:00:06	2024-11-01	31.70	30	3463
1101	09:00:06	2024-11-01	31.70	18	3464
1101	09:00:06	2024-11-01	31.70	15	3465
1101	09:00:06	2024-11-01	31.75	1	3466
1101	09:00:06	2024-11-01	31.75	1	3467
1101	09:00:06	2024-11-01	31.70	49	3468
1101	09:00:06	2024-11-01	31.75	9	3469
1101	09:00:06	2024-11-01	31.80	1	3470
1101	09:00:06	2024-11-01	31.75	1	3471
1101	09:00:06	2024-11-01	31.75	1	3472
1101	09:00:06	2024-11-01	31.75	1	3473
1101	09:00:06	2024-11-01	31.75	1	3474
1101	09:00:06	2024-11-01	31.70	552	3475
1102	13:30:00	2024-11-01	46.30	988	1
1102	13:24:52	2024-11-01	46.35	1	2
1102	13:24:34	2024-11-01	46.25	3	3
1102	13:24:34	2024-11-01	46.25	1	4
1102	13:24:24	2024-11-01	46.25	1	5
1102	13:24:23	2024-11-01	46.25	1	6
1102	13:24:23	2024-11-01	46.35	3	7
1102	13:24:23	2024-11-01	46.25	2	8
1102	13:24:22	2024-11-01	46.25	2	9
1102	13:24:21	2024-11-01	46.35	2	10
1102	13:24:20	2024-11-01	46.25	1	11
1102	13:24:18	2024-11-01	46.25	4	12
1102	13:24:17	2024-11-01	46.45	1	13
1102	13:24:17	2024-11-01	46.25	3	14
1102	13:24:17	2024-11-01	46.45	2	15
1102	13:24:17	2024-11-01	46.35	1	16
1102	13:24:16	2024-11-01	46.30	2	17
1102	13:24:16	2024-11-01	46.25	2	18
1102	13:24:15	2024-11-01	46.25	1	19
1102	13:24:15	2024-11-01	46.25	1	20
1102	13:24:15	2024-11-01	46.25	1	21
1102	13:24:15	2024-11-01	46.30	8	22
1102	13:24:15	2024-11-01	46.30	8	23
1102	13:24:15	2024-11-01	46.30	3	24
1102	13:24:13	2024-11-01	46.25	1	25
1102	13:24:12	2024-11-01	46.25	2	26
1102	13:24:11	2024-11-01	46.25	1	27
1102	13:24:01	2024-11-01	46.30	2	28
1102	13:23:52	2024-11-01	46.30	1	29
1102	13:23:51	2024-11-01	46.25	1	30
1102	13:23:48	2024-11-01	46.25	1	31
1102	13:23:35	2024-11-01	46.25	1	32
1102	13:23:29	2024-11-01	46.30	1	33
1102	13:23:29	2024-11-01	46.30	1	34
1102	13:23:25	2024-11-01	46.30	1	35
1102	13:23:16	2024-11-01	46.30	2	36
1102	13:23:14	2024-11-01	46.30	1	37
1102	13:23:12	2024-11-01	46.30	2	38
1102	13:23:12	2024-11-01	46.30	1	39
1102	13:23:06	2024-11-01	46.30	1	40
1102	13:23:05	2024-11-01	46.30	2	41
1102	13:22:58	2024-11-01	46.30	3	42
1102	13:22:50	2024-11-01	46.30	1	43
1102	13:22:40	2024-11-01	46.30	2	44
1102	13:22:37	2024-11-01	46.30	1	45
1102	13:22:36	2024-11-01	46.30	1	46
1102	13:22:29	2024-11-01	46.30	1	47
1102	13:22:28	2024-11-01	46.35	2	48
1102	13:22:27	2024-11-01	46.30	2	49
1102	13:22:25	2024-11-01	46.30	1	50
1102	13:22:17	2024-11-01	46.40	2	51
1102	13:22:17	2024-11-01	46.30	1	52
1102	13:22:17	2024-11-01	46.35	1	53
1102	13:22:15	2024-11-01	46.30	2	54
1102	13:22:15	2024-11-01	46.30	1	55
1102	13:22:15	2024-11-01	46.40	5	56
1102	13:22:15	2024-11-01	46.30	1	57
1102	13:22:15	2024-11-01	46.30	1	58
1102	13:22:14	2024-11-01	46.35	9	59
1102	13:22:14	2024-11-01	46.35	1	60
1102	13:22:09	2024-11-01	46.35	1	61
1102	13:22:04	2024-11-01	46.35	1	62
1102	13:21:51	2024-11-01	46.40	1	63
1102	13:21:49	2024-11-01	46.35	2	64
1102	13:21:38	2024-11-01	46.35	1	65
1102	13:21:22	2024-11-01	46.35	1	66
1102	13:21:20	2024-11-01	46.35	1	67
1102	13:21:17	2024-11-01	46.35	1	68
1102	13:20:50	2024-11-01	46.35	1	69
1102	13:20:47	2024-11-01	46.35	1	70
1102	13:20:32	2024-11-01	46.35	1	71
1102	13:20:30	2024-11-01	46.40	1	72
1102	13:20:28	2024-11-01	46.35	1	73
1102	13:20:23	2024-11-01	46.35	1	74
1102	13:20:20	2024-11-01	46.35	1	75
1102	13:20:20	2024-11-01	46.35	1	76
1102	13:20:14	2024-11-01	46.45	1	77
1102	13:20:03	2024-11-01	46.35	1	78
1102	13:19:26	2024-11-01	46.35	1	79
1102	13:19:16	2024-11-01	46.35	1	80
1102	13:18:59	2024-11-01	46.45	1	81
1102	13:18:59	2024-11-01	46.45	5	82
1102	13:18:29	2024-11-01	46.35	1	83
1102	13:18:28	2024-11-01	46.35	1	84
1102	13:18:08	2024-11-01	46.35	1	85
1102	13:18:06	2024-11-01	46.35	4	86
1102	13:17:54	2024-11-01	46.40	2	87
1102	13:17:53	2024-11-01	46.40	2	88
1102	13:17:52	2024-11-01	46.40	1	89
1102	13:17:51	2024-11-01	46.40	1	90
1102	13:17:51	2024-11-01	46.40	5	91
1102	13:17:47	2024-11-01	46.35	1	92
1102	13:17:41	2024-11-01	46.35	1	93
1102	13:17:39	2024-11-01	46.40	2	94
1102	13:17:39	2024-11-01	46.35	2	95
1102	13:17:32	2024-11-01	46.35	1	96
1102	13:17:08	2024-11-01	46.40	1	97
1102	13:17:02	2024-11-01	46.40	5	98
1102	13:16:56	2024-11-01	46.35	1	99
1102	13:16:55	2024-11-01	46.40	15	100
1102	13:16:54	2024-11-01	46.40	1	101
1102	13:16:35	2024-11-01	46.40	1	102
1102	13:16:06	2024-11-01	46.40	1	103
1102	13:15:38	2024-11-01	46.40	1	104
1102	13:15:19	2024-11-01	46.40	1	105
1102	13:14:57	2024-11-01	46.40	1	106
1102	13:14:56	2024-11-01	46.45	2	107
1102	13:14:56	2024-11-01	46.40	1	108
1102	13:14:56	2024-11-01	46.45	1	109
1102	13:14:55	2024-11-01	46.40	2	110
1102	13:14:54	2024-11-01	46.40	2	111
1102	13:14:52	2024-11-01	46.40	1	112
1102	13:14:52	2024-11-01	46.40	5	113
1102	13:14:51	2024-11-01	46.40	8	114
1102	13:14:41	2024-11-01	46.35	1	115
1102	13:14:32	2024-11-01	46.40	1	116
1102	13:14:31	2024-11-01	46.35	1	117
1102	13:13:44	2024-11-01	46.35	1	118
1102	13:13:44	2024-11-01	46.35	1	119
1102	13:13:14	2024-11-01	46.35	2	120
1102	13:13:13	2024-11-01	46.35	3	121
1102	13:13:10	2024-11-01	46.35	1	122
1102	13:12:57	2024-11-01	46.35	1	123
1102	13:12:47	2024-11-01	46.35	1	124
1102	13:12:33	2024-11-01	46.40	1	125
1102	13:12:09	2024-11-01	46.35	1	126
1102	13:12:00	2024-11-01	46.35	1	127
1102	13:11:50	2024-11-01	46.35	1	128
1102	13:11:49	2024-11-01	46.40	1	129
1102	13:11:22	2024-11-01	46.35	1	130
1102	13:11:13	2024-11-01	46.35	2	131
1102	13:10:57	2024-11-01	46.35	2	132
1102	13:10:53	2024-11-01	46.35	1	133
1102	13:10:35	2024-11-01	46.35	1	134
1102	13:09:56	2024-11-01	46.35	1	135
1102	13:09:48	2024-11-01	46.35	2	136
1102	13:09:47	2024-11-01	46.35	1	137
1102	13:09:00	2024-11-01	46.35	1	138
1102	13:08:59	2024-11-01	46.35	1	139
1102	13:08:12	2024-11-01	46.40	2	140
1102	13:08:12	2024-11-01	46.40	7	141
1102	13:08:12	2024-11-01	46.40	4	142
1102	13:08:12	2024-11-01	46.40	1	143
1102	13:08:02	2024-11-01	46.40	1	144
1102	13:08:00	2024-11-01	46.40	2	145
1102	13:07:50	2024-11-01	46.40	2	146
1102	13:07:25	2024-11-01	46.40	1	147
1102	13:07:05	2024-11-01	46.40	1	148
1102	13:06:38	2024-11-01	46.40	1	149
1102	13:06:21	2024-11-01	46.40	2	150
1102	13:06:08	2024-11-01	46.40	1	151
1102	13:05:58	2024-11-01	46.40	2	152
1102	13:05:50	2024-11-01	46.40	1	153
1102	13:05:37	2024-11-01	46.40	1	154
1102	13:05:11	2024-11-01	46.40	1	155
1102	13:05:03	2024-11-01	46.40	1	156
1102	13:04:56	2024-11-01	46.40	1	157
1102	13:04:48	2024-11-01	46.45	2	158
1102	13:04:46	2024-11-01	46.40	1	159
1102	13:04:30	2024-11-01	46.40	1	160
1102	13:04:21	2024-11-01	46.40	1	161
1102	13:04:20	2024-11-01	46.45	10	162
1102	13:04:19	2024-11-01	46.40	1	163
1102	13:04:16	2024-11-01	46.35	1	164
1102	13:04:14	2024-11-01	46.35	1	165
1102	13:03:57	2024-11-01	46.35	1	166
1102	13:03:49	2024-11-01	46.35	1	167
1102	13:03:30	2024-11-01	46.40	1	168
1102	13:03:28	2024-11-01	46.35	1	169
1102	13:03:17	2024-11-01	46.35	1	170
1102	13:03:14	2024-11-01	46.35	2	171
1102	13:03:12	2024-11-01	46.35	1	172
1102	13:03:11	2024-11-01	46.35	2	173
1102	13:02:41	2024-11-01	46.35	1	174
1102	13:02:20	2024-11-01	46.35	1	175
1102	13:01:53	2024-11-01	46.35	1	176
1102	13:01:23	2024-11-01	46.35	1	177
1102	13:01:06	2024-11-01	46.35	1	178
1102	13:00:53	2024-11-01	46.35	1	179
1102	13:00:26	2024-11-01	46.35	1	180
1102	13:00:21	2024-11-01	46.35	2	181
1102	13:00:19	2024-11-01	46.35	1	182
1102	13:00:18	2024-11-01	46.40	1	183
1102	13:00:18	2024-11-01	46.40	1	184
1102	13:00:11	2024-11-01	46.35	3	185
1102	13:00:08	2024-11-01	46.40	2	186
1102	13:00:08	2024-11-01	46.40	1	187
1102	13:00:07	2024-11-01	46.35	2	188
1102	12:59:53	2024-11-01	46.40	4	189
1102	12:59:52	2024-11-01	46.40	2	190
1102	12:59:52	2024-11-01	46.40	5	191
1102	12:59:51	2024-11-01	46.40	11	192
1102	12:59:31	2024-11-01	46.35	1	193
1102	12:59:29	2024-11-01	46.35	1	194
1102	12:59:14	2024-11-01	46.40	1	195
1102	12:58:49	2024-11-01	46.35	1	196
1102	12:58:44	2024-11-01	46.35	1	197
1102	12:58:32	2024-11-01	46.35	1	198
1102	12:58:09	2024-11-01	46.35	2	199
1102	12:57:57	2024-11-01	46.35	1	200
1102	12:57:49	2024-11-01	46.35	1	201
1102	12:57:35	2024-11-01	46.35	1	202
1102	12:57:09	2024-11-01	46.35	1	203
1102	12:56:42	2024-11-01	46.35	1	204
1102	12:56:39	2024-11-01	46.35	1	205
1102	12:56:38	2024-11-01	46.35	1	206
1102	12:56:22	2024-11-01	46.35	1	207
1102	12:56:16	2024-11-01	46.35	2	208
1102	12:55:41	2024-11-01	46.35	1	209
1102	12:55:39	2024-11-01	46.35	1	210
1102	12:55:35	2024-11-01	46.35	1	211
1102	12:54:47	2024-11-01	46.35	1	212
1102	12:54:44	2024-11-01	46.35	1	213
1102	12:54:30	2024-11-01	46.35	7	214
1102	12:54:00	2024-11-01	46.35	1	215
1102	12:53:47	2024-11-01	46.35	1	216
1102	12:53:44	2024-11-01	46.35	1	217
1102	12:53:34	2024-11-01	46.40	1	218
1102	12:53:29	2024-11-01	46.35	1	219
1102	12:53:12	2024-11-01	46.35	1	220
1102	12:53:07	2024-11-01	46.35	1	221
1102	12:52:50	2024-11-01	46.35	1	222
1102	12:52:33	2024-11-01	46.40	1	223
1102	12:52:25	2024-11-01	46.35	1	224
1102	12:51:53	2024-11-01	46.35	1	225
1102	12:51:42	2024-11-01	46.40	1	226
1102	12:51:38	2024-11-01	46.35	1	227
1102	12:50:56	2024-11-01	46.35	1	228
1102	12:50:50	2024-11-01	46.35	1	229
1102	12:50:19	2024-11-01	46.35	2	230
1102	12:50:03	2024-11-01	46.35	1	231
1102	12:49:59	2024-11-01	46.35	1	232
1102	12:49:16	2024-11-01	46.35	1	233
1102	12:49:03	2024-11-01	46.35	1	234
1102	12:48:28	2024-11-01	46.35	1	235
1102	12:48:15	2024-11-01	46.35	2	236
1102	12:48:11	2024-11-01	46.35	3	237
1102	12:48:07	2024-11-01	46.35	1	238
1102	12:48:02	2024-11-01	46.35	1	239
1102	12:47:41	2024-11-01	46.35	1	240
1102	12:47:11	2024-11-01	46.35	1	241
1102	12:46:55	2024-11-01	46.40	2	242
1102	12:46:53	2024-11-01	46.35	1	243
1102	12:46:50	2024-11-01	46.40	1	244
1102	12:46:29	2024-11-01	46.40	2	245
1102	12:46:15	2024-11-01	46.35	1	246
1102	12:46:06	2024-11-01	46.35	1	247
1102	12:45:24	2024-11-01	46.40	2	248
1102	12:45:22	2024-11-01	46.40	6	249
1102	12:45:20	2024-11-01	46.35	1	250
1102	12:45:19	2024-11-01	46.35	1	251
1102	12:45:19	2024-11-01	46.35	5	252
1102	12:45:19	2024-11-01	46.35	6	253
1102	12:45:19	2024-11-01	46.35	4	254
1102	12:45:19	2024-11-01	46.35	4	255
1102	12:45:19	2024-11-01	46.35	29	256
1102	12:45:19	2024-11-01	46.30	1	257
1102	12:44:49	2024-11-01	46.30	1	258
1102	12:44:31	2024-11-01	46.30	1	259
1102	12:44:23	2024-11-01	46.30	1	260
1102	12:44:17	2024-11-01	46.35	1	261
1102	12:43:46	2024-11-01	46.35	1	262
1102	12:43:44	2024-11-01	46.30	1	263
1102	12:43:30	2024-11-01	46.30	1	264
1102	12:43:27	2024-11-01	46.30	1	265
1102	12:43:09	2024-11-01	46.30	3	266
1102	12:42:57	2024-11-01	46.30	1	267
1102	12:42:40	2024-11-01	46.30	1	268
1102	12:42:31	2024-11-01	46.30	1	269
1102	12:42:16	2024-11-01	46.30	2	270
1102	12:42:09	2024-11-01	46.30	1	271
1102	12:41:35	2024-11-01	46.30	1	272
1102	12:41:22	2024-11-01	46.30	1	273
1102	12:41:20	2024-11-01	46.30	1	274
1102	12:40:40	2024-11-01	46.30	2	275
1102	12:40:39	2024-11-01	46.30	1	276
1102	12:40:30	2024-11-01	46.35	1	277
1102	12:40:20	2024-11-01	46.35	2	278
1102	12:40:06	2024-11-01	46.35	1	279
1102	12:39:50	2024-11-01	46.35	1	280
1102	12:39:50	2024-11-01	46.35	1	281
1102	12:39:49	2024-11-01	46.35	1	282
1102	12:39:43	2024-11-01	46.35	1	283
1102	12:39:43	2024-11-01	46.35	1	284
1102	12:39:11	2024-11-01	46.35	1	285
1102	12:39:10	2024-11-01	46.35	1	286
1102	12:39:02	2024-11-01	46.35	1	287
1102	12:38:47	2024-11-01	46.35	1	288
1102	12:38:44	2024-11-01	46.35	1	289
1102	12:38:20	2024-11-01	46.35	1	290
1102	12:38:16	2024-11-01	46.35	1	291
1102	12:38:15	2024-11-01	46.35	1	292
1102	12:38:13	2024-11-01	46.35	1	293
1102	12:37:54	2024-11-01	46.35	1	294
1102	12:37:51	2024-11-01	46.30	1	295
1102	12:37:28	2024-11-01	46.35	3	296
1102	12:37:28	2024-11-01	46.35	1	297
1102	12:37:17	2024-11-01	46.35	1	298
1102	12:37:10	2024-11-01	46.35	1	299
1102	12:37:10	2024-11-01	46.35	2	300
1102	12:37:00	2024-11-01	46.35	1	301
1102	12:36:55	2024-11-01	46.35	1	302
1102	12:36:40	2024-11-01	46.35	1	303
1102	12:36:22	2024-11-01	46.35	1	304
1102	12:36:15	2024-11-01	46.35	2	305
1102	12:36:15	2024-11-01	46.35	2	306
1102	12:36:10	2024-11-01	46.35	1	307
1102	12:35:59	2024-11-01	46.35	1	308
1102	12:35:53	2024-11-01	46.35	1	309
1102	12:35:06	2024-11-01	46.35	1	310
1102	12:35:06	2024-11-01	46.35	1	311
1102	12:35:04	2024-11-01	46.35	6	312
1102	12:35:04	2024-11-01	46.35	3	313
1102	12:35:03	2024-11-01	46.35	1	314
1102	12:35:03	2024-11-01	46.35	1	315
1102	12:35:03	2024-11-01	46.35	1	316
1102	12:34:50	2024-11-01	46.35	1	317
1102	12:34:50	2024-11-01	46.35	1	318
1102	12:34:21	2024-11-01	46.35	1	319
1102	12:34:18	2024-11-01	46.35	1	320
1102	12:34:17	2024-11-01	46.35	1	321
1102	12:34:15	2024-11-01	46.35	3	322
1102	12:34:15	2024-11-01	46.35	2	323
1102	12:34:09	2024-11-01	46.35	1	324
1102	12:34:07	2024-11-01	46.35	1	325
1102	12:34:07	2024-11-01	46.35	1	326
1102	12:34:05	2024-11-01	46.35	2	327
1102	12:34:03	2024-11-01	46.35	2	328
1102	12:34:01	2024-11-01	46.35	2	329
1102	12:34:00	2024-11-01	46.35	1	330
1102	12:33:59	2024-11-01	46.35	2	331
1102	12:33:38	2024-11-01	46.35	4	332
1102	12:33:31	2024-11-01	46.35	1	333
1102	12:33:11	2024-11-01	46.35	1	334
1102	12:33:09	2024-11-01	46.35	3	335
1102	12:32:44	2024-11-01	46.35	1	336
1102	12:32:35	2024-11-01	46.35	1	337
1102	12:32:15	2024-11-01	46.35	1	338
1102	12:31:56	2024-11-01	46.35	1	339
1102	12:31:50	2024-11-01	46.35	1	340
1102	12:31:40	2024-11-01	46.40	1	341
1102	12:31:35	2024-11-01	46.35	1	342
1102	12:31:19	2024-11-01	46.35	1	343
1102	12:31:09	2024-11-01	46.35	1	344
1102	12:30:39	2024-11-01	46.40	1	345
1102	12:30:25	2024-11-01	46.35	1	346
1102	12:30:23	2024-11-01	46.35	1	347
1102	12:30:22	2024-11-01	46.35	1	348
1102	12:29:47	2024-11-01	46.40	1	349
1102	12:29:40	2024-11-01	46.35	1	350
1102	12:29:34	2024-11-01	46.35	1	351
1102	12:29:27	2024-11-01	46.35	1	352
1102	12:29:20	2024-11-01	46.35	1	353
1102	12:28:51	2024-11-01	46.35	1	354
1102	12:28:47	2024-11-01	46.35	1	355
1102	12:28:43	2024-11-01	46.40	1	356
1102	12:28:31	2024-11-01	46.35	1	357
1102	12:28:25	2024-11-01	46.35	2	358
1102	12:28:15	2024-11-01	46.35	1	359
1102	12:28:00	2024-11-01	46.35	3	360
1102	12:28:00	2024-11-01	46.35	1	361
1102	12:27:35	2024-11-01	46.35	1	362
1102	12:27:33	2024-11-01	46.35	1	363
1102	12:27:18	2024-11-01	46.35	1	364
1102	12:27:13	2024-11-01	46.35	1	365
1102	12:27:12	2024-11-01	46.35	1	366
1102	12:26:43	2024-11-01	46.35	1	367
1102	12:26:39	2024-11-01	46.35	1	368
1102	12:26:25	2024-11-01	46.35	1	369
1102	12:25:43	2024-11-01	46.35	1	370
1102	12:25:43	2024-11-01	46.35	1	371
1102	12:25:38	2024-11-01	46.35	1	372
1102	12:25:28	2024-11-01	46.35	1	373
1102	12:25:23	2024-11-01	46.35	1	374
1102	12:25:03	2024-11-01	46.35	1	375
1102	12:24:50	2024-11-01	46.35	1	376
1102	12:24:47	2024-11-01	46.35	1	377
1102	12:24:38	2024-11-01	46.35	1	378
1102	12:24:13	2024-11-01	46.35	1	379
1102	12:24:03	2024-11-01	46.35	1	380
1102	12:23:53	2024-11-01	46.35	1	381
1102	12:23:51	2024-11-01	46.35	1	382
1102	12:23:28	2024-11-01	46.35	1	383
1102	12:23:16	2024-11-01	46.35	1	384
1102	12:23:03	2024-11-01	46.35	2	385
1102	12:22:55	2024-11-01	46.35	1	386
1102	12:22:55	2024-11-01	46.35	1	387
1102	12:22:34	2024-11-01	46.35	1	388
1102	12:22:28	2024-11-01	46.35	1	389
1102	12:22:25	2024-11-01	46.35	3	390
1102	12:21:59	2024-11-01	46.35	1	391
1102	12:21:44	2024-11-01	46.35	1	392
1102	12:21:41	2024-11-01	46.35	1	393
1102	12:21:03	2024-11-01	46.35	1	394
1102	12:20:54	2024-11-01	46.35	1	395
1102	12:20:35	2024-11-01	46.35	2	396
1102	12:20:24	2024-11-01	46.35	3	397
1102	12:20:24	2024-11-01	46.40	2	398
1102	12:20:24	2024-11-01	46.35	9	399
1102	12:20:24	2024-11-01	46.40	4	400
1102	12:20:24	2024-11-01	46.35	3	401
1102	12:20:24	2024-11-01	46.40	78	402
1102	12:20:07	2024-11-01	46.35	1	403
1102	12:20:06	2024-11-01	46.35	1	404
1102	12:19:19	2024-11-01	46.35	1	405
1102	12:19:11	2024-11-01	46.35	1	406
1102	12:18:47	2024-11-01	46.35	1	407
1102	12:18:32	2024-11-01	46.35	1	408
1102	12:18:15	2024-11-01	46.35	1	409
1102	12:18:08	2024-11-01	46.35	2	410
1102	12:18:08	2024-11-01	46.35	4	411
1102	12:18:08	2024-11-01	46.35	20	412
1102	12:18:08	2024-11-01	46.35	16	413
1102	12:18:06	2024-11-01	46.35	2	414
1102	12:17:44	2024-11-01	46.35	1	415
1102	12:17:19	2024-11-01	46.35	1	416
1102	12:16:57	2024-11-01	46.35	1	417
1102	12:16:26	2024-11-01	46.35	1	418
1102	12:16:23	2024-11-01	46.35	1	419
1102	12:16:10	2024-11-01	46.35	1	420
1102	12:16:09	2024-11-01	46.35	1	421
1102	12:15:27	2024-11-01	46.35	1	422
1102	12:15:22	2024-11-01	46.35	1	423
1102	12:14:35	2024-11-01	46.35	1	424
1102	12:14:31	2024-11-01	46.35	1	425
1102	12:13:55	2024-11-01	46.40	2	426
1102	12:13:48	2024-11-01	46.35	1	427
1102	12:13:35	2024-11-01	46.35	1	428
1102	12:13:09	2024-11-01	46.40	1	429
1102	12:13:07	2024-11-01	46.35	2	430
1102	12:13:06	2024-11-01	46.35	2	431
1102	12:12:58	2024-11-01	46.35	1	432
1102	12:12:39	2024-11-01	46.35	1	433
1102	12:11:54	2024-11-01	46.40	1	434
1102	12:11:47	2024-11-01	46.35	1	435
1102	12:11:43	2024-11-01	46.35	1	436
1102	12:10:47	2024-11-01	46.35	1	437
1102	12:10:38	2024-11-01	46.40	1	438
1102	12:09:51	2024-11-01	46.35	1	439
1102	12:09:34	2024-11-01	46.40	1	440
1102	12:09:27	2024-11-01	46.40	2	441
1102	12:08:55	2024-11-01	46.35	1	442
1102	12:08:26	2024-11-01	46.40	1	443
1102	12:08:04	2024-11-01	46.35	2	444
1102	12:08:00	2024-11-01	46.40	1	445
1102	12:07:59	2024-11-01	46.35	1	446
1102	12:07:49	2024-11-01	46.40	3	447
1102	12:07:35	2024-11-01	46.35	1	448
1102	12:07:14	2024-11-01	46.40	1	449
1102	12:07:03	2024-11-01	46.35	1	450
1102	12:07:01	2024-11-01	46.35	4	451
1102	12:06:56	2024-11-01	46.35	1	452
1102	12:06:34	2024-11-01	46.35	11	453
1102	12:06:27	2024-11-01	46.35	10	454
1102	12:06:27	2024-11-01	46.35	3	455
1102	12:06:07	2024-11-01	46.30	1	456
1102	12:06:01	2024-11-01	46.35	1	457
1102	12:05:11	2024-11-01	46.30	1	458
1102	12:04:56	2024-11-01	46.35	2	459
1102	12:04:53	2024-11-01	46.35	1	460
1102	12:04:18	2024-11-01	46.30	1	461
1102	12:04:15	2024-11-01	46.30	1	462
1102	12:03:44	2024-11-01	46.30	1	463
1102	12:03:38	2024-11-01	46.35	1	464
1102	12:03:19	2024-11-01	46.30	1	465
1102	12:03:06	2024-11-01	46.30	1	466
1102	12:02:33	2024-11-01	46.35	1	467
1102	12:02:23	2024-11-01	46.30	1	468
1102	12:01:27	2024-11-01	46.30	1	469
1102	12:01:26	2024-11-01	46.35	1	470
1102	12:00:31	2024-11-01	46.30	1	471
1102	12:00:27	2024-11-01	46.35	4	472
1102	12:00:25	2024-11-01	46.35	30	473
1102	12:00:23	2024-11-01	46.35	2	474
1102	12:00:17	2024-11-01	46.35	1	475
1102	11:59:54	2024-11-01	46.35	1	476
1102	11:59:35	2024-11-01	46.25	1	477
1102	11:59:35	2024-11-01	46.35	5	478
1102	11:59:30	2024-11-01	46.30	27	479
1102	11:58:58	2024-11-01	46.30	1	480
1102	11:58:39	2024-11-01	46.25	1	481
1102	11:58:12	2024-11-01	46.25	1	482
1102	11:57:43	2024-11-01	46.25	1	483
1102	11:56:47	2024-11-01	46.25	1	484
1102	11:56:16	2024-11-01	46.30	1	485
1102	11:56:02	2024-11-01	46.25	17	486
1102	11:56:02	2024-11-01	46.25	5	487
1102	11:55:51	2024-11-01	46.20	1	488
1102	11:54:55	2024-11-01	46.20	1	489
1102	11:53:59	2024-11-01	46.20	1	490
1102	11:53:04	2024-11-01	46.20	2	491
1102	11:53:03	2024-11-01	46.20	1	492
1102	11:52:13	2024-11-01	46.25	2	493
1102	11:52:11	2024-11-01	46.25	1	494
1102	11:52:11	2024-11-01	46.25	2	495
1102	11:52:08	2024-11-01	46.25	5	496
1102	11:52:07	2024-11-01	46.20	1	497
1102	11:52:07	2024-11-01	46.25	2	498
1102	11:52:07	2024-11-01	46.25	4	499
1102	11:52:07	2024-11-01	46.25	4	500
1102	11:51:11	2024-11-01	46.20	1	501
1102	11:50:56	2024-11-01	46.20	1	502
1102	11:50:52	2024-11-01	46.25	2	503
1102	11:50:47	2024-11-01	46.25	2	504
1102	11:50:17	2024-11-01	46.25	2	505
1102	11:50:15	2024-11-01	46.20	1	506
1102	11:50:06	2024-11-01	46.20	5	507
1102	11:50:06	2024-11-01	46.25	20	508
1102	11:50:06	2024-11-01	46.25	79	509
1102	11:49:19	2024-11-01	46.25	1	510
1102	11:48:55	2024-11-01	46.30	1	511
1102	11:48:23	2024-11-01	46.25	1	512
1102	11:47:59	2024-11-01	46.25	1	513
1102	11:47:58	2024-11-01	46.25	2	514
1102	11:47:57	2024-11-01	46.30	2	515
1102	11:47:57	2024-11-01	46.25	2	516
1102	11:47:57	2024-11-01	46.30	2	517
1102	11:47:57	2024-11-01	46.30	8	518
1102	11:47:57	2024-11-01	46.25	1	519
1102	11:47:57	2024-11-01	46.30	2	520
1102	11:47:57	2024-11-01	46.30	4	521
1102	11:47:57	2024-11-01	46.30	2	522
1102	11:47:57	2024-11-01	46.25	5	523
1102	11:47:57	2024-11-01	46.25	4	524
1102	11:47:57	2024-11-01	46.25	7	525
1102	11:47:57	2024-11-01	46.25	7	526
1102	11:47:57	2024-11-01	46.25	5	527
1102	11:47:57	2024-11-01	46.25	1	528
1102	11:47:57	2024-11-01	46.25	53	529
1102	11:47:57	2024-11-01	46.25	2	530
1102	11:47:56	2024-11-01	46.20	50	531
1102	11:47:49	2024-11-01	46.15	2	532
1102	11:47:27	2024-11-01	46.15	1	533
1102	11:46:31	2024-11-01	46.15	1	534
1102	11:46:30	2024-11-01	46.15	2	535
1102	11:45:35	2024-11-01	46.15	1	536
1102	11:45:14	2024-11-01	46.15	1	537
1102	11:45:13	2024-11-01	46.15	1	538
1102	11:45:11	2024-11-01	46.15	2	539
1102	11:44:39	2024-11-01	46.15	1	540
1102	11:43:52	2024-11-01	46.15	1	541
1102	11:43:52	2024-11-01	46.15	2	542
1102	11:43:43	2024-11-01	46.15	1	543
1102	11:43:08	2024-11-01	46.15	4	544
1102	11:43:06	2024-11-01	46.15	1	545
1102	11:42:47	2024-11-01	46.15	1	546
1102	11:42:39	2024-11-01	46.20	2	547
1102	11:42:36	2024-11-01	46.20	4	548
1102	11:42:36	2024-11-01	46.15	5	549
1102	11:42:34	2024-11-01	46.15	1	550
1102	11:42:34	2024-11-01	46.15	10	551
1102	11:42:33	2024-11-01	46.10	2	552
1102	11:41:58	2024-11-01	46.20	2	553
1102	11:41:57	2024-11-01	46.15	1	554
1102	11:41:55	2024-11-01	46.15	7	555
1102	11:41:55	2024-11-01	46.15	2	556
1102	11:41:52	2024-11-01	46.15	1	557
1102	11:41:52	2024-11-01	46.15	4	558
1102	11:41:52	2024-11-01	46.15	10	559
1102	11:41:52	2024-11-01	46.15	2	560
1102	11:41:51	2024-11-01	46.10	1	561
1102	11:41:14	2024-11-01	46.10	2	562
1102	11:40:55	2024-11-01	46.10	1	563
1102	11:39:59	2024-11-01	46.10	1	564
1102	11:39:55	2024-11-01	46.10	2	565
1102	11:39:06	2024-11-01	46.15	2	566
1102	11:39:03	2024-11-01	46.10	1	567
1102	11:38:36	2024-11-01	46.10	2	568
1102	11:38:13	2024-11-01	46.15	5	569
1102	11:38:10	2024-11-01	46.15	2	570
1102	11:38:07	2024-11-01	46.10	1	571
1102	11:38:06	2024-11-01	46.10	1	572
1102	11:37:36	2024-11-01	46.15	2	573
1102	11:37:35	2024-11-01	46.10	3	574
1102	11:37:35	2024-11-01	46.15	8	575
1102	11:37:35	2024-11-01	46.15	18	576
1102	11:37:16	2024-11-01	46.15	2	577
1102	11:37:13	2024-11-01	46.20	2	578
1102	11:37:11	2024-11-01	46.15	1	579
1102	11:36:37	2024-11-01	46.15	1	580
1102	11:36:15	2024-11-01	46.10	1	581
1102	11:35:57	2024-11-01	46.10	2	582
1102	11:35:19	2024-11-01	46.10	1	583
1102	11:35:08	2024-11-01	46.20	2	584
1102	11:35:05	2024-11-01	46.20	4	585
1102	11:35:05	2024-11-01	46.15	7	586
1102	11:35:05	2024-11-01	46.15	10	587
1102	11:35:05	2024-11-01	46.15	5	588
1102	11:34:38	2024-11-01	46.10	2	589
1102	11:34:24	2024-11-01	46.10	1	590
1102	11:34:23	2024-11-01	46.15	1	591
1102	11:34:19	2024-11-01	46.15	1	592
1102	11:34:05	2024-11-01	46.15	1	593
1102	11:34:03	2024-11-01	46.15	2	594
1102	11:34:03	2024-11-01	46.15	1	595
1102	11:33:27	2024-11-01	46.15	1	596
1102	11:33:25	2024-11-01	46.15	1	597
1102	11:33:19	2024-11-01	46.15	2	598
1102	11:33:10	2024-11-01	46.15	1	599
1102	11:32:31	2024-11-01	46.15	1	600
1102	11:32:00	2024-11-01	46.15	2	601
1102	11:31:35	2024-11-01	46.15	1	602
1102	11:30:54	2024-11-01	46.15	6	603
1102	11:30:54	2024-11-01	46.20	1	604
1102	11:30:54	2024-11-01	46.20	1	605
1102	11:30:54	2024-11-01	46.20	4	606
1102	11:30:54	2024-11-01	46.20	7	607
1102	11:30:54	2024-11-01	46.20	39	608
1102	11:30:41	2024-11-01	46.20	2	609
1102	11:30:39	2024-11-01	46.20	1	610
1102	11:29:50	2024-11-01	46.20	1	611
1102	11:29:43	2024-11-01	46.20	1	612
1102	11:29:22	2024-11-01	46.20	2	613
1102	11:28:47	2024-11-01	46.20	1	614
1102	11:28:11	2024-11-01	46.20	1	615
1102	11:28:08	2024-11-01	46.20	1	616
1102	11:28:03	2024-11-01	46.20	2	617
1102	11:27:51	2024-11-01	46.20	1	618
1102	11:26:55	2024-11-01	46.20	1	619
1102	11:26:44	2024-11-01	46.20	2	620
1102	11:26:33	2024-11-01	46.15	4	621
1102	11:26:33	2024-11-01	46.20	19	622
1102	11:26:33	2024-11-01	46.20	23	623
1102	11:25:59	2024-11-01	46.15	1	624
1102	11:25:59	2024-11-01	46.15	1	625
1102	11:25:25	2024-11-01	46.15	2	626
1102	11:25:03	2024-11-01	46.15	1	627
1102	11:24:07	2024-11-01	46.15	1	628
1102	11:24:06	2024-11-01	46.15	2	629
1102	11:23:36	2024-11-01	46.15	3	630
1102	11:23:36	2024-11-01	46.20	27	631
1102	11:23:11	2024-11-01	46.20	1	632
1102	11:23:10	2024-11-01	46.20	2	633
1102	11:23:00	2024-11-01	46.20	1	634
1102	11:22:47	2024-11-01	46.20	2	635
1102	11:22:15	2024-11-01	46.20	1	636
1102	11:21:28	2024-11-01	46.20	2	637
1102	11:21:19	2024-11-01	46.20	1	638
1102	11:20:23	2024-11-01	46.20	1	639
1102	11:20:09	2024-11-01	46.20	2	640
1102	11:20:08	2024-11-01	46.25	3	641
1102	11:19:39	2024-11-01	46.20	1	642
1102	11:19:27	2024-11-01	46.20	1	643
1102	11:19:12	2024-11-01	46.20	3	644
1102	11:19:12	2024-11-01	46.25	1	645
1102	11:19:12	2024-11-01	46.25	2	646
1102	11:19:12	2024-11-01	46.25	30	647
1102	11:18:50	2024-11-01	46.25	2	648
1102	11:18:31	2024-11-01	46.25	1	649
1102	11:18:21	2024-11-01	46.30	1	650
1102	11:18:21	2024-11-01	46.20	2	651
1102	11:18:21	2024-11-01	46.25	1	652
1102	11:18:21	2024-11-01	46.25	1	653
1102	11:18:20	2024-11-01	46.25	1	654
1102	11:18:19	2024-11-01	46.25	1	655
1102	11:18:18	2024-11-01	46.25	1	656
1102	11:18:17	2024-11-01	46.25	1	657
1102	11:18:16	2024-11-01	46.25	20	658
1102	11:18:13	2024-11-01	46.25	1	659
1102	11:18:09	2024-11-01	46.25	2	660
1102	11:18:08	2024-11-01	46.20	1	661
1102	11:18:08	2024-11-01	46.20	3	662
1102	11:18:03	2024-11-01	46.25	2	663
1102	11:17:35	2024-11-01	46.20	1	664
1102	11:17:31	2024-11-01	46.20	2	665
1102	11:16:51	2024-11-01	46.25	1	666
1102	11:16:39	2024-11-01	46.20	1	667
1102	11:16:19	2024-11-01	46.25	2	668
1102	11:16:17	2024-11-01	46.25	1	669
1102	11:16:15	2024-11-01	46.20	1	670
1102	11:16:12	2024-11-01	46.20	2	671
1102	11:15:43	2024-11-01	46.20	1	672
1102	11:15:33	2024-11-01	46.25	1	673
1102	11:15:02	2024-11-01	46.20	1	674
1102	11:14:58	2024-11-01	46.20	1	675
1102	11:14:55	2024-11-01	46.25	2	676
1102	11:14:53	2024-11-01	46.20	2	677
1102	11:14:47	2024-11-01	46.20	1	678
1102	11:14:16	2024-11-01	46.25	1	679
1102	11:13:51	2024-11-01	46.20	1	680
1102	11:13:40	2024-11-01	46.20	1	681
1102	11:13:34	2024-11-01	46.20	2	682
1102	11:13:29	2024-11-01	46.25	2	683
1102	11:13:28	2024-11-01	46.25	6	684
1102	11:13:14	2024-11-01	46.25	2	685
1102	11:12:55	2024-11-01	46.20	1	686
1102	11:12:54	2024-11-01	46.20	11	687
1102	11:12:50	2024-11-01	46.20	2	688
1102	11:12:48	2024-11-01	46.20	2	689
1102	11:12:46	2024-11-01	46.20	1	690
1102	11:11:59	2024-11-01	46.15	1	691
1102	11:11:03	2024-11-01	46.15	1	692
1102	11:10:07	2024-11-01	46.15	1	693
1102	11:10:00	2024-11-01	46.15	1	694
1102	11:09:57	2024-11-01	46.20	2	695
1102	11:09:57	2024-11-01	46.15	2	696
1102	11:09:56	2024-11-01	46.20	1	697
1102	11:09:54	2024-11-01	46.20	2	698
1102	11:09:53	2024-11-01	46.20	5	699
1102	11:09:50	2024-11-01	46.20	30	700
1102	11:09:26	2024-11-01	46.20	2	701
1102	11:09:22	2024-11-01	46.20	1	702
1102	11:09:19	2024-11-01	46.20	2	703
1102	11:09:18	2024-11-01	46.15	1	704
1102	11:09:17	2024-11-01	46.15	1	705
1102	11:09:16	2024-11-01	46.20	8	706
1102	11:09:16	2024-11-01	46.10	1	707
1102	11:09:16	2024-11-01	46.15	3	708
1102	11:09:16	2024-11-01	46.15	11	709
1102	11:09:16	2024-11-01	46.15	5	710
1102	11:09:16	2024-11-01	46.15	33	711
1102	11:09:16	2024-11-01	46.15	1	712
1102	11:09:11	2024-11-01	46.10	1	713
1102	11:08:15	2024-11-01	46.10	1	714
1102	11:07:20	2024-11-01	46.10	1	715
1102	11:07:19	2024-11-01	46.10	1	716
1102	11:06:24	2024-11-01	46.10	1	717
1102	11:06:23	2024-11-01	46.10	1	718
1102	11:05:42	2024-11-01	46.15	1	719
1102	11:05:27	2024-11-01	46.10	1	720
1102	11:04:31	2024-11-01	46.10	1	721
1102	11:03:51	2024-11-01	46.10	1	722
1102	11:03:50	2024-11-01	46.10	1	723
1102	11:03:48	2024-11-01	46.10	1	724
1102	11:03:42	2024-11-01	46.10	1	725
1102	11:03:42	2024-11-01	46.10	1	726
1102	11:03:41	2024-11-01	46.10	2	727
1102	11:03:41	2024-11-01	46.10	2	728
1102	11:03:40	2024-11-01	46.10	1	729
1102	11:03:39	2024-11-01	46.10	4	730
1102	11:03:38	2024-11-01	46.10	8	731
1102	11:03:37	2024-11-01	46.05	1	732
1102	11:03:36	2024-11-01	46.05	1	733
1102	11:03:35	2024-11-01	46.05	1	734
1102	11:02:39	2024-11-01	46.05	1	735
1102	11:02:15	2024-11-01	46.05	1	736
1102	11:01:53	2024-11-01	46.05	1	737
1102	11:01:49	2024-11-01	46.05	2	738
1102	11:01:43	2024-11-01	46.05	1	739
1102	11:01:17	2024-11-01	46.05	1	740
1102	11:00:59	2024-11-01	46.05	1	741
1102	11:00:48	2024-11-01	46.05	1	742
1102	11:00:47	2024-11-01	46.05	1	743
1102	10:59:58	2024-11-01	46.05	6	744
1102	10:59:58	2024-11-01	46.05	2	745
1102	10:59:58	2024-11-01	46.05	24	746
1102	10:59:51	2024-11-01	46.05	1	747
1102	10:59:48	2024-11-01	46.10	6	748
1102	10:59:47	2024-11-01	46.10	2	749
1102	10:59:47	2024-11-01	46.10	20	750
1102	10:58:55	2024-11-01	46.05	1	751
1102	10:58:32	2024-11-01	46.10	5	752
1102	10:58:31	2024-11-01	46.10	5	753
1102	10:58:30	2024-11-01	46.10	1	754
1102	10:58:29	2024-11-01	46.10	2	755
1102	10:58:29	2024-11-01	46.10	1	756
1102	10:58:29	2024-11-01	46.10	1	757
1102	10:58:28	2024-11-01	46.10	2	758
1102	10:58:28	2024-11-01	46.10	1	759
1102	10:58:28	2024-11-01	46.10	3	760
1102	10:58:28	2024-11-01	46.10	2	761
1102	10:58:27	2024-11-01	46.10	1	762
1102	10:58:27	2024-11-01	46.10	1	763
1102	10:58:27	2024-11-01	46.10	6	764
1102	10:58:27	2024-11-01	46.10	3	765
1102	10:58:27	2024-11-01	46.10	2	766
1102	10:58:27	2024-11-01	46.10	22	767
1102	10:58:27	2024-11-01	46.10	1	768
1102	10:58:14	2024-11-01	46.10	1	769
1102	10:58:14	2024-11-01	46.10	1	770
1102	10:58:14	2024-11-01	46.10	1	771
1102	10:58:11	2024-11-01	46.10	1	772
1102	10:58:11	2024-11-01	46.10	2	773
1102	10:57:59	2024-11-01	46.10	1	774
1102	10:57:48	2024-11-01	46.10	1	775
1102	10:57:48	2024-11-01	46.10	1	776
1102	10:57:41	2024-11-01	46.10	1	777
1102	10:57:41	2024-11-01	46.10	2	778
1102	10:57:36	2024-11-01	46.10	1	779
1102	10:57:35	2024-11-01	46.10	1	780
1102	10:57:03	2024-11-01	46.10	1	781
1102	10:56:40	2024-11-01	46.10	1	782
1102	10:56:14	2024-11-01	46.10	1	783
1102	10:56:11	2024-11-01	46.10	1	784
1102	10:56:07	2024-11-01	46.10	1	785
1102	10:56:02	2024-11-01	46.10	1	786
1102	10:55:38	2024-11-01	46.10	1	787
1102	10:55:38	2024-11-01	46.10	1	788
1102	10:55:12	2024-11-01	46.10	1	789
1102	10:55:11	2024-11-01	46.10	1	790
1102	10:54:49	2024-11-01	46.10	1	791
1102	10:54:45	2024-11-01	46.10	1	792
1102	10:54:15	2024-11-01	46.10	1	793
1102	10:53:20	2024-11-01	46.10	1	794
1102	10:53:19	2024-11-01	46.10	1	795
1102	10:53:12	2024-11-01	46.10	1	796
1102	10:53:09	2024-11-01	46.10	1	797
1102	10:53:09	2024-11-01	46.10	1	798
1102	10:52:27	2024-11-01	46.10	1	799
1102	10:52:23	2024-11-01	46.10	1	800
1102	10:51:53	2024-11-01	46.10	1	801
1102	10:51:41	2024-11-01	46.15	1	802
1102	10:51:27	2024-11-01	46.10	1	803
1102	10:51:17	2024-11-01	46.10	1	804
1102	10:51:14	2024-11-01	46.10	1	805
1102	10:51:13	2024-11-01	46.10	1	806
1102	10:51:12	2024-11-01	46.10	1	807
1102	10:51:12	2024-11-01	46.10	1	808
1102	10:51:12	2024-11-01	46.10	4	809
1102	10:50:59	2024-11-01	46.10	1	810
1102	10:49:46	2024-11-01	46.15	1	811
1102	10:49:11	2024-11-01	46.10	1	812
1102	10:49:08	2024-11-01	46.10	1	813
1102	10:49:08	2024-11-01	46.15	2	814
1102	10:49:06	2024-11-01	46.10	1	815
1102	10:48:43	2024-11-01	46.10	1	816
1102	10:48:33	2024-11-01	46.10	1	817
1102	10:48:14	2024-11-01	46.10	1	818
1102	10:48:10	2024-11-01	46.10	1	819
1102	10:47:54	2024-11-01	46.15	1	820
1102	10:47:38	2024-11-01	46.10	1	821
1102	10:46:45	2024-11-01	46.15	1	822
1102	10:46:20	2024-11-01	46.10	1	823
1102	10:46:20	2024-11-01	46.10	1	824
1102	10:45:36	2024-11-01	46.15	1	825
1102	10:44:13	2024-11-01	46.10	1	826
1102	10:43:46	2024-11-01	46.10	1	827
1102	10:43:45	2024-11-01	46.10	3	828
1102	10:43:45	2024-11-01	46.10	1	829
1102	10:43:45	2024-11-01	46.10	10	830
1102	10:43:45	2024-11-01	46.10	1	831
1102	10:43:45	2024-11-01	46.15	2	832
1102	10:43:43	2024-11-01	46.15	6	833
1102	10:43:42	2024-11-01	46.10	1	834
1102	10:43:42	2024-11-01	46.10	3	835
1102	10:43:42	2024-11-01	46.10	3	836
1102	10:43:42	2024-11-01	46.10	30	837
1102	10:40:27	2024-11-01	46.10	2	838
1102	10:40:25	2024-11-01	46.10	1	839
1102	10:40:24	2024-11-01	46.15	2	840
1102	10:40:24	2024-11-01	46.10	1	841
1102	10:40:24	2024-11-01	46.10	3	842
1102	10:40:23	2024-11-01	46.15	3	843
1102	10:40:23	2024-11-01	46.10	1	844
1102	10:40:23	2024-11-01	46.10	4	845
1102	10:40:23	2024-11-01	46.10	4	846
1102	10:40:23	2024-11-01	46.10	2	847
1102	10:40:23	2024-11-01	46.10	2	848
1102	10:40:23	2024-11-01	46.15	40	849
1102	10:40:23	2024-11-01	46.15	12	850
1102	10:40:23	2024-11-01	46.15	1	851
1102	10:40:22	2024-11-01	46.15	5	852
1102	10:40:22	2024-11-01	46.15	1	853
1102	10:40:22	2024-11-01	46.10	1	854
1102	10:40:22	2024-11-01	46.15	2	855
1102	10:40:22	2024-11-01	46.15	2	856
1102	10:40:21	2024-11-01	46.15	5	857
1102	10:40:10	2024-11-01	46.10	1	858
1102	10:40:10	2024-11-01	46.15	2	859
1102	10:39:43	2024-11-01	46.15	1	860
1102	10:39:07	2024-11-01	46.15	2	861
1102	10:39:07	2024-11-01	46.10	1	862
1102	10:38:51	2024-11-01	46.10	1	863
1102	10:38:50	2024-11-01	46.10	1	864
1102	10:38:49	2024-11-01	46.10	7	865
1102	10:38:31	2024-11-01	46.10	1	866
1102	10:37:53	2024-11-01	46.10	2	867
1102	10:37:52	2024-11-01	46.05	1	868
1102	10:37:51	2024-11-01	46.05	1	869
1102	10:37:51	2024-11-01	46.10	5	870
1102	10:37:18	2024-11-01	46.10	1	871
1102	10:37:07	2024-11-01	46.10	1	872
1102	10:36:50	2024-11-01	46.05	2	873
1102	10:36:39	2024-11-01	46.05	1	874
1102	10:36:39	2024-11-01	46.10	2	875
1102	10:36:39	2024-11-01	46.05	1	876
1102	10:36:38	2024-11-01	46.05	1	877
1102	10:36:38	2024-11-01	46.10	10	878
1102	10:35:54	2024-11-01	46.10	1	879
1102	10:35:54	2024-11-01	46.10	2	880
1102	10:35:52	2024-11-01	46.10	2	881
1102	10:35:51	2024-11-01	46.05	2	882
1102	10:35:51	2024-11-01	46.10	14	883
1102	10:35:16	2024-11-01	46.10	1	884
1102	10:34:39	2024-11-01	46.10	1	885
1102	10:33:36	2024-11-01	46.10	1	886
1102	10:33:21	2024-11-01	46.10	1	887
1102	10:33:06	2024-11-01	46.05	1	888
1102	10:33:05	2024-11-01	46.05	1	889
1102	10:33:05	2024-11-01	46.05	1	890
1102	10:32:20	2024-11-01	46.10	2	891
1102	10:31:58	2024-11-01	46.10	1	892
1102	10:31:56	2024-11-01	46.10	1	893
1102	10:30:25	2024-11-01	46.05	1	894
1102	10:30:15	2024-11-01	46.05	1	895
1102	10:28:36	2024-11-01	46.10	1	896
1102	10:28:36	2024-11-01	46.10	1	897
1102	10:28:34	2024-11-01	46.05	1	898
1102	10:28:34	2024-11-01	46.10	1	899
1102	10:28:32	2024-11-01	46.10	2	900
1102	10:28:29	2024-11-01	46.10	1	901
1102	10:27:18	2024-11-01	46.10	2	902
1102	10:27:17	2024-11-01	46.10	2	903
1102	10:27:17	2024-11-01	46.10	1	904
1102	10:27:14	2024-11-01	46.05	1	905
1102	10:27:14	2024-11-01	46.10	2	906
1102	10:27:13	2024-11-01	46.10	1	907
1102	10:27:03	2024-11-01	46.10	2	908
1102	10:27:00	2024-11-01	46.05	1	909
1102	10:27:00	2024-11-01	46.10	5	910
1102	10:26:46	2024-11-01	46.10	1	911
1102	10:26:38	2024-11-01	46.10	1	912
1102	10:26:17	2024-11-01	46.10	1	913
1102	10:26:06	2024-11-01	46.10	2	914
1102	10:26:04	2024-11-01	46.05	3	915
1102	10:26:04	2024-11-01	46.00	1	916
1102	10:26:04	2024-11-01	46.00	1	917
1102	10:26:04	2024-11-01	46.00	2	918
1102	10:26:04	2024-11-01	46.00	1	919
1102	10:26:04	2024-11-01	46.05	8	920
1102	10:26:04	2024-11-01	46.05	14	921
1102	10:26:04	2024-11-01	46.05	1	922
1102	10:24:35	2024-11-01	46.00	1	923
1102	10:24:14	2024-11-01	46.00	1	924
1102	10:24:13	2024-11-01	46.00	1	925
1102	10:24:13	2024-11-01	46.05	2	926
1102	10:24:13	2024-11-01	46.00	1	927
1102	10:23:01	2024-11-01	46.00	1	928
1102	10:22:28	2024-11-01	46.00	1	929
1102	10:22:27	2024-11-01	46.00	2	930
1102	10:22:22	2024-11-01	46.05	2	931
1102	10:22:21	2024-11-01	46.00	1	932
1102	10:22:21	2024-11-01	46.00	2	933
1102	10:22:20	2024-11-01	46.00	1	934
1102	10:22:19	2024-11-01	45.95	1	935
1102	10:22:19	2024-11-01	46.05	3	936
1102	10:22:17	2024-11-01	45.95	1	937
1102	10:22:17	2024-11-01	46.00	9	938
1102	10:22:17	2024-11-01	46.00	2	939
1102	10:22:17	2024-11-01	45.95	1	940
1102	10:22:17	2024-11-01	46.00	3	941
1102	10:22:17	2024-11-01	46.00	2	942
1102	10:22:16	2024-11-01	45.95	1	943
1102	10:22:15	2024-11-01	45.95	1	944
1102	10:22:14	2024-11-01	45.95	1	945
1102	10:22:13	2024-11-01	46.00	6	946
1102	10:22:05	2024-11-01	46.00	2	947
1102	10:22:04	2024-11-01	46.00	3	948
1102	10:22:03	2024-11-01	46.05	2	949
1102	10:22:01	2024-11-01	45.95	1	950
1102	10:22:01	2024-11-01	46.00	13	951
1102	10:21:55	2024-11-01	45.95	1	952
1102	10:21:50	2024-11-01	45.95	2	953
1102	10:21:49	2024-11-01	46.00	17	954
1102	10:21:49	2024-11-01	46.00	2	955
1102	10:21:45	2024-11-01	45.95	1	956
1102	10:21:41	2024-11-01	45.95	1	957
1102	10:21:36	2024-11-01	45.95	1	958
1102	10:21:36	2024-11-01	45.95	2	959
1102	10:20:05	2024-11-01	45.95	1	960
1102	10:20:05	2024-11-01	45.95	1	961
1102	10:20:02	2024-11-01	45.95	2	962
1102	10:19:22	2024-11-01	45.95	2	963
1102	10:19:20	2024-11-01	45.95	1	964
1102	10:19:20	2024-11-01	45.95	5	965
1102	10:18:27	2024-11-01	45.95	1	966
1102	10:18:23	2024-11-01	46.00	2	967
1102	10:18:22	2024-11-01	45.95	1	968
1102	10:18:13	2024-11-01	45.95	1	969
1102	10:18:13	2024-11-01	45.95	1	970
1102	10:18:13	2024-11-01	45.95	4	971
1102	10:18:00	2024-11-01	46.00	1	972
1102	10:17:23	2024-11-01	46.00	2	973
1102	10:17:21	2024-11-01	45.95	1	974
1102	10:16:06	2024-11-01	45.95	1	975
1102	10:16:05	2024-11-01	45.95	1	976
1102	10:16:05	2024-11-01	45.95	1	977
1102	10:16:05	2024-11-01	46.00	10	978
1102	10:16:04	2024-11-01	45.95	1	979
1102	10:15:14	2024-11-01	45.95	1	980
1102	10:14:56	2024-11-01	46.00	1	981
1102	10:14:49	2024-11-01	45.95	4	982
1102	10:14:13	2024-11-01	45.95	1	983
1102	10:14:13	2024-11-01	46.00	2	984
1102	10:14:11	2024-11-01	46.00	4	985
1102	10:14:09	2024-11-01	45.95	5	986
1102	10:14:08	2024-11-01	46.00	1	987
1102	10:14:08	2024-11-01	46.00	8	988
1102	10:14:08	2024-11-01	45.90	1	989
1102	10:14:08	2024-11-01	45.95	7	990
1102	10:14:08	2024-11-01	45.95	2	991
1102	10:14:07	2024-11-01	45.85	1	992
1102	10:14:06	2024-11-01	46.00	5	993
1102	10:14:06	2024-11-01	45.85	1	994
1102	10:14:06	2024-11-01	45.90	11	995
1102	10:14:06	2024-11-01	45.90	1	996
1102	10:14:06	2024-11-01	45.90	4	997
1102	10:14:05	2024-11-01	45.95	4	998
1102	10:14:05	2024-11-01	45.90	3	999
1102	10:14:05	2024-11-01	45.95	35	1000
1102	10:14:05	2024-11-01	45.95	2	1001
1102	10:14:05	2024-11-01	45.95	2	1002
1102	10:14:05	2024-11-01	45.90	1	1003
1102	10:14:05	2024-11-01	45.95	6	1004
1102	10:14:05	2024-11-01	45.95	3	1005
1102	10:14:04	2024-11-01	45.95	1	1006
1102	10:14:04	2024-11-01	45.95	8	1007
1102	10:14:04	2024-11-01	45.95	1	1008
1102	10:14:04	2024-11-01	45.95	3	1009
1102	10:14:04	2024-11-01	46.00	37	1010
1102	10:13:53	2024-11-01	46.00	1	1011
1102	10:13:53	2024-11-01	46.10	2	1012
1102	10:13:52	2024-11-01	46.10	2	1013
1102	10:13:52	2024-11-01	46.00	2	1014
1102	10:13:52	2024-11-01	46.00	4	1015
1102	10:13:52	2024-11-01	46.05	40	1016
1102	10:13:51	2024-11-01	46.05	1	1017
1102	10:13:51	2024-11-01	46.10	2	1018
1102	10:13:50	2024-11-01	46.05	2	1019
1102	10:13:34	2024-11-01	46.10	2	1020
1102	10:13:34	2024-11-01	46.05	1	1021
1102	10:13:34	2024-11-01	46.10	4	1022
1102	10:13:34	2024-11-01	46.05	1	1023
1102	10:13:33	2024-11-01	46.05	2	1024
1102	10:13:33	2024-11-01	46.10	23	1025
1102	10:13:32	2024-11-01	46.10	1	1026
1102	10:13:03	2024-11-01	46.10	2	1027
1102	10:13:03	2024-11-01	46.15	1	1028
1102	10:13:03	2024-11-01	46.10	1	1029
1102	10:13:03	2024-11-01	46.05	1	1030
1102	10:13:03	2024-11-01	46.10	11	1031
1102	10:13:02	2024-11-01	46.10	2	1032
1102	10:13:02	2024-11-01	46.15	20	1033
1102	10:12:39	2024-11-01	46.20	2	1034
1102	10:12:38	2024-11-01	46.15	1	1035
1102	10:12:38	2024-11-01	46.15	2	1036
1102	10:12:38	2024-11-01	46.20	20	1037
1102	10:12:18	2024-11-01	46.20	2	1038
1102	10:12:18	2024-11-01	46.15	2	1039
1102	10:12:16	2024-11-01	46.20	2	1040
1102	10:12:15	2024-11-01	46.15	1	1041
1102	10:12:15	2024-11-01	46.15	7	1042
1102	10:12:13	2024-11-01	46.15	1	1043
1102	10:12:08	2024-11-01	46.15	1	1044
1102	10:12:08	2024-11-01	46.15	5	1045
1102	10:11:58	2024-11-01	46.15	1	1046
1102	10:11:53	2024-11-01	46.15	1	1047
1102	10:11:48	2024-11-01	46.15	1	1048
1102	10:11:48	2024-11-01	46.15	10	1049
1102	10:11:40	2024-11-01	46.20	1	1050
1102	10:11:31	2024-11-01	46.15	1	1051
1102	10:10:32	2024-11-01	46.20	1	1052
1102	10:10:21	2024-11-01	46.15	1	1053
1102	10:10:04	2024-11-01	46.15	1	1054
1102	10:10:04	2024-11-01	46.15	2	1055
1102	10:09:57	2024-11-01	46.15	2	1056
1102	10:08:39	2024-11-01	46.15	1	1057
1102	10:07:12	2024-11-01	46.20	1	1058
1102	10:07:01	2024-11-01	46.20	2	1059
1102	10:07:00	2024-11-01	46.15	1	1060
1102	10:07:00	2024-11-01	46.15	2	1061
1102	10:06:55	2024-11-01	46.20	1	1062
1102	10:06:54	2024-11-01	46.15	2	1063
1102	10:06:47	2024-11-01	46.15	3	1064
1102	10:06:36	2024-11-01	46.15	1	1065
1102	10:06:31	2024-11-01	46.15	1	1066
1102	10:06:31	2024-11-01	46.15	4	1067
1102	10:06:10	2024-11-01	46.15	1	1068
1102	10:05:11	2024-11-01	46.15	1	1069
1102	10:04:44	2024-11-01	46.15	1	1070
1102	10:04:39	2024-11-01	46.20	1	1071
1102	10:04:39	2024-11-01	46.20	1	1072
1102	10:04:39	2024-11-01	46.20	4	1073
1102	10:04:38	2024-11-01	46.20	1	1074
1102	10:04:38	2024-11-01	46.25	38	1075
1102	10:04:38	2024-11-01	46.25	10	1076
1102	10:04:29	2024-11-01	46.30	2	1077
1102	10:04:29	2024-11-01	46.25	1	1078
1102	10:04:28	2024-11-01	46.30	3	1079
1102	10:04:27	2024-11-01	46.25	1	1080
1102	10:04:27	2024-11-01	46.25	1	1081
1102	10:04:27	2024-11-01	46.30	3	1082
1102	10:04:24	2024-11-01	46.30	2	1083
1102	10:04:22	2024-11-01	46.30	2	1084
1102	10:04:19	2024-11-01	46.25	1	1085
1102	10:04:19	2024-11-01	46.30	4	1086
1102	10:04:18	2024-11-01	46.25	1	1087
1102	10:04:18	2024-11-01	46.25	12	1088
1102	10:04:18	2024-11-01	46.25	1	1089
1102	10:04:17	2024-11-01	46.30	6	1090
1102	10:04:17	2024-11-01	46.25	1	1091
1102	10:04:17	2024-11-01	46.30	8	1092
1102	10:04:17	2024-11-01	46.25	6	1093
1102	10:04:17	2024-11-01	46.25	1	1094
1102	10:04:17	2024-11-01	46.25	1	1095
1102	10:04:17	2024-11-01	46.25	12	1096
1102	10:04:17	2024-11-01	46.25	2	1097
1102	10:04:17	2024-11-01	46.25	3	1098
1102	10:04:17	2024-11-01	46.20	3	1099
1102	10:04:17	2024-11-01	46.25	1	1100
1102	10:04:17	2024-11-01	46.25	1	1101
1102	10:04:17	2024-11-01	46.25	14	1102
1102	10:04:17	2024-11-01	46.25	31	1103
1102	10:03:59	2024-11-01	46.20	1	1104
1102	10:03:53	2024-11-01	46.20	1	1105
1102	10:03:04	2024-11-01	46.20	1	1106
1102	10:03:02	2024-11-01	46.20	1	1107
1102	10:02:02	2024-11-01	46.20	1	1108
1102	10:02:02	2024-11-01	46.20	1	1109
1102	10:01:57	2024-11-01	46.20	1	1110
1102	10:01:42	2024-11-01	46.20	1	1111
1102	10:01:41	2024-11-01	46.20	1	1112
1102	10:01:35	2024-11-01	46.20	1	1113
1102	10:01:35	2024-11-01	46.20	1	1114
1102	10:01:30	2024-11-01	46.20	2	1115
1102	10:01:30	2024-11-01	46.20	1	1116
1102	10:01:30	2024-11-01	46.20	3	1117
1102	10:01:30	2024-11-01	46.20	2	1118
1102	10:01:15	2024-11-01	46.20	2	1119
1102	10:01:15	2024-11-01	46.20	1	1120
1102	10:00:08	2024-11-01	46.20	1	1121
1102	10:00:05	2024-11-01	46.20	1	1122
1102	09:59:42	2024-11-01	46.20	1	1123
1102	09:59:42	2024-11-01	46.20	1	1124
1102	09:59:39	2024-11-01	46.20	1	1125
1102	09:59:38	2024-11-01	46.20	5	1126
1102	09:59:29	2024-11-01	46.20	1	1127
1102	09:59:27	2024-11-01	46.20	2	1128
1102	09:59:02	2024-11-01	46.25	11	1129
1102	09:59:02	2024-11-01	46.25	3	1130
1102	09:59:02	2024-11-01	46.20	1	1131
1102	09:59:02	2024-11-01	46.25	6	1132
1102	09:59:02	2024-11-01	46.25	8	1133
1102	09:59:02	2024-11-01	46.25	62	1134
1102	09:59:02	2024-11-01	46.25	2	1135
1102	09:59:01	2024-11-01	46.25	1	1136
1102	09:59:00	2024-11-01	46.25	1	1137
1102	09:58:37	2024-11-01	46.25	1	1138
1102	09:58:35	2024-11-01	46.25	1	1139
1102	09:55:48	2024-11-01	46.25	1	1140
1102	09:55:37	2024-11-01	46.25	2	1141
1102	09:55:35	2024-11-01	46.25	2	1142
1102	09:55:34	2024-11-01	46.25	9	1143
1102	09:54:07	2024-11-01	46.20	2	1144
1102	09:54:07	2024-11-01	46.25	2	1145
1102	09:54:06	2024-11-01	46.25	7	1146
1102	09:54:02	2024-11-01	46.25	20	1147
1102	09:53:50	2024-11-01	46.20	1	1148
1102	09:53:47	2024-11-01	46.20	2	1149
1102	09:53:45	2024-11-01	46.20	6	1150
1102	09:53:10	2024-11-01	46.20	1	1151
1102	09:52:59	2024-11-01	46.20	1	1152
1102	09:52:57	2024-11-01	46.25	2	1153
1102	09:52:15	2024-11-01	46.20	2	1154
1102	09:52:14	2024-11-01	46.20	1	1155
1102	09:52:08	2024-11-01	46.20	1	1156
1102	09:52:03	2024-11-01	46.20	5	1157
1102	09:51:58	2024-11-01	46.25	2	1158
1102	09:51:57	2024-11-01	46.20	10	1159
1102	09:51:23	2024-11-01	46.25	2	1160
1102	09:51:21	2024-11-01	46.20	4	1161
1102	09:50:33	2024-11-01	46.25	18	1162
1102	09:50:33	2024-11-01	46.25	1	1163
1102	09:48:56	2024-11-01	46.20	2	1164
1102	09:48:55	2024-11-01	46.20	1	1165
1102	09:48:55	2024-11-01	46.20	2	1166
1102	09:48:09	2024-11-01	46.20	2	1167
1102	09:48:06	2024-11-01	46.20	3	1168
1102	09:47:30	2024-11-01	46.15	1	1169
1102	09:47:29	2024-11-01	46.20	1	1170
1102	09:46:41	2024-11-01	46.20	1	1171
1102	09:46:22	2024-11-01	46.20	2	1172
1102	09:46:20	2024-11-01	46.20	1	1173
1102	09:46:20	2024-11-01	46.20	4	1174
1102	09:46:18	2024-11-01	46.20	12	1175
1102	09:46:17	2024-11-01	46.15	10	1176
1102	09:46:17	2024-11-01	46.15	24	1177
1102	09:46:16	2024-11-01	46.15	22	1178
1102	09:46:16	2024-11-01	46.15	17	1179
1102	09:44:40	2024-11-01	46.05	1	1180
1102	09:44:38	2024-11-01	46.15	2	1181
1102	09:44:38	2024-11-01	46.05	1	1182
1102	09:44:38	2024-11-01	46.05	5	1183
1102	09:44:38	2024-11-01	46.10	2	1184
1102	09:44:38	2024-11-01	46.05	1	1185
1102	09:44:37	2024-11-01	46.00	1	1186
1102	09:44:36	2024-11-01	46.10	1	1187
1102	09:44:36	2024-11-01	46.00	1	1188
1102	09:44:35	2024-11-01	46.10	6	1189
1102	09:44:35	2024-11-01	46.10	2	1190
1102	09:44:34	2024-11-01	46.00	6	1191
1102	09:44:33	2024-11-01	46.10	2	1192
1102	09:44:33	2024-11-01	46.10	3	1193
1102	09:44:33	2024-11-01	46.10	8	1194
1102	09:44:33	2024-11-01	46.10	6	1195
1102	09:44:32	2024-11-01	46.05	1	1196
1102	09:44:31	2024-11-01	46.00	2	1197
1102	09:44:31	2024-11-01	46.05	11	1198
1102	09:44:31	2024-11-01	46.00	3	1199
1102	09:44:30	2024-11-01	46.05	4	1200
1102	09:44:30	2024-11-01	46.00	1	1201
1102	09:44:30	2024-11-01	46.00	1	1202
1102	09:44:30	2024-11-01	46.10	14	1203
1102	09:44:30	2024-11-01	46.05	1	1204
1102	09:44:30	2024-11-01	46.00	2	1205
1102	09:44:30	2024-11-01	46.05	5	1206
1102	09:44:30	2024-11-01	46.00	5	1207
1102	09:44:30	2024-11-01	46.05	4	1208
1102	09:44:30	2024-11-01	46.05	2	1209
1102	09:44:30	2024-11-01	46.05	2	1210
1102	09:44:28	2024-11-01	46.05	1	1211
1102	09:44:28	2024-11-01	46.05	3	1212
1102	09:44:28	2024-11-01	46.05	2	1213
1102	09:44:28	2024-11-01	46.10	2	1214
1102	09:44:28	2024-11-01	46.10	13	1215
1102	09:44:27	2024-11-01	46.10	1	1216
1102	09:44:26	2024-11-01	46.10	4	1217
1102	09:44:26	2024-11-01	46.15	2	1218
1102	09:44:26	2024-11-01	46.10	1	1219
1102	09:44:26	2024-11-01	46.10	1	1220
1102	09:44:26	2024-11-01	46.15	3	1221
1102	09:44:26	2024-11-01	46.15	2	1222
1102	09:44:26	2024-11-01	46.15	3	1223
1102	09:44:26	2024-11-01	46.15	5	1224
1102	09:44:26	2024-11-01	46.15	4	1225
1102	09:44:26	2024-11-01	46.15	1	1226
1102	09:44:26	2024-11-01	46.15	10	1227
1102	09:44:19	2024-11-01	46.15	1	1228
1102	09:43:55	2024-11-01	46.20	2	1229
1102	09:43:53	2024-11-01	46.15	2	1230
1102	09:43:53	2024-11-01	46.15	1	1231
1102	09:43:53	2024-11-01	46.15	1	1232
1102	09:43:53	2024-11-01	46.20	3	1233
1102	09:43:52	2024-11-01	46.15	1	1234
1102	09:43:52	2024-11-01	46.15	1	1235
1102	09:43:52	2024-11-01	46.15	6	1236
1102	09:43:51	2024-11-01	46.15	2	1237
1102	09:43:51	2024-11-01	46.15	1	1238
1102	09:43:51	2024-11-01	46.20	2	1239
1102	09:43:51	2024-11-01	46.20	2	1240
1102	09:43:51	2024-11-01	46.15	2	1241
1102	09:43:51	2024-11-01	46.15	2	1242
1102	09:43:51	2024-11-01	46.15	1	1243
1102	09:43:50	2024-11-01	46.15	1	1244
1102	09:43:50	2024-11-01	46.15	2	1245
1102	09:43:50	2024-11-01	46.15	1	1246
1102	09:43:50	2024-11-01	46.15	4	1247
1102	09:43:50	2024-11-01	46.20	4	1248
1102	09:43:50	2024-11-01	46.20	3	1249
1102	09:43:50	2024-11-01	46.25	38	1250
1102	09:43:50	2024-11-01	46.25	1	1251
1102	09:43:47	2024-11-01	46.25	1	1252
1102	09:43:21	2024-11-01	46.25	1	1253
1102	09:42:37	2024-11-01	46.30	2	1254
1102	09:42:36	2024-11-01	46.25	1	1255
1102	09:42:20	2024-11-01	46.25	1	1256
1102	09:41:53	2024-11-01	46.25	1	1257
1102	09:41:19	2024-11-01	46.20	1	1258
1102	09:41:16	2024-11-01	46.30	2	1259
1102	09:41:15	2024-11-01	46.20	3	1260
1102	09:41:14	2024-11-01	46.25	3	1261
1102	09:41:14	2024-11-01	46.25	2	1262
1102	09:41:14	2024-11-01	46.30	35	1263
1102	09:41:14	2024-11-01	46.30	1	1264
1102	09:40:47	2024-11-01	46.30	1	1265
1102	09:40:43	2024-11-01	46.30	1	1266
1102	09:40:35	2024-11-01	46.30	1	1267
1102	09:40:24	2024-11-01	46.35	1	1268
1102	09:40:04	2024-11-01	46.35	1	1269
1102	09:39:32	2024-11-01	46.30	1	1270
1102	09:39:11	2024-11-01	46.30	1	1271
1102	09:39:08	2024-11-01	46.30	2	1272
1102	09:39:06	2024-11-01	46.30	4	1273
1102	09:39:04	2024-11-01	46.25	3	1274
1102	09:39:04	2024-11-01	46.30	1	1275
1102	09:39:04	2024-11-01	46.30	17	1276
1102	09:39:04	2024-11-01	46.30	1	1277
1102	09:38:15	2024-11-01	46.30	1	1278
1102	09:38:06	2024-11-01	46.30	1	1279
1102	09:37:22	2024-11-01	46.30	1	1280
1102	09:37:06	2024-11-01	46.30	1	1281
1102	09:36:12	2024-11-01	46.25	1	1282
1102	09:36:11	2024-11-01	46.25	1	1283
1102	09:36:10	2024-11-01	46.25	1	1284
1102	09:36:09	2024-11-01	46.25	1	1285
1102	09:36:09	2024-11-01	46.25	2	1286
1102	09:35:55	2024-11-01	46.25	2	1287
1102	09:35:53	2024-11-01	46.25	5	1288
1102	09:35:26	2024-11-01	46.25	1	1289
1102	09:35:18	2024-11-01	46.25	1	1290
1102	09:35:13	2024-11-01	46.20	1	1291
1102	09:35:08	2024-11-01	46.20	1	1292
1102	09:35:07	2024-11-01	46.20	1	1293
1102	09:35:05	2024-11-01	46.25	2	1294
1102	09:35:05	2024-11-01	46.20	1	1295
1102	09:35:05	2024-11-01	46.20	1	1296
1102	09:35:04	2024-11-01	46.20	1	1297
1102	09:34:50	2024-11-01	46.20	1	1298
1102	09:34:50	2024-11-01	46.20	3	1299
1102	09:34:50	2024-11-01	46.25	2	1300
1102	09:34:46	2024-11-01	46.25	1	1301
1102	09:34:04	2024-11-01	46.20	1	1302
1102	09:33:51	2024-11-01	46.20	7	1303
1102	09:33:48	2024-11-01	46.20	2	1304
1102	09:33:46	2024-11-01	46.20	2	1305
1102	09:33:44	2024-11-01	46.20	10	1306
1102	09:33:36	2024-11-01	46.20	2	1307
1102	09:33:16	2024-11-01	46.20	2	1308
1102	09:33:15	2024-11-01	46.20	10	1309
1102	09:32:05	2024-11-01	46.20	2	1310
1102	09:32:02	2024-11-01	46.15	1	1311
1102	09:31:54	2024-11-01	46.20	4	1312
1102	09:31:52	2024-11-01	46.10	2	1313
1102	09:31:52	2024-11-01	46.10	3	1314
1102	09:31:51	2024-11-01	46.20	7	1315
1102	09:31:51	2024-11-01	46.15	3	1316
1102	09:31:51	2024-11-01	46.15	1	1317
1102	09:31:50	2024-11-01	46.20	9	1318
1102	09:31:50	2024-11-01	46.20	4	1319
1102	09:31:50	2024-11-01	46.25	4	1320
1102	09:31:50	2024-11-01	46.30	47	1321
1102	09:31:50	2024-11-01	46.30	9	1322
1102	09:31:13	2024-11-01	46.35	2	1323
1102	09:31:13	2024-11-01	46.30	1	1324
1102	09:31:05	2024-11-01	46.30	1	1325
1102	09:30:13	2024-11-01	46.30	1	1326
1102	09:30:12	2024-11-01	46.30	1	1327
1102	09:29:48	2024-11-01	46.30	1	1328
1102	09:29:43	2024-11-01	46.30	5	1329
1102	09:28:31	2024-11-01	46.30	1	1330
1102	09:27:16	2024-11-01	46.30	2	1331
1102	09:27:08	2024-11-01	46.30	1	1332
1102	09:27:07	2024-11-01	46.35	2	1333
1102	09:27:04	2024-11-01	46.35	1	1334
1102	09:27:04	2024-11-01	46.35	2	1335
1102	09:27:04	2024-11-01	46.35	5	1336
1102	09:26:48	2024-11-01	46.35	1	1337
1102	09:26:48	2024-11-01	46.35	4	1338
1102	09:26:46	2024-11-01	46.30	1	1339
1102	09:26:46	2024-11-01	46.35	1	1340
1102	09:26:38	2024-11-01	46.35	2	1341
1102	09:26:32	2024-11-01	46.35	1	1342
1102	09:25:24	2024-11-01	46.30	2	1343
1102	09:25:22	2024-11-01	46.30	8	1344
1102	09:25:03	2024-11-01	46.30	3	1345
1102	09:25:02	2024-11-01	46.35	5	1346
1102	09:25:01	2024-11-01	46.35	5	1347
1102	09:25:01	2024-11-01	46.40	1	1348
1102	09:25:01	2024-11-01	46.40	3	1349
1102	09:25:01	2024-11-01	46.45	4	1350
1102	09:25:01	2024-11-01	46.40	19	1351
1102	09:25:01	2024-11-01	46.45	3	1352
1102	09:25:01	2024-11-01	46.45	2	1353
1102	09:25:01	2024-11-01	46.45	3	1354
1102	09:25:01	2024-11-01	46.45	3	1355
1102	09:25:01	2024-11-01	46.45	28	1356
1102	09:25:01	2024-11-01	46.45	3	1357
1102	09:24:58	2024-11-01	46.45	4	1358
1102	09:24:57	2024-11-01	46.40	1	1359
1102	09:24:56	2024-11-01	46.45	3	1360
1102	09:24:55	2024-11-01	46.40	1	1361
1102	09:24:53	2024-11-01	46.40	3	1362
1102	09:24:53	2024-11-01	46.45	4	1363
1102	09:24:51	2024-11-01	46.40	4	1364
1102	09:24:51	2024-11-01	46.40	30	1365
1102	09:24:50	2024-11-01	46.40	1	1366
1102	09:23:50	2024-11-01	46.40	1	1367
1102	09:23:46	2024-11-01	46.40	1	1368
1102	09:23:06	2024-11-01	46.40	2	1369
1102	09:23:05	2024-11-01	46.40	4	1370
1102	09:23:05	2024-11-01	46.40	16	1371
1102	09:22:55	2024-11-01	46.40	1	1372
1102	09:22:46	2024-11-01	46.40	1	1373
1102	09:22:02	2024-11-01	46.35	2	1374
1102	09:22:01	2024-11-01	46.40	1	1375
1102	09:20:51	2024-11-01	46.35	1	1376
1102	09:20:39	2024-11-01	46.35	3	1377
1102	09:20:23	2024-11-01	46.35	2	1378
1102	09:20:02	2024-11-01	46.35	1	1379
1102	09:18:45	2024-11-01	46.40	2	1380
1102	09:18:43	2024-11-01	46.35	2	1381
1102	09:18:43	2024-11-01	46.35	5	1382
1102	09:18:41	2024-11-01	46.40	1	1383
1102	09:18:36	2024-11-01	46.40	1	1384
1102	09:18:31	2024-11-01	46.40	4	1385
1102	09:17:57	2024-11-01	46.40	2	1386
1102	09:17:54	2024-11-01	46.40	2	1387
1102	09:17:52	2024-11-01	46.40	20	1388
1102	09:17:45	2024-11-01	46.40	2	1389
1102	09:17:43	2024-11-01	46.40	3	1390
1102	09:17:41	2024-11-01	46.40	20	1391
1102	09:17:32	2024-11-01	46.35	5	1392
1102	09:17:31	2024-11-01	46.40	2	1393
1102	09:17:30	2024-11-01	46.35	4	1394
1102	09:16:56	2024-11-01	46.30	2	1395
1102	09:16:49	2024-11-01	46.30	2	1396
1102	09:16:47	2024-11-01	46.40	2	1397
1102	09:16:47	2024-11-01	46.30	6	1398
1102	09:16:46	2024-11-01	46.35	6	1399
1102	09:16:46	2024-11-01	46.35	2	1400
1102	09:16:46	2024-11-01	46.35	2	1401
1102	09:16:46	2024-11-01	46.35	4	1402
1102	09:16:46	2024-11-01	46.35	2	1403
1102	09:16:45	2024-11-01	46.30	4	1404
1102	09:16:45	2024-11-01	46.35	6	1405
1102	09:16:45	2024-11-01	46.30	3	1406
1102	09:16:45	2024-11-01	46.30	2	1407
1102	09:16:45	2024-11-01	46.30	6	1408
1102	09:16:45	2024-11-01	46.25	2	1409
1102	09:16:45	2024-11-01	46.30	2	1410
1102	09:16:45	2024-11-01	46.30	2	1411
1102	09:16:45	2024-11-01	46.30	2	1412
1102	09:16:45	2024-11-01	46.25	1	1413
1102	09:16:45	2024-11-01	46.25	4	1414
1102	09:16:43	2024-11-01	46.25	6	1415
1102	09:16:42	2024-11-01	46.30	20	1416
1102	09:16:42	2024-11-01	46.25	1	1417
1102	09:16:42	2024-11-01	46.30	5	1418
1102	09:16:42	2024-11-01	46.30	1	1419
1102	09:16:42	2024-11-01	46.30	1	1420
1102	09:16:40	2024-11-01	46.30	2	1421
1102	09:16:38	2024-11-01	46.30	1	1422
1102	09:16:38	2024-11-01	46.30	3	1423
1102	09:16:37	2024-11-01	46.30	3	1424
1102	09:16:37	2024-11-01	46.30	2	1425
1102	09:16:37	2024-11-01	46.30	10	1426
1102	09:16:31	2024-11-01	46.30	2	1427
1102	09:16:30	2024-11-01	46.35	1	1428
1102	09:16:30	2024-11-01	46.35	2	1429
1102	09:16:30	2024-11-01	46.35	2	1430
1102	09:16:29	2024-11-01	46.35	2	1431
1102	09:16:29	2024-11-01	46.35	6	1432
1102	09:16:29	2024-11-01	46.40	2	1433
1102	09:16:28	2024-11-01	46.40	2	1434
1102	09:16:28	2024-11-01	46.40	3	1435
1102	09:16:26	2024-11-01	46.40	1	1436
1102	09:16:06	2024-11-01	46.45	1	1437
1102	09:16:05	2024-11-01	46.45	1	1438
1102	09:16:05	2024-11-01	46.45	3	1439
1102	09:16:03	2024-11-01	46.45	4	1440
1102	09:16:01	2024-11-01	46.50	6	1441
1102	09:16:01	2024-11-01	46.50	2	1442
1102	09:16:01	2024-11-01	46.50	9	1443
1102	09:16:01	2024-11-01	46.50	6	1444
1102	09:16:01	2024-11-01	46.50	30	1445
1102	09:15:59	2024-11-01	46.55	1	1446
1102	09:15:26	2024-11-01	46.50	2	1447
1102	09:15:23	2024-11-01	46.55	1	1448
1102	09:15:23	2024-11-01	46.50	1	1449
1102	09:14:46	2024-11-01	46.55	1	1450
1102	09:14:05	2024-11-01	46.50	2	1451
1102	09:13:39	2024-11-01	46.50	1	1452
1102	09:13:30	2024-11-01	46.50	2	1453
1102	09:13:29	2024-11-01	46.50	1	1454
1102	09:12:48	2024-11-01	46.50	1	1455
1102	09:12:39	2024-11-01	46.50	2	1456
1102	09:12:21	2024-11-01	46.45	1	1457
1102	09:12:15	2024-11-01	46.45	1	1458
1102	09:12:12	2024-11-01	46.50	1	1459
1102	09:12:12	2024-11-01	46.50	2	1460
1102	09:12:12	2024-11-01	46.55	68	1461
1102	09:12:12	2024-11-01	46.55	8	1462
1102	09:12:12	2024-11-01	46.55	6	1463
1102	09:12:12	2024-11-01	46.55	2	1464
1102	09:12:10	2024-11-01	46.55	1	1465
1102	09:12:04	2024-11-01	46.55	1	1466
1102	09:11:59	2024-11-01	46.55	1	1467
1102	09:11:37	2024-11-01	46.55	1	1468
1102	09:11:21	2024-11-01	46.55	10	1469
1102	09:11:21	2024-11-01	46.55	14	1470
1102	09:11:21	2024-11-01	46.55	6	1471
1102	09:11:21	2024-11-01	46.55	1	1472
1102	09:11:11	2024-11-01	46.55	2	1473
1102	09:11:11	2024-11-01	46.55	1	1474
1102	09:11:07	2024-11-01	46.55	1	1475
1102	09:10:46	2024-11-01	46.60	2	1476
1102	09:10:36	2024-11-01	46.55	1	1477
1102	09:10:27	2024-11-01	46.55	1	1478
1102	09:10:17	2024-11-01	46.55	1	1479
1102	09:10:14	2024-11-01	46.60	1	1480
1102	09:10:13	2024-11-01	46.55	2	1481
1102	09:10:11	2024-11-01	46.55	2	1482
1102	09:10:09	2024-11-01	46.55	2	1483
1102	09:10:09	2024-11-01	46.60	1	1484
1102	09:10:07	2024-11-01	46.55	2	1485
1102	09:09:53	2024-11-01	46.55	1	1486
1102	09:09:43	2024-11-01	46.55	1	1487
1102	09:09:30	2024-11-01	46.60	1	1488
1102	09:09:08	2024-11-01	46.60	2	1489
1102	09:09:07	2024-11-01	46.60	4	1490
1102	09:09:04	2024-11-01	46.60	1	1491
1102	09:09:04	2024-11-01	46.60	1	1492
1102	09:09:04	2024-11-01	46.60	31	1493
1102	09:08:54	2024-11-01	46.60	1	1494
1102	09:08:50	2024-11-01	46.60	5	1495
1102	09:08:17	2024-11-01	46.55	3	1496
1102	09:08:16	2024-11-01	46.60	1	1497
1102	09:07:42	2024-11-01	46.60	1	1498
1102	09:07:32	2024-11-01	46.55	1	1499
1102	09:07:11	2024-11-01	46.60	2	1500
1102	09:07:11	2024-11-01	46.60	1	1501
1102	09:07:09	2024-11-01	46.55	2	1502
1102	09:06:47	2024-11-01	46.60	2	1503
1102	09:06:44	2024-11-01	46.60	6	1504
1102	09:06:40	2024-11-01	46.55	29	1505
1102	09:06:40	2024-11-01	46.55	1	1506
1102	09:06:40	2024-11-01	46.55	5	1507
1102	09:06:38	2024-11-01	46.50	17	1508
1102	09:06:38	2024-11-01	46.50	27	1509
1102	09:06:13	2024-11-01	46.50	1	1510
1102	09:05:41	2024-11-01	46.45	1	1511
1102	09:04:33	2024-11-01	46.55	2	1512
1102	09:04:31	2024-11-01	46.45	6	1513
1102	09:04:23	2024-11-01	46.55	2	1514
1102	09:04:21	2024-11-01	46.55	5	1515
1102	09:04:21	2024-11-01	46.50	8	1516
1102	09:04:20	2024-11-01	46.55	2	1517
1102	09:04:20	2024-11-01	46.55	2	1518
1102	09:04:20	2024-11-01	46.55	21	1519
1102	09:04:20	2024-11-01	46.60	1	1520
1102	09:03:27	2024-11-01	46.45	1	1521
1102	09:03:23	2024-11-01	46.60	1	1522
1102	09:03:19	2024-11-01	46.45	1	1523
1102	09:03:09	2024-11-01	46.60	2	1524
1102	09:03:09	2024-11-01	46.45	3	1525
1102	09:03:08	2024-11-01	46.60	2	1526
1102	09:03:06	2024-11-01	46.45	2	1527
1102	09:02:54	2024-11-01	46.60	1	1528
1102	09:02:39	2024-11-01	46.45	3	1529
1102	09:02:33	2024-11-01	46.45	3	1530
1102	09:02:27	2024-11-01	46.45	3	1531
1102	09:02:21	2024-11-01	46.60	2	1532
1102	09:02:21	2024-11-01	46.45	3	1533
1102	09:02:20	2024-11-01	46.60	3	1534
1102	09:01:41	2024-11-01	46.60	2	1535
1102	09:01:39	2024-11-01	46.60	4	1536
1102	09:01:37	2024-11-01	46.60	2	1537
1102	09:01:36	2024-11-01	46.60	13	1538
1102	09:01:35	2024-11-01	46.60	2	1539
1102	09:01:34	2024-11-01	46.60	95	1540
1102	09:01:34	2024-11-01	46.60	63	1541
1102	09:01:34	2024-11-01	46.60	7	1542
1102	09:01:29	2024-11-01	46.60	2	1543
1102	09:01:28	2024-11-01	46.70	2	1544
1102	09:01:26	2024-11-01	46.65	2	1545
1102	09:01:26	2024-11-01	46.65	1	1546
1102	09:01:26	2024-11-01	46.65	2	1547
1102	09:01:24	2024-11-01	46.65	1	1548
1102	09:01:24	2024-11-01	46.60	3	1549
1102	09:01:23	2024-11-01	46.65	2	1550
1102	09:01:23	2024-11-01	46.65	3	1551
1102	09:01:23	2024-11-01	46.65	3	1552
1102	09:01:22	2024-11-01	46.60	10	1553
1102	09:01:21	2024-11-01	46.65	2	1554
1102	09:01:21	2024-11-01	46.65	9	1555
1102	09:01:20	2024-11-01	46.60	2	1556
1102	09:01:20	2024-11-01	46.60	8	1557
1102	09:01:20	2024-11-01	46.60	4	1558
1102	09:01:20	2024-11-01	46.60	54	1559
1102	09:01:20	2024-11-01	46.60	1	1560
1102	09:01:02	2024-11-01	46.55	1	1561
1102	09:01:02	2024-11-01	46.55	2	1562
1102	09:01:02	2024-11-01	46.55	5	1563
1102	09:01:00	2024-11-01	46.50	5	1564
1102	09:00:12	2024-11-01	46.55	2	1565
1102	09:00:09	2024-11-01	46.35	3	1566
1102	09:00:09	2024-11-01	46.55	3	1567
1102	09:00:09	2024-11-01	46.35	3	1568
1102	09:00:09	2024-11-01	46.45	3	1569
1102	09:00:08	2024-11-01	46.55	2	1570
1102	09:00:07	2024-11-01	46.45	1	1571
1102	09:00:07	2024-11-01	46.50	1	1572
1102	09:00:07	2024-11-01	46.50	3	1573
1102	09:00:07	2024-11-01	46.55	1	1574
1102	09:00:07	2024-11-01	46.55	2	1575
1102	09:00:07	2024-11-01	46.55	1	1576
1102	09:00:07	2024-11-01	46.55	2	1577
1102	09:00:07	2024-11-01	46.55	24	1578
1102	09:00:03	2024-11-01	46.55	1	1579
1102	09:00:03	2024-11-01	46.55	1	1580
1102	09:00:03	2024-11-01	46.60	2	1581
1102	09:00:03	2024-11-01	46.60	175	1582
1103	14:30:00	2024-11-01	17.70	1	1
1103	13:30:00	2024-11-01	17.70	31	2
1103	13:24:47	2024-11-01	17.50	1	3
1103	13:24:41	2024-11-01	17.50	1	4
1103	13:24:14	2024-11-01	17.60	1	5
1103	13:24:14	2024-11-01	17.50	1	6
1103	13:24:14	2024-11-01	17.50	1	7
1103	13:24:14	2024-11-01	17.60	2	8
1103	13:24:14	2024-11-01	17.50	6	9
1103	13:24:08	2024-11-01	17.65	1	10
1103	13:24:03	2024-11-01	17.65	1	11
1103	13:24:03	2024-11-01	17.65	1	12
1103	13:23:57	2024-11-01	17.65	2	13
1103	13:23:56	2024-11-01	17.45	1	14
1103	13:23:56	2024-11-01	17.45	1	15
1103	13:23:56	2024-11-01	17.45	1	16
1103	13:23:56	2024-11-01	17.45	1	17
1103	13:23:56	2024-11-01	17.60	2	18
1103	13:23:56	2024-11-01	17.60	2	19
1103	13:23:56	2024-11-01	17.45	1	20
1103	13:23:56	2024-11-01	17.50	9	21
1103	13:22:39	2024-11-01	17.60	1	22
1103	13:22:37	2024-11-01	17.55	1	23
1103	13:22:37	2024-11-01	17.60	2	24
1103	13:21:54	2024-11-01	17.70	2	25
1103	13:21:53	2024-11-01	17.60	1	26
1103	13:21:53	2024-11-01	17.60	1	27
1103	13:21:52	2024-11-01	17.70	2	28
1103	13:19:06	2024-11-01	17.70	1	29
1103	13:18:02	2024-11-01	17.70	1	30
1103	13:18:00	2024-11-01	17.70	1	31
1103	13:17:53	2024-11-01	17.60	1	32
1103	13:17:53	2024-11-01	17.65	1	33
1103	13:17:53	2024-11-01	17.65	5	34
1103	13:10:07	2024-11-01	17.65	1	35
1103	13:10:06	2024-11-01	17.65	1	36
1103	13:10:06	2024-11-01	17.55	1	37
1103	13:10:05	2024-11-01	17.55	1	38
1103	13:10:05	2024-11-01	17.60	2	39
1103	13:10:05	2024-11-01	17.60	5	40
1103	13:10:05	2024-11-01	17.60	3	41
1103	13:09:46	2024-11-01	17.55	1	42
1103	12:55:53	2024-11-01	17.55	1	43
1103	12:55:52	2024-11-01	17.55	1	44
1103	12:54:45	2024-11-01	17.55	1	45
1103	12:54:43	2024-11-01	17.60	1	46
1103	12:54:43	2024-11-01	17.60	5	47
1103	12:44:46	2024-11-01	17.55	1	48
1103	12:43:06	2024-11-01	17.60	1	49
1103	12:39:01	2024-11-01	17.60	1	50
1103	12:24:47	2024-11-01	17.55	1	51
1103	12:24:45	2024-11-01	17.55	2	52
1103	12:24:43	2024-11-01	17.60	12	53
1103	12:19:26	2024-11-01	17.55	1	54
1103	12:19:24	2024-11-01	17.55	1	55
1103	12:16:03	2024-11-01	17.55	2	56
1103	12:16:03	2024-11-01	17.55	1	57
1103	12:04:40	2024-11-01	17.50	1	58
1103	11:59:36	2024-11-01	17.45	1	59
1103	11:59:33	2024-11-01	17.60	1	60
1103	11:54:48	2024-11-01	17.45	1	61
1103	11:54:46	2024-11-01	17.60	7	62
1103	11:54:46	2024-11-01	17.60	2	63
1103	11:49:42	2024-11-01	17.60	1	64
1103	11:39:47	2024-11-01	17.45	1	65
1103	11:39:47	2024-11-01	17.55	5	66
1103	11:35:30	2024-11-01	17.55	1	67
1103	11:29:39	2024-11-01	17.40	1	68
1103	11:29:36	2024-11-01	17.55	1	69
1103	11:24:37	2024-11-01	17.40	1	70
1103	11:24:37	2024-11-01	17.50	6	71
1103	11:19:00	2024-11-01	17.50	1	72
1103	11:15:04	2024-11-01	17.40	1	73
1103	11:15:04	2024-11-01	17.45	1	74
1103	11:15:02	2024-11-01	17.45	5	75
1103	11:15:02	2024-11-01	17.45	1	76
1103	11:15:02	2024-11-01	17.45	4	77
1103	11:15:02	2024-11-01	17.45	1	78
1103	11:06:37	2024-11-01	17.40	2	79
1103	10:56:04	2024-11-01	17.40	1	80
1103	10:54:32	2024-11-01	17.40	2	81
1103	10:54:31	2024-11-01	17.45	10	82
1103	10:42:10	2024-11-01	17.45	1	83
1103	10:37:45	2024-11-01	17.40	1	84
1103	10:31:02	2024-11-01	17.40	1	85
1103	10:30:58	2024-11-01	17.40	1	86
1103	10:30:00	2024-11-01	17.45	1	87
1103	10:25:17	2024-11-01	17.40	1	88
1103	10:24:26	2024-11-01	17.45	2	89
1103	10:18:34	2024-11-01	17.45	1	90
1103	10:18:18	2024-11-01	17.40	1	91
1103	10:18:14	2024-11-01	17.45	2	92
1103	10:16:09	2024-11-01	17.40	1	93
1103	10:16:09	2024-11-01	17.40	5	94
1103	10:11:31	2024-11-01	17.40	1	95
1103	10:04:42	2024-11-01	17.40	1	96
1103	10:04:42	2024-11-01	17.40	1	97
1103	10:04:38	2024-11-01	17.40	3	98
1103	10:03:34	2024-11-01	17.40	1	99
1103	10:01:13	2024-11-01	17.40	1	100
1103	09:56:10	2024-11-01	17.40	2	101
1103	09:51:17	2024-11-01	17.45	1	102
1103	09:49:03	2024-11-01	17.45	1	103
1103	09:49:01	2024-11-01	17.45	1	104
1103	09:46:23	2024-11-01	17.40	1	105
1103	09:46:22	2024-11-01	17.45	8	106
1103	09:46:22	2024-11-01	17.45	2	107
1103	09:44:27	2024-11-01	17.40	1	108
1103	09:44:26	2024-11-01	17.40	3	109
1103	09:41:44	2024-11-01	17.40	2	110
1103	09:32:03	2024-11-01	17.40	1	111
1103	09:31:47	2024-11-01	17.40	1	112
1103	09:31:47	2024-11-01	17.45	1	113
1103	09:31:21	2024-11-01	17.45	1	114
1103	09:26:48	2024-11-01	17.40	1	115
1103	09:25:22	2024-11-01	17.40	3	116
1103	09:23:55	2024-11-01	17.40	3	117
1103	09:23:55	2024-11-01	17.40	12	118
1103	09:16:37	2024-11-01	17.40	3	119
1103	09:15:12	2024-11-01	17.45	1	120
1103	09:14:51	2024-11-01	17.45	1	121
1103	09:12:39	2024-11-01	17.40	2	122
1103	09:12:36	2024-11-01	17.40	6	123
1103	09:09:50	2024-11-01	17.45	1	124
1103	09:09:35	2024-11-01	17.45	1	125
1103	09:08:20	2024-11-01	17.45	1	126
1103	09:07:09	2024-11-01	17.45	2	127
1103	09:05:00	2024-11-01	17.40	1	128
1103	09:05:00	2024-11-01	17.40	2	129
1103	09:05:00	2024-11-01	17.40	5	130
1103	09:00:18	2024-11-01	17.50	6	131
\.


--
-- TOC entry 3437 (class 0 OID 16410)
-- Dependencies: 220
-- Data for Name: stock_info; Type: TABLE DATA; Schema: bstock; Owner: bstockuser
--

COPY bstock.stock_info (stock_code, stock_name, stock_type) FROM stdin;
1102	亞泥	1
1103	嘉泥	1
1104	環泥	1
1108	幸褔	1
1109	信大	1
1110	東泥	1
1201	味全	1
1203	味王	1
1210	大成	1
1213	大飲	1
1215	卜蜂	1
1216	統一	1
1217	愛之味	1
1218	泰山	1
1219	福壽	1
1220	台榮	1
1225	福懋油	1
1227	佳格	1
1229	聯華	1
1231	聯華食	1
1232	大統益	1
1233	天仁	1
1234	黑松	1
1235	興泰	1
1236	宏亞	1
1240	茂生農經	0
1256	鮮活果汁-KY	1
1259	安心	0
1264	德麥	0
1268	漢來美食	0
1301	台塑	1
1303	南亞	1
1304	台聚	1
1305	華夏	1
1307	三芳	1
1308	亞聚	1
1309	台達化	1
1310	台苯	1
1312	國喬	1
1313	聯成	1
1314	中石化	1
1315	達新	1
1316	上曜	1
1319	東陽	1
1321	大洋	1
1323	永裕	1
1324	地球	1
1326	臺化	1
1336	台翰	0
1337	再生-KY	1
1338	廣華-KY	1
1339	昭輝	1
1340	勝悅-KY	1
1341	富林-KY	1
1342	八貫	1
1402	遠東新	1
1409	新纖	1
1410	南染	1
1413	宏洲	1
1414	東和	1
1416	廣豐	1
1417	嘉裕	1
1418	東華	1
1419	新紡	1
1423	利華	1
1432	大魯閣	1
1434	福懋	1
1435	中福	1
1436	華友聯	1
1437	勤益控	1
1438	三地開發	1
1439	雋揚	1
1440	南紡	1
1441	大東	1
1442	名軒	1
1443	立益	1
1444	力麗	1
1445	大宇	1
1446	宏和	1
1447	力鵬	1
1449	佳和	1
1451	年興	1
1452	宏益	1
1453	大將	1
1454	台富	1
1455	集盛	1
1456	怡華	1
1457	宜進	1
1459	聯發	1
1460	宏遠	1
1463	強盛	1
1464	得力	1
1465	偉全	1
1466	聚隆	1
1467	南緯	1
1468	昶和	1
1470	大統新創	1
1471	首利	1
1472	三洋實業	1
1473	台南	1
1474	弘裕	1
1475	業旺	1
1476	儒鴻	1
1477	聚陽	1
1503	士電	1
1504	東元	1
1506	正道	1
1512	瑞利	1
1514	亞力	1
1515	力山	1
1516	川飛	1
1517	利奇	1
1519	華城	1
1521	大億	1
1522	堤維西	1
1524	耿鼎	1
1525	江申	1
1526	日馳	1
1527	鑽全	1
1528	恩德	1
1529	樂事綠能	1
1530	亞崴	1
1531	高林股	1
1532	勤美	1
1533	車王電	1
1535	中宇	1
1536	和大	1
1537	廣隆	1
1538	正峰	1
1539	巨庭	1
1540	喬福	1
1541	錩泰	1
1558	伸興	1
1560	中砂	1
1563	巧新	1
1565	精華	0
1568	倉佑	1
1569	濱川	0
1570	力肯	0
1580	新麥	0
1582	信錦	1
1583	程泰	1
1584	精剛	0
1586	和勤	0
1587	吉茂	1
1589	永冠-KY	1
1590	亞德客-KY	1
1591	駿吉-KY	0
1593	祺驊	0
1595	川寶	0
1597	直得	1
1598	岱宇	1
1599	宏佳騰	0
1603	華電	1
1604	聲寶	1
1605	華新	1
1608	華榮	1
1609	大亞	1
1611	中電	1
1612	宏泰	1
1614	三洋電	1
1615	大山	1
1616	億泰	1
1617	榮星	1
1618	合機	1
1626	艾美特-KY	1
1701	中化	1
1702	南僑	1
1707	葡萄王	1
1708	東鹼	1
1709	和益	1
1710	東聯	1
1711	永光	1
1712	興農	1
1713	國化	1
1714	和桐	1
1717	長興	1
1718	中纖	1
1720	生達	1
1721	三晃	1
1722	台肥	1
1723	中碳	1
1725	元禎	1
1726	永記	1
1727	中華化	1
1730	花仙子	1
1731	美吾華	1
1732	毛寶	1
1733	五鼎	1
1734	杏輝	1
1735	日勝化	1
1736	喬山	1
1737	臺鹽	1
1742	台蠟	0
1752	南光	1
1760	寶齡富錦	1
1762	中化生	1
1773	勝一	1
1776	展宇	1
1777	生泰	0
1781	合世	0
1783	和康生	1
1784	訊聯	0
1785	光洋科	0
1786	科妍	1
1788	杏昌	0
1789	神隆	1
1795	美時	1
1796	金穎生技	0
1799	易威	0
1802	台玻	1
1805	寶徠	1
1806	冠軍	1
1808	潤隆	1
1809	中釉	1
1810	和成	1
1813	寶利徠	0
1815	富喬	0
1817	凱撒衛	1
1903	士紙	1
1904	正隆	1
1905	華紙	1
1906	寶隆	1
1907	永豐餘	1
1909	榮成	1
2002	中鋼	1
2006	東和鋼鐵	1
2007	燁興	1
2008	高興昌	1
2009	第一銅	1
2010	春源	1
2012	春雨	1
2013	中鋼構	1
2014	中鴻	1
2015	豐興	1
2017	官田鋼	1
2020	美亞	1
2022	聚亨	1
2023	燁輝	1
2024	志聯	1
2025	千興	1
2027	大成鋼	1
2029	盛餘	1
2030	彰源	1
2031	新光鋼	1
2032	新鋼	1
2033	佳大	1
2034	允強	1
2035	唐榮	0
2038	海光	1
2049	上銀	1
2059	川湖	1
2061	風青	0
2062	橋椿	1
2063	世鎧	0
2064	晉椿	0
2065	世豐	0
2066	世德	0
2067	嘉鋼	0
2069	運錩	1
2070	精湛	0
2073	雄順	0
2101	南港	1
2102	泰豐	1
2103	台橡	1
2104	國際中橡	1
2105	正新	1
2106	建大	1
2107	厚生	1
2108	南帝	1
2109	華豐	1
2114	鑫永銓	1
2115	六暉-KY	1
2201	裕隆	1
2204	中華	1
2206	三陽工業	1
2207	和泰車	1
2208	台船	1
2211	長榮鋼	1
2221	大甲	0
2227	裕日車	1
2228	劍麟	1
2230	泰茂	0
2231	為升	1
2233	宇隆	1
2235	謚源	0
2236	百達-KY	1
2239	英利-KY	1
2241	艾姆勒	1
2243	宏旭-KY	1
2247	汎德永業	1
2248	華勝-KY	2
2249	湧盛	2
2250	IKKA-KY	1
2252	為昇科	2
2254	巨鎧精密-創	1
2255	凱銳光電	2
2256	歐特明	2
2258	鴻華先進-創	1
2301	光寶科	1
2302	麗正	1
2303	聯電	1
2305	全友	1
2308	台達電	1
2312	金寶	1
2313	華通	1
2314	台揚	1
2316	楠梓電	1
2317	鴻海	1
2321	東訊	1
2323	中環	1
2324	仁寶	1
2327	國巨	1
2328	廣宇	1
2329	華泰	1
2330	台積電	1
2331	精英	1
2332	友訊	1
2337	旺宏	1
2338	光罩	1
2340	台亞	1
2342	茂矽	1
2344	華邦電	1
2345	智邦	1
2347	聯強	1
2348	海悅	1
2349	錸德	1
2351	順德	1
2352	佳世達	1
2354	鴻準	1
2355	敬鵬	1
2356	英業達	1
2357	華碩	1
2358	廷鑫	1
2359	所羅門	1
2360	致茂	1
2362	藍天	1
2364	倫飛	1
2365	昆盈	1
2367	燿華	1
2368	金像電	1
2369	菱生	1
2371	大同	1
2373	震旦行	1
2374	佳能	1
2375	凱美	1
2376	技嘉	1
2377	微星	1
2379	瑞昱	1
2380	虹光	1
2382	廣達	1
2383	台光電	1
1325	恒大	1
2385	群光	1
2387	精元	1
2388	威盛	1
2390	云辰	1
2392	正崴	1
2393	億光	1
2395	研華	1
2397	友通	1
2399	映泰	1
2401	凌陽	1
2402	毅嘉	1
2404	漢唐	1
2405	輔信	1
2406	國碩	1
2408	南亞科	1
2409	友達	1
2412	中華電	1
2413	環科	1
2414	精技	1
2415	錩新	1
2417	圓剛	1
2419	仲琦	1
2420	新巨	1
2421	建準	1
2423	固緯	1
2424	隴華	1
2425	承啟	1
2426	鼎元	1
2427	三商電	1
2428	興勤	1
2429	銘旺科	1
2430	燦坤	1
2431	聯昌	1
2433	互盛電	1
2434	統懋	1
2436	偉詮電	1
2438	翔耀	1
2439	美律	1
2440	太空梭	1
2441	超豐	1
2442	新美齊	1
2443	昶虹	1
2444	兆勁	1
2449	京元電子	1
2450	神腦	1
2451	創見	1
2453	凌群	1
2454	聯發科	1
2455	全新	1
2457	飛宏	1
2458	義隆	1
2459	敦吉	1
2460	建通	1
2461	光群雷	1
2462	良得電	1
2464	盟立	1
2465	麗臺	1
2466	冠西電	1
2467	志聖	1
2468	華經	1
2471	資通	1
2472	立隆電	1
2474	可成	1
2476	鉅祥	1
2477	美隆電	1
2478	大毅	1
2480	敦陽科	1
2481	強茂	1
2482	連宇	1
2483	百容	1
2484	希華	1
2485	兆赫	1
2486	一詮	1
2488	漢平	1
2489	瑞軒	1
2491	吉祥全	1
2492	華新科	1
2493	揚博	1
2495	普安	1
2496	卓越	1
2497	怡利電	1
2498	宏達電	1
2501	國建	1
2504	國產	1
2505	國揚	1
2506	太設	1
2509	全坤建	1
2511	太子	1
2514	龍邦	1
2515	中工	1
2516	新建	1
2520	冠德	1
2524	京城	1
2527	宏璟	1
2528	皇普	1
2530	華建	1
2534	宏盛	1
2535	達欣工	1
2536	宏普	1
2537	聯上發	1
2538	基泰	1
2539	櫻花建	1
2540	愛山林	1
2542	興富發	1
2543	皇昌	1
2545	皇翔	1
2546	根基	1
2547	日勝生	1
2548	華固	1
2596	綠意	0
2353	宏碁	1
2597	潤弘	1
2601	益航	1
2603	長榮	1
2605	新興	1
2606	裕民	1
2607	榮運	1
2608	嘉里大榮	1
2609	陽明	1
2610	華航	1
2611	志信	1
2612	中航	1
2613	中櫃	1
2614	東森	1
2615	萬海	1
2616	山隆	1
2617	台航	1
2618	長榮航	1
2630	亞航	1
2633	台灣高鐵	1
2634	漢翔	1
2636	台驊控股	1
2637	慧洋-KY	1
2640	大車隊	0
2641	正德	0
2642	宅配通	1
2643	捷迅	0
2644	中信造船	2
2645	長榮航太	1
2646	星宇航空	2
2701	萬企	1
2702	華園	1
2704	國賓	1
2705	六福	1
2706	第一店	1
2707	晶華	1
2712	遠雄來	1
2718	晶悅	0
2719	燦星旅	0
2722	夏都	1
2723	美食-KY	1
2724	藝舍-KY	0
2726	雅茗-KY	0
2727	王品	1
2729	瓦城	0
2731	雄獅	1
2732	六角	0
2733	維格餅家	2
2734	易飛網	0
2736	富野	0
2739	寒舍	1
2740	天蔥	0
2741	老四川	2
2743	山富	0
2745	五福	0
2748	雲品	1
2750	桃禧	2
2751	王座	2
2752	豆府	0
2753	八方雲集	1
2754	亞洲藏壽司	0
2755	揚秦	0
2756	聯發國際	0
2758	路易莎咖啡	2
2760	巨宇翔	2
2761	橘焱胡同	2
2762	世界健身-KY	1
2801	彰銀	1
2809	京城銀	1
2812	台中銀	1
2816	旺旺保	1
2820	華票	1
2832	台產	1
2834	臺企銀	1
2836	高雄銀	1
2838	聯邦銀	1
2845	遠東銀	1
2849	安泰銀	1
2850	新產	1
2851	中再保	1
2852	第一保	1
2855	統一證	1
2867	三商壽	1
2880	華南金	1
2881	富邦金	1
2882	國泰金	1
2883	開發金	1
2884	玉山金	1
2885	元大金	1
2886	兆豐金	1
2887	台新金	1
2888	新光金	1
2889	國票金	1
2890	永豐金	1
2891	中信金	1
2892	第一金	1
2897	王道銀行	1
2901	欣欣	1
2903	遠百	1
2904	匯僑	1
2905	三商	1
2906	高林	1
2908	特力	1
2910	統領	1
2911	麗嬰房	1
2912	統一超	1
2913	農林	1
2915	潤泰全	1
2916	滿心	0
2923	鼎固-KY	1
2924	宏太-KY	0
2926	誠品生活	0
2929	淘帝-KY	1
2937	集雅社	0
2938	床的世界	2
2939	凱羿-KY	1
2941	米斯特	0
2942	京站	2
2945	三商家購	1
2947	振宇五金	0
2948	寶陞	0
2949	欣新網	0
3002	歐格	1
3003	健和興	1
3004	豐達科	1
3005	神基	1
3006	晶豪科	1
3008	大立光	1
3010	華立	1
3013	晟銘電	1
3014	聯陽	1
3015	全漢	1
3016	嘉晶	1
3017	奇鋐	1
3018	隆銘綠能	1
3019	亞光	1
3021	鴻名	1
5383	金利精密工業股份有限公司	0
3022	威強電	1
3023	信邦	1
3024	憶聲電子	1
3025	星通	1
3026	禾伸堂	1
3027	盛達	1
3028	增你強	1
3029	零壹	1
3030	德律	1
3031	佰鴻	1
3032	偉訓	1
3033	威健	1
3034	聯詠	1
3035	智原	1
3036	文曄	1
3037	欣興	1
3038	全台	1
3040	遠見	1
3041	揚智	1
3042	晶技	1
3043	科風	1
3044	健鼎	1
3045	台灣大	1
3047	訊舟	1
3048	益登	1
3049	精金	1
3050	鈺德	1
3051	力特	1
3052	夆典	1
3054	立萬利	1
3055	蔚華科	1
3056	富華新	1
3057	喬鼎	1
3058	立德	1
3059	華晶科	1
3060	銘異	1
3062	建漢	1
3064	泰偉	0
3066	李洲	0
3067	全域	0
3071	協禧	0
3073	天方能源	0
3078	僑威	0
3081	聯亞	0
3083	網龍	0
3085	新零售	0
3086	華義	0
3088	艾訊	0
3090	日電貿	1
3092	鴻碩	1
3093	港建*	0
3094	聯傑	1
3095	及成	0
3097	拍檔	2
3105	穩懋	0
3114	好德	0
3115	富榮綱	0
3117	年程	2
3118	進階	0
3122	笙泉	0
3128	昇銳	0
3130	一零四	1
3131	弘塑	0
3135	凌航	2
3138	耀登	1
3141	晶宏	0
3147	大綜	0
3149	正達	1
3150	鈺寶-創	1
3152	璟德	0
3158	嘉實	2
3162	精確	0
3163	波若威	0
3164	景岳	1
3167	大量	1
3168	眾福科	1
3169	亞信	0
3171	新洲	0
3176	基亞	0
3178	公準	0
3184	微邦	2
3188	鑫龍騰	0
3189	景碩	1
3191	雲嘉南	0
3202	樺晟	0
3205	佰研	0
3206	志豐	0
3207	耀勝	0
3209	全科	1
3211	順達科	0
3213	茂訊	0
3217	優群	0
3218	大學光	0
3219	倚強科	0
3221	台嘉碩	0
3224	三顧	0
3226	至寶電	0
3227	原相	0
3228	金麗科	0
3229	晟鈦	1
3230	錦明	0
3231	緯創	1
3232	昱捷	0
3234	光環	0
3236	千如	0
3252	海灣	0
3257	虹冠電	1
3259	鑫創	0
3260	威剛	0
3264	欣銓	0
3265	台星科	0
3266	昇陽	1
3268	海德威	0
3272	東碩	0
3276	宇環	0
3284	太普高	0
3285	微端	0
3287	廣寰科	0
3288	點晶	0
3289	宜特	0
3290	東浦	0
3293	鈊象	0
3294	英濟	0
3296	勝德	1
3297	杭特	0
3303	岱稜	0
3305	昇貿	1
3306	鼎天	0
3308	聯德	1
3310	佳穎	0
3311	閎暉	1
3312	弘憶股	1
3313	斐成	0
3317	尼克森	0
3321	同泰	1
3322	建舜電	0
3323	加百裕	0
3324	雙鴻	0
3325	旭品	0
3332	幸康	0
3338	泰碩	1
3339	泰谷	0
3346	麗清	1
3349	寶德	0
3354	律勝	0
3356	奇偶	1
3357	臺慶科	0
3360	尚立	0
3362	先進光	0
3363	上詮	0
3372	典範	0
3373	熱映	0
3374	精材	0
3376	新日興	1
3379	彬台	0
3380	明泰	1
3388	崇越電	0
3390	旭軟	0
3402	漢科	0
3406	玉晶光	1
3413	京鼎	1
3416	融程電	1
3419	譁裕	1
3426	台興	0
3430	奇鈦科	0
3432	台端	1
3434	哲固	0
3437	榮創	1
3438	類比科	0
3441	聯一光	0
3443	創意	1
3444	利機	0
3447	展達	1
3450	聯鈞	1
3454	晶睿	1
3455	由田	0
3465	進泰電子	0
3466	德晉	0
3467	台灣精材	2
3479	安勤	0
3481	群創	1
3483	力致	0
3484	崧騰	0
3485	敘豐	2
3489	森寶	0
3490	單井	0
3491	昇達科	0
3492	長盛	0
3494	誠研	1
3498	陽程	0
3499	環天科	0
3501	維熹	1
3504	揚明光	1
3508	位速	0
3511	矽瑪	0
3512	皇龍	0
3515	華擎	1
3516	亞帝歐	0
3518	柏騰	1
3520	華盈	0
3521	鴻翊	0
3522	御頂	0
3523	迎輝	0
3526	凡甲	0
3527	聚積	0
3528	安馳	1
3529	力旺	0
3530	晶相光	1
3531	先益	0
3532	台勝科	1
3533	嘉澤	1
3535	晶彩科	1
3537	堡達	0
3540	曜越	0
3541	西柏	0
3543	州巧	1
3545	敦泰	1
3546	宇峻	0
3548	兆利	0
3550	聯穎	1
3551	世禾	0
3552	同致	0
3555	博士旺	0
3556	禾瑞亞	0
3557	嘉威	1
3558	神準	0
3563	牧德	1
3564	其陽	0
3567	逸昌	0
3570	大塚	0
3576	聯合再生	1
3577	泓格	0
3580	友威科	0
3581	博磊	0
3583	辛耘	1
3585	聯致	2
3587	閎康	0
3588	通嘉	1
3591	艾笛森	1
3592	瑞鼎	1
3593	力銘	1
3594	磐儀	0
3595	山太士	2
3596	智易	1
3597	映興	0
3603	建祥國際	2
3605	宏致	1
3607	谷崧	1
3609	三一東林	0
3611	鼎翰	0
3615	安可	0
3617	碩天	1
3622	洋華	1
3623	富晶通	0
3624	光頡	0
3625	西勝	0
3628	盈正	0
3629	地心引力	0
3630	新鉅科	0
3631	晟楠	0
3632	研勤	0
3633	云光	2
3645	達邁	1
3646	艾恩特	0
3652	精聯	1
3653	健策	1
3659	百辰	2
3661	世芯-KY	1
3663	鑫科	0
3664	安瑞-KY	0
3665	貿聯-KY	1
3666	光耀	0
3669	圓展	1
3672	康聯訊	0
3673	TPK-KY	1
3675	德微	0
3678	聯享	2
3679	新至陞	1
3680	家登	0
3684	榮昌	0
3685	元創精密	0
3686	達能	1
3687	歐買尬	0
3689	湧德	0
3691	碩禾	0
3693	營邦	0
3694	海華	1
3701	大眾控	1
3702	大聯大	1
3703	欣陸	1
3704	合勤控	1
3705	永信	1
3706	神達	1
3707	漢磊	0
3708	上緯投控	1
3709	鑫聯大投控	0
3710	連展投控	0
3711	日月光投控	1
3712	永崴投控	1
3713	新晶投控	0
3714	富采	1
3715	定穎投控	1
4102	永日	0
4104	佳醫	1
4105	東洋	0
4106	雃博	1
4107	邦特	0
4108	懷特	1
4109	加捷生醫	0
4111	濟生	0
4113	聯上	0
4114	健喬	0
4115	善德生技	2
4116	明基醫	0
4117	普生	2
4119	旭富	1
4120	友華	0
4121	優盛	0
4123	晟德	0
4126	太醫	0
4127	天良	0
4128	中天	0
4129	聯合	0
4130	健亞	0
4131	浩泰	0
4132	國鼎	2
4133	亞諾法	1
4137	麗豐-KY	1
4138	曜亞	0
4139	馬光-KY	0
4142	國光生	1
4147	中裕	0
4148	全宇生技-KY	1
4150	優你康	2
4153	鈺緯	0
4154	樂威科-KY	0
4155	訊映	1
4157	太景＊-KY	0
4160	訊聯基因	0
4161	聿新科	0
4162	智擎	0
4163	鐿鈦	0
4164	承業醫	1
4166	友霖	2
4167	松瑞藥	0
4168	醣聯	0
4169	泰宗	2
4170	鑫品生醫	2
4171	瑞基	0
4172	因華	2
4173	久裕	0
4174	浩鼎	0
4175	杏一	0
4183	褔永生技	0
4186	尖端醫	2
4188	安克	0
4190	佐登-KY	1
4192	杏國	0
4194	禾生技	2
4195	基米	2
4197	暐世	2
4198	欣大健康	0
4205	中華食	0
4207	環泰	0
4303	信立	0
4304	勝昱	0
4306	炎洲	1
4401	東隆興	0
4402	福大	0
4406	新昕纖	0
4413	飛寶企業	0
4414	如興	1
4416	三圓	0
4417	金洲	0
4419	皇家美食	0
4420	光明	0
4426	利勤	1
4430	耀億	0
4431	敏成健康	2
4432	銘旺實	0
4433	興采	0
4438	廣越	1
4439	冠星-KY	1
4440	宜新實業	1
4441	振大環球	2
4442	竣邦-KY	0
4502	健信	0
4503	金雨	0
4506	崇友	0
4510	高鋒	0
4513	福裕	0
4523	永彰	0
4526	東台	1
4528	江興鍛	0
4529	淳紳	0
4530	宏易	0
4532	瑞智	1
4533	協易機	0
4534	慶騰	0
4535	至興	0
4536	拓凱	1
4537	旭東	2
4538	大詠城	0
4540	全球傳動	1
4541	晟田	0
4542	科嶠	0
4543	萬在	0
4544	春日	2
4545	銘鈺	1
4546	長亨	2
4549	桓達	0
4550	長佳	0
4551	智伸科	1
4552	力達-KY	1
4553	盛復	2
4554	橙的	0
4555	氣立	1
4556	旭然	0
4557	永新-KY	1
4558	寶緯	0
4559	久裕興	2
4560	強信-KY	1
4561	健椿	0
4562	穎漢	1
4563	百德	0
4564	元翎	1
4565	宏偉	2
4566	時碩工業	1
4568	科際精密	0
4569	六方科-KY	1
4570	傑生	2
4571	鈞興-KY	1
4572	駐龍	1
4573	高明鐵	2
4575	銓寶	2
4576	大銀微系統	1
4577	達航科技	0
4580	捷流閥業	0
4581	光隆精密-KY	1
4582	聚恆科技	2
4583	台灣精銳	1
4584	君帆	0
4587	寶元數控	2
4588	玖鼎電力	1
4589	碩陽電機	2
4609	唐鋒	0
4702	中美實	0
4706	大恭	0
4707	磐亞	0
4711	永純	0
4712	褔格創新	0
4714	永捷	0
4716	大立	0
4720	德淵	1
4721	美琪瑪	0
4722	國精化	1
4724	宣捷幹細胞	2
4726	永昕	0
4728	雙美	0
4729	熒茂	0
4732	彥臣	2
4735	豪展	0
4736	泰博	1
4737	華廣	1
4738	尚化	2
4739	康普	1
4741	泓瀚	0
4743	合一	0
4744	皇將	0
4745	合富-KY	0
4746	台耀	1
4747	強生	0
4749	新應材	2
4754	國碳科	0
4755	三福化	1
4760	勤凱	0
4763	材料-KY	1
4764	雙鍵	1
4765	磐采	2
4766	南寶	1
4767	誠泰科技	0
4768	晶呈科技	0
4770	上品	1
4771	望隼	1
4772	台特化	2
4773	高福	2
4804	大略-KY	0
4807	日成-KY	1
4903	聯光通	0
4904	遠傳	1
4905	台聯電	0
4906	正文	1
4907	富宇	0
4908	前鼎	0
4909	新復興	0
4911	德英	0
4912	聯德控股-KY	1
4915	致伸	1
4916	事欣科	1
4919	新唐	1
4923	力士	0
4924	欣厚-KY	0
4925	智微	2
4927	泰鼎-KY	1
4930	燦星網	1
4931	新盛力	0
4933	友輝	0
4934	太極	1
4935	茂林-KY	1
4938	和碩	1
4939	亞電	0
4942	嘉彰	1
4943	康控-KY	1
4945	陞達科技	0
4946	辣椒	0
4949	有成精密	1
4950	牧東	0
4951	精拓科	0
4952	凌通	1
4953	緯軟	0
4956	光鋐	1
4958	臻鼎-KY	1
4960	誠美材	1
4961	天鈺	1
4966	譜瑞-KY	0
4967	十銓	1
4968	立積	1
4971	IET-KY	0
4972	湯石照明	0
4973	廣穎	0
4974	亞泰	0
4976	佳凌	1
4977	眾達-KY	1
4979	華星光	0
4980	佐臻	2
4987	科誠	0
4989	榮科	1
4991	環宇-KY	0
4994	傳奇	1
4995	晶達	0
4999	鑫禾	1
5007	三星	1
5009	榮剛	0
5011	久陽	0
5013	強新	0
5014	建錩	0
5015	華祺	0
5016	松和	0
5201	凱衛	0
5202	力新	0
5203	訊連	1
5205	中茂	0
5206	坤悅	0
5209	新鼎	0
5210	寶碩	0
5211	蒙恬	0
5212	凌網	0
5213	亞昕	0
5215	科嘉-KY	1
5220	萬達光電	0
5222	全訊	1
5223	安力-KY	0
5225	東科-KY	1
5227	立凱-KY	0
5228	鈺鎧	0
5230	雷笛克光學	0
5233	有量	2
5234	達興材料	1
5236	凌陽創新	0
5240	建騰	2
5243	乙盛-KY	1
5244	弘凱	1
5245	智晶	0
5246	勵威電子	2
5248	景傳	2
5251	天鉞電	0
5254	欣訊科技	2
5258	虹堡	1
5262	立達	2
5263	智崴	0
5267	龍翩	2
5269	祥碩	1
5271	紘通	2
5272	笙科	0
5274	信驊	0
5276	達輝-KY	0
5277	葳天	2
5278	尚凡國際	0
5283	禾聯碩	1
5284	jpp-KY	1
5285	界霖	1
5287	數字	0
5288	豐祥-KY	1
5289	宜鼎	0
5291	邑昇	0
5292	華懋	1
5297	廣化	2
5299	杰力	0
5301	寶得利	0
5302	太欣	0
5306	桂盟	1
5309	系統電	0
5310	天剛	0
5312	寶島科	0
5314	世紀	0
5315	光聯	0
5321	美而快	0
5324	士開	0
5328	華容	0
5340	建榮	0
5344	立衛	0
5345	天揚	0
5347	世界	0
5348	正能量智能	0
5351	鈺創	0
5353	台林	0
1101	台泥	1
5355	佳總	0
5356	協益	0
5364	力麗店	0
5371	中光電	0
5381	合正	0
5386	青雲	0
5388	中磊	1
5392	能率	0
5398	慕康生醫	0
5403	中菲	0
5410	國眾	0
5425	台半	0
5426	振發	0
5432	新門	0
5434	崇越	1
5438	東友	0
5439	高技	0
5443	均豪	0
5450	南良	0
5452	佶優	0
5455	昇益	0
5457	宣德	0
5460	同協	0
5464	霖宏	0
5465	富驊	0
5468	凱鈺	0
5469	瀚宇博	1
5471	松翰	1
5474	聰泰	0
5475	德宏	0
5478	智冠	0
5481	新華	0
5483	中美晶	0
5484	慧友	1
5487	通泰	0
5488	松普	0
5489	彩富	0
5490	同亨	0
5493	三聯	0
5498	凱崴	0
5508	永信建	0
5511	德昌	0
5512	力麒	0
5514	三豐	0
5515	建國	1
5516	雙喜	0
5519	隆大	1
5520	力泰	0
5521	工信	1
5522	遠雄	1
5523	豐謙	0
5525	順天	1
5529	鉅陞	0
5530	龍巖	0
5531	鄉林	1
5533	皇鼎	1
5534	長虹	1
5536	聖暉*	0
5538	東明-KY	1
5543	桓鼎-KY	0
5546	永固-KY	1
5547	久舜	2
5548	安倉	0
5601	台聯櫃	0
5603	陸海	0
5604	中連	0
5607	遠雄港	1
5608	四維航	1
5609	中菲行	0
5701	劍湖山	0
5703	亞都	0
5704	老爺知	0
5706	鳳凰	1
5859	遠壽	2
5863	瑞興銀	2
5864	致和證	0
5871	中租-KY	1
5876	上海商銀	1
5878	台名	0
5880	合庫金	1
5902	德記	0
5903	全家	0
5904	寶雅	0
5905	南仁湖	0
5906	台南-KY	1
5907	大洋-KY	1
6005	群益證	1
6015	宏遠證	0
6016	康和證	0
6020	大展證	0
6021	美好證券	0
6023	元大期	0
6024	群益期	1
6026	福邦證	0
6027	德信	2
6028	公勝保經	2
6035	悠遊卡	2
6101	寬魚國際	0
6103	合邦	0
6104	創惟	0
6108	競國	1
6109	亞元	0
6111	大宇資	0
6112	邁達特	1
6113	亞矽	0
6114	久威	0
6115	鎰勝	1
6116	彩晶	1
6117	迎廣	1
6118	建達	0
6120	達運	1
6121	新普	0
6122	擎邦	0
2432	倚天酷碁-創	1
6123	上奇	0
6124	業強	0
6125	廣運	0
6126	信音	0
6127	九豪	0
6128	上福	1
6129	普誠	0
6130	上亞科技	0
6133	金橋	1
6134	萬旭	0
6136	富爾特	1
6138	茂達	0
6139	亞翔	1
6140	訊達	0
6141	柏承	1
6142	友勁	1
6143	振曜	0
6144	得利影	0
6146	耕興	0
6147	頎邦	0
6148	驊宏資	0
6150	撼訊	0
6151	晉倫	0
6152	百一	1
6153	嘉聯益	1
6154	順發	0
6155	鈞寶	1
6156	松上	0
6158	禾昌	0
6160	欣技	0
6161	捷波	0
6163	華電網	0
6164	華興	1
6165	浪凡	1
6166	凌華	1
6167	久正	0
6168	宏齊	1
6169	昱泉	0
6170	統振	0
6171	大城地產	0
6173	信昌電	0
6175	立敦	0
6176	瑞儀	1
6177	達麗	1
6179	亞通	0
6180	橘子	0
6182	合晶	0
6183	關貿	1
6184	大豐電	1
6185	幃翔	0
6186	新潤	0
6187	萬潤	0
6188	廣明	0
6189	豐藝	1
6190	萬泰科	0
6191	精成科	1
6192	巨路	1
6194	育富	0
6195	詩肯	0
6196	帆宣	1
1513	中興電	1
6197	佳必琪	1
6198	瑞築	0
6199	天品	0
6201	亞弘電	1
6202	盛群	1
6203	海韻電	0
6204	艾華	0
6205	詮欣	1
6206	飛捷	1
6207	雷科	0
6208	日揚	0
6209	今國光	1
6210	慶生	0
6212	理銘	0
6213	聯茂	1
6214	精誠	1
6215	和椿	1
6216	居易	1
6217	中探針	0
6218	豪勉	0
6219	富旺	0
6220	岳豐	0
6221	晉泰	0
6222	上揚	0
6223	旺矽	0
6224	聚鼎	1
6225	天瀚	1
6226	光鼎	1
6227	茂綸	0
6228	全譜	0
6229	研通	0
6230	尼得科超眾	1
6231	系微	0
6233	旺玖	0
6234	高僑	0
6235	華孚	1
6236	中湛	0
6237	驊訊	0
6239	力成	1
6240	松崗	0
6241	易通展	0
6242	立康	0
6243	迅杰	1
6244	茂迪	0
6245	立端	0
6246	臺龍	0
6248	沛波	0
6257	矽格	1
6259	百徽	0
6261	久元	0
6263	普萊德	0
6264	富裔	0
6266	泰詠	0
6269	台郡	1
6270	倍微	0
6271	同欣電	1
6272	驊陞	2
6274	台燿	0
6275	元山	0
6276	安鈦克	0
6277	宏正	1
6278	台表科	1
6279	胡連	0
6281	全國電	1
6282	康舒	1
6283	淳安	1
6284	佳邦	0
6287	元隆	0
6288	聯嘉	1
6290	良維	0
6291	沛亨	0
6292	迅德	0
6294	智基	0
6403	群登	2
6405	悅城	1
6407	相互	2
6409	旭隼	1
6411	晶焱	0
6412	群電	1
6414	樺漢	1
6415	矽力*-KY	1
6416	瑞祺電通	1
6417	韋僑	0
6418	詠昇	0
6419	京晨科	0
6423	億而得-創	1
6425	易發	0
6426	統新	1
6428	淘米	2
6431	光麗-KY	1
6432	今展科	0
6434	達輝光電	2
6435	大中	0
6438	迅得	1
6441	廣錠	0
6442	光聖	1
6443	元晶	1
6446	藥華藥	1
6449	鈺邦	1
6451	訊芯-KY	1
6456	GIS-KY	1
6457	紘康	0
6461	益得	0
6462	神盾	0
6464	台數科	1
6465	威潤	0
6469	大樹	0
6470	宇智	0
6472	保瑞	1
6473	美賣*	2
3011	今晧	1
6474	華豫寧	2
6477	安集	1
6482	弘煜科	0
6483	原創生醫	2
6485	點序	0
6486	互動	0
6488	環球晶	0
6491	晶碩	1
6492	生華科	0
6493	雷虎生	2
6494	九齊	0
6495	納諾＊-KY	2
6496	科懋	0
6498	久禾光	2
6499	益安	0
6504	南六	1
6505	台塑化	1
6508	惠光	0
6509	聚和	0
6510	精測	0
6512	啟發電	0
6514	芮特-KY	0
6515	穎崴	1
6516	勤崴國際	0
6517	保勝光學	0
6518	康科特	2
6523	達爾膚	0
6525	捷敏-KY	1
6526	達發	1
6527	明達醫	0
6530	創威	0
6531	愛普*	1
6532	瑞耘	0
6533	晶心科	1
6534	正瀚-創	1
6535	順藥	0
6536	碩豐	2
6538	倉和	0
6539	麗彤	2
6541	泰福-KY	1
6542	隆中	0
6543	普惠醫工	2
6546	正基	0
6547	高端疫苗	0
6548	長科*	0
6549	景凱	2
6550	北極星藥業-KY	1
6552	易華電	1
6555	榮炭	2
6556	勝品	0
6558	興能高	1
6559	研晶	2
6560	欣普羅	0
6561	是方	0
6564	安特羅	2
6565	物聯	2
6568	宏觀	0
6569	醫揚	0
6570	維田	0
6572	博錸	2
2028	威致	1
6573	虹揚-KY	1
6574	霈方	0
6576	逸達	0
6577	勁豐	0
6578	達邦蛋白	0
6579	研揚	1
6580	台睿	2
6581	鋼聯	1
6582	申豐	1
6583	友松	2
6584	南俊國際	0
6585	鼎基	1
6586	醣基	2
6588	東典光電	0
6589	台康生技	0
6590	普鴻	0
6591	動力-KY	1
6592	和潤企業	1
6593	台灣銘板	0
6595	光禹國際	2
6596	寬宏藝術	0
6597	立誠	2
6598	ABC-KY	1
6599	普達系統	2
6603	富強鑫	0
6605	帝寶	1
6606	建德工業	1
6609	瀧澤科	0
6610	安成生技	2
6612	奈米醫材	0
6613	朋億*	0
6615	慧智	0
6616	特昇-KY	0
6617	共信-KY	0
6618	永虹先進	2
6620	漢達	2
6621	華宇藥	2
6622	百聿數碼	2
6624	萬年清	0
6625	必應	1
6626	唯數	2
6629	泰金-KY	0
6634	欣耀	2
6637	醫影	0
6638	沅聖	2
6639	源大環能	2
6640	均華	0
6641	基士德-KY	1
6642	富致	0
6643	M31	0
6645	金萬林-創	1
6648	斯其大	2
6649	台生材	0
6650	帝圖	2
6651	全宇昕	0
6652	雅祥生醫	2
6654	天正國際	0
6655	科定	1
6657	華安	1
6658	聯策	1
6661	威健生技	0
6662	樂斯科	0
6664	群翊	0
6665	康聯生醫	2
6666	羅麗芬-KY	1
6667	信紘科	0
6668	中揚光	1
6669	緯穎	1
6670	復盛應用	1
6671	三能-KY	1
6672	騰輝電子-KY	1
6673	和詮	2
6674	鋐寶科技	1
6676	祥翊	2
6677	瑩碩生技	2
6679	鈺太	0
6680	鑫創電子	0
6682	華旭矽材	2
6683	雍智科技	0
6684	安格	0
6689	伊雲谷	1
6691	洋基工程	1
6692	進能服	0
6693	廣閎科	0
6695	芯鼎	1
6696	仁新	2
6697	東捷資訊	0
6698	旭暉應材	1
6699	奇邑	2
6703	軒郁	0
6704	國璽幹細胞	2
6705	振躍精密	2
6706	惠特	1
6707	富基電通	2
6708	天擎	0
6709	昱厚生技	2
6712	長聖	0
6715	嘉基	1
6716	應廣	0
6719	力智	1
6720	久昌	2
6721	信實	0
6722	輝創	2
6723	傑智環境	2
6725	矽科宏晟	2
6727	亞泰金屬	0
6728	上洋	0
6729	機光科技	2
6730	常廣	2
6732	昇佳電子	0
6733	博晟生醫	0
6734	安盛生	2
6735	美達科技	0
6738	鼎恒	2
6737	秀育	2
6739	竹陞科技	2
6741	91APP*-KY	0
6742	澤米	1
6743	安普新	1
6744	豐技生技	2
6747	亨泰光	0
6748	亞果生醫	2
6750	泰創工程	2
6751	智聯服務	0
6752	叡揚	0
6753	龍德造船	1
6754	匯僑設計	1
6755	連鋐科技	2
6756	威鋒電子	1
6757	台灣虎航-創	1
6758	冠亞	2
6761	穩得	0
6762	達亞	0
6763	綠界科技	0
6764	亞洲教育	2
6767	台微醫	0
6768	志強-KY	1
6770	力積電	1
6771	平和環保-創	1
6775	穎台科技	2
6780	學習王	2
6781	AES-KY	1
6782	視陽	1
6784	天凱科技	2
6785	昱展新藥	0
6786	芯測	2
6787	晶瑞光	2
6788	華景電	0
6789	采鈺	1
6790	永豐實	1
6791	虎門科技	0
6792	詠業	1
6793	天力離岸	2
6794	向榮生技-創	1
6796	晉弘	1
6797	圓點奈米	2
6798	展逸	2
6799	來頡	1
6803	崑鼎	0
6804	明係	0
6805	富世達	1
6806	森崴能源	1
6807	峰源-KY	1
6808	三鼎生技	2
6810	新穎生醫	2
6812	梭特	2
6814	路迦生醫	2
6815	晶鑽生醫	2
6816	捷智商訊	2
6817	溫士頓	2
6818	連騰	2
6819	眾智	2
6820	連訊	2
6821	聯寶	0
6823	濾能	0
6825	和暢科技	2
6826	和淞	2
6827	巨生醫	2
6829	千附精密	0
6830	汎銓	1
6831	邁科	2
6832	金鼎科	2
6833	太康精密	2
6834	天二科技	1
6835	圓裕	1
6839	開陽能源	2
6840	東研信超	0
6841	長佳智能	0
6842	一元素	2
6843	進典	0
6844	諾貝兒	0
6846	綠茵	0
6847	普瑞博	2
6848	拉法醫	2
6850	光鼎生技	2
6854	錼創科技－ＫＹ創	1
6855	數泓科	0
6856	鑫傳	0
6858	愛比科技	2
6859	伯特光	0
6861	睿生光電	1
6863	永道-KY	1
6864	元樟生技	2
6865	偉康科技	0
6867	坦德科技	2
6868	采威國際	2
6869	雲豹能源	1
6870	騰雲	0
6872	浩宇生醫	2
6873	泓德能源-創	1
6874	倍力	0
6875	國邑*	0
6876	朗齊生醫*	2
6877	鏵友益	0
6878	歐付寶	2
6879	大江基因	2
6881	潤德	0
6882	甲尚	2
6884	海柏特	2
6885	全福生技	2
6886	遠東生技	2
6890	來億-KY	1
6891	樂迦再生	2
6892	台寶生醫	2
6894	衛司特	0
6895	宏碩系統	0
6898	程曦資訊	2
6899	創為精密	0
6901	鑽石投資	1
6902	走著瞧-創	1
6903	巨漢	0
6904	伯鑫	0
6906	現觀科	1
6909	創控	2
6910	德鴻科技	2
6911	群運	2
6912	益鈞環科*	2
6913	鴻呈	2
6914	阜爾運通	1
6915	美強光	2
6916	華凌	1
6917	竟天	2
6918	愛派司	2
6919	康霈*	2
6920	恆勁科技	2
6922	宸曜	0
6923	中台	2
6925	意藍	2
6926	聖安生醫	2
6927	聯合聚晶	2
6928	攸泰科技	1
6929	佑全	0
6931	青松健康	2
6932	水星生醫*	2
6933	AMAX-KY	1
6934	心誠鎂	2
6935	王子製藥	2
6936	永鴻生技	2
6937	天虹	1
6938	藍新資訊	2
6939	啟弘生技	2
6940	格斯科技*	2
6944	兆聯實業	2
6945	圓祥生技	2
6947	台鎔科技	2
6949	沛爾生醫-創	1
6951	青新-創	1
6952	大武山	1
6953	家碩	0
6955	邦睿生技	2
6957	裕慶-KY	1
6958	日盛台駿	2
6959	兆捷科技	2
6961	旅天下	2
6963	品元	2
6967	汎瑋材料	2
6968	萬達寵物	2
6971	惠民實業	2
6973	永立榮	2
6976	育世博-KY	2
6977	聯純	2
6979	勝釩	2
6980	鐳洋科技	2
6982	大井泵浦	2
6983	華洋精機	2
6984	交流資服	2
6986	和迅	2
6990	華鉬	2
6994	富威電力	2
6996	力領科技	2
6997	博弘	2
7402	邑錡	0
7419	達勝	2
7427	華上生醫	2
7443	凡事康	2
7507	環拓科技	2
7516	清淨海	2
7530	鋒魁科技	2
7547	碩網	2
7551	知識科技	2
7555	美萌	2
7556	意德士	0
7558	群利	2
7561	光晟生技	2
7562	博來科技	2
7566	亞果遊艇	2
7575	安美得	2
7578	利百景	2
7583	國際海洋	2
7590	怡和國際	2
7595	世基生醫	2
7607	通用幹細胞*	2
7610	聯友金屬	2
7631	聚賢研發	2
7642	昶瑞機電	2
7702	前端風電	2
7703	銳澤	2
7704	明遠精密	2
7705	三商餐飲	2
7707	益芯科	2
7708	全家餐飲	2
7709	榮田	2
7712	博盛半導體	2
7713	威力德生醫	2
7714	創泓科技	2
7715	裕山	2
7716	昱臺國際	2
7718	友鋮	2
7719	碳基	2
7721	微程式	2
7722	LINEPAY	2
7723	築間	2
7725	列特博	2
7726	暄達	2
7728	光焱科技	2
7729	仲恩生醫	2
7730	暉盛	2
7732	金興精密	2
7734	印能科技	2
7736	虎山	2
7738	東聯互動	2
7742	天弘化	2
7584	樂意	0
7743	金利食安	2
7747	昕奇雲端	2
7750	新代	2
7751	竑騰	2
7752	世紀樺欣	2
7754	安基生技	2
7757	金色三麥	2
8011	台通	1
8016	矽創	1
8021	尖點	1
8024	佑華	0
8027	鈦昇	0
8028	昇陽半導體	1
8032	光菱	0
8033	雷虎	1
8034	榮群	0
8038	長園科	0
8039	台虹	1
8040	九暘電	0
8041	東元精電	2
8042	金山電	0
8043	蜜望實	0
8044	網家	0
8045	達運光電	2
8046	南電	1
8047	星雲	0
8048	德勝	0
8049	晶采	0
8050	廣積	0
8054	安國	0
8059	凱碩	0
8064	東捷	0
8066	來思達	0
8067	志旭	0
8068	全達	0
8069	元太	0
8070	長華*	1
8071	能率網通	0
8072	陞泰	1
8074	鉅橡	0
8076	伍豐	0
8080	印鉐	0
8081	致新	1
8083	瑞穎	0
8084	巨虹	0
8085	福華	0
8086	宏捷科	0
8087	華鎂鑫	0
8088	品安	0
8089	康全電訊	0
8091	翔名	0
8092	建暐	0
8093	保銳科	0
8096	擎亞電子	0
8097	常珵	0
8098	慶康科技	2
8099	大世科	0
8101	華冠	1
8102	傑霖科技	2
8103	瀚荃	1
8104	錸寶	1
8105	凌巨	1
8107	大億金茂	0
8109	博大	0
8110	華東	1
8112	至上	1
8114	振樺電	1
8119	公信	2
8121	越峰	0
8131	福懋科	1
8147	正淩	0
8150	南茂	1
8155	博智	0
8272	全景軟體	2
8298	威睿	2
8329	台視	2
8359	錢櫃	2
8390	金益鼎	0
8401	白紗科	0
8403	盛弘	0
8404	百和興業-KY	1
8409	商之器	0
8410	森田	0
8411	福貞-KY	1
8415	大國鋼	0
8416	實威	0
8420	明揚	0
8421	旭源	0
8422	可寧衛	1
8423	保綠-KY	0
8424	惠普	0
8426	紅木-KY	0
8429	金麗-KY	1
8431	匯鑽科	0
8432	東生華	0
8433	弘帆	0
8435	鉅邁	0
8436	大江	0
8437	大地-KY	0
8438	昶昕	1
8440	綠電	0
8442	威宏-KY	1
8443	阿瘦	1
8444	綠河-KY	0
8446	華研	0
8450	霹靂	0
8454	富邦媒	1
8455	大拓-KY	0
8458	影一	2
8462	柏文	1
8463	潤泰材	1
8464	億豐	1
8467	波力-KY	1
8472	夠麻吉	0
8473	山林水	1
8476	台境	1
8477	創業家	0
8478	東哥遊艇	1
8481	政伸	1
8482	商億-KY	1
8487	愛爾達-創	1
8488	吉源-KY	1
8489	三貝德	0
8499	鼎炫-KY	1
8905	裕國	0
8906	花王企業	0
8908	欣雄	0
8916	光隆	0
8917	欣泰	0
8921	沈氏印刷	0
8923	時報	0
8924	大田	0
8926	台汽電	1
8927	北基	0
8928	鉅明	0
8929	富堡	0
8930	青鋼	0
8931	大汽電	0
8932	智通	0
8933	愛地雅	0
8935	邦泰	0
8936	國統	0
8937	合騏	0
8938	明安	0
8940	新天地	1
8941	關中	0
8942	森鉅	0
8996	高力	1
9103	美德醫療-DR	1
9105	泰金寶-DR	1
9110	越南控-DR	1
9136	巨騰-DR	1
9802	鈺齊-KY	1
9902	台火	1
9904	寶成	1
9905	大華	1
9906	欣巴巴	1
9907	統一實	1
9908	大台北	1
9910	豐泰	1
9911	櫻花	1
9912	偉聯	1
9914	美利達	1
9917	中保	1
9918	欣天然	1
9919	康那香	1
9921	巨大	1
9924	福興	1
9925	新保	1
9926	新海	1
9927	泰銘	1
9928	中視	1
9929	秋雨	1
9930	中聯資源	1
9931	欣高	1
9933	中鼎	1
9934	成霖	1
9935	慶豐富	1
9937	全國	1
9938	百和	1
9939	宏全	1
9940	信義	1
9941	裕融	1
9942	茂順	1
9943	好樂迪	1
9944	新麗	1
9945	潤泰新	1
9946	三發地產	1
9949	琉園	0
9950	萬國通	0
9951	皇田	0
9955	佳龍	1
9957	燁聯	2
9958	世紀鋼	1
9960	邁達康	0
9962	有益	0
3046	建碁	1
4305	世堃	0
4527	堃霖	0
6174	安碁	0
6265	堃昶	0
6285	啟碁	1
6506	双邦	0
6690	安碁資訊	0
6776	展碁國際	1
6811	宏碁資訊	0
6857	宏碁智醫	2
6908	宏碁遊戲	2
7706	宏碁創達	2
8077	洛碁	0
8111	立碁	0
8466	美喆-KY	1
0001	荷銀鴻運	\N
0029	富邦店	\N
0054	元大台商50	\N
0058	富邦發達	\N
0059	富邦金融	\N
0060	新台灣	\N
1258	其祥-KY	\N
1262	綠悅-KY	\N
1311	福聚	\N
1450	新藝纖維	\N
1462	東雲	\N
1520	復盛	\N
1523	臺灣開億	\N
1575	國直	\N
1704	榮化	\N
1724	台硝	\N
1787	福盈科	\N
1791	光惠	\N
1794	露絲科	\N
1902	台紙	\N
2112	奇菱	\N
2229	富海	\N
2335	清三電子	\N
2336	致伸	\N
2350	環電	\N
2394	普立爾	\N
2416	世平	\N
2446	全懋	\N
2447	鼎新	\N
2452	乾坤	\N
2469	力信	\N
2470	品佳	\N
2473	思源	\N
2526	大陸工程	\N
2577	亞昕	\N
2591	高逸	\N
2592	志品	\N
2626	凌天	\N
2628	正利	\N
2714	華國	\N
2717	易遊網	\N
2735	鄉村	\N
2807	竹商銀	\N
2825	中央產險	\N
2920	海景	\N
2921	和樂	\N
2928	紅馬-KY	\N
3009	奇美電	\N
3012	廣輝	\N
3020	奇普仕	\N
3053	鼎營	\N
3063	飛信	\N
3074	群環	\N
3075	億泰利	\N
3082	視通	\N
3084	光威	\N
3087	翔準	\N
3109	精鼎科	\N
3127	能元	\N
3137	瑞積	\N
3144	新揚科	\N
3146	真通	\N
3195	統寶	\N
3214	元砷	\N
3216	東雅電	\N
3222	奇景	\N
3237	永洋	\N
3239	帝華	\N
3267	泰陞	\N
3270	威瀚	\N
3271	其樂達	\N
3282	商杰	\N
3283	益進	\N
3307	遠業	\N
3328	亞微電	\N
3331	新像科	\N
3348	中華聯	\N
3350	邰港	\N
3365	米輯	\N
3366	威播	\N
3389	志遠	\N
3394	龍泰	\N
3396	普樺	\N
3397	協泰	\N
3404	儷耀	\N
3408	常憶	\N
3414	榮眾	\N
3422	億泰興	\N
3423	聚興	\N
3429	彥陽	\N
3435	德之傑	\N
3442	宇力	\N
3452	益通	\N
3469	銓祐科	\N
3470	恆碩	\N
3474	華亞科	\N
3475	富晶	\N
3482	智成	\N
3496	大朋電	\N
3502	鉅航	\N
3503	東又悅	\N
3505	聯線上	\N
3506	友昱	\N
3517	洲磊	\N
3538	曜鵬	\N
3542	芽莊	\N
3544	宣茂科	\N
3547	凱鼎	\N
3549	光宸	\N
3553	力積	\N
3559	全智科	\N
3560	建欣科	\N
3568	馥鴻	\N
3571	兆宏	\N
3573	穎台	\N
3575	琉明	\N
3578	義發	\N
3582	凌耀	\N
3586	鋐達	\N
3598	奕力	\N
3604	立碁光	\N
3606	特佳	\N
3612	富鴻齊	\N
3619	冠輝	\N
3635	晶量	\N
3638	Ｆ－ＩＭＬ	\N
3649	長裕	\N
3658	漢微科	\N
4103	百略	\N
4124	期美科技	\N
4125	喬聯	\N
4134	台欣生	\N
4140	康富	\N
4141	龍燈-KY	\N
4144	康聯-KY	\N
4152	台微體	\N
4180	安成藥	\N
4181	百丹特	\N
4725	信昌化	\N
4727	德邑醫學	\N
4734	正揚	\N
4752	聯超	\N
4758	桐寶	\N
4805	華聯	\N
4910	陽慶	\N
4913	宇辰	\N
4921	宏陽	\N
4922	桑緹亞	\N
4932	瑞晶	\N
4941	晶積	\N
4947	昂寶-KY	\N
4963	八陽	\N
4990	晶美	\N
5017	新泰伸	\N
5102	富強	\N
5204	得捷	\N
5219	瑪居禮	\N
5221	合晶光	\N
5231	鑫晶鑽	\N
5239	宏鈺	\N
5261	創傑	\N
5264	鎧勝-KY	\N
5265	琉明斯	\N
5266	Ｆ＊ＡＳ	\N
5275	誠加	\N
5282	富昱科	\N
5294	鋒霖	\N
5296	台矽能	\N
5305	敦南	\N
5318	威豪	\N
5325	大騰電子	\N
5336	華特電子	\N
5395	圓方	\N
5467	聯福生	\N
5480	統盟	\N
5492	亞智科	\N
5605	遠東航空	\N
5820	日盛金	\N
6022	大眾證	\N
6038	街口電支	\N
6105	瑞傳	\N
6110	艾群	\N
6159	詮鼎	\N
6178	振遠	\N
6211	福登	\N
6247	淇譽電	\N
6256	華傑	\N
6268	華普	\N
6280	崇貿	\N
6286	立錡	\N
6293	國威	\N
6297	祥德	\N
6401	助群	\N
6402	基泰營	\N
6422	君耀-KY	\N
6429	華德	\N
6445	經絡醫	\N
6453	健永	\N
6471	聯生藥	\N
6487	源一	\N
6507	新力美	\N
6553	豐華	\N
6554	中美冠科-KY	\N
6604	儒億	\N
6614	資拓宏宇	\N
6632	向上國際	\N
6681	宏星技術	\N
8008	建興電	\N
8010	益和	\N
8017	展茂	\N
8026	康和資訊	\N
8035	聯測	\N
8036	光華	\N
8037	聖立	\N
8052	台腦	\N
8055	大紘	\N
8060	力竑	\N
8078	華寶	\N
8079	誠遠	\N
8127	利汎	\N
8130	聯達	\N
8143	晶揚	\N
8172	勝開	\N
8191	洲磊	\N
8204	德恩富	\N
8219	光林電	\N
8221	慧榮	\N
8259	捷耀	\N
8264	台視訊	\N
8266	中日新	\N
8325	中廣	\N
8361	金協昌	\N
8393	格上	\N
8406	金可-KY	\N
8418	捷必勝-KY	\N
8480	泰昇-KY	\N
8485	信吉媒	\N
8497	格威傳媒	\N
8705	東隆五金	\N
8707	中精機	\N
8913	全銓	\N
8945	大新店	\N
9104	萬宇科	\N
9106	新焦點-DR	\N
9151	旺旺	\N
9157	陽光能源-DR	\N
9188	精熙-DR	\N
9915	億豐綜合	\N
9965	永儲	\N
910322	康師傅控股有限公司	1
910861	神州數碼控股有限公司	1
911608	明輝環球企業有限公司	1
911622	聚亨企業集團(泰國)大眾有限公司	1
911868	同方友友控股有限公司	1
912000	晨訊科技集團有限公司	1
0050	元大台灣50	1
0051	元大中型100	1
0052	富邦科技	1
0053	元大電子	1
0055	元大MSCI金融	1
0056	元大高股息	1
0057	富邦摩台	1
0061	元大寶滬深	1
1260	富味鄉	2
1269	乾杯	2
1271	晨暉生技	2
1293	利統	2
1294	漢田生技	2
1295	生合	2
1343	旭東環保	2
1480	東盟開發	2
1507	日立永大	\N
1763	奇美實業	\N
1775	大東樹脂	\N
1816	富元	\N
1818	願景	\N
2068	優頻	\N
2319	大眾電腦	\N
2759	咖碼	\N
2940	歐都納	\N
3166	偉僑	\N
3241	大船	\N
3280	龍生	\N
3334	主向位	\N
3347	泰鼎	\N
3368	歐驊	\N
3386	新德科技	\N
3387	彰德	\N
3462	瑋鋒科技	\N
3463	宏塑	\N
3566	太陽光	\N
3616	泓辰材料	\N
3676	ＧＩＨ	\N
3681	實盈	\N
3683	仲博科技	\N
4149	ＫＹ宜佰	\N
4522	大寶精工	\N
4547	主新德	\N
4585	達明	\N
4590	富田電機	\N
4769	謙華	\N
4962	龍翰科技	\N
4985	富相	\N
5218	達運精密	\N
5250	奇岩綠能	\N
5839	開發銀	\N
5848	台新銀行	\N
5867	康和期	\N
6003	大華證	\N
6030	樂天銀行	\N
6031	連線銀行	\N
6039	將來銀	\N
6040	全支付	\N
6299	文麥	\N
1585	鎧鉅	2
1594	日高	2
1623	大東電	2
1780	立弘	2
2071	震南鐵	2
2072	世紀風電	2
2237	華德動能	2
2245	詠勝昌	2
6427	弘勝光電	\N
6467	泰合藥	\N
6484	惠合	\N
6502	國隆纖維	\N
6587	鑫聖	\N
6608	慶鴻	\N
6633	萊鎂醫	\N
6659	天明製藥	\N
6726	鑫亞電通	\N
6736	碩辣椒	\N
6746	全科綜電	\N
6749	遊購	\N
6760	金品軒鍊金廠	\N
6772	錫安生技	\N
6778	安麗莎醫療器材	\N
6779	鋐昇	\N
6822	台創材	\N
6824	信力生	\N
6849	奇鼎科技	\N
6851	立視科	\N
6866	中大生醫	\N
6871	新鑫	\N
6897	極上教育	\N
6905	榮福	\N
6921	嘉雨思科技	\N
6930	盛新材料	\N
6942	威達高科	\N
6950	KKT-KY	\N
6954	全盈支付	\N
6969	成信實業	\N
6972	博瑞達	\N
6978	愛盛科技	\N
6985	台新人壽	\N
6992	萬勝發科技	\N
6993	凱納	\N
6995	野獸國	\N
7421	易宏	\N
7426	亞基科	\N
7570	紫金堂	\N
7720	火星控股	\N
7746	智安	\N
7748	鑫囍創業	\N
7753	星亞	\N
7758	睿騰能源	\N
7759	視航生醫	\N
7760	享溫馨	\N
7763	崇舜	\N
8157	創圓	\N
8197	研能科技	\N
8343	科學城	\N
8391	森霸電力	\N
8427	基勝-KY	\N
8452	北都數位	\N
8475	新彰	\N
9964	欣泉投資	\N
6404	通訊-KY	\N
8349	恒耀	0
8162	微矽電子-創	1
8163	達方	1
8171	天宇	0
8176	智捷	0
8182	加高	0
8183	精星	0
8201	無敵	1
8210	勤誠	1
8213	志超	1
8215	明基材	1
8222	寶一	1
8227	巨有科技	0
8234	新漢	0
8240	華宏	0
8249	菱光	1
8255	朋程	0
8261	富鼎	1
8271	宇瞻	1
8277	商丞	0
8279	生展	0
8284	三竹	0
8289	泰藝	0
8291	尚茂	0
8299	群聯	0
8341	日友	1
8342	益張	0
8354	冠好	0
8358	金居	0
8367	建新國際	1
8374	羅昇	1
8383	千附	0
4806	桂田文創	0
7731	火星生技*	2
6883	微電能源	2
7767	仁大	\N
2363	矽統	1
7762	吉晟生	2
8491	真好玩	\N
6529	圖霸	\N
6635	瑞利智工	\N
6999	瀚醫生技	2
7756	多那之	2
7744	崴寶	\N
7765	中華資安	\N
7724	諾亞克	\N
7761	三大未來科技	\N
2341	英群	\N
7710	東擎科技	\N
7711	永擎電子	\N
6838	台新藥	1
\.


--
-- TOC entry 3438 (class 0 OID 16415)
-- Dependencies: 221
-- Data for Name: user_account; Type: TABLE DATA; Schema: bstock; Owner: bstockuser
--

COPY bstock.user_account (id, phone, mail, gender, birth_date, role_id, create_time, update_time, update_by, user_password, status, user_name) FROM stdin;
1	0938017103	a26404691@gmail.com	M	1993-04-10	1	\N	\N	\N	$2a$12$xuPo14rKp10B3/85Jy30Buj5WzcGRriFoE4sosIQUgNIqymcjKiQC	\N	\N
20240624131612609e6d81	12345678901	a6031000@gmail.com	M	2024-06-24	3	2024-06-24 05:04:05.188+00	2024-06-24 13:16:12.592+00	\N	$2a$10$lIxRdYWLC9L.j4Bz0AxbX.37i6RJH7oHQHvPGSPr4Jj8pCRIPwP7u	1	John Doe
\.


--
-- TOC entry 3281 (class 2606 OID 16431)
-- Name: stock_info StockInfo_pkey; Type: CONSTRAINT; Schema: bstock; Owner: bstockuser
--

ALTER TABLE ONLY bstock.stock_info
    ADD CONSTRAINT "StockInfo_pkey" PRIMARY KEY (stock_code);


--
-- TOC entry 3272 (class 2606 OID 16433)
-- Name: securities_firms_day_operate securities_firms_day_operate_pk; Type: CONSTRAINT; Schema: bstock; Owner: bstockuser
--

ALTER TABLE ONLY bstock.securities_firms_day_operate
    ADD CONSTRAINT securities_firms_day_operate_pk PRIMARY KEY (stock_code, trading_date, seq);


--
-- TOC entry 3274 (class 2606 OID 16435)
-- Name: shareholder_structure shareholder_structure_pk; Type: CONSTRAINT; Schema: bstock; Owner: bstockuser
--

ALTER TABLE ONLY bstock.shareholder_structure
    ADD CONSTRAINT shareholder_structure_pk PRIMARY KEY (id);


--
-- TOC entry 3278 (class 2606 OID 16437)
-- Name: stock_day_price stock_day_price_unique; Type: CONSTRAINT; Schema: bstock; Owner: bstockuser
--

ALTER TABLE ONLY bstock.stock_day_price
    ADD CONSTRAINT stock_day_price_unique UNIQUE (stock_code, trading_day);


--
-- TOC entry 3286 (class 2606 OID 16439)
-- Name: user_account user_account_pk; Type: CONSTRAINT; Schema: bstock; Owner: bstockuser
--

ALTER TABLE ONLY bstock.user_account
    ADD CONSTRAINT user_account_pk PRIMARY KEY (id);


--
-- TOC entry 3288 (class 2606 OID 16441)
-- Name: user_account user_account_unique; Type: CONSTRAINT; Schema: bstock; Owner: bstockuser
--

ALTER TABLE ONLY bstock.user_account
    ADD CONSTRAINT user_account_unique UNIQUE (mail);


--
-- TOC entry 3270 (class 1259 OID 16442)
-- Name: role_info_id_idx; Type: INDEX; Schema: bstock; Owner: bstockuser
--

CREATE INDEX role_info_id_idx ON bstock.role_info USING btree (id);


--
-- TOC entry 3275 (class 1259 OID 16443)
-- Name: shareholder_structure_stock_code_idx; Type: INDEX; Schema: bstock; Owner: bstockuser
--

CREATE INDEX shareholder_structure_stock_code_idx ON bstock.shareholder_structure USING btree (stock_code);


--
-- TOC entry 3276 (class 1259 OID 16444)
-- Name: shareholder_structure_week_of_year_idx; Type: INDEX; Schema: bstock; Owner: bstockuser
--

CREATE INDEX shareholder_structure_week_of_year_idx ON bstock.shareholder_structure USING btree (week_of_year);


--
-- TOC entry 3279 (class 1259 OID 16445)
-- Name: stock_day_price_week_of_year_idx; Type: INDEX; Schema: bstock; Owner: bstockuser
--

CREATE INDEX stock_day_price_week_of_year_idx ON bstock.stock_day_price USING btree (week_of_year);


--
-- TOC entry 3289 (class 1259 OID 16458)
-- Name: stock_exchange_detail_stock_code_idx; Type: INDEX; Schema: bstock; Owner: bstockuser
--

CREATE INDEX stock_exchange_detail_stock_code_idx ON bstock.stock_exchange_detail USING btree (stock_code, trading_date);


--
-- TOC entry 3282 (class 1259 OID 16446)
-- Name: user_account_id_idx; Type: INDEX; Schema: bstock; Owner: bstockuser
--

CREATE INDEX user_account_id_idx ON bstock.user_account USING btree (id);


--
-- TOC entry 3283 (class 1259 OID 16447)
-- Name: user_account_mail_idx; Type: INDEX; Schema: bstock; Owner: bstockuser
--

CREATE UNIQUE INDEX user_account_mail_idx ON bstock.user_account USING btree (mail);


--
-- TOC entry 3284 (class 1259 OID 16448)
-- Name: user_account_phone_idx; Type: INDEX; Schema: bstock; Owner: bstockuser
--

CREATE INDEX user_account_phone_idx ON bstock.user_account USING btree (phone);


-- Completed on 2024-11-05 01:27:11 UTC

--
-- PostgreSQL database dump complete
--

