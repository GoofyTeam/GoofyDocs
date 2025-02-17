package com.goofy.GoofyFiles;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class GoofyFilesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoofyFilesApplication.class, args);
    }

    // Mapping de la racine pour afficher "Hello, World!"
    @GetMapping("/")
    public String helloWorld() {
        return "Hello, World!";
    }
}