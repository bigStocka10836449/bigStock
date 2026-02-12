package com.bigstock.biz.controller;

import com.bigstock.biz.service.MonthlyRevenueQueryService;
import com.bigstock.biz.vo.MonthlyRevenueVo;
import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DbDebugController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/dbinfo")
    public Map<String, Object> dbinfo() {
        return jdbcTemplate.queryForMap("""
            select
              current_database() as db,
              current_schema() as schema,
              inet_server_addr()::text as server_addr,
              inet_server_port() as server_port
        """);
    }

    @GetMapping("/count-basic")
    public Map<String, Object> count() {
        return jdbcTemplate.queryForMap("""
            select count(*) as cnt
            from bstock.stock_basic_info
        """);
    }
}
