//package com.bigstock.auth.service;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import com.bigstock.sharedComponent.entity.RoleInfo;
//import com.bigstock.sharedComponent.entity.UserAccount;
//import com.bigstock.sharedComponent.service.RoleInfoService;
//import com.bigstock.sharedComponent.service.UserAccountService;
//import com.google.common.collect.Lists;
//
//import io.jsonwebtoken.JwtException;
//import lombok.RequiredArgsConstructor;
//
//@RequiredArgsConstructor
//public class BigStockUserDetailsService implements UserDetailsService {
//
//	
//	
//	private final UserAccountService userAccountService;
//	private final RoleInfoService roleInfoService;
//
//	@Override
//	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//		Optional<UserAccount> userOptional = userAccountService.findByEmailIgnoreCase(username).stream().findFirst();
//		if (userOptional.isEmpty()) {
//			userOptional = userAccountService.getByPhone(username).stream().findFirst();
//		}
//		UserAccount user = userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found"));
//		String roleId = userOptional.get().getRoleId();
//		RoleInfo roleInfo = roleInfoService.getByRoleId(roleId)
//				.orElseThrow(() -> new JwtException("role can not found"));
//		// Compare the user's input password with the encoded password in the database
//		List<String> roles = Lists.newArrayList(roleInfo.getRoleName());
//
//		// Convert roles to SimpleGrantedAuthority objects
//		List<SimpleGrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new)
//				.collect(Collectors.toList());
//		return new org.springframework.security.core.userdetails.User(username, user.getUserPassword(), authorities);
//	}
//}
