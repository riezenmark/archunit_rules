package com.example.archunitrules;

import com.example.archunitrules.common.exception.BaseParametrizedException;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import lombok.Generated;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.metaAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArchUnitRulesApplicationTests {
    JavaClasses classes = new ClassFileImporter().importPackages("com.example.archunitrules");

    @Test
    void properlyNamedServiceInterfacesHasProperlyNamedServiceImplementations() {
        ArchRule interfacesRule = classes()
                .that().resideInAPackage("..service")
                .should().haveNameMatching(".+Service$")
                .andShould().beInterfaces();
        ArchRule implementations = classes()
                .that().resideInAPackage("..service.impl")
                .should().haveNameMatching(".+ServiceImpl$")
                .andShould().notBeInterfaces()
                .andShould(implementCorrespondingServiceInterface());
        interfacesRule.allowEmptyShould(true).check(classes);
        implementations.allowEmptyShould(true).check(classes);
    }

    private ArchCondition<JavaClass> implementCorrespondingServiceInterface() {
        return new ArchCondition<>("implements corresponding service interface") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String serviceImplementationClassName = item.getSimpleName();
                String baseName = serviceImplementationClassName.substring(0, serviceImplementationClassName.lastIndexOf("ServiceImpl"));
                item.getRawInterfaces().stream()
                        .map(JavaClass::getSimpleName)
                        .filter(interfaceName -> interfaceName.startsWith(baseName))
                        .filter(interfaceName -> interfaceName.endsWith("Service"))
                        .findAny()
                        .ifPresentOrElse(
                                interfaceName -> {
                                },
                                () -> events.add(SimpleConditionEvent.violated(item, "No corresponding service interface found for " + serviceImplementationClassName))
                        );
            }
        };
    }

    @Test
    void layersAreAccessedFromTopToBottomOnly() {
        LayeredArchitecture architecture = layeredArchitecture().consideringOnlyDependenciesInLayers()
                .layer("Controller").definedBy("..controller")
                .layer("Service").definedBy("..service")
                .layer("Repository").definedBy("..repository")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");
        architecture.allowEmptyShould(true).check(classes);
    }

    @Test
    void requestsWithValidationAnnotationsAreProperlyValidated() {
        ArchRule validatedRequestsRole = classes()
                .that().resideInAPackage("..controller.request")
                .and().containAnyFieldsThat(isAnnotatedWithAnyValidationAnnotation())
                .should(beValidatedRequestBodyInCorrespondingController());
        validatedRequestsRole.allowEmptyShould(true).check(classes);
    }

    private DescribedPredicate<JavaField> isAnnotatedWithAnyValidationAnnotation() {
        return new DescribedPredicate<>("is annotated with any validation annotation") {
            @Override
            public boolean test(JavaField javaField) {
                return javaField.isAnnotatedWith(annotationFromAnyPackage(
                        "javax.validation.constraints",
                        "jakarta.validation.constraints",
                        "org.hibernate.validator.constraints"
                ));
            }
        };
    }

    private static DescribedPredicate<JavaAnnotation<?>> annotationFromAnyPackage(String... annotationPackages) {
        return new DescribedPredicate<>("annotation package equals any of passed packages") {
            @Override
            public boolean test(JavaAnnotation<?> javaAnnotation) {
                return Arrays.stream(annotationPackages)
                        .anyMatch(annotationPackage -> javaAnnotation.getRawType()
                                .getPackage().getName().equals(annotationPackage));
            }
        };
    }

    private ArchCondition<JavaClass> beValidatedRequestBodyInCorrespondingController() {
        return new ArchCondition<>("is validated request body") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String packageName = item.getPackage().getName();
                String correspondingControllerPackageName = packageName.substring(0, packageName.lastIndexOf('.'));
                boolean allRequestBodyParametersAreValidated = classes.stream()
                        .filter(javaClass -> javaClass.getPackage().getName().equals(correspondingControllerPackageName))
                        .filter(javaClass -> javaClass.isMetaAnnotatedWith(Controller.class))
                        .map(JavaClass::getMethods)
                        .flatMap(Collection::stream)
                        .map(JavaCodeUnit::getParameters)
                        .flatMap(Collection::stream)
                        .filter(javaParameter -> javaParameter.getRawType().equals(item))
                        .allMatch(javaParameter -> javaParameter.isAnnotatedWith(RequestBody.class) &&
                                javaParameter.isAnnotatedWith(Valid.class));
                if (!allRequestBodyParametersAreValidated) {
                    events.add(SimpleConditionEvent.violated(item, "Not all request body parameters with validation annotations are validated in controller"));
                }
            }
        };
    }

    @Test
    void allExceptionsHaveHandlers() {
        ArchRule exceptionHandlersRule = classes()
                .that().resideInAPackage("..exception")
                .and(areChildrenOf(BaseParametrizedException.class))
                .should(haveCorrespondingExceptionHandlerInPackage("..handler"));
        exceptionHandlersRule.allowEmptyShould(true).check(classes);
    }

    private DescribedPredicate<JavaClass> areChildrenOf(Class<?> superClass) {
        return new DescribedPredicate<>("are children of " + superClass.getSimpleName()) {
            @Override
            public boolean test(JavaClass item) {
                return item.getRawSuperclass()
                        .filter(javaClass -> javaClass.reflect().equals(superClass))
                        .isPresent();
            }
        };
    }

    private ArchCondition<JavaClass> haveCorrespondingExceptionHandlerInPackage(String packagePath) {
        return new ArchCondition<>("has corresponding exception handler in " + packagePath) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                classes.stream()
                        .filter(javaClass -> PackageMatchers.of(packagePath).test(javaClass.getPackageName()))
                        .filter(javaClass -> javaClass.isMetaAnnotatedWith(ControllerAdvice.class))
                        .map(JavaClass::getAllMethods)
                        .flatMap(Collection::stream)
                        .filter(javaMethod -> javaMethod.isAnnotatedWith(ExceptionHandler.class))
                        .filter(javaMethod -> javaMethod.isAnnotatedWith(annotationWithPropertyContaining("value", item)))
                        .findAny()
                        .ifPresentOrElse(
                                javaMethod -> {
                                },
                                () -> events.add(SimpleConditionEvent.violated(item, "exception has no corresponding exception handler in " + packagePath))
                        );
            }
        };
    }

    private DescribedPredicate<JavaAnnotation<?>> annotationWithPropertyContaining(String property, Object value) {
        return new DescribedPredicate<>("is annotated with annotation with value containing " + value.toString()) {
            @Override
            public boolean test(JavaAnnotation<?> javaAnnotation) {
                Object annotationPropertyValue = javaAnnotation.getProperties().get(property);
                if (annotationPropertyValue.getClass().isArray()) {
                    return Arrays.asList((Object[]) annotationPropertyValue).contains(value);
                } else {
                    return annotationPropertyValue.equals(value);
                }
            }
        };
    }

    @Test
    void controllerNamesHaveProperPostfix() {
        ArchRule controllerNamesRule = classes()
                .that().resideInAPackage("..controller")
                .should().haveNameMatching(".+Controller$");
        controllerNamesRule.allowEmptyShould(true).check(classes);
    }

    @Test
    void repositoryNamesHaveProperPostfix() {
        ArchRule repositoryNamesRule = classes()
                .that().resideInAPackage("..repository")
                .should().haveNameMatching(".+Repository$");
        repositoryNamesRule.allowEmptyShould(true).check(classes);
    }

    @Test
    void customRepositoryNamesHaveProperPostfix() {
        ArchRule customRepositoryNamesRule = classes()
                .that().resideInAPackage("..repository.custom")
                .should().haveNameMatching(".+RepositoryCustom$");
        customRepositoryNamesRule.allowEmptyShould(true).check(classes);
    }

    @Test
    void enumerationPackageHasEnumsOnly() {
        ArchRule enumsRule = classes()
                .that().resideInAPackage("..enumeration")
                .should().beEnums();
        enumsRule.allowEmptyShould(true).check(classes);
    }

    @Test
    void requestDtosHasProperNamesAndAreRecords() {
        ArchRule requestsRule = classes()
                .that().resideInAPackage("..controller.request")
                .should().beRecords()
                .andShould().haveNameMatching(".+Rq$");
        requestsRule.allowEmptyShould(true).check(classes);
    }

    @Test
    void responseDtosHasProperNamesAndAreRecords() {
        ArchRule responseRule = classes()
                .that().resideInAPackage("..controller.response")
                .and().areNotAnnotatedWith(Generated.class)
                .should().beRecords()
                .andShould().haveNameMatching(".+Response$");
        responseRule.allowEmptyShould(true).check(classes);
    }

    @Test
    void controllersHavePublicStaticFinalPathField() {
        ArchRule controllerPathsRule = classes()
                .that().resideInAPackage("..controller")
                .should(haveFieldWithName("PATH"));
        ArchRule filedNamesRule = fields()
                .that().areDeclaredInClassesThat().resideInAPackage("..controller")
                .and().haveName("PATH")
                .should().bePublic()
                .andShould().beStatic()
                .andShould().beFinal()
                .andShould().haveRawType(String.class);
        controllerPathsRule.allowEmptyShould(true).check(classes);
        filedNamesRule.allowEmptyShould(true).check(classes);
    }

    private ArchCondition<JavaClass> haveFieldWithName(String fieldName) {
        return new ArchCondition<>("has field with name " + fieldName) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getFields().stream()
                        .filter(javaField -> javaField.getName().equals(fieldName))
                        .findAny()
                        .ifPresentOrElse(
                                javaField -> {
                                },
                                () -> events.add(SimpleConditionEvent.violated(item, "no field with name " + fieldName))
                        );
            }
        };
    }

    @Test
    void allServicesMustBeAnnotatedWithServiceAnnotations() {
        ArchRule serviceAnnotationsRule = classes()
                .that().resideInAPackage("..service.impl")
                .should().beAnnotatedWith(Service.class);
        serviceAnnotationsRule.allowEmptyShould(true).check(classes);
    }

    @Test
    void allControllersMustBeMetaAnnotatedWithControllerAnnotations() {
        ArchRule controllerAnnotationsRule = classes()
                .that().resideInAPackage("..controller")
                .should().beMetaAnnotatedWith(Controller.class);
        controllerAnnotationsRule.allowEmptyShould(true).check(classes);
    }

    @Test
    void allEntitiesMustBeAnnotatedWithEntityAnnotations() {
        ArchRule entityAnnotationsRule = classes()
                .that().resideInAPackage("..entity")
                .and().areNotAnnotatedWith(Generated.class)
                .should().beAnnotatedWith(Entity.class)
                .andShould(beAnnotatedWithNamedTableAnnotations());
        entityAnnotationsRule.allowEmptyShould(true).check(classes);
    }

    private ArchCondition<JavaClass> beAnnotatedWithNamedTableAnnotations() {
        return new ArchCondition<>("is annotated with named @Table annotations") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                Stream.of(item)
                        .map(JavaClass::getAnnotations)
                        .flatMap(Collection::stream)
                        .filter(javaAnnotation -> javaAnnotation.getRawType().reflect().equals(Table.class))
                        .map(JavaAnnotation::getProperties)
                        .map(annotationProperties -> annotationProperties.get("name"))
                        .findAny()
                        .map(String.class::cast)
                        .filter(tableName -> !tableName.isBlank())
                        .ifPresentOrElse(
                                tableName -> {
                                },
                                () -> events.add(SimpleConditionEvent.violated(item, "entity has no named @Table annotation"))
                        );

            }
        };
    }

    @Test
    void mapperMethodsMustHaveProperNamesAndArgumentNames() {
        ArchRule toEntityMethodNamesRule = methods()
                .that().areDeclaredInClassesThat().resideInAPackage("..mapper")
                .and().haveRawReturnType(annotatedWith(Entity.class))
                .should().haveName("toEntity");
        ArchRule fromRequestArgumentNamesRule = methods()
                .that().areDeclaredInClassesThat().resideInAPackage("..mapper")
                .and(haveParameterFromPackage("..controller.request"))
                .should(haveParameterName("request"));
        ArchRule toResponseMethodNamesRule = methods()
                .that().areDeclaredInClassesThat().resideInAPackage("..mapper")
                .and().haveRawReturnType(resideInAPackage("..controller.response"))
                .should().haveName("toResponse");
        toEntityMethodNamesRule.allowEmptyShould(true).check(classes);
        fromRequestArgumentNamesRule.allowEmptyShould(true).check(classes);
        toResponseMethodNamesRule.allowEmptyShould(true).check(classes);
    }

    private DescribedPredicate<JavaMethod> haveParameterFromPackage(String packagePath) {
        return new DescribedPredicate<>("has parameters from " + packagePath) {
            @Override
            public boolean test(JavaMethod javaMethod) {
                return PackageMatchers.of(packagePath).test(javaMethod.getOwner().getPackageName());
            }
        };
    }

    private ArchCondition<JavaMethod> haveParameterName(String parameterName) {
        return new ArchCondition<>("has parameter with name " + parameterName) {
            @Override
            public void check(JavaMethod item, ConditionEvents events) {
                Arrays.stream(item.reflect().getParameters())
                        .filter(parameter -> parameter.getName().equals(parameterName))
                        .findAny()
                        .ifPresentOrElse(
                                parameter -> {
                                },
                                () -> events.add(SimpleConditionEvent.violated(item, "no parameter with name " + parameterName))
                        );
            }
        };
    }

    @Test
    void utilityClassesHaveProperMethods() {
        ArchRule staticMethodsRule = methods()
                .that().areDeclaredInClassesThat().resideInAPackage("..util")
                .should().beStatic();
        ArchRule privateConstructorRule = classes()
                .that().resideInAPackage("..util")
                .should().haveOnlyPrivateConstructors();
        staticMethodsRule.allowEmptyShould(true).check(classes);
        privateConstructorRule.allowEmptyShould(true).check(classes);
    }

    @Test
    void constantClassesHaveProperFields() {
        ArchRule methodsRule = classes()
                .that().resideInAPackage("..constant")
                .should().haveOnlyPrivateConstructors()
                .andShould(notContainAnyMethodsExceptConstructor());
        ArchRule publicStaticFinalFieldsRule = fields()
                .that().areDeclaredInClassesThat().resideInAPackage("..constant")
                .should().bePublic()
                .andShould().beStatic()
                .andShould().beFinal()
                .andShould().haveNameMatching("^(?!_)[A-Z_]+(?<!_)$");
        methodsRule.allowEmptyShould(true).check(classes);
        publicStaticFinalFieldsRule.allowEmptyShould(true).check(classes);
    }

    private ArchCondition<JavaClass> notContainAnyMethodsExceptConstructor() {
        return new ArchCondition<>("does not contain any methods except constructor") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                long numberOfNonConstructorMethods = item.getMethods().stream()
                        .filter(javaMethod -> !javaMethod.isConstructor())
                        .count();
                if (numberOfNonConstructorMethods > 0) {
                    events.add(SimpleConditionEvent.violated(item, "there are non-constructor methods"));
                }
            }
        };
    }

    @Test
    void componentAnnotatedFieldsArePrivateAndFinal() {
        ArchRule componentFieldsRule = fields()
                .that().areDeclaredInClassesThat().areMetaAnnotatedWith(Component.class)
                .and().haveRawType(metaAnnotatedWith(Component.class))
                .should().bePrivate()
                .andShould().beFinal();
        componentFieldsRule.allowEmptyShould(true).check(classes);
    }

    @Test
    void thereAreNoTopLevelLayerPackages() {
        ArchRule noTopLevelLayerPackagesRule = noClasses().should().resideInAnyPackage(
                "com.example.archunitrules.entity",
                "com.example.archunitrules.repository",
                "com.example.archunitrules.service",
                "com.example.archunitrules.mapper",
                "com.example.archunitrules.dto",
                "com.example.archunitrules.controller",
                "com.example.archunitrules.constant",
                "com.example.archunitrules.enumeration",
                "com.example.archunitrules.exception",
                "com.example.archunitrules.mapper",
                "com.example.archunitrules.handler"
        );
        noTopLevelLayerPackagesRule.allowEmptyShould(true).check(classes);
    }
}
