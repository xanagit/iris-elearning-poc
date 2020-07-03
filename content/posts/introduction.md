---
title: Introduction
date: 2020-07-03
published: true
tags: ["Springboot", "Tests unitaires", "Java"]
series: false
cover_image: ./images/springboot-unit-tests/springboot-unit-tests-cover-img.png
canonical_url: false
description: "Test Unitaires avec SpringBoot API REST SuAPI"
---

##### Les structures d'erreur

La structure de retour d'erreur à utiliser est la suivante : ''List<fr.su.suapi.objects.error.ErrorDO>''.

Un certain nombre d'utilitaires, décrits ci-dessous, vont permettre de faciliter la gestion de ces classes.

La structure JSON d'un objet ErrorDO est la suivante:

```javascript
{
  "field" : "nomDuChamp",
  "code" : "code.de.l'erreur",
  "label" : "Le message d'erreur",
  "path" : [ "chemin", "accès", 2, "nomDuChamp"],
  "value" : "valeur actuelle",
  "limit" : "valeur limite de la contrainte"
}
```

Exemple:
Une contrainte de taille non respectée sur une chaine de caractères:

```java
@Length(min=1, max = 2)
String field = "value";
```

L'erreur de retour sera:

```javascript
{
"field" : "field",
"code" : "string.max",
"label" : "la longueur doit être comprise entre 1 et 2 caractères",
"path" : [ "field" ],
"value" : "value",
"limit" : 2
}
```

##### Les fichiers de message

Les messages d'erreur doivent être externalisés dans des fichiers properties. Dans l'enveloppe de base, deux fichiers properties sont référencés : un fichier d'erreur générique (module ''SuApi'' du ''SuTools''), et un fichier spécifique où vous allez pouvoir renseigner les messages d'erreur spécifiques à votre application.

Dans l'enveloppe API, la déclaration de ces fichiers est automatique.

Extrait de la classe principale de l'application (''MonAppliApplication'') :

```java
@Bean
public MessageSource messageSource() {
ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
messageSource.setBasenames("classpath:/messages", "classpath:/defaultErrorMessages");

    return messageSource;

}
```

##### Gestion des erreurs

SuAPI met à disposition 2 fonctionalités de gestion d'erreur. La première permet de générer automatiquement un objet **ErrorDO** à partir d'annotation hibernate validator. La seconde permet de réaliser ses propres validations et générer la liste d'**ErrorDO** associée.

#### Erreurs gérées automatiquement

Tous les champs annotés par des annotations hibernate validator (@NotNull, @Size, ...) sont directement géré par SuAPI qui se charge de la transforamtion en liste d'**ErrorDO** des éventuelles ConstraintViolatation générée lors de la validation d'un BE.

### Exemples de mapping d'erreur

## AssertFalse

Déclaration dans un BE:

```java
@AssertFalse boolean bool = true;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "bool",
"code" : "msg.error.code",
"label" : "Custom Label",
"path" : [ "bool" ],
"value" : true,
"limit" : null
}
```

---

Déclaration dans un BE:

```java
@Valid Object[] rootPath = new Object[] {
new Object(), new Object(), new Object() {
@AssertFalse Boolean bool = true;
}
};
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "bool",
"code" : "any.invalid",
"label" : "doit être faux",
"path" : [ "rootPath", 2, "bool" ],
"value" : true,
"limit" : null
}
```

---

== AssertTrue ==
Déclaration dans un BE:

```java
@AssertTrue Boolean bool = false;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "bool",
"code" : "any.invalid",
"label" : "doit être vrai",
"path" : [ "bool" ],
"value" : false,
"limit" : null
}
```

---

== CodePointLength ==
Déclaration dans un BE:

```java
@CodePointLength(min = 1, max = 4) String field = "abcdef";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "la longueur doit être comprise entre 1 et 4 caractères",
"path" : [ "field" ],
"value" : "abcdef",
"limit" : 4
}
```

---

== CreditCardNumber ==
Déclaration dans un BE:

```java
@CreditCardNumber String field = "invalid";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "string.creditCard",
"label" : "numéro de carte de crédit invalide",
"path" : [ "field" ],
"value" : "invalid",
"limit" : null
}
```

---

== Currency ==
Déclaration dans un BE:

```java
@Currency(value = { "EUR", "CHF" })
MonetaryAmount field = getDefaultAmountFactory().setCurrency(getCurrency("USD")).setNumber(200).create();
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "devise invalide (doit faire partie de [EUR, CHF])",
"path" : [ "field" ],
"value" : null,
"limit" : [ "EUR", "CHF" ]
}
```

---

== DecimalMax ==
Déclaration dans un BE:

```java
@DecimalMax("2") String field = "4";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.max",
"label" : "doit être inférieur ou égal à 2",
"path" : [ "field" ],
"value" : "4",
"limit" : "2"
}
```

---

Déclaration dans un BE:

```java
@DecimalMax("1") float field = 2.0F;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.max",
"label" : "doit être inférieur ou égal à 1",
"path" : [ "field" ],
"value" : 2.0,
"limit" : "1"
}
```

---

== DecimalMin ==
Déclaration dans un BE:

```java
@DecimalMin("10") String field = "2";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.min",
"label" : "doit être supérieur ou égal à 10",
"path" : [ "field" ],
"value" : "2",
"limit" : "10"
}
```

---

Déclaration dans un BE:

```java
@DecimalMin("10") float field = 2.0F;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.min",
"label" : "doit être supérieur ou égal à 10",
"path" : [ "field" ],
"value" : 2.0,
"limit" : "10"
}
```

---

== Digits ==
Déclaration dans un BE:

```java
@Digits(integer = 1, fraction = 1) String field = "100.100";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.precision",
"label" : "valeur numérique hors limite (<1 chiffres>.<1 chiffres> attendu)",
"path" : [ "field" ],
"value" : "100.100",
"limit" : null
}
```

---

== DurationMax ==
Déclaration dans un BE:

```java
@DurationMax(days = 2) Duration field = Duration.ofDays(4);
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "doit être plus court que ou égal à 2 jours",
"path" : [ "field" ],
"value" : null,
"limit" : null
}
```

---

== DurationMin ==
Déclaration dans un BE:

```java
@DurationMin(days = 2) Duration field = Duration.ofDays(1);
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "doit être plus long que ou égal à 2 jours",
"path" : [ "field" ],
"value" : null,
"limit" : null
}
```

---

== EAN ==
Déclaration dans un BE:

```java
@EAN(type = EAN.Type.EAN8) String field = "invalid";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "code barre EAN8 invalide",
"path" : [ "field" ],
"value" : "invalid",
"limit" : "EAN8"
}
```

---

== Email ==
Déclaration dans un BE:

```java
@Email String field = "invalid";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "string.email",
"label" : "doit être une adresse email bien formée",
"path" : [ "field" ],
"value" : "invalid",
"limit" : null
}
```

---

== Future ==
Déclaration dans un BE:

```java
@Future Date field = new Date(Long.MIN_VALUE);
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "doit être dans le futur",
"path" : [ "field" ],
"value" : -9223372036854775808,
"limit" : null
}
```

---

== FutureOrPresent ==
Déclaration dans un BE:

```java
@FutureOrPresent Date field = new Date(Long.MIN_VALUE);
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "doit être dans le présent ou dans le futur",
"path" : [ "field" ],
"value" : -9223372036854775808,
"limit" : null
}
```

---

== ISBN ==
Déclaration dans un BE:

```java
@ISBN(type = ISBN.Type.ISBN_10) String field = "value";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "ISBN invalide",
"path" : [ "field" ],
"value" : "value",
"limit" : "ISBN_10"
}
```

---

== Length ==
Déclaration dans un BE:

```java
@Length(max = 2) String field = "value";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "string.max",
"label" : "la longueur doit être comprise entre 0 et 2 caractères",
"path" : [ "field" ],
"value" : "value",
"limit" : 2
}
```

---

Déclaration dans un BE:

```java
@Length(min=1, max = 2) String field = "value";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "string.max",
"label" : "la longueur doit être comprise entre 1 et 2 caractères",
"path" : [ "field" ],
"value" : "value",
"limit" : 2
}
```

---

Déclaration dans un BE:

```java
@Length(min = 10) String field = "value";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "string.min",
"label" : "la longueur doit être comprise entre 10 et 2147483647 caractères",
"path" : [ "field" ],
"value" : "value",
"limit" : 10
}
```

---

== LuhnCheck ==
Déclaration dans un BE:

```java
@LuhnCheck String field = "abc";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "le chiffre de contrôle pour abc est invalide, le contrôle Luhn Modulo 10 a échoué",
"path" : [ "field" ],
"value" : "abc",
"limit" : null
}
```

---

== Max ==
Déclaration dans un BE:

```java
@Max(2) String field = "4";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.max",
"label" : "doit être au maximum égal à 2",
"path" : [ "field" ],
"value" : "4",
"limit" : 2
}
```

---

Déclaration dans un BE:

```java
@Max(1) float field = 2.0F;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.max",
"label" : "doit être au maximum égal à 1",
"path" : [ "field" ],
"value" : 2.0,
"limit" : 1
}
```

---

== Min ==
Déclaration dans un BE:

```java
@Min(10) String field = "2";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.min",
"label" : "doit être au minimum égal à 10",
"path" : [ "field" ],
"value" : "2",
"limit" : 10
}
```

---

Déclaration dans un BE:

```java
@Min(10) float field = 2.0F;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.min",
"label" : "doit être au minimum égal à 10",
"path" : [ "field" ],
"value" : 2.0,
"limit" : 10
}
```

---

== Mod10Check ==
Déclaration dans un BE:

```java
@Mod10Check String field = "100.100";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "le chiffre de contrôle pour 100.100 est invalide, le contrôle Modulo 10 a échoué",
"path" : [ "field" ],
"value" : "100.100",
"limit" : null
}
```

---

== Negative ==
Déclaration dans un BE:

```java
@Negative int field = 0;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.negative",
"label" : "doit être strictement négatif",
"path" : [ "field" ],
"value" : 0,
"limit" : null
}
```

---

Déclaration dans un BE:

```java
@Negative int field = 1;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "number.negative",
"label" : "doit être strictement négatif",
"path" : [ "field" ],
"value" : 1,
"limit" : null
}
```

---

== NegativeOrZero ==
Déclaration dans un BE:

```java
@NegativeOrZero int field = 1;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "doit être négatif ou égal à 0",
"path" : [ "field" ],
"value" : 1,
"limit" : null
}
```

---

== NotBlank ==
Déclaration dans un BE:

```java
@NotBlank String field = " ";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.required",
"label" : "ne peut pas être vide",
"path" : [ "field" ],
"value" : " ",
"limit" : null
}
```

---

== NotEmpty ==
Déclaration dans un BE:

```java
@NotEmpty String field = EMPTY;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.required",
"label" : "ne peut pas être vide",
"path" : [ "field" ],
"value" : "",
"limit" : null
}
```

---

== NotNull ==
Déclaration dans un BE:

```java
@NotNull String field;
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.required",
"label" : "ne peut pas être nul",
"path" : [ "field" ],
"value" : null,
"limit" : null
}
```

---

== Null ==
Déclaration dans un BE:

```java
@Null String field = "abc";
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "doit être nul",
"path" : [ "field" ],
"value" : "abc",
"limit" : null
}
```

---

== Past ==
Déclaration dans un BE:

```java
@Past Date field = new Date(Long.MAX_VALUE);
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "doit être dans le passé",
"path" : [ "field" ],
"value" : 9223372036854775807,
"limit" : null
}
```

---

== PastOrPresent ==
Déclaration dans un BE:

```java
@PastOrPresent Date field = new Date(Long.MAX_VALUE);
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "doit être dans le passé ou dans le présent",
"path" : [ "field" ],
"value" : 9223372036854775807,
"limit" : null
}
```

---

Déclaration dans un BE:

```java
@PastOrPresent LocalDate field = LocalDateTime.of(2019, 04, 12, 15, 30).toLocalDate();
```

ErrorDO au format JSON renvoyé:

```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "doit être dans le passé ou dans le présent",
"path" : [ "field" ],
"value" : {
"year" : 4019,
"month" : "APRIL",
"dayOfYear" : 102,
"leapYear" : false,
"monthValue" : 4,
"dayOfMonth" : 12,
"dayOfWeek" : "FRIDAY",
"era" : "CE",
"chronology" : {
"id" : "ISO",
"calendarType" : "iso8601"
}
}
"limit" : null
}
```

---

== Pattern ==
Déclaration dans un BE:

````java
@Pattern(regexp = "[A-Z]+") String field = "abc";
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "string.regex.base",
"label" : "doit respecter \"[A-Z]+\"",
"path" : [ "field" ],
"value" : "abc",
"limit" : "[A-Z]+"
}
```

---

== Positive ==
Déclaration dans un BE:
```java
@Positive int field = 0;
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "number.positive",
"label" : "doit être strictement positif",
"path" : [ "field" ],
"value" : 0,
"limit" : null
}
```

---

Déclaration dans un BE:
```java
@Positive int field = -1;
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "number.positive",
"label" : "doit être strictement positif",
"path" : [ "field" ],
"value" : -1,
"limit" : null
}
```

---

== PositiveOrZero ==
Déclaration dans un BE:
```java
@PositiveOrZero int field = -1;
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "doit être positif ou égal à 0",
"path" : [ "field" ],
"value" : -1,
"limit" : null
}
```

---

== Range ==
Déclaration dans un BE:
```java
@Range(min = 10) String field = "1";
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "string.min",
"label" : "doit être entre 10 et 9223372036854775807",
"path" : [ "field" ],
"value" : "1",
"limit" : 10
}
```

---

Déclaration dans un BE:
```java
@Range(max = 1) int field = 20;
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "number.max",
"label" : "doit être entre 0 et 1",
"path" : [ "field" ],
"value" : 20,
"limit" : 1
}
```

---

== SafeHtml ==
Déclaration dans un BE:
```java
@SafeHtml(whitelistType = SafeHtml.WhiteListType.NONE) String field = "<p>wrong hmtl</p>";
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "peut contenir du HTML dangereux",
"path" : [ "field" ],
"value" : "<p>wrong hmtl</p>",
"limit" : null
}
```

---

== ScriptAssert ==
Déclaration dans un BE:
```java
@Valid ScriptAssertTest field = new ScriptAssertTest();

@ScriptAssert(lang = "javascript", script = "\_this < 25")
class ScriptAssertTest {
private int property = 60;
}
````

ErrorDO au format JSON renvoyé:

````javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "le script \"\_this < 25\" n'a pas été évalué à vrai",
"path" : [ "field" ],
"value" : null,
"limit" : null
}
```

---

== Size ==
Déclaration dans un BE:
```java
@Size(min = 10) String field = "value";
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "string.min",
"label" : "la taille doit être comprise entre 10 et 2147483647",
"path" : [ "field" ],
"value" : "value",
"limit" : 10
}
```

---

Déclaration dans un BE:
```java
@Size(max = 1) List<String> field = asList("value", "value2");
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "array.max",
"label" : "la taille doit être comprise entre 0 et 1",
"path" : [ "field" ],
"value" : [ "value", "value2" ],
"limit" : 1
}
```

---

Déclaration dans un BE:
```java
@Size(max = 1) List<ObjectTest> field = asList(new ObjectTest("Patrick", 44), new ObjectTest("Sébastien", 38));
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "array.max",
"label" : "la taille doit être comprise entre 0 et 1",
"path" : [ "field" ],
"value" : [ {
"name" : "Patrick",
"age" : 44
}, {
"name" : "Sébastien",
"age" : 38
} ],
"limit" : 1
}
````

---

== UniqueElements ==
Déclaration dans un BE:

````java
@UniqueElements List<String> field = asList("a", "b", "a");
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "array.unique",
"label" : "ne doit contenir que des éléments uniques",
"path" : [ "field" ],
"value" : [ "a", "b", "a" ],
"limit" : null
}
```

---

== URL ==
Déclaration dans un BE:
```java
@URL String field = "invalid url";
```
ErrorDO au format JSON renvoyé:
```javascript
{
"field" : "field",
"code" : "any.invalid",
"label" : "URL mal formée",
"path" : [ "field" ],
"value" : "invalid url",
"limit" : null
}
```

#### Validations (des annotations et manuelles) ####

Dans le cas ou les annotations du BE sont suffisantes (pas de besoin de test manuel), il est possible d'utiliser directement la classe ''fr.su.suapi.validation.BEAnnotationsValidator''.

Si des validations manuelles complémentaires aux annotaions sont nécessaires, SuAPI fourni une classe ''AbstractBEValidator'' à hériter pour réaliser la validation d'un BE. Cette classe contient les méthodes suivantes:

- //List<ErrorDO> **customValidation**(T object)//: méthode abstraite à implémenter qui réalisera les diverses validations manuelles et retournera une liste d'object **ErrorDO**.
- //final List<ErrorDO> **validateAndReturn**(T object)//: méthode à appeler depuis le service (package //services.business.impl//) pour réaliser la validation des annotation hibernate et les éventuelles validations manuelles pour un BE donné. Retourne une liste d'objet **ErrorDO** ou une liste vide si aucune erreur n'est trouvée.
- //final void **validateAndThrow**(T object)//: Réalise le même traitement que **validateAndReturn** et lance une **BusinessException** si au moins une erreur est trouvée.
- //final List<ErrorDO> **validateAndReturn**(List<T> objects)//: réalise le même traitement que **validateAndReturn** pour une liste de BE.
- //final void **validateAndThrow**(List<T> objects)//: réalise le même traitement que **validateAndThrow** pour une liste de BE.

=== Exemple d'implémentation ===

Voici un exemple d'implémentation d'un validateur de BE en utilisant la classe ''AbstractBEValidator''. Pour une explication de l'utilisation des classes ErrorDOBuilderFactory et ErrorDOBuilder, voir dans la **section suivante**.

```java
 class AbstractBEValidatorImpl extends AbstractBEValidator<TestBE> {

         ErrorDOBuilderFactory factory;

         // Constructeur
         public AbstractBEValidatorImpl(
                 ErrorDOMapper errorDOMapper, Validator validator,
                 ErrorDOBuilderFactory errorDOBuilderFactory) {
             super(errorDOMapper, validator);
             factory = errorDOBuilderFactory;
         }

         // Implémentation de la méthode abstraite customValidation de la classe AbstractBEValidator.
         // Utilisation de ErrorDOBuilder pour la concaténation des diverses listes d'ErrorDO
         @Override
         public List<ErrorDO> customValidation(TestBE object) {
             ErrorDOBuilder builder = factory.getBuilder();
             return builder.appendErrors(checkDates(object)).appendErrors(checkStr(object)).getAll();
         }

         // Vérifie que la date start est antérieur à la date end
         // Utilisation de ErrorDOBuilder pour construire la liste d'ErreurDO de retour
         private List<ErrorDO> checkDates(TestBE object) {
             ErrorDOBuilder builder = factory.getBuilder();
             if (object.getStart().after(object.getEnd())) {
                 return builder.appendCodeAndLabel("invalid.start").appendPathAndField("start")
                         .appendCurrentAndLimitValue(object.getStart(), object.getEnd()).getAll();
             }

             return new ArrayList<>();
         }

         // Vérifie que le champ str ne contient pas le charactère '#'
         // Utilisation de ErrorDOBuilder pour construire la liste d'ErreurDO de retour
         private List<ErrorDO> checkStr(TestBE object) {
             ErrorDOBuilder builder = factory.getBuilder();
             if (object.getStr().contains("#")) {
                 return builder.appendCodeAndLabel("invalid.str").appendPathAndField("str")
                         .appendCurrentAndLimitValue(object.getStr(), null).getAll();
             }

             return new ArrayList<>();
         }
     }

     // BE à tester
     class TestBE {
         @NotNull
         private Integer id;
         @Length(min = 5)
         private String str;

         private Date start;
         private Date end;

         // Constructor, getter & setters
         ...
     }

````

=== Exemple d'utilisation de BEAnnotationsValidator (pas de validation manuelle) ===
Dans le cas ou les annotations du BE sont suffisantes (pas de besoin de test manuel), il n'est pas nécessaire de créer une classe d'implémentation de **AbstractBEValidator**. Il est possible d'utiliser directement la classe ''fr.su.suapi.validation.BEAnnotationsValidator''. Exemple d'implémentation pour un service client:

```java
@Service
public class ClientsLBS implements ClientsILBS {

private BEAnnotationsValidator<ClientBE> validator;
private ClientIDAO clientDAO;

public ClientsLBS(ClientIDAO clientDAO, BEAnnotationsValidator<ClientBE> validator) {
this.clientDAO = clientDAO;
this.validator = validator;
}

@Override
public ClientBE createClient(ClientBE clientBE) throws BusinessException {
validator.validateAndThrow(clientBE);

    // enregistrement
    return clientDAO.save(clientBE);

}
}
```

#### Comment écrire les messages d'erreur dans le fichier properties ?

Contrairement aux versions 4._ et 5._, la version 6.\* de SuAPI ne nécessite plus le préfix **error** et les suffixes **label** & **field** dans le fichier //messages.properties//.
De plus, tous les messages issus d'erreurs de validation liés aux annotations hibernate ne nécessitent pas de code d'erreur dans le fichier //messages.properties//.

<WRAP column 47% box>
<WRAP indent>
<color tomato><del>error.</del></color><color #3269a8>**client.ref.obligatoire**</color><color tomato><del>.label</del></color>=<color #1d7306>La référence est obligatoire</color>
<color tomato><del>error.</del></color><color #3269a8>**client.ref.obligatoire**</color><color tomato><del>.field</del></color>=<color #1d7306>reference</color>
</WRAP>
</WRAP>

<WRAP column 47% box>
<WRAP indent>
\\
<color #3269a8>**client.ref.obligatoire**</color>=<color #1d7306>La référence est obligatoire</color>
</WRAP>
</WRAP>

\\

<WRAP tip 97%>
Pour construire le code de l'erreur, il faut essayer au maximum de se rapprocher du pattern suivant : ''nomEntite.nomChamp.causeErreur''.

Exemples :

- ''client.ref.obligatoire'' : le champ "référence" de l'entité "client" est obligatoire
- ''client.nom.taillemax'' : le champ "nom" de l'entité "client" ne doit pas dépasser 15 caractères
- ''client.nom.format'' : le champ "nom" de l'entité "client" ne doit pas contenir de numériques
  </WRAP>

##### Utilitaires pour gérer les erreurs

La classe ''fr.su.suapi.exception.util.ErrorDOBuilder'' facilite la création d'un ''ErreurDO'' à partir d'un code d'erreur. La récupération du builder se fait en utilisant la factory ''fr.su.suapi.exception.util.ErrorDOBuilderFactory''.

Exemple de récupération et d'utilisation de ''ErrorDOBuilder'' en utilisant la factory ''ErrorDOBuilderFactory'':

```java
@Autowire
ErrorDOBuilderFactory factory;

factory.getBuilder().appendCodeAndLabel("invalid.field")
.appendPathAndField("path", 2, "field")
.appendCurrentAndLimitValue(object.getStart(), object.getEnd())
.appendNewErrorDO().appendCodeAndLabel("invalid.field2")
.getAll();
}
```

Le builder fourni les méthodes suivantes: - **appendCodeAndLabel**(String code, Object... args): ajoute le code de l'erreur et les arguments éventuels du label associé dans **messages.properties** - **appendCurrentAndLimitValue**(Object currentValue, Object limitValue): ajoute la valeur courante et la valeur limite du champ - **appendErrors**(List<ErrorDO> errors): ajoute une liste d'erreur à celle du builder - **appendNewErrorDO**(): ajoute un nouvel objet **ErrorDO** à la liste du builder. Les méthodes **appendCurrentAndLimitValue**, **appendCodeAndLabel** et **appendPathAndField** s'appliquerons à ce nouvel objet. - **appendPathAndField**(Object... path): ajoute le chemin vers le champ en erreur. - **clear**(): réinitialise la liste d'**ErrorDO** du builder - **getAll**(): récupère la liste des **ErrorDO** créés - **getOne**(): récupère l'objet **ErrorDO** en cours de création dans le builder

Exemple:
Le code suivant:

```java
      @Autowire
      ErrorDOBuilderFactory factory;

      factory.getBuilder().appendCodeAndLabel("invalid.field")
           .appendPathAndField("path", 2, "field")
           .appendCurrentAndLimitValue("abc#", "#")
           .appendNewErrorDO().appendCodeAndLabel("invalid.field2")
           .getAll();

```

Donnera le JSON suivant:

```javascript
[
  {
    field: "field",
    code: "invalid.field",
    label:
      "<le label qui correspond au code invalid.field dans le fichier messages.properties>",
    path: ["path", 2, "field"],
    value: "abc#",
    limit: "#",
  },
  {
    field: null,
    code: "invalid.field2",
    label:
      "<le label qui correspond au code invalid.field2 dans le fichier messages.properties>",
    path: null,
    value: null,
    limit: null,
  },
];
```

Dans la couche d'exposition, il suffit de renvoyer l'exception, un intercepteur automatique (module SuAPI : ''fr.su.suapi.exception.handler.GlobalExceptionHandler'') se chargera de l'exposition.

##### Utilitaire pour le test de classes de validation (AbstractBEValidator)

La classe de test BEValidatorTest permet de tester l'implémentation de la validation (par annotation et manuelle) réalisée à l'aide de AbstractBEValidator ([[gestion_des_erreurs_suapi_6#exemple_d_implementation| Exemple d'implémentation de validation de BE]]).

L'utilisation se fait comme suit:

=== Héritage de BEValidatorTest ===
Il faut hériter de BEValidatorTest en spécifiant le type du BE testé:

```java
class AbstractBEValidatorTest extends BEValidatorTest<AbstractBEValidatorTest.TestBE>
```

===@Before===

```java
Il faut faire appel à la méthode setUp de la classe mère:

@Before
public void setUp() {
super.setUp();
beValidator = new BEValidatorImpl(errorFactory, validator, errorDOBuilderFactory);
}
```

=== Implémentation de la méthode abstraite getObjectValid ===
Il faut faire appel à la méthode setUp de la classe mère:

```java
@Override
public TestBE getObjectValid() {
return new TestBE(1, "abcde", new Date(10L), new Date(20L));
}
```

=== Exemple complet d'implémentation de test ===

```java
import fr.su.suapi.exception.util.ErrorDOBuilder;
import fr.su.suapi.exception.util.ErrorDOBuilderFactory;
import fr.su.suapi.exception.util.ErrorDOMapper;
import fr.su.suapi.objects.error.ErrorDO;
import fr.su.suapi.utils.validation.BEValidatorTest;
import org.hibernate.validator.constraints.Length;
import org.junit.Before;
import org.junit.Test;

import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AbstractBEValidatorTest extends BEValidatorTest<AbstractBEValidatorTest.TestBE> {

    @Before
    public void setUp() {
        super.setUp();
        beValidator = new AbstractBEValidatorTest.AbstractBEValidatorImpl(errorDOMapper, validator,
                errorDOBuilderFactory);
    }

    @Override
    public TestBE getObjectValid() {
        return new TestBE(1, "abcde", new Date(10L), new Date(20L));
    }

    @Test
    public void testBE_is_not_valid_when_id_is_not_present() {
        object.setId(null);

        checkInvalidityOfCode(object, "any.required");
    }

    @Test
    public void testBE_is_not_valid_when_start_date_is_after_end_date() {
        object.setStart(new Date(40L));

        checkInvalidityOfCode(object, "invalid.start");
        checkInvalidityOfField(object, "start");
        checkInvalidityOfLabel(object, "message");
        checkInvalidityOfLimit(object, new Date(20L));
        checkInvalidityOfValue(object, new Date(40L));
        checkInvalidityOfPath(object, "start");
    }

    @Test
    public void testBE_is_not_valid_when_str_contains_hash() {
        object.setStr("__#__");

        checkInvalidityOfCode(object, "invalid.str");
    }

    class AbstractBEValidatorImpl extends AbstractBEValidator<TestBE> {

        ErrorDOBuilderFactory factory;

        public AbstractBEValidatorImpl(
                ErrorDOMapper errorDOMapper, Validator validator,
                ErrorDOBuilderFactory errorDOBuilderFactory) {
            super(errorDOMapper, validator);
            factory = errorDOBuilderFactory;
        }

        @Override
        public List<ErrorDO> customValidation(TestBE object) {
            ErrorDOBuilder builder = factory.getBuilder();
            return builder.appendErrors(checkDates(object)).appendErrors(checkStr(object)).getAll();
        }

        private List<ErrorDO> checkDates(TestBE object) {
            ErrorDOBuilder builder = factory.getBuilder();
            if (object.getStart().after(object.getEnd())) {
                return builder.appendCodeAndLabel("invalid.start").appendPathAndField("start")
                        .appendCurrentAndLimitValue(object.getStart(), object.getEnd()).getAll();
            }

            return new ArrayList<>();
        }

        private List<ErrorDO> checkStr(TestBE object) {
            ErrorDOBuilder builder = factory.getBuilder();
            if (object.getStr().contains("#")) {
                return builder.appendCodeAndLabel("invalid.str").appendPathAndField("str")
                        .appendCurrentAndLimitValue(object.getStr(), null).getAll();
            }

            return new ArrayList<>();
        }
    }

    class TestBE {
        @NotNull
        private Integer id;
        @Length(min = 5)
        private String str;

        private Date start;
        private Date end;

        // Constructeur, getters & setters

    }

}
```

```

```
