<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHWithdraw

*Chèques de SHCoins et bouteilles d'EXP (remplace BeastWithdraw, compatible avec ses objets)*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale-3aa655?style=for-the-badge" alt="SkyHand Opale">

</div>

---

## Présentation

Chèques de SHCoins et bouteilles d'EXP (remplace BeastWithdraw, compatible avec ses objets)

Utilisé sur : **Opale**.

## Dépendances

- Obligatoires : `Vault`, `NBTAPI`
- Optionnelles : `Essentials`

## Commandes

| Commande | Alias | Description | Permission |
|---|---|---|---|
| `/cheque` | — | Transforme des SHCoins en chèque. | — |
| `/xpbottle` | — | Met de l'EXP en bouteille. | — |
| `/shwithdraw` | — | Administration des chèques et bouteilles d'EXP. | `shwithdraw.admin` |

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shwithdraw.cheque.withdraw` | /cheque (shwithdraw.cheque.withdraw.all pour /cheque all) | `false` |
| `shwithdraw.cheque.redeem` | Encaisser un chèque | `false` |
| `shwithdraw.xpbottle.withdraw` | /xpbottle (shwithdraw.xpbottle.withdraw.all pour /xpbottle all) | `false` |
| `shwithdraw.xpbottle.redeem` | Boire une bouteille d'EXP | `false` |
| `shwithdraw.admin` | Commandes /shwithdraw (give, reload) et contournement des limites et taxes | `op` |

## Configuration

Fichiers par défaut repris du serveur (secrets remplacés par `CHANGE_ME`) :

- `config.yml`

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
