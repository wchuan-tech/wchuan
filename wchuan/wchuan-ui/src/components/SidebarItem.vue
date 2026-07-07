<!-- src/components/SidebarItem.vue -->
<template>
  <!-- 1. 权限与显隐过滤：
       - visible === '0' (显示状态)
       - menuType !== 'F' (侧边栏不显示按钮)
       - hasMenuPerm (拥有权限或子项有权限)
  -->
  <template v-if="item.visible === '0' && item.menuType !== 'F' && hasMenuPerm(item)">

    <!-- 情况 A：目录 (M) -> 渲染为 el-sub-menu -->
    <el-sub-menu v-if="item.menuType === 'M'" :index="item.path">
      <template #title>
        <el-icon v-if="item.icon && item.icon !== '#'"><component :is="getIcon(item.icon)" /></el-icon>
        <span>{{ item.menuName }}</span>
      </template>
      <!-- 递归调用自身，渲染子菜单 -->
      <SidebarItem
          v-for="child in item.children"
          :key="child.id"
          :item="child"
      />
    </el-sub-menu>

    <!-- 情况 B：菜单 (C) -> 渲染为 el-menu-item -->
    <el-menu-item v-else :index="item.path">
      <el-icon v-if="item.icon && item.icon !== '#'"><component :is="getIcon(item.icon)" /></el-icon>
      <template #title>
        <span>{{ item.menuName }}</span>
      </template>
    </el-menu-item>

  </template>
</template>

<script setup lang="ts">
import * as Icons from '@element-plus/icons-vue'

defineProps<{ item: any }>()

// 获取当前用户的权限
const userPermissions = JSON.parse(localStorage.getItem('permissions') || '[]')
const userId = localStorage.getItem('userId')

/**
 * 权限判断：管理员或拥有该标识/子标识的用户可见
 */
const hasMenuPerm = (menu: any) => {
  if (userId === '1' || userPermissions.includes('*')) return true
  if (!menu.perms) return true // 没配权限的默认开放

  // 逻辑：用户拥有该权限，或者拥有该权限下级的任何权限（如拥有 system:user:add 就能看 system 目录）
  return userPermissions.some((p: string) =>
      p === menu.perms || p.startsWith(menu.perms + ':')
  )
}

const getIcon = (name: string) => {
  return (Icons as any)[name] || (Icons as any).Menu
}
</script>