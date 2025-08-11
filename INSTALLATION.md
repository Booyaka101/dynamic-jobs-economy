# 🐶 BooPug Studios - Dynamic Jobs & Economy Pro
## Easy Installation Guide

### 📋 **Requirements**
- Minecraft Server (Spigot/Paper) 1.20.4+
- Java 17 or higher
- **Optional but Recommended**: Vault plugin for economy integration

### 🚀 **Quick Install (3 Steps)**

#### **Step 1: Download & Place**
1. Download `DynamicJobsEconomy-1.0.0.jar` from releases
2. Place it in your server's `plugins/` folder
3. **That's it!** No additional files needed - everything is included!

#### **Step 2: Start Server**
1. Start your Minecraft server
2. The plugin will automatically:
   - Create default configuration files
   - Set up the database (SQLite by default)
   - Load all job types and settings
   - Display a welcome message in console

#### **Step 3: Basic Setup (Optional)**
1. Edit `plugins/DynamicJobsEconomy/config.yml` if desired
2. Run `/djeconomy reload` to apply changes
3. **Ready to use!**

### 🎮 **First Steps for Players**

#### **For Players:**
```
/jobs                    - See available jobs
/jobs join miner        - Join the miner job
/gigs list              - Browse available gigs
/business create MyShop restaurant - Start a business
```

#### **For Admins:**
```
/djeconomy reload       - Reload configuration
/djeconomy setjobxp <player> <job> <xp> - Manage player progress
```

### ⚙️ **Configuration**

The plugin works perfectly with **default settings**, but you can customize:

#### **Database Options:**
- **SQLite** (Default) - No setup required, works immediately
- **MySQL** - For larger servers, edit database section in config
- **MongoDB** - For advanced setups

#### **Economy Integration:**
- **Vault** (Recommended) - Automatically detected and used
- **Internal Economy** - Fallback system, works without Vault

### 🔧 **Advanced Setup**

#### **MySQL Setup (Optional):**
```yaml
database:
  type: mysql
  host: localhost
  port: 3306
  database: minecraft_economy
  username: your_username
  password: your_password
```

#### **Job Customization:**
Edit the jobs section in config.yml to modify:
- XP requirements per level
- Income rates
- Job perks and bonuses

### 🆘 **Troubleshooting**

#### **Common Issues:**

**Plugin won't load:**
- Check server version (needs 1.20.4+)
- Verify Java version (needs 17+)
- Check console for error messages

**Economy not working:**
- Install Vault plugin for best experience
- Plugin has built-in economy as fallback

**Database errors:**
- Default SQLite works out-of-box
- For MySQL, verify connection details
- Check file permissions in plugins folder

**Commands not working:**
- Verify player has permissions
- Check if plugin loaded successfully
- Use `/plugins` to confirm plugin is enabled

### 📞 **Support**

- **Documentation**: Check the README.md file
- **Commands**: Use `/jobs help`, `/gigs help`, `/business help`
- **Issues**: Contact BooPug Studios support

### 🎉 **You're Ready!**

Your BooPug Studios Dynamic Jobs & Economy Pro plugin is now installed and ready to transform your server's economy!

**Pro Tip**: Start with the default settings - they're designed to work great out-of-the-box. You can always customize later!
