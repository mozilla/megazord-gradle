# Mozilla Application Services megazord Gradle Plugin

Plugin for consuming Mozilla Application Services megazord native libraries.

<p align="left">
    <a alt="Version badge" href="https://plugins.gradle.org/plugin/org.mozilla.appservices.megazord-gradle.megazord-gradle">
        <img src="https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/org/mozilla/appservices/megazord-gradle/megazord-gradle/org.mozilla.appservices.megazord-gradle.megazord-gradle.gradle.plugin/maven-metadata.xml.svg?label=megazord-gradle&colorB=brightgreen" /></a>
</p>

## Overview

Mozilla Application Services publishes many native (Rust) code libraries that stand alone: each
published Android ARchive (AAR) contains managed code (`classes.jar`) and multiple `.so` library
files (one for each supported architecture).  That means consuming multiple such libraries entails
at least two `.so` libraries, and each of those libraries includes the entire Rust standard library
as well as (potentially many) duplicated dependencies.  To save space and allow cross-component
native-code Link Time Optimization (LTO, i.e., inlining, dead code elimination, etc) Application
Services also publishes composite libraries -- so called *megazord libraries* or just *megazords* --
that compose multiple Rust components into a single optimized `.so` library file.  The managed code
can be easily configured to use such a megazord without additional changes.

The `megazord-gradle` plugin makes it easy to consume such megazord libraries.

## Getting started

Add the plugin to your root `build.gradle`, like:

```groovy
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath 'gradle.plugin.org.mozilla.megazord-gradle:megazord-gradle:0.1.0'
    }
}
```

Then add a stanza like:

```groovy
apply plugin: 'megazord-gradle'
```

to `build.gradle` files that consume Application Services Android libraries, either directly, like:

```groovy
dependencies {
    implementation 'org.mozilla.sync15:logins:0.11.2'
    implementation 'org.mozilla.fxaclient:fxaclient:0.11.2'
}
```

or transitively, like:

```groovy
dependencies {
    implementation 'org.mozilla.components:service-firefox-accounts:0.34.0'
    implementation 'org.mozilla.components:service-sync-logins:0.34.0'
}
```

You should see substitutions like:

```
Substituting 'org.mozilla.appservices.composites:lockbox' for 'org.mozilla.sync15:logins'
Substituting 'org.mozilla.appservices.composites:lockbox' for 'org.mozilla.fxaclient:fxaclient
```

## Configuration

```groovy
megazord {
    // Define new or modify existing megazord definitions.
    megazords {
        newMegazord {
            moduleIdentifier 'stringGroup:andModule'
            moduleIdentifier 'stringGroup', 'stringModule'
            component 'stringGroup1:andModule1'
            component 'stringGroup1', 'stringModule1'
            component 'stringGroup2:andModule2'
            component 'stringGroup2', 'stringModule2'
            ...
        }
    }

    // Reset to the default megazord definitions.
    useMozillaMegazords()

    // Allow a megazord to be strictly larger than the matched components.
    failIfMegazordIsStrictSuperset = false
}
```

### `failIfMegazordIsStrictSuperset`

By default, the plugin fails if a configuration has a megazord that matches all the known components but includes additional components.  Set this to `false`
to allow such a megazord, like:

```
megazord {
   failIfMegazordIsStrictSuperset = false
}
```

**This may add additional component dependencies to the configuration.**

### `megazords`

New megazord definitions can be defined, and existing megazord definitions modified, using the
`megazords` block.  For example, the "lockbox" megazord could be defined like:

```groovy
megazord {
    megazords {
        lockbox {
            moduleIdentifier 'org.mozilla.appservices.composites:lockbox'
            component 'org.mozilla.fxaclient:fxaclient'
            component 'org.mozilla.sync15:logins'
        }
    }
}
```

while the existing "reference-browser" megazord could be modified to match the "lockbox" megazord
like:

```groovy
megazord {
    megazords {
        "reference-browser" {
            moduleIdentifier 'org.mozilla.appservices.composites:reference-browser'
            components.clear()
            component 'org.mozilla.fxaclient:fxaclient'
            component 'org.mozilla.sync15:logins'
        }
    }
}
```
