![Blaze Map Logo](https://raw.githubusercontent.com/LordFokas/BlazeMap/master/images/BlazeMap_Logo.png)

The open source mapping mod for Forge.

Blaze Map is designed to be easily extensible, providing an API to make it simple for other mods
to integrate their own mapping needs. See [Cartography](https://github.com/LordFokas/Cartography) as an example.

Blaze Map also works independently on both the client and the server, meaning you can either connect
to a server with Blaze Map to centralise the mapping work _or_ you can connect to a vanilla server and
have the mapping run purely client side.

## For Contributors

To build Blaze Map locally, make sure to have a copy of the Embeddium binary (jar) stored under `/libs`.
This is because Embeddium is a build time dependency, even if only an optional runtime dependency.

You can see what version of the Embeddium binary is needed by looking at which version is listed 
in `build.gradle` under "dependencies". 

You may need to create the `/libs` folder at the root of the project.

### Local Dev

To set up your IDE to be able to access the deobfuscated Minecraft classes, make sure your IDE
is configured for both Java 17 and Gradle and then run the setup applicable to your IDE:

```powershell
gradlew genEclipseRuns
gradlew genIntellijRuns
gradlew genVSCodeRuns
```

To build and run the local dev server in single player mode (client with integrated server):

```powershell
gradlew runClient
```

To build and run just the server:

```powershell
gradlew runServer
```

To view all available commands:

```powershell
gradlew tasks
```

### Prod Jar

To build the prod jar:

```powershell
gradlew jar
```

The built jar can then be found under `/build/libs`.

To build the prod jar and then move it into the mod folder of your Minecraft instance in one command
(using CMD or PowerShell on Windows):

```powershell
# Replace <path-to-mc-instance> with the actual path to your Minecraft instance first
del "build\libs\BlazeMap-*.jar" & gradlew jar && del "<path-to-mc-instance>\mods\BlazeMap-*" && copy "build\libs\BlazeMap-*.jar" "<path-to-mc-instance>\mods"
```

You can find the logs for the latest run of that server at `"<path-to-mc-instance>\logs\latest.log"`.

To test why a mixin isn't working from a prod jar, add the following to the JVM Arguments in the
Minecraft Launcher. This will add mixin debug logs to the normal server logs as well as outputting
the mixed in version of each `.class` to `.mixin.out` in that instance's folder:

```
-Dmixin.debug.export=true -Dmixin.debug.verbose=true -Dmixin.debug.countInjections=true 
```

### Running Client Side Only

To easily start a local Minecraft server with Forge for the sake of testing how Blaze Map works when
_only_ installed client side, you can spin up a local server in docker with the following:

```powershell
# Replace <minecraft-version> with your desired Minecraft version, eg 1.20.1
docker run -it -p 25565:25565 -e EULA=TRUE -e VERSION=<minecraft-version> -e TYPE=FORGE -e ONLINE_MODE=FALSE itzg/minecraft-server
```

There are other ways you can start up a local Minecraft server too of course, but this is one of
the simplest.
