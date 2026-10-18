# Minecraft Tutorial Mod

Een Minecraft-mod voor **Fabric 1.21.11** (Java 21) waarmee spelers stap voor stap bouwwerken
kunnen nabouwen met behulp van voorbeeldblokken (ghost blocks).

## Functies

- **Ghost blocks**: transparante-achtige voorbeeldblokken tonen waar blokken geplaatst moeten worden
  (geïmplementeerd als client-only item displays).
- **Blokcontrole**: de server controleert elke halve seconde of het juiste blok inclusief
  blockstate (bv. de juiste traporiëntatie) is geplaatst.
- **Foutmarkering**: verkeerde blokken krijgen een rode translucent overlay.
- **Stappenplan**: na het voltooien van een stap verschijnt automatisch de volgende.
- **Voortgangsweergave**: voortgangsbalk boven de hotbar en informatie linksboven.
- **Materiaalbeheer**: benodigde materialen worden per stap gegeven, instelbaar via de gamerule
  `tutorial_give_materials` (standaard aan).
- **Wereldopslag**: tutorials en voortgang per speler worden per wereld opgeslagen.
- **Multiplayer**: alles is servergestuurd; werkt op servers met meerdere spelers tegelijk.
- **Bouwplatform**: `/tutorial make` bouwt een 16x16 platform van gras en dirt en maakt lucht erboven.

## Commando's

| Commando | Functie |
| --- | --- |
| `/tutorial start <naam>` | Start een tutorial (hervat opgeslagen voortgang) |
| `/tutorial make <naam> <x> <z> <grondDiepte> <luchtHoogte>` | Maak een tutorial + bouwplatform (op-persmissie) |
| `/tutorial make step <nummer> <naam>` | Leg de huidige inhoud van het bouwgebied vast als bouwstap (op-persmissie) |
| `/tutorial edit <step> <naam>` | Neem een bestaande stap opnieuw op (op-persmissie) |
| `/tutorial remove step` | Verwijder de laatste stap (op-persmissie) |
| `/tutorial skip <step>` | Sla over naar een bepaalde stap |
| `/tutorial stop` | Stop de huidige tutorial |
| `/tutorial list` | Toon alle tutorials |

`make step`, `edit` en `remove step` werken op de tutorial die je als laatste hebt aangemaakt met
`/tutorial make` (of op de enige bestaande tutorial).

## Workflow

1. `/tutorial make huis 0 0 3 8` — bouwt een platform met 3 blokken grond en 8 blokken lucht
   op (0, 0), met het gras op hoogte van je voeten.
2. Bouw het eerste deel van je bouwwerk in het platform.
3. `/tutorial make step 1 Fundering` — vastgelegd.
4. Herhaal bouwen + `make step` voor elke volgende stap.
5. `/tutorial start huis` — en volg de tutorial. De ghosts tonen wat waar moet, foute blokken
   worden rood gemarkeerd en na elke stap krijg je automatisch de materialen.

## Installatie

1. Installeer de [Fabric Loader](https://fabricmc.net/use/) voor Minecraft 1.21.11.
2. Download de [Fabric API](https://modrinth.com/mod/fabric-api) voor 1.21.11 en zet die in de mods-map.
3. Zet `tutorialmod-1.0.0.jar` (te vinden onder `build/libs/` na bouwen, of via Releases) in de mods-map.

## Bouwen

```bash
./gradlew build
```

De mod-jar verschijnt in `build/libs/`. Je hebt hiervoor JDK 25 nodig om Gradle te draaien;
de mod zelf compileert naar Java 21 (de runtime-versie van Minecraft 1.21.11).

## Technische structuur

- `TutorialManager` — beheert tutorialsessies per speler (serverzijde).
- `TutorialSession` — volgt voortgang en controleert geplaatste blokken.
- `TutorialDefinition` / `TutorialStep` — tutorialgegevens (bouwgebied + verwachte blockstates).
- `TutorialSaveData` — wereldopslag van tutorials en spelerprogressie.
- `TutorialCommands` — alle `/tutorial` commando's.
- `net.TutorialStepPayload` / `net.TutorialProgressPayload` — netwerksynchronisatie server → client.
- `client.TutorialClientState` — ghost blocks via client-only item displays.
- `client.TutorialHud` — voortgangsbalk boven de hotbar en info linksboven.

## Beperkingen / todo

- Ghost blocks renderen als echte blokmodellen (via item displays); door veranderingen in de
  render-pipelines van 1.21.11 is de "transparant"-renderoptie van displays komen te vervallen.
  De schaalgroei staat in `TutorialClientState` (`GHOST_SCALE`) en is eenvoudig aan te passen.
- Het bouwgebied is vast 16x16, gecentreerd op de opgegeven (x, z).

## Licentie

CC0-1.0
