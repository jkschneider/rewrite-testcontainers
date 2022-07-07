package org.openrewrite.testcontainers

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class CopyFileToContainerTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(CopyFileToContainer())
    }

    @Test
    fun copyFileToContainer() = rewriteRun(
        { spec -> spec.parser(JavaParser.fromJavaVersion().classpath("testcontainers").build()) },
        java(
            """
                import org.testcontainers.containers.BindMode;
                import static org.testcontainers.containers.BindMode.READ_ONLY;
                import org.testcontainers.containers.Container;
                
                class Test {
                    Container<?> test(Container<?> c) {
                        c.withClasspathResourceMapping("a", "b", BindMode.READ_ONLY);
                        c.withClasspathResourceMapping("a".toUpperCase(), "b", READ_ONLY);
                    }
                }
            """,
            """
                import org.testcontainers.containers.Container;
                import org.testcontainers.utility.MountableFile;
                
                class Test {
                    Container<?> test(Container<?> c) {
                        c.withCopyFileToContainer(MountableFile.forClasspathResource("a"), "b");
                        c.withCopyFileToContainer(MountableFile.forClasspathResource("a".toUpperCase()), "b");
                    }
                }
            """
        )
    )
}
