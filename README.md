The purposes of this lib is to make it as easy as possible to create containers in
the style of Dank Null with slots exceeding the usual stack size of 64. As this lib is
light weight and doesn't add any content you are expected to Jar In Jar it like

```groovy
implementation jarJar(fg.deobf("mod.traister101:Extended-Slot-Capacity-1.20.1:1.0")) {
    jarJar.ranged(it as Dependency, "[1.0,)")
}
```

This will never be published to curse or other mod hosts. At best it might get published
to a public maven in which case this README will be updated with how to get it from there