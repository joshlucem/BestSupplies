# BestSupplies

**Centro de Abastecimiento** - A comprehensive supply center plugin for Paper 1.21.10

A feature-rich Minecraft plugin providing daily rewards, weekly bank cheques, and food rations based on player ranks.

## Features

### Daily Rewards System
- 7-day calendar with different rewards per day
- **Only the current day is claimable** - past days are marked as expired
- Streak system with bonus multipliers:
  - Days 3-6: +5% bonus
  - Days 7-13: +10% bonus  
  - Days 14+: +15% bonus
- Special milestone rewards at day 7, 14, and 30
- Streak resets if player misses a day

### Weekly Bank Cheque
- Physical cheque item given to inventory
- Anti-duplication via UUID tracking in database
- Right-click to redeem for Vault economy balance
- If inventory is full, cheque goes to pending deliveries
- Weekly reset configurable (default: Monday)

### Food Rations
- Rank-based food packs with cooldowns
- Multiple pack types per rank (exploration, combat, work)
- Configurable items per pack
- Visual cooldown display in GUI

### Status Panel
- Overview of all systems at a glance
- Click to navigate directly to each section
- Shows pending items count

### Pending Deliveries
- Safe storage for unclaimed items
- Items saved when inventory is full
- Withdraw individual items or all at once

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/supplies` | `bestsupplies.use` | Open main supply center hub |
| `/supplies daily` | `bestsupplies.daily` | Open daily rewards calendar |
| `/supplies bank` | `bestsupplies.bank` | Open bank cheque panel |
| `/supplies food` | `bestsupplies.food` | Open food rations panel |
| `/supplies status` | `bestsupplies.status` | Open status overview |
| `/supplies pending` | `bestsupplies.pending` | Open pending deliveries |

### Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/supplies admin reload` | `bestsupplies.admin` | Reload all configuration files |
| `/supplies admin reset daily <player>` | `bestsupplies.admin` | Reset player's daily claim for today |
| `/supplies admin reset weekly <player>` | `bestsupplies.admin` | Reset player's weekly cheque claim |
| `/supplies admin reset food <player> [packId]` | `bestsupplies.admin` | Reset player's food cooldowns |
| `/supplies admin givecheque <player> <amount>` | `bestsupplies.admin` | Give a cheque to a player |
| `/supplies admin debug <player>` | `bestsupplies.admin` | View player debug information |

## Permissions

| Permission | Description |
|------------|-------------|
| `bestsupplies.use` | Access the supply center |
| `bestsupplies.daily` | Claim daily rewards |
| `bestsupplies.bank` | Access bank cheques |
| `bestsupplies.food` | Access food rations |
| `bestsupplies.status` | View status panel |
| `bestsupplies.pending` | Access pending deliveries |
| `bestsupplies.admin` | Admin commands access |

### Rank Permissions

Configure rank permissions in `ranks.yml`:
- `bestsupplies.rank.netherite`
- `bestsupplies.rank.diamond`
- `bestsupplies.rank.gold`
- `bestsupplies.rank.iron`
- `bestsupplies.rank.stone`
- `bestsupplies.rank.default`

## Configuration

### config.yml
```yaml
# Timezone for date calculations
timezone: "America/Lima"

# Database settings
database:
  type: "sqlite"  # sqlite or mysql
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: ""

# Weekly reset configuration
weekly:
  reset-day: "MONDAY"  # Day of week for weekly reset
  reset-hour: 0        # Hour of reset (0-23)

# GUI settings
gui:
  update-interval: 20  # Ticks between countdown updates
```

### daily.yml
Configure rewards for each day of the week:
```yaml
days:
  MONDAY:
    icon: "GOLD_INGOT"
    display_name: "<gold>Lunes Dorado</gold>"
    description:
      - "<gray>Empieza la semana</gray>"
    money: 100.0
    items:
      - "BREAD:16"
    commands: []
```

### ranks.yml
Configure rank benefits:
```yaml
ranks:
  - id: "netherite"
    permission: "bestsupplies.rank.netherite"
    display_name: "<dark_red>Netherite</dark_red>"
    weekly_money: 5000.0
    food_cooldown: "6h"
    packs:
      exploracion:
        display_name: "Pack de Exploración"
        icon: "COMPASS"
        items:
          - "COOKED_BEEF:32"
          - "GOLDEN_APPLE:8"
```

## Dependencies

### Required
- **Paper 1.21.10+** - Server software
- **Vault** - Economy integration

### Optional
- **PlaceholderAPI** - Placeholder support

## PlaceholderAPI Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%bestsupplies_streak%` | Current daily streak count |
| `%bestsupplies_rank%` | Player's current rank ID |
| `%bestsupplies_rank_display%` | Player's rank display name |
| `%bestsupplies_daily_status%` | Daily claim status |
| `%bestsupplies_bank_status%` | Bank cheque status |
| `%bestsupplies_food_status%` | Food rations status |
| `%bestsupplies_pending_count%` | Pending items count |
| `%bestsupplies_weekly_amount%` | Weekly cheque amount |
| `%bestsupplies_next_daily%` | Time until daily reset |
| `%bestsupplies_next_weekly%` | Time until weekly reset |

## Installation

1. Download the latest release
2. Place `BestSupplies.jar` in your server's `plugins` folder
3. Install **Vault** if not already installed
4. Install an economy plugin (EssentialsX, CMI, etc.)
5. Start/restart your server
6. Configure files in `plugins/BestSupplies/`
7. Use `/supplies admin reload` to apply changes

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/BestSupplies.git

# Navigate to project directory
cd BestSupplies

# Build with Maven
mvn clean package

# Output jar will be in target/BestSupplies-1.0.0.jar
```

## Requirements

- Java 21 or higher
- Paper 1.21.10 or compatible fork
- Vault economy plugin

## License

This project is proprietary software. All rights reserved.

## Support

For support, please open an issue on GitHub or contact the development team.

---

**Made with ♥ by Nullith Studios**
