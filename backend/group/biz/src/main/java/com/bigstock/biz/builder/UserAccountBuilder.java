package com.bigstock.biz.builder;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bigstock.biz.domain.vo.UserRegistryInfo;
import com.bigstock.sharedComponent.entity.UserAccount;

@Mapper(componentModel = "spring")
public interface UserAccountBuilder {

	@Mapping(source = "userRegistryInfo.phone", target = "phone")
	@Mapping(source = "userRegistryInfo.mail", target = "email")
	@Mapping(source = "userRegistryInfo.gender", target = "gender")
	@Mapping(source = "userRegistryInfo.birthDate", target = "birthDate")
	@Mapping(source = "userRegistryInfo.createTime", target = "createTime")
	@Mapping(source = "userRegistryInfo.updateTime", target = "updateTime")
	@Mapping(source = "userRegistryInfo.updateBy", target = "updateBy")
	@Mapping(source = "userRegistryInfo.userPassword", target = "userPassword")
	@Mapping(source = "userRegistryInfo.status", target = "status")
	@Mapping(source = "userRegistryInfo.userName", target = "userName")
	UserAccount userRegistryInfoToUserAccount(UserRegistryInfo userRegistryInfo);
}
