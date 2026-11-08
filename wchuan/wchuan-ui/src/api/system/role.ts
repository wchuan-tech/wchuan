import request from '@/utils/request'
import type { Result } from '@/api/types'
import type { Dept } from '@/api/system/dept'

export type DataScope = '1' | '2' | '3' | '4' | '5'

export interface Role {
  id: number
  roleName: string
  roleKey: string
  roleSort: number
  dataScope: DataScope
  status: '0' | '1'
  remark?: string
  createTime?: string
}

export interface RoleQuery {
  roleName?: string
  roleKey?: string
  status?: '' | '0' | '1'
}

export interface RoleForm {
  id?: number
  roleName: string
  roleKey: string
  roleSort: number
  dataScope: DataScope
  status: '0' | '1'
  remark?: string
  menuIds: number[]
  deptIds: number[]
}

export interface MenuTreeNode {
  id: number
  label: string
  menuName: string
  children?: MenuTreeNode[]
}

export interface RoleMenuTree {
  checkedKeys: number[]
  halfCheckedKeys: number[]
  menus: MenuTreeNode[]
}

export interface RoleDeptTree {
  checkedKeys: number[]
  depts: Dept[]
}

export interface DataScopePayload {
  id: number
  dataScope: DataScope
  deptIds: number[]
}

export const listRole = (params?: RoleQuery) =>
  request.get<unknown, Result<Role[]>>('/system/role/list', { params })

export const getRole = (roleId: number) =>
  request.get<unknown, Result<Role>>(`/system/role/${roleId}`)

export const addRole = (data: RoleForm) =>
  request.post<RoleForm, Result<void>>('/system/role', data)

export const updateRole = (data: RoleForm) =>
  request.put<RoleForm, Result<void>>('/system/role', data)

export const delRole = (roleId: number) =>
  request.delete<unknown, Result<void>>(`/system/role/${roleId}`)

export const getRoleDeptTree = (roleId: number) =>
  request.get<unknown, Result<RoleDeptTree>>(`/system/dept/roleDeptTreeselect/${roleId}`)

export const getDeptTree = () =>
  request.get<unknown, Result<Dept[]>>('/system/dept/treeselect')

export const updateRoleDataScope = (data: DataScopePayload) =>
  request.put<DataScopePayload, Result<void>>('/system/role/dataScope', data)

export const getMenuTree = () =>
  request.get<unknown, Result<MenuTreeNode[]>>('/system/menu/treeselect')

export const getRoleMenuTree = (roleId: number) =>
  request.get<unknown, Result<RoleMenuTree>>(`/system/menu/roleMenuTreeselect/${roleId}`)
