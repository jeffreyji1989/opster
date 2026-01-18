<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <el-menu
        :default-active="$route.path"
        class="el-menu-vertical"
        background-color="#304156"
        text-color="#fff"
        active-text-color="#409EFF"
        router
      >
        <div class="logo">Opster System</div>
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>首页 Dashboard</span>
        </el-menu-item>
        <el-menu-item index="/project">
          <el-icon><Folder /></el-icon>
          <span>项目管理</span>
        </el-menu-item>
        <el-menu-item index="/server">
          <el-icon><Monitor /></el-icon>
          <span>服务器管理</span>
        </el-menu-item>
        <el-menu-item index="/service">
          <el-icon><Cpu /></el-icon>
          <span>服务管理</span>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><View /></el-icon>
          <span>项目监控</span>
        </el-menu-item>
        <el-menu-item index="/deployment-record">
          <el-icon><Document /></el-icon>
          <span>发版记录</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-content">
          <span>Opster 运维管理系统</span>
          <span class="user-info">Admin</span>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}
.el-menu-vertical {
  height: 100%;
  border-right: none;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-weight: bold;
  font-size: 20px;
  background-color: #2b3649;
}
.el-header {
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  padding: 0 20px;
}
.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.user-info {
  font-weight: bold;
}
.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
