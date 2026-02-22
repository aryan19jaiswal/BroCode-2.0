import api from './api';
import type { AuthRequest, RegisterRequest, ApiResponse, LoginResponse, ProfileResponse, UserProfile } from '@/types';

export const authService = {
  async login(data: AuthRequest): Promise<LoginResponse> {
    const res = await api.post<LoginResponse>('/user/login', data);
    return res.data;
  },

  async register(data: RegisterRequest): Promise<ApiResponse> {
    const res = await api.post<ApiResponse>('/user/register', data);
    return res.data;
  },

  async logout(): Promise<ApiResponse> {
    const res = await api.get<ApiResponse>('/user/logout');
    return res.data;
  },

  async getProfile(): Promise<ProfileResponse> {
    const res = await api.get<ProfileResponse>('/user/profile');
    return res.data;
  },

  async updateProfile(data: UserProfile): Promise<ApiResponse> {
    const res = await api.patch<ApiResponse>('/user/profile', data);
    return res.data;
  },
};
