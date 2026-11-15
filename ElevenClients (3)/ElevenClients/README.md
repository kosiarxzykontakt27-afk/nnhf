# ElevenClients (Fabric 1.21.4)

Budowanie: wrzuc na GitHub, zakladka Actions -> build -> Artifacts -> ElevenClients (.jar).

    gradle wrapper --gradle-version 8.12
    ./gradlew build        # Windows: gradlew.bat build

Gotowy plik: build/libs/ElevenClients-0.1.0.jar  (NIE ten z "-sources")
Wrzuc do .minecraft/mods razem z Fabric API 0.119.2+1.21.4.

Config: .minecraft/config/elevenclients.properties  (guiScale=0.65, accent=0EA5E9)
Menu glowne jest podmieniane przez mixin (MinecraftClientMixin).
