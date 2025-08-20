# Simple DI - A Lightweight Dependency Injection Framework

Simple DI is a lightweight, zero-dependency (other than `jakarta.inject-api`) dependency injection framework for Java. It implements the [Jakarta Dependency Injection specification (JSR-330)](https://jakarta.ee/specifications/inject/2.0/), providing a simple and powerful way to manage dependencies in your applications.

## Features

*   **JSR-330 Compliant:** Implements the standard Jakarta Dependency Injection annotations.
*   **Lightweight:** No external dependencies other than the `jakarta.inject-api`.
*   **Multiple Injection Types:** Supports constructor, field, and method injection.
*   **Qualifiers:** Use qualifiers to bind and inject different implementations of the same interface.
*   **Scopes:** Supports `@Singleton` scope out of the box and allows defining custom scopes.
*   **Dependency Validation:** Automatically checks for missing and cyclic dependencies at startup.

## Getting Started

To use Simple DI in your project, add the following dependency to your `build.gradle` file.

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.wangyousong.practice:simple-di:1.0-SNAPSHOT' // Replace with the actual version
    implementation 'jakarta.inject:jakarta.inject-api:2.0.1'
}
```

*Note: This project is not yet published to any repository. To use it, you will need to build it from source and publish it to your local Maven repository.*

## Usage

### Basic Setup

The core of the framework is the `ContextConfig` class, which is used to configure bindings, and the `Context` interface, which is used to retrieve components.

```java
// 1. Create a ContextConfig instance
ContextConfig config = new ContextConfig();

// 2. Bind your components
config.bind(MyService.class, MyServiceImpl.class);
config.bind(MyRepository.class, new MyRepositoryImpl()); // Bind to an instance

// 3. Create the context
Context context = config.getContext();

// 4. Retrieve your components
MyService myService = context.get(ComponentRef.of(MyService.class)).get();
```

### Injection

Simple DI supports all standard JSR-330 injection types.

#### Constructor Injection

```java
public class MyServiceImpl implements MyService {
    private final MyRepository repository;

    @Inject
    public MyServiceImpl(MyRepository repository) {
        this.repository = repository;
    }

    // ...
}
```

#### Field Injection

```java
public class MyServiceImpl implements MyService {
    @Inject
    private MyRepository repository;

    // ...
}
```

#### Method Injection

```java
public class MyServiceImpl implements MyService {
    private MyRepository repository;

    @Inject
    public void setRepository(MyRepository repository) {
        this.repository = repository;
    }

    // ...
}
```

### Qualifiers

If you have multiple implementations of the same interface, you can use qualifiers to distinguish between them.

First, define a qualifier annotation:

```java
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageType {
    String value();
}
```

Then, use the qualifier to bind different implementations:

```java
config.bind(MessageService.class, new EmailService(), new MessageTypeLiteral("email"));
config.bind(MessageService.class, new SmsService(), new MessageTypeLiteral("sms"));
```

Finally, use the qualifier to inject the desired implementation:

```java
public class MyService {
    @Inject
    @MessageType("email")
    private MessageService emailService;

    @Inject
    @MessageType("sms")
    private MessageService smsService;

    // ...
}
```

### Scopes

Simple DI supports the `@Singleton` scope by default.

```java
@Singleton
public class MySingletonService {
    // ...
}

// Bind the singleton
config.bind(MySingletonService.class, MySingletonService.class);

// Every time you retrieve this service, you will get the same instance.
MySingletonService instance1 = context.get(ComponentRef.of(MySingletonService.class)).get();
MySingletonService instance2 = context.get(ComponentRef.of(MySingletonService.class)).get();
assertSame(instance1, instance2);
```

You can also define custom scopes. First, define a scope annotation:

```java
@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCustomScope {
}
```

Then, create a `ScopeProvider` and register it with the `ContextConfig`:

```java
public class MyCustomScopeProvider implements ScopeProvider {
    // ... implementation of the scope provider
}

config.scope(MyCustomScope.class, new MyCustomScopeProvider());
```

Now you can use your custom scope on your components:

```java
@MyCustomScope
public class MyScopedService {
    // ...
}
```

## Building from Source

To build the project from source, clone the repository and use the Gradle wrapper:

```bash
git clone https://github.com/your-username/simple-di.git
cd simple-di
./gradlew build
```

To publish the artifact to your local Maven repository, run:

```bash
./gradlew publishToMavenLocal
```

This will make it available to other projects on your local machine.
