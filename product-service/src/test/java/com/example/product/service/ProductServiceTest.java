package com.example.product.service;

import com.example.product.model.Product;
import com.example.product.model.RemoteMedia;
import com.example.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class ProductServiceTest {

    private ProductService productService;
    private RepoHolder fakeRepoHolder;

    @BeforeEach
    void setUp() {
        fakeRepoHolder = new RepoHolder();
        ProductRepository repo = createProxy(fakeRepoHolder);
        productService = new ProductService(repo, null, new FakeRestTemplate(), new NullAuditService());
    }

    @Test
    void getAllProducts_returnsEmptyListWhenNone() {
        var all = productService.getAllProducts(0, 10);
        assertThat(all).isEmpty();
    }

    @Test
    void getProductById_returnsWhenExists() {
        Product p = new Product();
        p.setId("p1");
        p.setUserId("u1");
        // save via proxy
        fakeRepoHolder.saved.put("p1", p);
        var opt = productService.getProductById("p1");
        assertThat(opt).isPresent();
        assertThat(opt.get().getId()).isEqualTo("p1");
    }

    @Test
    void getProductById_emptyWhenNotFound() {
        var opt = productService.getProductById("missing");
        assertThat(opt).isEmpty();
    }

    @Test
    void addImages_successAddsUrl() {
        Product p = new Product();
        p.setId("p1");
        p.setUserId("user-1");
        p.setImageUrls(new ArrayList<>());
        fakeRepoHolder.saved.put("p1", p);

        FakeRestTemplate frt = (FakeRestTemplate) ReflectionTestUtils.getField(productService, "restTemplate");
        RemoteMedia media = new RemoteMedia();
        media.setId("m1");
        media.setUserId("user-1");
        frt.register("m1", media);

        var updated = productService.addImages("p1", List.of("m1"), "user-1");
        assertThat(updated.getImageUrls()).containsExactly("/api/media/images/m1");
    }

    @Test
    void addImages_throwsNotFoundWhenRemoteMissing() {
        Product p = new Product();
        p.setId("p1");
        p.setUserId("user-1");
        fakeRepoHolder.saved.put("p1", p);

        FakeRestTemplate frt = (FakeRestTemplate) ReflectionTestUtils.getField(productService, "restTemplate");
        frt.setThrowNotFoundFor("m404");

        assertThatThrownBy(() -> productService.addImages("p1", List.of("m404"), "user-1"))
            .isInstanceOf(com.example.product.exception.ResourceNotFoundException.class);
    }

    @Test
    void addImages_throwsMediaValidationOnRestClientException() {
        Product p = new Product();
        p.setId("p1");
        p.setUserId("user-1");
        fakeRepoHolder.saved.put("p1", p);

        FakeRestTemplate frt = (FakeRestTemplate) ReflectionTestUtils.getField(productService, "restTemplate");
        frt.setThrowRestClientFor("mbad");

        assertThatThrownBy(() -> productService.addImages("p1", List.of("mbad"), "user-1"))
            .isInstanceOf(com.example.product.exception.MediaValidationException.class);
    }

    @Test
    void addImages_throwsAccessDeniedWhenMediaNotOwned() {
        Product p = new Product();
        p.setId("p1");
        p.setUserId("user-1");
        fakeRepoHolder.saved.put("p1", p);

        FakeRestTemplate frt = (FakeRestTemplate) ReflectionTestUtils.getField(productService, "restTemplate");
        RemoteMedia media = new RemoteMedia();
        media.setId("m2");
        media.setUserId("other-user");
        frt.register("m2", media);

        assertThatThrownBy(() -> productService.addImages("p1", List.of("m2"), "user-1"))
            .isInstanceOf(com.example.product.exception.AccessDeniedException.class);
    }

    // ===== Helper fake implementations =====

    private static ProductRepository createProxy(RepoHolder holder) {
        java.lang.reflect.InvocationHandler h = new java.lang.reflect.InvocationHandler() {
            @Override
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("save".equals(name) && args != null && args.length == 1) {
                    @SuppressWarnings("unchecked")
                    Product p = (Product) args[0];
                    if (p.getId() == null) p.setId("p-" + System.nanoTime());
                    holder.saved.put(p.getId(), p);
                    return p;
                }
                if ("findById".equals(name) && args != null && args.length == 1) {
                    return Optional.ofNullable(holder.saved.get((String) args[0]));
                }
                if ("findAll".equals(name) && args != null && args.length == 1) {
                    return new PageImpl<>(List.copyOf(holder.saved.values()), (Pageable) args[0], holder.saved.size());
                }
                if ("deleteById".equals(name) && args != null && args.length == 1) {
                    holder.saved.remove((String) args[0]);
                    return null;
                }
                if ("count".equals(name) && (args == null || args.length == 0)) {
                    return (long) holder.saved.size();
                }
                // default fallback
                return null;
            }
        };
        return (ProductRepository) java.lang.reflect.Proxy.newProxyInstance(
                ProductRepository.class.getClassLoader(),
                new Class[]{ProductRepository.class},
                h
        );
    }

    private static final class RepoHolder {
        final Map<String, Product> saved = new HashMap<>();
    }

    private static class FakeRestTemplate extends RestTemplate {
        private final Map<String, RemoteMedia> registry = new HashMap<>();
        private String throwNotFoundFor = null;
        private String throwRestFor = null;

        void register(String id, RemoteMedia media) { registry.put(id, media); }
        void setThrowNotFoundFor(String id) { this.throwNotFoundFor = id; }
        void setThrowRestClientFor(String id) { this.throwRestFor = id; }

        @Override
        public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException {
            // url ends with /images/{id}/meta
            String id = url.substring(url.lastIndexOf('/') - 7 + 1);
            // Fallback: parse last path segment before /meta
            String[] parts = url.split("/");
            id = parts[parts.length - 2];

            if (throwNotFoundFor != null && throwNotFoundFor.equals(id)) {
                throw HttpClientErrorException.create(org.springframework.http.HttpStatus.NOT_FOUND, "Not Found", null, null, null);
            }
            if (throwRestFor != null && throwRestFor.equals(id)) {
                throw new RestClientException("boom");
            }
            @SuppressWarnings("unchecked")
            T t = (T) registry.get(id);
            return t;
        }
    }

    private static class NullAuditService extends AuditService {
        @Override
        public void logWriteOperation(String userId, String action, String entityType, String entityId, String details) {
            // no-op for tests
        }
    }
}
