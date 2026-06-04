package com.example.product.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ProductCreateRequestTest {

    private static ValidatorFactory vf;
    private static Validator validator;

    @BeforeAll
    static void init() {
        vf = Validation.buildDefaultValidatorFactory();
        validator = vf.getValidator();
    }

    @AfterAll
    static void close() {
        vf.close();
    }

    @Test
    void validRequest_hasNoViolations() {
        ProductCreateRequest req = new ProductCreateRequest("Name", "Desc", 9.99, 1);
        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    void nameNotBlank_validationFails() {
        ProductCreateRequest req = new ProductCreateRequest(null, "Desc", 9.99, 1);
        Set<ConstraintViolation<ProductCreateRequest>> v = validator.validate(req);
        assertThat(v).anyMatch(cv -> cv.getPropertyPath().toString().equals("name"));
    }

    @Test
    void descriptionNotBlank_validationFails() {
        ProductCreateRequest req = new ProductCreateRequest("Name", "", 9.99, 1);
        Set<ConstraintViolation<ProductCreateRequest>> v = validator.validate(req);
        assertThat(v).anyMatch(cv -> cv.getPropertyPath().toString().equals("description"));
    }

    @Test
    void priceNotNullAndPositive_validationFails() {
        ProductCreateRequest req = new ProductCreateRequest("Name", "Desc", 0.0, 1);
        Set<ConstraintViolation<ProductCreateRequest>> v = validator.validate(req);
        assertThat(v).anyMatch(cv -> cv.getPropertyPath().toString().equals("price"));
    }

    @Test
    void quantityNotNullAndPositive_validationFails() {
        ProductCreateRequest req = new ProductCreateRequest("Name", "Desc", 1.0, 0);
        Set<ConstraintViolation<ProductCreateRequest>> v = validator.validate(req);
        assertThat(v).anyMatch(cv -> cv.getPropertyPath().toString().equals("quantity"));
    }

    @Test
    void settersAndGetters_work() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName("n");
        req.setDescription("d");
        req.setPrice(1.2);
        req.setQuantity(2);
        assertThat(req.getName()).isEqualTo("n");
        assertThat(req.getDescription()).isEqualTo("d");
        assertThat(req.getPrice()).isEqualTo(1.2);
        assertThat(req.getQuantity()).isEqualTo(2);
    }
}
