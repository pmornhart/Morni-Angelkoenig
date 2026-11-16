# Morni Angelkönig

@STAND.md

## Regeln für dieses Projekt

- Repo ist **öffentlich**: keine Geheimnisse, Tokens oder privaten Daten committen (Geheimes nur in `.env`, nie im Repo).
  Auch keine Test-Server-Ordner, Welten, Spielerdaten, Bilder mit Spiel-Texturen oder RCON-Passwörter.
- Das Repo heißt `pmornhart/morni-angelkoenig`, der Modus heißt **Angelkönig** (englisch „Angler King“), die Jar
  `Morni-Angelkoenig.jar`, das Plugin `MorniAngelkoenig`, die Kennung im Netzwerk `angelkoenig`.
- Texte im Spiel auf Deutsch (Standard) und Englisch, freundlich, keine Fachbegriffe ohne Erklärung. Alle Texte stehen
  in `Texts.java` (MiniMessage, beide Sprachen); `TextsTest` prüft Platzhalter, Zeichen, dass jeder Schlüssel aus dem
  Code existiert und dass jeder Fisch, jeder Köder, jede Angel, jedes Kleidungsstück und jeder Fangplatz einen Namen hat.
- **„Verfasst mit Claude 🤖“** als letzte Zeile in Commit-Messages (durch eine Leerzeile getrennt), in PRs, Issues und
  Release-Notes; einmal als Fußzeile im README.
- Commits auf Deutsch mit Stichpunkten `- Neu:` / `- Behoben:` / `- Verbessert:` / `- Entfernt:`.
- Keine fremden Marken, Figuren, Namen, Texte oder Grafiken (nichts von Mojang, Nintendo, Hypixel oder anderen Servern
  und Spielen): nur die Spielidee „gemütlich angeln, sammeln, verkaufen“, eigene Fische, eigene Namen, eigene Welt.
  Nur Minecraft-Gegenstände und -Blöcke, keine eigenen Texturen.
- **Nichts für echtes Geld**: Taler gibt es nur im Spiel (Fänge verkaufen, Aufgaben, Turniere).
- Die Netzwerk-Schnittstelle `src/api/java/de/moinmorni/api/MoinMorni.java` ist eine Kopie aus `pmornhart/moinmorni.net`
  (`plugins/moinmorni-network/src/main/java/de/moinmorni/api/MoinMorni.java`) und muss **genau gleich** bleiben. Sie wird
  nur mitübersetzt und kommt **nicht** in die Jar. Nur `net/Connected` darf sie benutzen.
- Angelkönig muss auch ohne Netzwerk laufen (`softdepend: [MoinMorni]`).
- Paper 26.x, Java 25. Der Code muss auch gegen Paper 26.1 übersetzen (`mvn -Ppaper-26.1 compile`, läuft in der CI).
  Blocknamen von 26.x benutzen (z. B. `iron_chain`, nicht `chain`; `short_grass`, nicht `grass`).
- Regeln, die sich ohne Server prüfen lassen (Fische, Fangtabellen, Größen, Preise, Köder, Angeln, Aufgaben, Turnier,
  Wetter, Bestenliste, Fischbuch), gehören ins Paket `fish` bzw. `data` – ohne Server, mit Tests. Werte dort nur
  zusammen mit den Tests ändern.
- Die Welt entsteht direkt beim Erzeugen der Chunks aus dem Code (`world/Terrain` + `world/Plan`). Ändern sich Gelände
  oder Bauten: `world/WorldSetup.VERSION` erhöhen – dann entsteht die Welt beim nächsten Start neu (Spielerdaten liegen
  außerhalb der Welt). Gewässer müssen dicht sein (`TerrainTest`), Fangplätze auf Wasser stehen.
- Neue Version: `<version>` in `pom.xml` erhöhen und `docs/releases/v<version>.md` anlegen; ein Push auf `main` baut das
  Release mit `Morni-Angelkoenig.jar` – das Netzwerk holt es sich von dort.
- Vor jeder Veröffentlichung (Merge nach `main`) Philips Go abwarten.
- Nach jeder sichtbaren Änderung Bilder wie im echten Minecraft (echte Client-Texturen nur lokal, nie ins Repo).
  Für einen schnellen Blick von oben: `KarteBild` in den Tests schreibt `target/karte.png`.
- „Alles im Panel“: jede Einstellung, die per Befehl geht, muss auch im Panel gehen. Darum hat Angelkönig nur die
  `config.yml` und Verwaltungs-Befehle zum Testen (`/angeladmin`); die Anbindung an den Einstellungs-Katalog gehört ins
  Netzwerk-Repo.
- Bedrock-Spieler (Geyser) müssen alles können: Menüs sind Truhen-Fenster, keine Steuerung, die nur Java kann.
- Echter Test nach jeder Änderung am Spielablauf: `test/run.sh` (siehe README), Ports 25970/26070.
- `STAND.md` nach jedem größeren Schritt aktualisieren.
