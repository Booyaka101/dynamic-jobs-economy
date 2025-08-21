# 🚀 Quick Start Guide - BooPug Studios Dynamic Jobs & Economy Pro

## 🎯 **Get Started in 5 Minutes!**

### **For Server Admins:**

#### **1. Install (30 seconds)**
```bash
# Just drop the JAR file in your plugins folder - that's it!
# No dependencies required, everything is included
# Use the JAR from target (e.g. DynamicJobsEconomy-1.0.5.jar)
```

#### **2. First Launch**
When you start your server, you'll see:
```
[INFO] [DynamicJobsEconomy] Starting Dynamic Jobs & Economy Pro...
[INFO] [DynamicJobsEconomy] BooPug Studios - Welcome to your new economy!
[INFO] [DynamicJobsEconomy] Database initialized successfully
[INFO] [DynamicJobsEconomy] All systems ready - economy is live!
```

#### **3. Test Commands**
```
/djeconomy status        # Check plugin status
/jobs list              # See all available jobs
/gigs list              # View gig marketplace
/business gui           # Open the business GUI
```

### 🛠️ Admin Setup (Vault, DB, Permissions)

- Vault (optional, recommended):
  - Install Vault and a compatible economy plugin to use the server-wide economy.
  - In `plugins/DynamicJobsEconomy/config.yml`, set `integrations.vault.enabled: true` and `integrations.vault.use_vault_economy: true`.
  - Without Vault, the plugin uses its internal economy.

- Database:
  - Default `sqlite` works out-of-the-box.
  - MySQL example:
    ```yaml
    database:
      type: mysql
      mysql:
        host: localhost
        port: 3306
        database: dynamicjobs
        username: your_user
        password: your_pass
        useSSL: false
    ```
  - MongoDB example:
    ```yaml
    database:
      type: mongodb
      mongodb:
        connection_string: "mongodb://localhost:27017/dynamicjobs"
    ```

- Permissions:
  - Admin: `djeconomy.admin`
  - Granular: `djeconomy.system.reload`, `djeconomy.admin.economy`, `djeconomy.admin.level.get|set|reset|addxp`, `djeconomy.admin.history.view`, `djeconomy.admin.jobs.refresh|invalidate`
  - Player GUI: `djeconomy.gui.access`

- Command safety (large amounts):
  - In `config.yml`:
    ```yaml
    economy:
      admin_confirmation:
        threshold: 100000.0
        expiry_seconds: 30
    ```
  - Use `/djeconomy confirm` to finalize large admin money actions.

### **For Players:**

#### **🏆 Your First Job (2 minutes)**
```
1. /jobs                 # See what jobs are available
2. /jobs join miner      # Join the miner job
3. Go mine some blocks   # Start earning XP and money!
4. /jobs stats           # Check your progress
```

#### **💼 Start a Business (3 minutes)**
```
1. /business create "My Shop" restaurant    # Create your business
2. /business info 1                         # Check business details
3. /business hire PlayerName 1              # Hire an employee
4. /business deposit 1 1000                 # Add startup capital
```

#### **🎯 Try the Gig Economy (2 minutes)**
```
1. /gigs list                              # Browse available work
2. /gigs create "Build a house" 500 "Need a nice house built"  # Post work
3. /gigs accept 1                          # Accept someone's gig
4. /gigs complete 1                        # Complete and get paid
```

## 📊 **Default Economy Settings**

Your plugin comes pre-configured with balanced settings:

### **Jobs Available:**
- **Miner** - Earn from mining blocks
- **Chef** - Cook food for profit
- **Farmer** - Grow crops and raise animals
- **Builder** - Construction and crafting
- **Merchant** - Trading and commerce

### **Starting Balances:**
- New players: $1,000
- Job XP: Starts at 0, max level 100
- Business creation cost: $1,000
- Gig posting fee: $50

### **Automatic Features:**
- ✅ Database auto-setup (SQLite)
- ✅ Economy integration (Vault or internal)
- ✅ Job XP from gameplay
- ✅ Level-up rewards
- ✅ Business profit tracking

## 🎮 **Essential Commands Reference**

### **Jobs System:**
```
/jobs                    # Main jobs menu
/jobs list               # Available jobs
/jobs join <job>         # Join a job
/jobs leave <job>        # Leave a job
/jobs stats              # Your job progress
/jobs info <job>         # Job details
```

### **Gig Economy:**
```
/gigs list               # Browse gigs
/gigs create <title> <payment> <description>  # Post work
/gigs accept <id>        # Accept a gig
/gigs complete <id>      # Complete work
/gigs mine               # Your gigs
```

### **Business Management:**
```
/business create <name> <type>              # Start business
/business list                              # Your businesses
/business info <id>                         # Business details
/business hire <player> <business_id>       # Hire employee
/business fire <player> <business_id>       # Fire employee
/business deposit <business_id> <amount>    # Add funds
/business withdraw <business_id> <amount>   # Take funds
/business gui                               # Open the business GUI (perm: djeconomy.gui.access)
/business menu                              # Alias for GUI (perm: djeconomy.gui.access)
```

### **Admin Commands:**
```
/djeconomy reload                           # Reload config
/djeconomy status                          # Plugin status
/djeconomy setlevel <player> <job> <level> # Set player job level (online/offline)
/djeconomy addxp <player> <job> <amount>   # Add XP to a player's job (online only)
```

## 🔧 **Customization Made Easy**

### **Want to change job rewards?**
Edit `plugins/DynamicJobsEconomy/config.yml`:
```yaml
jobs:
  miner:
    income_per_block: 2.0    # Change from default 1.0
    xp_per_block: 5          # Change from default 2
```

### **Want different starting money?**
```yaml
economy:
  starting_balance: 2000     # Change from default 1000
```

### **Want to use MySQL instead?**
```yaml
database:
  type: mysql
  host: localhost
  database: minecraft_economy
  username: your_user
  password: your_pass
```

## 🎉 **You're All Set!**

**Congratulations!** Your server now has a complete economy system with:
- ✅ Jobs with progression
- ✅ Freelance gig marketplace  
- ✅ Business empire building
- ✅ Dynamic economy management

**Players can start earning immediately** - no additional setup required!

---
*Made with ❤️ by BooPug Studios*
