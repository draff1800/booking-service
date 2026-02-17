package com.draff1800.booking_service.common.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

// Temporary Controller to verify global error handling

@RestController
@RequestMapping("/demo")
public class DemoValidationController {

  public record DemoRequest(
      @NotBlank(message = "name is required")
      @Size(max = 50, message = "name must be <= 50 characters")
      String name,

      @Email(message = "email must be valid")
      @NotBlank(message = "email is required")
      String email
  ) {}

  @PostMapping("/validate")
  public String validate(@Valid @RequestBody DemoRequest req) {
    return "ok";
  }
}
