package com.github.juanfranciscofernandezherreros.library.test.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests that use the library-base starters.
 * <p>
 * Annotates the test class with {@link SpringBootTest} and activates the
 * {@code test} Spring profile, providing a common baseline for all
 * integration tests in consumer projects.
 *
 * <pre>{@code
 * @ExtendWith(SpringExtension.class)
 * class MyServiceIntegrationTest extends BaseIntegrationTest {
 *     // ...
 * }
 * }</pre>
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    // Intentionally empty — subclasses add test-specific fields and methods.
}
