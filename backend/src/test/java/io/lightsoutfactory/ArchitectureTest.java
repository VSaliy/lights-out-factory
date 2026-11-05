package io.lightsoutfactory;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "io.lightsoutfactory", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest
    static final ArchRule apiMustNotAccessPersistenceRepositories =
            noClasses().that().resideInAPackage("..api..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");
}
