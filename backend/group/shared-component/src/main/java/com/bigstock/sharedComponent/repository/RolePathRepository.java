package com.bigstock.sharedComponent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bigstock.sharedComponent.entity.RolePath;

@Repository
public interface RolePathRepository extends JpaRepository<RolePath, Long> {

    // 根據角色ID查詢其可訪問的路徑
    List<RolePath> findByRoleId(Long roleId);
}
