<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHWithdraw

*Chèques de SHCoins et bouteilles d'EXP (remplace BeastWithdraw, compatible avec ses objets)*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale-3aa655?style=for-the-badge" alt="SkyHand Opale">

</div>

---

## Présentation

Chèques de SHCoins et bouteilles d'EXP (remplace BeastWithdraw, compatible avec ses objets)

Les créations, encaissements et dons sont écrits dans SHLogs (`plugins/SHLogs/SHWithdraw/`) : le fichier `cheques` pour les chèques et `bouteilles-exp` pour les bouteilles.

Utilisé sur : **Opale**.

## Dépendances

- Obligatoires : `Vault`, `NBTAPI`
- Optionnelles : `Essentials`, `SHLogs`

## Commandes

| Commande | Alias | Description | Permission |
|---|---|---|---|
| `/cheque <montant\|all> [nombre]` | — | Transforme des SHCoins en chèque. | `shwithdraw.cheque.withdraw` (`all` : `shwithdraw.cheque.withdraw.all`) |
| `/xpbottle <xp\|all\|<niveaux>l> [nombre]` | — | Met de l'EXP en bouteille. | `shwithdraw.xpbottle.withdraw` (`all` : `shwithdraw.xpbottle.withdraw.all`) |
| `/shwithdraw givecashnote <joueur> <montant> [nombre] [signataire]` | `givenote` | Donne des chèques à un joueur. | `shwithdraw.admin.givecashnote` |
| `/shwithdraw givecashnoteall <montant> [nombre] [signataire]` | — | Donne des chèques à tous les joueurs en ligne. | `shwithdraw.admin.givecashnoteall` |
| `/shwithdraw givexpbottle <joueur> <xp> [nombre] [enchanteur]` | `givexpb` | Donne des bouteilles d'EXP à un joueur. | `shwithdraw.admin.givexpbottle` |
| `/shwithdraw givexpbottleall <xp> [nombre] [enchanteur]` | `givexpball` | Donne des bouteilles d'EXP à tous les joueurs en ligne. | `shwithdraw.admin.givexpbottleall` |
| `/shwithdraw info` | — | Affiche le type et la valeur réellement stockée (celle qui sera encaissée) de l'objet tenu en main. | `shwithdraw.admin.info` |
| `/shwithdraw reload` | — | Recharge `config.yml` et `messages.yml`. Un fichier YAML invalide laisse l'ancienne configuration en place et affiche le fichier et la ligne de l'erreur. | `shwithdraw.admin.reload` |

Les sous-commandes de `/shwithdraw` marchent en console, sauf `info`. L'encaissement se fait par clic droit sur l'objet (`shwithdraw.cheque.redeem`, `shwithdraw.xpbottle.redeem`).

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shwithdraw.cheque.withdraw` | `/cheque` | `false` |
| `shwithdraw.cheque.withdraw.all` | `/cheque all` | `op` |
| `shwithdraw.cheque.redeem` | Encaisser un chèque | `false` |
| `shwithdraw.xpbottle.withdraw` | `/xpbottle` | `false` |
| `shwithdraw.xpbottle.withdraw.all` | `/xpbottle all` | `op` |
| `shwithdraw.xpbottle.redeem` | Boire une bouteille d'EXP | `false` |
| `shwithdraw.bypass` | Pas de minimum, de maximum ni de taxe sur `/cheque` et `/xpbottle` | `op` |
| `shwithdraw.admin` | Toutes les sous-commandes de `/shwithdraw` et `shwithdraw.bypass` | `op` |
| `shwithdraw.admin.givecashnote`, `.givecashnoteall`, `.givexpbottle`, `.givexpbottleall`, `.info`, `.reload` | Une permission par sous-commande de `/shwithdraw` | `op` |

## Configuration

Fichiers par défaut repris du serveur (secrets remplacés par `CHANGE_ME`). Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHWithdraw/
├── <a href="src/main/resources/config.yml">config.yml</a>
└── <a href="src/main/resources/messages.yml">messages.yml</a>
</pre>

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
