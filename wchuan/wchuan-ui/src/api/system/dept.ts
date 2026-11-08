import request from '@/utils/request'
import type { Result } from '@/api/types'

export interface Dept {
  id: number
  deptId: number
  parentId: number
  ancestors?: string
  deptName: string
  orderNum: number
  leader?: string
  phone?: string
  email?: string
  status: '0' | '1'
  delFlag?: string
  createTime?: string
  children?: Dept[]
}

export interface DeptQuery {
  deptName?: string
  status?: '' | '0' | '1'
}

export interface DeptForm {
  id?: number
  deptId?: number
  parentId: number
  deptName: string
  orderNum: number
  leader?: string
  phone?: string
  email?: string
  status: '0' | '1'
}

export const listDept = (params?: DeptQuery) =>
  request.get<unknown, Result<Dept[]>>('/system/dept/list', { params })

export const getDept = (deptId: number) =>
  request.get<unknown, Result<Dept>>(`/system/dept/${deptId}`)

export const listDeptExcludeChild = (deptId: number) =>
  request.get<unknown, Result<Dept[]>>(`/system/dept/list/exclude/${deptId}`)

export const addDept = (data: DeptForm) =>
  request.post<DeptForm, Result<void>>('/system/dept', data)

export const updateDept = (data: DeptForm) =>
  request.put<DeptForm, Result<void>>('/system/dept', {
    ...data,
    deptId: data.deptId ?? data.id
  })

export const delDept = (deptId: number) =>
  request.delete<unknown, Result<void>>(`/system/dept/${deptId}`)
