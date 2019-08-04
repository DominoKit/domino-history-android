# domino-history-android

Adding support for [domino-rest](https://github.com/DominoKit/domino-history) in android applications.

**Please note that the library needs a minimum SDK version 24 and it uses Java 8.**

## Setup

- Adding these two dependencies:

```
implementation 'org.dominokit:domino-history-android:1.0-rc.4-SNAPSHOT'
```

> To use the snapshot version without building locally, configure the snapshot repository
```
repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots"
    }
    maven {
        url "https://repo.vertispan.com/gwt-snapshot"
    }
}
```

- Some of the dependencies have files under `META-INF` so we need to set the packaging options for android apk:
```
packagingOptions {
    exclude 'META-INF/gwt/*'
    exclude '**/*.gwt.xml'
}
```

#### for other features, please refer to [the main documentation for domino-history](https://github.com/DominoKit/domino-history)
=======
