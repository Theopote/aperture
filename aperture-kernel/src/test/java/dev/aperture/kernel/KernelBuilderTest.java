package dev.aperture.kernel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KernelBuilder.
 */
@DisplayName("Kernel Builder")
class KernelBuilderTest {

	@Test
	@DisplayName("Build with default configuration")
	void testDefaultBuild() {
		// Act
		ApertureKernel kernel = ApertureKernel.builder().build();

		// Assert
		assertNotNull(kernel);
		assertFalse(kernel.isClosed());

		kernel.close();
	}

	@Test
	@DisplayName("Build with custom cache capacity")
	void testWithCacheCapacity() {
		// Act
		ApertureKernel kernel = ApertureKernel.builder()
			.withCacheCapacity(500)
			.build();

		// Assert
		assertNotNull(kernel);
		kernel.close();
	}

	@Test
	@DisplayName("Build with zero cache capacity (disabled)")
	void testWithZeroCacheCapacity() {
		// Act
		ApertureKernel kernel = ApertureKernel.builder()
			.withCacheCapacity(0)
			.build();

		// Assert
		assertNotNull(kernel);
		kernel.close();
	}

	@Test
	@DisplayName("Negative cache capacity throws exception")
	void testNegativeCacheCapacity() {
		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> {
			ApertureKernel.builder().withCacheCapacity(-1);
		});
	}

	@Test
	@DisplayName("Build with debug logging enabled")
	void testWithDebugLogging() {
		// Act
		ApertureKernel kernel = ApertureKernel.builder()
			.enableDebugLogging()
			.build();

		// Assert
		assertNotNull(kernel);
		kernel.close();
	}

	@Test
	@DisplayName("Build with debug logging disabled")
	void testWithoutDebugLogging() {
		// Act
		ApertureKernel kernel = ApertureKernel.builder()
			.disableDebugLogging()
			.build();

		// Assert
		assertNotNull(kernel);
		kernel.close();
	}

	@Test
	@DisplayName("Build with custom thread pool size")
	void testWithThreadPoolSize() {
		// Act
		ApertureKernel kernel = ApertureKernel.builder()
			.withAsyncThreadPoolSize(8)
			.build();

		// Assert
		assertNotNull(kernel);
		kernel.close();
	}

	@Test
	@DisplayName("Zero thread pool size throws exception")
	void testZeroThreadPoolSize() {
		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> {
			ApertureKernel.builder().withAsyncThreadPoolSize(0);
		});
	}

	@Test
	@DisplayName("Negative thread pool size throws exception")
	void testNegativeThreadPoolSize() {
		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> {
			ApertureKernel.builder().withAsyncThreadPoolSize(-1);
		});
	}

	@Test
	@DisplayName("Build with custom executor service")
	void testWithCustomExecutor() {
		// Arrange
		var executor = Executors.newFixedThreadPool(2);

		try {
			// Act
			ApertureKernel kernel = ApertureKernel.builder()
				.withExecutorService(executor)
				.build();

			// Assert
			assertNotNull(kernel);
			kernel.close();

		} finally {
			executor.shutdown();
		}
	}

	@Test
	@DisplayName("Build for testing")
	void testBuildForTesting() {
		// Act
		ApertureKernel kernel = KernelBuilder.buildForTesting();

		// Assert
		assertNotNull(kernel);
		assertFalse(kernel.isClosed());

		kernel.close();
	}

	@Test
	@DisplayName("Build for production")
	void testBuildForProduction() {
		// Act
		ApertureKernel kernel = KernelBuilder.buildForProduction();

		// Assert
		assertNotNull(kernel);
		assertFalse(kernel.isClosed());

		kernel.close();
	}

	@Test
	@DisplayName("Builder is reusable")
	void testBuilderReusable() {
		// Arrange
		KernelBuilder builder = ApertureKernel.builder()
			.withCacheCapacity(100);

		// Act
		ApertureKernel kernel1 = builder.build();
		ApertureKernel kernel2 = builder.build();

		// Assert
		assertNotNull(kernel1);
		assertNotNull(kernel2);
		assertNotSame(kernel1, kernel2, "Each build should create new instance");

		kernel1.close();
		kernel2.close();
	}

	@Test
	@DisplayName("Fluent API chaining")
	void testFluentAPI() {
		// Act & Assert - Should not throw
		assertDoesNotThrow(() -> {
			ApertureKernel kernel = ApertureKernel.builder()
				.withCacheCapacity(200)
				.withAsyncThreadPoolSize(4)
				.enableDebugLogging()
				.build();

			kernel.close();
		});
	}
}
