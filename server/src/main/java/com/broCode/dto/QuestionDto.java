package com.broCode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDto {

    @NotBlank(message = "Question must not be blank")
    @Size(max = 4000, message = "Question must be at most 4000 characters")
    String question;

    @Size(max = 36, message = "Session ID must be at most 36 characters")
    String sessionId;
}
