package com.mauri.backend;

import com.mauri.backend.bootstrap.DevPortConflictGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        DevPortConflictGuard.replaceRunningInstanceIfNeeded(args);
        SpringApplication.run(BackendApplication.class, args);
    }
}
