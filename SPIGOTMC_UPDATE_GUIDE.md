# SpigotMC Update Guide - Dynamic Jobs & Economy Pro v1.0.5

## 📋 **Step-by-Step SpigotMC Update Process**

### **Step 1: Prepare Your Update**

1. **Update Version Number**
   - Ensure `pom.xml` version is `1.0.5`
   - Build new JAR: `mvn clean package -DskipTests`
   - Your new JAR will be in `target/`:
     - `DynamicJobsEconomy-1.0.5.jar` (shaded, deploy this one)

2. **Prepare Update Materials**
   - New JAR file (`DynamicJobsEconomy-1.0.5.jar`)
   - Changelog (see below for SpigotMC format)
   - Any new screenshots (optional)

### **Step 2: Login to SpigotMC**

1. Go to [SpigotMC.org](https://www.spigotmc.org)
2. Login to your account
3. Navigate to your resource: "Dynamic Jobs & Economy Pro"

### **Step 3: Upload New Version**

1. **Click "Update Resource"** (on your resource page)
2. **Upload New File**:
   - Click "Choose File" 
   - Select `DynamicJobsEconomy-1.0.5.jar`
   - Set version name: `1.0.5`
3. **Add Update Title**: `v1.0.5 - Polish & Performance Reporting Fix`

### **Step 4: Add Changelog (SpigotMC Format)**

Copy and paste this changelog into the update description:

---

## 🎉 Dynamic Jobs & Economy Pro v1.0.5 — Polish & Performance Reporting Fix

### ✨ Improvements
- Minor polish across business command help and GUI documentation.

### 🐛 Bug Fixes
- Fixed a critical bug in `ConsolidatedBusinessManager.getBusinessPerformanceReport(...)` where a missing return could lead to incorrect or empty results.

### 📚 Docs
- Updated README/INSTALLATION/QUICK_START to reference 1.0.5 artifacts and clarified admin vs player command visibility.

---

Compatibility: 1.20.4 – 1.21.x  |  Integrations: Vault, WorldGuard, McMMO, LuckPerms  |  Databases: SQLite, MySQL, MongoDB

---

**🆙 Update Instructions**:
1. Stop your server
2. Replace the old JAR with `DynamicJobsEconomy-1.0.5.jar`
3. Start your server
4. Run `/djeconomy reload` (optional)

**💬 Need Help?** Join our Discord or create a discussion thread!

---

### **Step 5: Additional Update Options**

**Optional Enhancements**:
- **Add Screenshots**: If you have new UI screenshots, upload them
- **Update Description**: Update main resource description if needed
- **Add Tags**: Ensure tags include "economy", "jobs", "business", "security"
- **Set Category**: Make sure it's in "Misc" or "Economy" category

### **Step 6: Publish Update**

1. **Review Everything**: Double-check changelog and file
2. **Click "Update Resource"**: This publishes your update
3. **Notify Users**: SpigotMC will automatically notify followers

### **Step 7: Post-Update Actions**

**Immediate Actions**:
- Monitor discussion thread for user feedback
- Check download stats and user reviews
- Respond to any questions or issues

**Marketing Actions**:
- Post update announcement in relevant Discord servers
- Share on social media if applicable
- Consider creating a brief video showcasing new features

---

## 📝 **SpigotMC Update Best Practices**

### **Changelog Format Tips**:
- Use emojis for visual appeal (✅ 🎉 🛡️ 🚀)
- Group changes by category (Security, Features, Bug Fixes)
- Highlight major improvements first
- Include compatibility information
- Mention breaking changes clearly
- Add update instructions

### **Version Numbering**:
- **Major.Minor.Patch** (e.g., 1.0.1)
- **Patch** (0.0.X): Bug fixes, small improvements
- **Minor** (0.X.0): New features, enhancements
- **Major** (X.0.0): Breaking changes, major rewrites

### **File Naming**:
- Always include version in filename
- The produced JAR is shaded by default
- Example: `DynamicJobsEconomy-1.0.5.jar`

---

## 🎯 **Success Metrics to Track**

After your update, monitor:
- **Download Count**: How many people download the update
- **User Reviews**: New ratings and feedback
- **Discussion Activity**: Questions and feature requests
- **Bug Reports**: Any issues with the new version

---

**🎉 Congratulations on your first major update!** This security and polish update will significantly improve user trust and plugin reliability.
