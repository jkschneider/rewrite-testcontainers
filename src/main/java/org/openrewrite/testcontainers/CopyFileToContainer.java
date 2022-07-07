package org.openrewrite.testcontainers;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class CopyFileToContainer extends Recipe {
    private static final MethodMatcher withClasspathResourceMapping = new MethodMatcher(
            "org.testcontainers.containers.Container withClasspathResourceMapping(String, String, org.testcontainers.containers.BindMode)", true);

    @Override
    public String getDisplayName() {
        return "Copy files to container";
    }

    @Override
    public String getDescription() {
        return "Use the suggested pattern to copy files to container.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new UsesMethod<>(withClasspathResourceMapping);
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final JavaTemplate withCopyFileToContainer = JavaTemplate
                    .builder(this::getCursor, "#{any(org.testcontainers.containers.Container)}.withCopyFileToContainer(" +
                            "MountableFile.forClasspathResource(#{any(java.lang.String)}), #{any(java.lang.String)}")
                    .javaParser(() -> JavaParser.fromJavaVersion().classpath("testcontainers").build())
                    .imports("org.testcontainers.utility.MountableFile")
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);
                if (withClasspathResourceMapping.matches(m) && isReadOnlyBindMode(m)) {
                    m = m.withTemplate(withCopyFileToContainer, m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0), m.getArguments().get(1));
                    maybeAddImport("org.testcontainers.utility.MountableFile");
                    maybeRemoveImport("org.testcontainers.containers.BindMode");
                }
                return m;
            }

            private boolean isReadOnlyBindMode(J.MethodInvocation method) {
                Expression expression = method.getArguments().get(2);
                JavaType.Variable bindMode = null;
                if (expression instanceof J.FieldAccess) {
                    bindMode = ((J.FieldAccess) expression).getName().getFieldType();
                } else if (expression instanceof J.Identifier) {
                    bindMode = ((J.Identifier) expression).getFieldType();
                }
                return bindMode != null && bindMode.getName().equals("READ_ONLY");
            }
        };
    }
}
