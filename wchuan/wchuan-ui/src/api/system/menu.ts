import request from '@/utils/request'
import type { Result, SysMenu } from '@/api/types'

export const listMenu = () =>
  request.get<unknown, Result<SysMenu[]>>('/system/menu/list')

export const getMenu = (id: number) =>
  request.get<unknown, Result<SysMenu>>(`/system/menu/${id}`)

export const addMenu = (data: SysMenu) =>
  request.post<SysMenu, Result<void>>('/system/menu', data)

export const updateMenu = (data: SysMenu) =>
  request.put<SysMenu, Result<void>>('/system/menu', data)

export const delMenu = (id: number) =>
  request.delete<unknown, Result<void>>(`/system/menu/${id}`)

export const repairMenu = () =>
  request.post<unknown, Result<void>>('/system/menu/repair')
