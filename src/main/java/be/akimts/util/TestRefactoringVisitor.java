package be.akimts.util;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;

public class TestRefactoringVisitor extends ModifierVisitor<Void> {

    private boolean addedAssert = false;

    public boolean isAddedAssert() {
        return addedAssert;
    }

    @Override
    public MethodDeclaration visit(MethodDeclaration md, Void arg) {
        md.getAnnotations().forEach( annotation -> {
            if( annotation.getNameAsString().equals("Test") && annotation.isNormalAnnotationExpr() ){
                NormalAnnotationExpr annotationExp = annotation.asNormalAnnotationExpr();

                annotationExp.getPairs()
                        .stream()
                        .filter(pair -> pair.getNameAsString().equals("expected"))
                        .findFirst()
                        .ifPresent(pair -> {
                            md.setJavadocComment("TODO CHECK VALIDITY: static imports and stubbing");
                            annotation.replace(new MarkerAnnotationExpr("Test"));
                            addAssertThrows(md, pair.getValue().toString());
                            this.addedAssert = true;
                        });

            }
        } );
        return md;
    }


    private void addAssertThrows(MethodDeclaration md, String exceptionClass) {
        Statement statement = md.getBody()
                .map(BlockStmt::getStatements)
                .map(stmts -> {
                    ExpressionStmt stmt = stmts.getLast()
                            .orElseThrow()
                            .asExpressionStmt();

                    Expression stmtExp = stmt.getExpression();
                    if( stmt.getExpression().isMethodCallExpr() && stmt.getExpression().asMethodCallExpr().getNameAsString().contains("verify") ){
                        MethodCallExpr verifyMethodCallExpr = stmt.getExpression().asMethodCallExpr();
                        if( verifyMethodCallExpr.getNameAsString().startsWith("verifyZeroInteractions") ){
                            MethodCallExpr newVerifyMethodCallExpr = new MethodCallExpr();
                            newVerifyMethodCallExpr.setName("verifyNoInteractions");
                            newVerifyMethodCallExpr.setArguments(verifyMethodCallExpr.getArguments());
                            verifyMethodCallExpr.replace(newVerifyMethodCallExpr);
                        }

                        stmt = stmts.get( stmts.size()-2 ).toExpressionStmt().orElseThrow();
                    }
                    return stmt;
                })
                .orElseThrow();

        LambdaExpr lambdaExpr = new LambdaExpr();
        lambdaExpr.setParameters(new NodeList<>());
        lambdaExpr.setBody(statement);
        lambdaExpr.setEnclosingParameters(true);

        MethodCallExpr assertThrowsExpr = new MethodCallExpr();
        assertThrowsExpr.addArgument(exceptionClass);
        assertThrowsExpr.setName("assertThrows");
        assertThrowsExpr.addArgument( lambdaExpr);

        Statement assertThrowsStmt = new ExpressionStmt(assertThrowsExpr);

        md.getBody().orElseThrow().replace(statement, assertThrowsStmt);
    }
}
