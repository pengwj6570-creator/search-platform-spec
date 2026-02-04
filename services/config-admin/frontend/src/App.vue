<template>
  <el-container style="height: 100vh">
    <el-header style="background-color: #409EFF; color: white; display: flex; align-items: center;">
      <span style="font-size: 20px; font-weight: bold;">🔍 企业搜索中台 - 配置管理</span>
    </el-header>
    <el-container>
      <el-aside width="200px" style="background-color: #f5f7fa">
        <el-menu
          :default-active="activeMenu"
          @select="handleMenuSelect"
          style="height: 100%"
        >
          <el-menu-item index="sources">
            <el-icon><Connection /></el-icon>
            <span>数据源管理</span>
          </el-menu-item>
          <el-menu-item index="objects">
            <el-icon><Document /></el-icon>
            <span>搜索对象</span>
          </el-menu-item>
          <el-menu-item index="index">
            <el-icon><Search /></el-icon>
            <span>索引管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <source-manager v-if="activeMenu === 'sources'" />
        <object-manager v-if="activeMenu === 'objects'" />
        <index-manager v-if="activeMenu === 'index'" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import SourceManager from './components/SourceManager.vue'
import ObjectManager from './components/ObjectManager.vue'
import IndexManager from './components/IndexManager.vue'

const activeMenu = ref('sources')

const handleMenuSelect = (key) => {
  activeMenu.value = key
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
}
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
