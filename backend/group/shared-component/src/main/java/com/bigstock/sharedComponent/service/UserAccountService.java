package com.bigstock.sharedComponent.service;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.UserAccount;
import com.bigstock.sharedComponent.repository.UserAccountRepository;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public List<UserAccount> getAll() {
        return userAccountRepository.findAll();
    }

    public Optional<UserAccount> getById(String id) {
        return userAccountRepository.findById(id);
    }

    public List<UserAccount> getByPhone(String phone) {
        return userAccountRepository.findByPhone(phone);
    }

    public Optional<UserAccount> findByEmail(String email) {
        return userAccountRepository.findByEmail(email);
    }

    public List<UserAccount> findByBirthDateAfter(Date birthDate) {
        return userAccountRepository.findByBirthDateAfter(birthDate);
    }

    public UserAccount save(UserAccount userAccount) {
        return userAccountRepository.save(userAccount);
    }

    public List<UserAccount> insert(List<UserAccount> userAccounts) {
        return userAccountRepository.saveAll(userAccounts);
    }

    public void deleteById(String id) {
        userAccountRepository.deleteById(id);
    }

    public void delete(UserAccount userAccount) {
        userAccountRepository.delete(userAccount);
    }
    
    public Optional<UserAccount> findUserByEmail(String email) {
    	Optional<UserAccount>  userAccountOp = userAccountRepository.findByEmail(email);
		if (userAccountOp.isEmpty()) {
			userAccountOp = userAccountRepository.findByPhone(email).stream().findFirst();
		}
		return userAccountOp;
    }
}
