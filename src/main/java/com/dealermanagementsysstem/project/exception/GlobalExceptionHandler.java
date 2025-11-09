package com.dealermanagementsysstem.project.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
	public String handleBusinessException(BusinessException ex, RedirectAttributes redirectAttributes) {
        log.error("[BUSINESS_ERROR] {} - {}",ex.getErrorCode().getCode(),ex.getMessage(),ex);
		redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		return "redirect:" + ex.getErrorCode().getRedirectPath();
	}

	@ExceptionHandler(Exception.class)
	public String handleGenericException(Exception ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred");
		return "redirect:/customer/list";
	}
}


