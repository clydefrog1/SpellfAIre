export interface UserResponse {
  id: string;
  email: string;
  username: string;
  avatarBase64?: string | null;
  rating?: number;
}

export interface AuthResponse {
  accessToken: string;
  user: UserResponse;
}

export interface RefreshResponse {
  accessToken: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateProfileRequest {
  username?: string;
  currentPassword?: string;
  newPassword?: string;
  avatarBase64?: string | null;
}
