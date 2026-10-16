package com.sliit.musicstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleBusinessError(RuntimeException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/business-error";
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public void handleNotFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        // Quietly return 404 for missing static resources like favicon.ico
    }

    /**
     * Catch-all so unexpected errors (bugs) show the real exception message and
     * type on-screen instead of the generic Whitelabel Error Page, making it
     * possible to diagnose issues without digging through the console.
     */
    @ExceptionHandler(Exception.class)
    public String handleUnexpectedError(Exception ex, Model model) {
        log.error("Unhandled exception while processing request", ex);
        model.addAttribute("message", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        return "error/business-error";
    }
}
