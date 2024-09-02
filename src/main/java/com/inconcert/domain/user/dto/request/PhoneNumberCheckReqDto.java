package com.inconcert.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PhoneNumberCheckReqDto {
    @NotBlank
    private String phoneNumber;
}
