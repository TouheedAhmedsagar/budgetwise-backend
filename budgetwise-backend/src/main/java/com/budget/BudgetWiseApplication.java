package com.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;


@OpenAPIDefinition(
		info=@Info(title="Budget Wise Rest API",
		description="tourist rest api",
		contact=@Contact( name="Touheed",email="touhidsagar2002@gmail.com"
				)
		
				
				))
@SpringBootApplication
public class BudgetWiseApplication {
    public static void main(String[] args) {
        SpringApplication.run(BudgetWiseApplication.class, args);
    }
}
