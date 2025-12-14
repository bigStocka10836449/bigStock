package com.bigstock.biz.domain.vo;

import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User Account Value Object")
public class UserRegistryInfo {

    @Schema(description = "Phone number of the user", example = "12345678901")
    private String phone;

	@Schema(description = "Email address of the user", requiredMode = RequiredMode.REQUIRED, example = "example@mail.com")
    @NotBlank(message = "Mail is mandatory")
    private String mail;

    @Schema(description = "Gender of the user", example = "M")
    private String gender;

    @Schema(description = "Birth date of the user", example = "1990-01-01")
    private Date birthDate;

    @Schema(description = "Time when the user account was created")
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    @Schema(description = "Time when the user account was last updated")
    @Builder.Default
    @JsonIgnore
    private LocalDateTime updateTime = LocalDateTime.now();

    @Schema(description = "Updated by user", example = "admin")
    @JsonIgnore
    private String updateBy;

    @Schema(description = "Password of the user account", example = "encryptedPassword", requiredMode = RequiredMode.REQUIRED)
    private String userPassword;

    @JsonIgnore
    @Builder.Default
    private String status = "2";

    @Schema(description = "Name of the user", example = "John Doe")
    private String userName;

    @Schema(description = "User account name", requiredMode = RequiredMode.REQUIRED, example = "john_doe")
    @NotBlank(message = "User account is mandatory")
    private String userId;

}
