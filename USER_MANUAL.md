# 📖 User Manual - BooPug Studios Dynamic Jobs & Economy Pro

## 🎯 **Complete Player Guide**

### **Getting Started (New Players)**

#### **Your First 5 Minutes:**
1. **Join the server** - You automatically get $1,000 starting money
2. **Choose a job**: `/jobs` → `/jobs join miner`
3. **Start working**: Mine blocks to earn XP and money
4. **Check progress**: `/jobs stats` to see your advancement
5. **Level up**: Earn perks and higher income as you progress

#### **Understanding the Economy:**
- **Jobs**: Permanent careers with progression (Miner, Chef, Farmer, Builder, Merchant)
- **Gigs**: Temporary freelance work posted by other players
- **Businesses**: Own companies, hire employees, build an empire
- **Money**: Earned through jobs, gigs, and business profits

### **🏆 Jobs System - Detailed Guide**

#### **Available Jobs:**
| Job | How to Earn | Starting Pay | Max Level |
|-----|-------------|--------------|-----------|
| **Miner** | Mine any blocks | $1 per block | 100 |
| **Chef** | Cook food items | $2 per item | 100 |
| **Farmer** | Harvest crops | $1.5 per harvest | 100 |
| **Builder** | Place blocks | $0.5 per block | 100 |
| **Merchant** | Trade with villagers | $3 per trade | 100 |

#### **Job Commands:**
```
/jobs                    # Main menu with all options
/jobs list               # See all available jobs
/jobs join <job>         # Join a specific job
/jobs leave <job>        # Leave a job (keeps XP)
/jobs stats              # Your current job levels and XP
/jobs info <job>         # Detailed info about a job
/jobs top <job>          # Leaderboard for a job
```

#### **Job Progression:**
- **XP Gain**: Automatic when performing job activities
- **Levels**: 1-100, each level increases income and unlocks perks
- **Multiple Jobs**: You can have multiple jobs simultaneously
- **Perks**: Higher levels give speed boosts, luck bonuses, and more income

### **💼 Gig Economy - Freelance Work**

#### **For Workers (Accepting Gigs):**
```
/gigs list               # Browse available work
/gigs accept <id>        # Accept a gig
/gigs complete <id>      # Mark work as complete (get paid)
/gigs mine               # See your active gigs
```

#### **For Employers (Posting Work):**
```
/gigs create <title> <payment> <description>
# Example: /gigs create "Build a house" 1000 "Need a 10x10 house with basement"
```

#### **Gig System Features:**
- **Posting Fee**: $50 to post a gig (prevents spam)
- **Commission**: 5% taken from payment (server fee)
- **Status Tracking**: OPEN → IN_PROGRESS → COMPLETED
- **Payment Protection**: Money held in escrow until completion

### **🏢 Business Management - Build Your Empire**

#### **Starting a Business:**
```
/business create "My Company" restaurant
# Cost: $1,000 (one-time fee)
# Types: restaurant, shop, farm, construction, service
```

#### **Managing Your Business:**
```
/business list                              # Your businesses
/business info <id>                         # Business details
/business hire <player> <business_id>       # Hire employees
/business fire <player> <business_id>       # Fire employees
/business deposit <business_id> <amount>    # Add funds
/business withdraw <business_id> <amount>   # Take profits
```

#### **Business Features:**
- **Separate Finances**: Business money separate from personal
- **Employee System**: Hire other players to work for you
- **Growth Potential**: Expand with multiple businesses
- **Profit Sharing**: Set up employee payment systems

### **💰 Economy Tips & Strategies**

#### **Making Money Fast:**
1. **Start with Mining**: Easiest job, consistent income
2. **Level Up**: Higher levels = more money per action
3. **Accept Gigs**: Quick cash for specific tasks
4. **Start Small Business**: Long-term wealth building
5. **Multiple Jobs**: Diversify your income sources

#### **Advanced Strategies:**
- **Business Empire**: Own multiple businesses in different sectors
- **Employee Network**: Hire skilled players for your businesses
- **Gig Specialization**: Become known for specific types of work
- **Market Timing**: Post gigs when demand is high

### **🎮 User Interface Guide**

#### **Chat Colors Explained:**
- **§6Yellow**: Important information, titles
- **§aGreen**: Success messages, money earned
- **§cRed**: Errors, insufficient funds
- **§7Gray**: Help text, descriptions
- **§fWhite**: Standard text, names

#### **Command Shortcuts:**
- `/j` → `/jobs` (if enabled by admin)
- `/g` → `/gigs` (if enabled by admin)
- `/b` → `/business` (if enabled by admin)

### **🔧 Customization Options**

#### **Personal Settings:**
- **Job Focus**: Choose 1-2 jobs to focus on for faster progression
- **Gig Preferences**: Specialize in certain types of work
- **Business Strategy**: Decide between multiple small or one large business

#### **Server Integration:**
- **Vault Compatible**: Works with any economy plugin
- **Permission Support**: Integrates with LuckPerms, GroupManager, etc.
- **WorldGuard**: Respects protected regions
- **McMMO**: Bonus XP when McMMO skills level up

### **🛡️ Admin Commands**

Admins with `djeconomy.admin` can manage jobs, economy, and cache:

```
/djeconomy reload
  Reloads configuration and refreshes managers.

/djeconomy setlevel <player> <job> <level>
  Sets a player's job level. Works for online and offline players.
  - Job name is case-insensitive.
  - Level is clamped to the job's valid range.

/djeconomy addxp <player> <job> <amount>
  Adds XP to a player's job (online only).
  - Job name is case-insensitive.
  - Player must have joined the job.

/djeconomy refreshjobs <player>
  Immediately reloads a player's job data from the database (online only).

/djeconomy invalidatejobs <player>
  Invalidates the player's cached job data so it reloads on next access (online only).
```

Notes:
- Refresh/Invalidate are restricted to online players to keep cache handling consistent.
- Setting level for online players updates cache + DB; for offline players it upserts directly to DB.

#### Tab Completion
- Admin tab completion is case-insensitive.
- Player name suggestions appear when typing the second argument for `setlevel`, `addxp`, `refreshjobs`, and `invalidatejobs`.
- For `economy`, suggestions:
  - Second argument: `give`, `take`, `set`.
  - Third argument: online player names.
- For `setlevel` and `addxp`, the third argument suggests job names (case-insensitive).

### **🆘 Troubleshooting**

#### **Common Issues:**

**"Not earning money from job activities"**
- Check you've joined the job: `/jobs stats`
- Verify you're in an allowed world
- Some blocks may not count (check with admin)

**"Can't create business"**
- Need $1,000 in your account
- Business name might be taken
- Check you have permission

**"Gig commands not working"**
- Verify gig ID exists: `/gigs list`
- Check you have permission to use gigs
- Make sure you're the right person (poster vs worker)

**"Economy not working"**
- Server may be using different economy plugin
- Check with admin about Vault setup
- Plugin has internal economy as backup

#### **Getting Help:**
1. **In-Game**: `/jobs help`, `/gigs help`, `/business help`
2. **Ask Staff**: Use `/helpop` or contact admins
3. **Check Config**: Admins can verify plugin settings

### **📊 Statistics & Leaderboards**

#### **Track Your Progress:**
- **Job Levels**: See your advancement in each career
- **Total Earnings**: Track lifetime income from all sources
- **Business Success**: Monitor company profits and growth
- **Gig Completion**: Build reputation through completed work

#### **Server Rankings:**
- **Top Earners**: Richest players on the server
- **Job Masters**: Highest level in each job category
- **Business Tycoons**: Most successful business owners
- **Gig Champions**: Most reliable freelance workers

### **🎉 Advanced Features**

#### **For Experienced Players:**
- **Multi-Business Empires**: Own businesses in every sector
- **Employee Management**: Build teams and delegate work
- **Market Manipulation**: Use gigs to control server economy
- **Mentorship**: Help new players learn the system

#### **Integration with Other Plugins:**
- **Shops**: Use economy to buy/sell in player shops
- **Auctions**: Bid on items with your earned money
- **Towny**: Fund town projects with business profits
- **Factions**: Economic warfare and trade agreements

---

## 🐶 **BooPug Studios - Thank You!**

This economy system is designed to be **fun, fair, and rewarding**. Whether you're a casual player earning pocket money or an ambitious entrepreneur building a business empire, there's something for everyone.

**Remember**: The best strategy is to have fun and help others succeed too!

*For technical support or suggestions, contact your server administrators.*
