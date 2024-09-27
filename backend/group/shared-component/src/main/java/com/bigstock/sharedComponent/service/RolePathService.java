package com.bigstock.sharedComponent.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock;
import com.bigstock.sharedComponent.entity.RolePath;
import com.bigstock.sharedComponent.repository.RolePathRepository;

@Service
public class RolePathService {

    private final RolePathRepository rolePathRepository;

    @Autowired
    public RolePathService(RolePathRepository rolePathRepository) {
        this.rolePathRepository = rolePathRepository;
    }

	@BigStockCacheableWithLock(value = "longLivedCache", key = "'allRolePaths'")
    public List<RolePath> getAllRolePaths() {
        return rolePathRepository.findAll();
    }

    public Optional<RolePath> getRolePathById(Long id) {
        return rolePathRepository.findById(id);
    }

    // 根據角色ID查詢其可訪問的路徑
    public List<RolePath> getPathsByRoleId(Long roleId) {
        return rolePathRepository.findByRoleId(roleId);
    }

    // 新增或更新 RolePath
    public RolePath saveRolePath(RolePath rolePath) {
        return rolePathRepository.save(rolePath);
    }

    // 刪除 RolePath
    public void deleteRolePathById(Long id) {
        rolePathRepository.deleteById(id);
    }
}
