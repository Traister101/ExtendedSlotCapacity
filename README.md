The purposes of this lib is to make it as easy as possible to create containers in
the style of Dank Null with slots exceeding the usual stack size of 64. As this lib is
lightweight and doesn't add any content you are expected to Jar In Jar it. Examples for [Kotlin](#kotlin-example)
and [Groovy](#groovy-example) are down below.

This will never be published to curse or other mod hosts. Instead, grab it from GithubPackages

### Build file examples

These assume a `extendedSlotCapacityVersion` variable with the version of the library you want to depend on.
Additionally, they use the [gpr-for-gradle](https://plugins.gradle.org/plugin/io.github.0ffz.github-packages) plugin
which makes depending on GithubPackages more convenient. Consult
the [GithubPackage docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
for how to do it with Gradle alone

<details>
<summary>Kotlin example</summary>

#### Kotlin example

```kotlin
repositories {
    githubPackage("traister101/ExtendedSlotCapacity") {
        name = "Extended Slot Capacity"
        content {
            includeGroup("mod.traister101")
        }
    }
}

dependencies {
    jarJar(implementation(fg.deobf("mod.traister101:Extended-Slot-Capacity-1.20.1:$extendedSlotCapacityVersion")) {
        jarJar.ranged(this, "[$extendedSlotCapacityVersion,)")
    })
}
```

</details>

<details>
<summary>Groovy example</summary>

#### Groovy example

```groovy
repositories {
    githubPackage.invoke("traister101/ExtendedSlotCapacity")
}

dependencies {
    implementation jarJar(fg.deobf("mod.traister101:Extended-Slot-Capacity-1.20.1:extendedSlotCapacityVersion")) {
        jarJar.ranged(it as Dependency, "[extendedSlotCapacityVersion,)")
    }
}
```

</details>