package com.example.product.config;

import com.example.product.model.Product;
import com.example.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;

class DataInitializerTest {

    @Test
    void run_createsFiveProducts_whenRepositoryEmpty() throws Exception {
        DataInitializer init = new DataInitializer();
        RepoHolder holder = new RepoHolder();
        ProductRepository repo = createProxy(holder);
        ReflectionTestUtils.setField(init, "productRepository", repo);

        init.run();

        assertThat(holder.saved).hasSize(5);
    }

    @Test
    void run_skipsWhenRepositoryNotEmpty() throws Exception {
        DataInitializer init = new DataInitializer();
        RepoHolder holder = new RepoHolder();
        holder.count = 1;
        ProductRepository repo = createProxy(holder);
        ReflectionTestUtils.setField(init, "productRepository", repo);

        init.run();

        assertThat(holder.saved).isEmpty();
    }

    private static ProductRepository createProxy(RepoHolder holder) {
        InvocationHandler h = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("count".equals(name) && (args == null || args.length == 0)) {
                    return holder.count;
                }
                if ("save".equals(name) && args != null && args.length == 1) {
                    @SuppressWarnings("unchecked")
                    Object entity = args[0];
                    holder.saved.add((Product) entity);
                    return entity;
                }
                if ("findAll".equals(name) && (args == null || args.length == 0)) {
                    return List.copyOf(holder.saved);
                }
                if ("findAll".equals(name) && args != null && args.length == 1) {
                    return new PageImpl<>(List.copyOf(holder.saved), (org.springframework.data.domain.Pageable) args[0], holder.saved.size());
                }
                // default stubs
                return null;
            }
        };

        return (ProductRepository) Proxy.newProxyInstance(
                ProductRepository.class.getClassLoader(),
                new Class[]{ProductRepository.class},
                h
        );
    }

    private static final class RepoHolder {
        final List<Product> saved = new ArrayList<>();
        long count = 0;
    }
}
