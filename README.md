# Cardinal-Components-API-Forge

forge port for [CCA](https://github.com/OnyxStudios/Cardinal-Components-API)

To compensate for the lack of entrypoints in the forge, I will use two solutions.

The first one is to use the annotations under `io.github.tt432.ccaforge.util` package to get the class location.

The second is to define the relationship between the class and the corresponding key in mods.toml, which can be
implemented in the `io.github.tt432.ccaforge.entrypointes` package

---

The mod has been uploaded to modrinth and you can simply use [modrinth maven](https://docs.modrinth.com/docs/tutorials/maven/)
to depend on this project, here is a simple example to use directly:
```gradle
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        filter {
            includeGroup "maven.modrinth"
        }
    }
}

dependencies {
    implementation("maven.modrinth:ccaforge:{mc_version}-{mod_version}")
}
```

---

The custom/cardinal-components that were in mod.json are now defined in mods.toml.

To prevent possible future expansion, we will define an array of strings called cardinal-components in the custom table.

The following is a simple demonstration:

```toml
[custom]
cardinal-components = []
```

For the same reason, I will define the array cardinal-components in the entrypoints table.

Again, the following is a simple example:

```toml
[entrypoints]
cardinal-components = []
```

A description of the toml format can be found at https://toml.io/.

---

Because of the difference in the event system between fabric and forge, this mod also makes adjustments to the events.

The events are now moved to the io.github.tt432.ccaforge.event package to distinguish them from the previous events,
and the comparison between the new event and the original event is as follows

| original              | new                   |
|-----------------------|-----------------------|
| PlayerCopyCallback    | CcaPlayerCopyEvent    |
| PlayerSyncCallback    | CcaPlayerSyncEvent    |
| TrackingStartCallback | CcaTrackingStartEvent |

---

For various reasons, it was finally decided to remove BucketableMixin,
which means that the components attached to the fish will not be saved when bucketed.

This may change and I hope someone does the saving feature.