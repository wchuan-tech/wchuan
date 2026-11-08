import request from '@/utils/request'
import type { Result } from '@/api/types'

export type CustomerStage = '线索' | '初访' | '意向' | '报价' | '签约' | '输单'
export type FollowupType = '电话' | '拜访' | '微信' | '系统'

export interface Customer {
  id: number
  customerName: string
  phone?: string
  wechat?: string
  companyName?: string
  poolType: 0 | 1
  ownerUserId?: number
  ownerUserName?: string
  lastFollowupTime?: string
  lastFollowupRecord?: string
  lockStatus: 0 | 1
  customerStage: CustomerStage
  createTime?: string
}

export interface FollowupRecord {
  id: number
  customerId: number
  followupType: FollowupType
  customerStage?: CustomerStage
  content: string
  nextFollowupTime?: string
  createBy?: string
  createTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface CustomerQuery {
  pageNum: number
  pageSize: number
  customerName?: string
  phone?: string
  customerStage?: CustomerStage | ''
}

export interface CustomerAssignPayload {
  customerId: number
  targetUserId: number
}

export interface CustomerCreatePayload {
  customerName: string
  phone?: string
  wechat?: string
  companyName?: string
  customerStage: CustomerStage
}

export interface FollowupCreatePayload {
  customerId: number
  followupType: Exclude<FollowupType, '系统'>
  customerStage?: CustomerStage
  content: string
  nextFollowupTime?: string
}

export interface SalesOption {
  id: number
  userName: string
  nickName?: string
  deptId?: number
}

export const getPublicPoolList = (params: CustomerQuery) =>
  request.get<unknown, Result<PageResult<Customer>>>('/crm/customer/public-pool/list', { params })

export const getPrivatePoolList = (params: CustomerQuery) =>
  request.get<unknown, Result<PageResult<Customer>>>('/crm/customer/private-pool/list', { params })

export const claimCustomer = (id: number) =>
  request.post<unknown, Result<Customer>>(`/crm/customer/claim/${id}`)

export const assignCustomer = (data: CustomerAssignPayload) =>
  request.post<CustomerAssignPayload, Result<Customer>>('/crm/customer/assign', data)

export const returnCustomerToPool = (id: number) =>
  request.post<unknown, Result<Customer>>(`/crm/customer/return/${id}`)

export const transferCustomer = (data: CustomerAssignPayload) =>
  request.post<CustomerAssignPayload, Result<Customer>>('/crm/customer/transfer', data)

export const addFollowup = (data: FollowupCreatePayload) =>
  request.post<FollowupCreatePayload, Result<FollowupRecord>>('/crm/customer/followup', data)

export const getFollowupList = (customerId: number) =>
  request.get<unknown, Result<FollowupRecord[]>>(`/crm/customer/followup/list/${customerId}`)

export const createCustomer = (data: CustomerCreatePayload) =>
  request.post<CustomerCreatePayload, Result<Customer>>('/crm/customer', data)

export const getSalesOptions = () =>
  request.get<unknown, Result<SalesOption[]>>('/crm/customer/sales/options')
