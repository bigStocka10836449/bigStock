package com.bigstock.biz.controller;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

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
