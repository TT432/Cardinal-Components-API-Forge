* CCA-Forge

forge port for [CCA](https://github.com/OnyxStudios/Cardinal-Components-API)

To compensate for the lack of entrypoints in the forge, I will use two solutions.

The first one is to use the annotations under `io.github.tt432.ccaforge.util` package to get the class location.

The second is to define the relationship between the class and the corresponding key in mods.toml, which can be
implemented in the `io.github.tt432.ccaforge.entrypointes` package 

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