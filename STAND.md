# Stand – Morni Angelkönig

**Was es ist:** Der Angel-Modus des MoinMorni-Netzwerks (Paper-Plugin `MorniAngelkoenig`, Java 25, Paper 26.x):
gemütliches Angeln in einem großen Alpen-Tal – jederzeit betretbar, ohne Runden. Eigene Fische der Alpen, vier
Fangplätze, Fischhändler, Fischbuch, Aufgaben des Tages, Angel-Turnier alle 15 Minuten, Bestenlisten.
Kennung im Netzwerk `angelkoenig`, englisch „Angler King“.

## Aufbau

- `AngelkoenigPlugin` (Start, Takt, Leiste, Zustand), `Settings`, `Texts` (de/en), `Ui`, `Items`, `Holo`, `Sidebar`
  (übernimmt die Rang-Teams des Netzwerks), `Commands` (Spieler-Befehle + `/angeladmin`).
- `fish/` (ohne Server, mit Tests): `Species` (20 Fische + 3 Fundstücke, Größen, Gewicht, Fangplätze, Tageszeit- und
  Wetterfaktoren), `Rarity` (inkl. Fischbuch-Bonus), `Spot`, `DayTime`, `Weather` (Wetterwechsel), `Rod`, `Bait`,
  `Cosmetic`, `CatchRules` (Gewichte, Ziehen, Messen), `Catch`, `Prices`, `Units`, `Quest` + `DailyQuests`,
  `Tournament` (Takt, Ankündigung, Wertung).
- `data/`: `PlayerData` (Taler, Fischbuch, Angeln, Köder, Kleidung, Aufgaben, YAML), `Records` (Rekorde je Art,
  meiste Fänge, Turniersiege, Angelkönig), `Store`.
- `world/`: `Terrain` (Gelände aus Rauschen, Aushüben, Felsstufe, Wegen, Gewässern; Biome), `Shape`, `Noise`,
  `Layout` (feste Stellen), `Plan` + `Kit` + `Valley` (alle Bauten), `Surface` (Blöcke je Chunk inkl. Bäume, Blumen,
  Schilf, Seerosen), `Palette`, `AngelGenerator` (Chunk-Erzeuger), `WorldSetup` (`VERSION` 12, Neubau, Schilder, Banner).
- `play/`: `Angling` (Biss, Fang, Fischbuch, Aufgaben, Verkauf, Netzwerk-Statistik), `FishItems`, `Hotbar`, `Menus`
  (Fischhändler, Köderdose, Kleiderschrank, Fischbuch, Aufgaben, Wegweiser), `Tournaments`, `Boards`, `Npcs`,
  `Ambience`, `Seats`. `listen/`: `PlayerListener`, `FishingListener`, `GuardListener`. `net/`, `menu/` wie die anderen Modi.
- `test/`: `run.sh`, `angelkoenig-test.cjs`, `rcon.cjs`, `fake-network/`.

## Stand (16.11.2026)

- Version 0.1.1 (Branch `claude/zurueck-ins-spiel`, wartet auf Philips Go): **Zurück ins Spiel** (Philip: „mitten im
  Spiel nochmal fragen, ob man wirklich zur Lobby will; nach einem Absturz zurück ins Spiel“). Netzwerk-Schnittstelle 2
  (byte-gleiche Kopie; `askToLobby`, `playing`, `holdSlot`; älteres Netzwerk-Plugin → `LinkageError` einmal im Log,
  sonst wie bisher). Wer im Tal ist, macht beim laufenden Turnier mit und „spielt“ (Start, Nachzügler, Ende, Abbruch;
  `Tournaments.syncPlaying`), der Lobby-Knopf ruft `askToLobby`. Regel neu: Wer das Tal während des Turniers verlässt,
  gibt seinen Platz auf – nach einem Verbindungsabbruch erst nach `rueckkehr-sekunden` (neu oben in der config.yml,
  Vorgabe 90, 0 = sofort; Regeln in `fish/Tournament`: `away`, `back`, `expired`, `releaseAway`), `holdSlot`.
  Rechtzeitig zurück: Platz und bester Fang bleiben, man angelt dort weiter, wo man war (`welcome(p, keepPlace)`),
  „Willkommen zurück!“ mit Platz; zu spät: Platz frei, liegt er vorn, wird die neue Führung angesagt; endet das Turnier
  vorher, zählt der Platz mit Preis und das Angebot geht zurück (holdSlot 0). Neu für Owner/Admins: `/angeladmin geld
  [spieler] <betrag>` (Taler dazu, ohne Spieler für einen selbst, zählt nicht als „verdient“, de/en); `status` zeigt
  `weg`. Test-Netzwerk schreibt `playing`, `ask-lobby`, `hold-slot`; `test/run.sh` mit `MEMORY` (Vorgabe 2G).
  JUnit 6.1.3. Geprüft: 51 Unit-Tests (4 neu in `TournamentTest`), Paper 26.1 übersetzt, echt (`test/run.sh`,
  `rueckkehr-sekunden: 20`) **100/100**, Log ohne Fehler – neu: im Turnier spielen alle im Tal, Lobby-Knopf fragt
  nach (Anna bleibt), Anna geht (holdSlot 20, Bert liest es englisch, ihr Platz zählt weiter, `weg=1`) und kommt
  rechtzeitig zurück („Dein Platz im Turnier ist frei geblieben: Hecht (5,50 kg), Platz 1“, am selben Steg, holdSlot
  0), Bert kommt zu spät (Platz frei, holdSlot 0, beim Wiederkommen wie sonst im Dorf), nach dem Turnier spielt
  niemand, `geld` an Carl, an sich selbst, falscher Betrag erklärt den Befehl, Lobby-Knopf ohne Turnier ohne
  Nachfrage.

- Version 0.1.0 (Branch `claude/erster-stand`, noch nicht veröffentlicht – wartet auf Philips Go).
- 47 Unit-Tests grün (Fangtabellen, Köder, Angeln, Größen, Preise, Aufgaben, Turnier, Wetter, Tageszeit, Spielerdaten,
  Bestenliste, Texte; Gelände: alle Gewässer dicht, Fangplätze stehen vor ihrem Wasser, Wege begehbar, keine alten
  Blocknamen). Übersetzt gegen Paper-API 26.3 und 26.1.
- Echter Test (`test/run.sh`, Paper 26.1.2, drei Test-Spieler): **83/83**, Server-Log ohne Fehler – Ankunft,
  Schnellleiste, Englisch, Wegweiser, echtes Angeln an allen vier Plätzen (Bergsee, Bach, Moor, Eisloch),
  Fang-Gegenstand mit Name/Länge/Gewicht/Ort/Wert, Legende mit Ansage für alle, Fischbuch mit Bonus, Aufgabe erledigt,
  Fischhändler (einzeln und alles verkaufen, Angel und Köder kaufen, zu teuer), Köder wird verbraucht, Sitzen und
  Aufstehen, Party (Carl landet bei Anna), Turnier mit Führung, Sieg, Preis, Angelkönig, Königskrone, `/top`, `/stats`,
  Netzwerk-Statistik und Zustand, Zeit/Wetter, Wiederkommen (alles bleibt), Lobby-Knopf. Neubau der Welt bei neuer
  Bau-Version geprüft.
- Bilder mit echtem Client 26.3 (nur lokal, `~/mm-cache/bilder-angelkoenig/`): Ankunft, Steg, Seeblick, Händler,
  Wasserfall, Moor, Eisloch, Kapelle, Tafeln, Tal von oben, laufendes Turnier mit Leiste. Danach verbessert:
  Felswände ohne Streifen, Schnee nur auf Fichtenspitzen, Wasser im Brunnen sichtbar, Händlername frei.
- Schönheitsrunde 2 (Bau-Version 9): Berge natürlicher – Gipfel mit eigener Form (länglich, gedreht, Grate als
  Ausläufer), Massive mit Nebengipfeln, Vorgipfel, Kare, Erosionsrinnen, Felsbänder, Geröllfelder, Almmatten, Latschen,
  Schnee in Rinnen und Mulden statt als Kappe; Wasserfall breiter (4 Blöcke) und in zwei Stufen über eine bemooste
  Felsstufe, Felsflanke daneben gestuft mit Vorsprüngen, Moos, Farn, Ranken, Felsbrocken an der Gumpe, Gischt an Stufe
  und Gumpe; Händlerschild „Xaver / Fische / Fish“. Neubau 5 → 6 → … → 9 echt geprüft, echter Test wieder 78/78,
  Bilder `~/mm-cache/bilder-angelkoenig/v2/`.
- Feinschliff (Bau-Version 12): Die Kette im Süden (Blick vom See) sind jetzt kräftige Alpengipfel statt
  Felsnadeln – breiter Fuß, Profil zwischen Pyramide und S-Kurve, drei bis vier massige Grate je Gipfel, Nebengipfel
  dicht am Hauptgipfel (hoher Grat dazwischen), flachere Rinnen, etwas weiter vom See weg, so dass die Gipfel samt
  Gipfelkreuz frei vor dem Himmel stehen; Norden, Osten und Westen unverändert. Schnee auf flachen Stellen nur noch
  fleckig. Neubau 9 → 10 → 11 → 12 echt geprüft, Bild `~/mm-cache/bilder-angelkoenig/v3/05-see-blick.jpg`.
- Biss-Hinweis „❗ Biss! Schnell einholen!“ in der Aktionsleiste; Verwaltung `/angeladmin fisch` (Fang in den Korb)
  und `/angeladmin fischbuch alle|leer|<art>` für Bilder und Tests; `status` meldet, wie lange der letzte Biss her ist.
  Echter Test (Wartezeiten im Test robuster gegen Last).
- Angelsystem mit echtem Client 26.3 fotografiert (nur lokal, `~/mm-cache/bilder-angelkoenig/system/`): Fischbuch
  teilweise (mit „???“) und komplett, Beschreibung je Seltenheit und Fundstück, echter Fang mit Biss-Hinweis und
  Legenden-Titel, Fischhändler (Verkauf, Angeln, Köder, Kleidung), Köderdose, Kleiderschrank mit Königskrone, Aufgaben,
  Wegweiser, Turnier mit Leiste oben (Restzeit, Führender), Seitenleiste und Siegermeldung, Bestenlisten am Dorfplatz.
  Dabei verbessert: kürzere Fang-Nachricht, Feuerwerk nicht mehr vor dem Gesicht, Turnier-Titel überdeckt die Leiste
  nicht, Kleidung ohne Rüstungsleiste, lange Bestenliste nicht mehr unter dem Vordach, keine „Rezept freigeschaltet“-
  Hinweise und keine Minecraft-Fortschritte („Anglerglück“) im Tal, „Verdient“ zählt alle Taler, Countdown über eine
  Stunde als „3:13:10“, Beschreibungen in Menüs grau statt lila, wer eine Legende fängt, bekommt die Ansage für alle
  nicht noch einmal, Köder-Hinweis in der Aktionsleiste nur noch, wenn die Dose fast leer ist (Anzahl steht an der
  Köderdose und in der Leiste). Turnier-Automatik im laufenden Server gesehen: zur Viertelstunde Vorwarnung und Start
  mit zwei Anglern.
- **Name wieder MoinMorni** (16.11. abends): Die Umbenennung vom Nachmittag ist per `git revert` zurückgenommen, der
  neue Name war nicht verfügbar – Paket `de.moinmorni.angelkoenig`, Schnittstelle `de.moinmorni.api.MoinMorni`,
  `softdepend: [MoinMorni]`, Recht `moinmorni.angelkoenig.admin`, Texte und Leiste mit MoinMorni/moinmorni.net,
  Test-Netzwerk „MoinMorni“; der Feinschliff danach bleibt. `run.sh` räumt ältere Test-Netzwerk-Jars weg. Plugin-Name,
  Datenordner (`plugins/MorniAngelkoenig/`) und Kennung `angelkoenig` waren nie anders. Danach: 47 Unit-Tests,
  `-Ppaper-26.1 compile` und echter Test **83/83** grün, Angelkönig meldet sich beim Test-Netzwerk „MoinMorni“.
- Beobachtet (nicht geklärt): Einmal blieb im echten Client 26.3 (über ViaVersion an Paper 26.1.2) Regen sichtbar,
  obwohl das Tal laut Server klar war; nach neuem Einloggen weg. Wohl Wetter-Abgleich über Via, nicht das Plugin.
- Funde behoben: Wegweiser-Platz am Bergsee warf den Schwimmer auf die Stegkante; Fänge landeten im Wasser statt im
  Korb (kommen jetzt sicher an); Sitz holte einen nach einer Reise zurück; `brick_planks` gibt es nicht.

## Offen

- Philip: Go für `main` mit 0.1.1 (Zurück ins Spiel, Taler zum Testen).
- Netzwerk-Repo: Einstellungs-Katalog um `rueckkehr-sekunden` ergänzen (0–600, Vorgabe 90).
- Nicht echt geprüft: Rückkehr über das echte Netzwerk (Lobby-Angebot „Zurück ins Spiel“).
- Nicht echt geprüft: Bedrock (Menüs sind Truhen), Ruderboote mit echtem Client, Turnier-Automatik zur Viertelstunde
  im laufenden Server (nur Unit-Test; im Test per Befehl), Paper 26.3 als Server (nur gebaut).
- Netzwerk-Repo: `MODE_REPOS` (`angelkoenig: 'pmornhart/Morni-Angelkoenig'`), Lobby-Figur, Webseite (Modus-Seite,
  Bestenlisten `catches`/`heaviest_g`/`species`/`tournaments_won`), Einstellungs-Katalog für die `config.yml`.
