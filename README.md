# AnpcTest - prototype de deplacement fluide

Prerequis serveur : Paper 1.21.11, Java 21, plugin BetterModel 2.2.0 (les 3.x exigent Java 25).

Build : `mvn package` ou GitHub Actions (artefact AnpcTest). Jar dans target/.

Commande : /anpctest [modele] [blocs] [vitesse] [pas_ticks] [animation]
Exemples :
- /anpctest                        blue_wizard, 10 blocs, 4 blocs/s, chaque tick, animation walk
- /anpctest demon_knight 10 3 1 walk
- /anpctest blue_wizard 10 4 2     compare un envoi toutes les 2 ticks
- /anpctest limb:steve 10 4 1 roll modele de joueur avec ton skin (steve n'a que l'animation roll)
