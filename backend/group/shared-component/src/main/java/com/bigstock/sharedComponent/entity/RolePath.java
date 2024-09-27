package com.bigstock.sharedComponent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

@Entity
@ToString
@Data
@Table(schema = "bstock", name = "role_path")
public class RolePath {
    // 主鍵
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 角色 ID
    @Column(name = "role_id")
    private Long roleId;

    // 角色允許訪問的 URL 路徑
    @Column(name = "role_allowed_url_path")
    private String roleAllowedUrlPath;
}
