package com.engine.paper.domain;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class DomainArchitectureTest {

    @Test
    @DisplayName("Domain layer must have zero dependencies on engine, renderer, infrastructure, or template")
    void domainShouldBeIndependent() {
        ArchRule rule = noClasses().that().resideInAPackage("com.engine.paper.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.engine.paper.engine..",
                        "com.engine.paper.renderer..",
                        "com.engine.paper.infrastructure..",
                        "com.engine.paper.template..",
                        "org.apache.pdfbox.."
                );

        rule.check(new ClassFileImporter().importPackages("com.engine.paper"));
    }

    @Test
    @DisplayName("Domain models and layout records should be clean POJOs/records")
    void domainModelsShouldBePublic() {
        ArchRule rule = classes().that().resideInAPackage("com.engine.paper.domain.model..")
                .should().bePublic();

        rule.check(new ClassFileImporter().importPackages("com.engine.paper"));
    }
}
