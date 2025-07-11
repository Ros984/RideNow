import axios from 'axios';
import {
LoginDto,
SignupDto,
LoginResponseDto,
UserDto,
RideRequestDto,
PageRideDto,
RideStartDto,
RatingDto,
RideBookingDto,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_REACT_APP_API_URL || 'http://localhost:8080/api';

// Axios instance
const api = axios.create({
baseURL: API_BASE_URL,
headers: {
'Content-Type': 'application/json',
},
});

// 🔐 Attach access token automatically
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 🔁 Handle 401 + try refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
          const { data } = await axios.post<LoginResponseDto>(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          });

          localStorage.setItem('accessToken', data.accessToken);
          return api(originalRequest);
        }
      } catch (refreshError) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

// 🔐 Auth API
export const authAPI = {
  login: (data: LoginDto) => api.post<LoginResponseDto>('/auth/login', data),
  signup: (data: SignupDto) => api.post('/auth/signup', data),
  getRoles: () => api.get<string[]>('/auth/roles'),
  onBoardDriver: (userId: string, vehicleId: string) =>
    api.post(`/auth/onBoardNewDriver/${userId}`, { vehicleId }),
  refresh: (refreshToken: string) => api.post<LoginResponseDto>('/auth/refresh', { refreshToken }),
  getUserByEmail: (email: string) => api.get<UserDto>(`/auth/user?email=${email}`), // newly added
};

// 🧍 Rider API
export const riderAPI = {
  requestRide: (data: RideBookingDto) => api.post<RideRequestDto>('/riders/requestRide', data),
  getMyRides: (page = 0, size = 10) =>
    api.get<PageRideDto>(`/riders/getMyRides?page=${page}&size=${size}`),
  getMyProfile: () => api.get<UserDto>('/riders/getMyProfile'),
  rateDriver: (rideId: string, rating: RatingDto) =>
    api.post(`/riders/rateDriver`, { rideId, ...rating }),
  cancelRide: (rideId: string) => api.post(`/riders/cancelRide/${rideId}`),
};

// 🚗 Driver API
export const driverAPI = {
  acceptRide: (rideRequestId: string) => api.post(`/drivers/acceptRide/${rideRequestId}`),
  startRide: (rideRequestId: string, data: RideStartDto) =>
    api.post(`/drivers/startRide/${rideRequestId}`, data),
  endRide: (rideId: string) => api.post(`/drivers/endRide/${rideId}`),
  cancelRide: (rideId: string) => api.post(`/drivers/cancelRide/${rideId}`),
  rateRider: (rideId: string, rating: RatingDto) =>
    api.post(`/drivers/rateRider`, { rideId, ...rating }),
  getMyRides: (page = 0, size = 10) =>
    api.get<PageRideDto>(`/drivers/getMyRides?page=${page}&size=${size}`),
  getMyProfile: () => api.get<UserDto>('/drivers/getMyProfile'),
  getAvailableRides: () => api.get<RideRequestDto[]>('/drivers/availableRides'),
};

export default api;
