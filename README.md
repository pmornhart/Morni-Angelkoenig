# Morni Angelkönig

Der Angel-Modus des Minecraft-Netzwerks **MoinMorni** (moinmorni.net): **Gemütlich angeln im Alpen-Tal** – jederzeit
betretbar, ohne Runden. Eigene Fische der Alpen, vier Fangplätze, Fischhändler, Fischbuch, Aufgaben des Tages und
alle 15 Minuten ein Angel-Turnier. Ein Paper-Plugin für Paper 26.x (Java 25). Es läuft im Netzwerk und auch ganz allein.

Englischer Name: **Angler King**. Kennung im Netzwerk: `angelkoenig`.

## So wird gespielt

1. Man kommt im **Dorf am See** an: Dorfplatz mit dem Brunnen der goldenen Forelle, Steg mit Ruderbooten, Bootshaus,
   Fischerhaus, Lagerfeuer mit Bänken, Tafelwand mit den Bestenlisten – und **Fischhändler Xaver** an seinem Stand.
2. In der Schnellleiste: **Angel**, **Köderdose**, **Wegweiser** (Kompass), **Fischbuch**, **Aufgaben des Tages**,
   **Kleiderschrank** und **zur Lobby**. Alle Fenster sind Truhen – geht auch mit Bedrock.
3. Angel auswerfen (Rechtsklick), warten bis der Schwimmer untertaucht, einholen (Rechtsklick). Jeder Fang ist ein
   eigener Gegenstand mit Name, Seltenheit, Länge, Gewicht, Fangplatz, Tageszeit, Datum und Wert.
4. Fänge bringt man zu Xaver. Für die Taler gibt es bessere **Angeln** (schnellerer Biss, öfter Seltenes),
   **Köder** (locken bestimmte Arten) und **Kleidung** zum Angeben. Taler gibt es nur im Spiel – nichts für echtes Geld.
5. **Angel-Turnier** zur vollen Viertelstunde (eine Minute vorher angesagt, ab 2 Leuten): 5 Minuten, der schwerste
   Fisch gewinnt. Leiste oben mit Restzeit und Führendem, Preise für Platz 1–3, der Sieger wird **Angelkönig** und
   bekommt die Königskrone. Wer im Tal ist, macht mit; wer das Tal verlässt, gibt seinen Platz auf. **Zurück ins
   Spiel:** Verliert jemand während des Turniers die Verbindung (zum Beispiel, weil das Spiel abstürzt), bleibt sein
   Platz samt bestem Fang `rueckkehr-sekunden` lang frei (Vorgabe 90, 0 = aus). Rechtzeitig zurück – direkt oder über
   „Zurück ins Spiel“ in der Lobby – angelt er dort weiter, wo er war; endet das Turnier vorher, zählt sein Platz.

## Fangplätze

| Platz | Wo | Typische Fische |
|---|---|---|
| **Bergsee** | großer See am Dorf, Steg mit Plattform, Ruderboote | Rotauge, Flussbarsch, Renke, Seesaibling, Zander, Waller, Seeforelle – Legende: Seekönig |
| **Gebirgsbach** | Gumpe unter dem Wasserfall im Westen, Plattform, Marterl | Bachforelle, Äsche, Steinkrebs, Huchen – Legende: Goldforelle |
| **Moorweiher** | im Osten, Bohlenweg, Schilf, Seerosen, Torfhütte, nachts Irrlichter | Karausche, Schleie, Hecht, Aal (nur nachts) – Legende: Moorkönig |
| **Eisloch** | zugefrorener Karsee hoch oben im Südosten, Eisfischer-Hütte | Renke, Seesaibling, Rutte – Legende: Kristallsaibling |

20 Fischarten in fünf Seltenheiten (gewöhnlich bis legendär) und drei Fundstücke (alter Stiefel, Treibholz,
Flaschenpost). Tageszeit (Morgen, Tag, Abend, Nacht) und Wetter (klar, Regen, Gewitter; am Kar Schnee) ändern, was
beißt; die Tafel an jedem Platz und der Wegweiser zeigen, was gerade gern beißt. Morgennebel über dem Wasser,
springende Fische, Gischt am Wasserfall.

## Angeln, Köder, Kleidung

| Angel | Preis | Biss | Glück |
|---|---|---|---|
| Haselrute | – | gemütlich | – |
| Eschenrute | 250 | flott | ★ |
| Meisterrute | 1200 | schnell | ★★ |
| Königsrute | 5000 | blitzschnell | ★★★ |

Köder (je Packung): Regenwurm, Maden, Teigkugel, Kunstfliege, Köderfisch, Goldspinner (lockt Legenden). Kleidung:
Fischerhut, Strohhut, Trachtenhut, Anglerweste, gelbe Regenjacke, Lederhose, Wathose, Gummistiefel – und die
Königskrone (nur für Turniersieger).

## Befehle

| Befehl | Was er tut |
|---|---|
| `/fischbuch` · `/aufgaben` · `/wegweiser` · `/koeder` · `/kleiderschrank` | dieselben Fenster wie in der Schnellleiste |
| `/turnier` | wann das nächste Turnier ist |
| `/stats [spieler]` · `/top [faenge\|turniere\|rekorde]` | Statistik und Bestenlisten |
| `/angeladmin info\|status\|turnier start\|ende\|stopp\|zeit …\|wetter …\|taler …\|geld [spieler] <betrag>\|naechster …\|fisch …\|fischbuch …\|kleidung …\|aufgabe …\|melden\|neubauen\|neuladen` | Verwaltung und Tests (`moinmorni.angelkoenig.admin`); `fisch <spieler> <art> [cm]` legt einen Fang in den Korb, `fischbuch <spieler> alle\|leer\|<art>` füllt oder leert das Fischbuch (für Bilder), `geld [spieler] <betrag>` gibt Taler dazu – ohne Spieler einem selbst (für Owner und Admins zum Testen), `taler <spieler> <betrag>` setzt sie, `kleidung <spieler> <stück>\|alle\|aus` schenkt und zieht Kleidung an (für Bilder) |

Einstellungen stehen in der `config.yml` (Zurück ins Spiel, Tageszeit, Wetter, Biss-Zeiten, Startgeld, Turnier an/aus,
Abstand, Dauer, Preise).


## Eigenes Aussehen (Angelkönig-Paket)

Fische, Hüte und Kleidung tragen ihren Namen als `custom_model_data` (`moinmorni:fisch/<art>`,
`moinmorni:kleidung/<stück>`; Listen in `docs/fisch-modelle.txt` und `docs/kleidung-modelle.txt`). Das Paket mit den
Zeichnungen und 3D-Modellen baut das MoinMorni-Netzwerk. Angelkönig bietet es beim Verbinden selbst an, wenn
`plugins/MorniAngelkoenig/paket.json` (`url`, `sha1`, `id`, `prompt`) da ist – so weiß es, wer das Paket geladen hat,
und zeigt Hüte als 3D-Modell bzw. Weste, Jacke, Hosen und Stiefel mit eigenem Muster nur denen, die es sehen können;
alle anderen sehen die gewohnte Lederkleidung. Ohne Paket, auf älteren Versionen und auf Bedrock bleibt alles
Minecraft.

Im MoinMorni-Netzwerk steht in `paket.json` nur `{"netz": true}`: Das Paket kommt dann vom Proxy, einmal beim Betreten
des Netzwerks, und gilt auf allen Servern (kein Laden beim Wechseln). Angelkönig bietet selbst nichts an und liest aus
den Metadaten `moinmorni:paket` des Spielers, ob er es geladen hat.

## Im Netzwerk

Mit dem Netzwerk-Plugin („MoinMorni“, Schnittstelle `de.moinmorni.api.MoinMorni`): Sprache, Party (wer später kommt,
landet bei seiner Party), Statistik, Zustand und zurück zur Lobby. Ohne Netzwerk: deutsche Texte, Statistik nur lokal.
Ab Schnittstelle 2 „spielt“ jeder im Tal, solange ein Turnier läuft (dann fragen „/lobby“ und der Lobby-Knopf erst
nach), und ein freigehaltener Turnierplatz erscheint in der Lobby als „Zurück ins Spiel“; mit einem älteren
Netzwerk-Plugin läuft alles wie bisher.

- Statistik (`stats("angelkoenig", …)`): hochgezählt `catches`, `legendary`, `tournaments_won`; Bestwerte
  `heaviest_g` (schwerster Fisch in Gramm) und `species` (Arten im Fischbuch).
- Zustand: `open` (man kann angeln) oder `tournament` (Turnier läuft), dazu die Spielerzahl.
- Lokal: `spieler/<uuid>.yml`, `bestenliste.yml`.

## Die Welt

Das Tal entsteht beim Erzeugen der Chunks direkt aus dem Code: Berge mit Graten und Gipfeln ringsum, darin
ausgehoben Tal, Dorf, Bachtal, Moor und Kar; Gewässer, Wege, Bäume, Blumen, Schilf, Seerosen und alle Bauten
(Seekapelle mit Zwiebelturm am Südufer, Gipfelkreuz). Ändert sich etwas, entsteht die Welt mit einer neuen
`WorldSetup.VERSION` neu (Spielerdaten liegen außerhalb).

## Bauen und testen

```bash
mvn package                      # Tests + target/Morni-Angelkoenig.jar (Java 25)
mvn -Ppaper-26.1 compile         # passt der Code auch zu Paper 26.1?
mvn test -Dkarte=true -Dtest=KarteBild   # Blick von oben: target/karte.png
test/run.sh                      # echter Test: Paper-Server + Test-Spieler (mineflayer)
```

`test/run.sh` richtet einen Paper-26.1.2-Server ein (offline, Port 25970, RCON 26070, schneller Biss) mit einem
Test-Netzwerk (`test/fake-network`: Bert spricht Englisch, Anna und Carl sind eine Party) und schickt Test-Spieler
durchs Tal (`test/angelkoenig-test.cjs`): echtes Angeln an allen vier Plätzen, Fischhändler, Angel und Köder kaufen,
Fischbuch, Aufgaben, Turnier, Bestenliste, Statistik, Englisch, Party, Wiederkommen.

---

Verfasst mit Claude 🤖
