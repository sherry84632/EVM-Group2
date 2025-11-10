package com.dealermanagementsysstem.project.dto;

import com.dealermanagementsysstem.project.Model.DTODealer;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerForm {
    Integer customerID;
    @NotBlank(message = "Full name ís required")
    @Size(min = 2, max = 50, message = "Full name must be between 2 and 50 characters.")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Full name can only contain letters and spaces.")
    String fullName;
    @Email(message = "Please enter a valid email address")
    @NotBlank(message = "Email can not be blank")
    String email;
    String vehicleInterest;
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Invalid phone number")
    String phone;
    @NotBlank(message = "Address cannot be blank")
    @Pattern(regexp = "^[\\p{L}0-9\\s,./-]+$", message = "Address can only contain letters and spaces.")
    String address;
    @Past(message = "Birth date must be in the past")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate birthDate;
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    String note;
    @Future(message = "Test Drive date must be in the future")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    LocalDateTime testDriveSchedule;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    DTODealer dealer;
}
