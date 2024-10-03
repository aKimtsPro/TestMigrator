package be.akimts.util;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.utils.SourceRoot;

import java.nio.file.Paths;
import java.util.List;

public class JUnit4ToJUnit5Refactoring {

    public static void main(String[] args) throws Exception {
        // Path to the project or file containing the JUnit 4 test cases
        String path = "C:\\Users\\a.kimtsaris\\IdeaProjects\\nestor\\procedure-service\\procedure-service-server\\src\\test\\java\\be\\wavenet\\egov\\procedure\\service";

        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        parserConfiguration.setDetectOriginalLineSeparator(true);

        // Initialize JavaParser and source root
        SourceRoot sourceRoot = new SourceRoot(Paths.get(path), parserConfiguration);

        // Parse all the .java files in the source root
        sourceRoot.tryToParse("");

        List<CompilationUnit> cus = sourceRoot.getCompilationUnits();

        cus.forEach( cu -> {
            TestRefactoringVisitor visitor = new TestRefactoringVisitor();
            cu.accept( new TestRefactoringVisitor(), null );

            addStaticImportAll(cu, "org.junit.jupiter.api.Assertions");
            addStaticImportAll(cu, "org.mockito.Mockito");

            System.out.println(cu);
        });

        sourceRoot.saveAll();
    }

    private static void addStaticImportAll(CompilationUnit cu, String importName) {
        boolean importExists = cu.getImports().stream()
                .anyMatch(importStmt -> importStmt.isStatic() && importStmt.getNameAsString().equals(importName));

        if (!importExists) {
            ImportDeclaration staticImport = new ImportDeclaration(importName, true, true);
            cu.addImport(staticImport);
        }
    }
}
