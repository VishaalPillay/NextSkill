package com.nextskill.dto;

import lombok.Data;
import java.util.List;

@Data
public class PythonNLPResponse {
    private String fullName;
    private String email;
    private String phoneNumber;
    private List<SkillData> skills;
    // You can add lists for experiences, certifications, etc. here later

    @Data
    public static class SkillData {
        private String skillName;
    }
}